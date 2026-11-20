/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.datavalue;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pins the org unit filters of {@link DataValueStore#getDeflatedDataValues}: which org units' data
 * is returned for each combination of org units, org unit level, {@link
 * DeflatedDataValueParams#isOrganisationUnitSubtrees()} and {@link
 * DeflatedDataValueParams#isIncludeDescendants()}. The two flags are independent: subtrees decides
 * whether the org units are roots or exact matches, includeDescendants decides whether the level
 * filter is {@code =} or {@code >=}.
 *
 * @see <a href="https://dhis2.atlassian.net/browse/DHIS2-21966">DHIS2-21966</a>
 */
@Transactional
class DeflatedDataValueOrgUnitFilterTest extends PostgresIntegrationTestBase {
  @Autowired private DataValueStore dataValueStore;
  @Autowired private DataDumpService dataDumpService;
  @Autowired private DataElementService dataElementService;
  @Autowired private CategoryService categoryService;
  @Autowired private OrganisationUnitService organisationUnitService;

  private DataElement dataElement;

  private Period period;

  /** Org units by their one-letter code. */
  private final Map<Character, OrganisationUnit> orgUnits = new HashMap<>();

  /**
   * Hierarchy, with one data value on every org unit:
   *
   * <pre>
   * Level 1: R
   * Level 2: A, B        (children of R)
   * Level 3: C, D, E     (C and D children of A, E child of B)
   * </pre>
   */
  @BeforeEach
  void setUp() {
    dataElement = createDataElement('X');
    dataElementService.addDataElement(dataElement);
    period = createPeriod("202301");
    CategoryOptionCombo coc = categoryService.getDefaultCategoryOptionCombo();

    addOrgUnit('R', null);
    addOrgUnit('A', 'R');
    addOrgUnit('B', 'R');
    addOrgUnit('C', 'A');
    addOrgUnit('D', 'A');
    addOrgUnit('E', 'B');

    DataValue[] values =
        orgUnits.values().stream()
            .map(ou -> new DataValue(dataElement, period, ou, coc, coc, "1"))
            .toArray(DataValue[]::new);
    assertEquals(values.length, dataDumpService.upsertValuesForJdbcTest(values));
  }

  static Stream<Arguments> orgUnitFilters() {
    // roots, level, subtrees, includeDescendants, expected org units with data
    return Stream.of(
        // no org unit filters at all
        Arguments.of("", null, false, false, "ABCDER"),
        // no level: subtrees decides between exact org units and their subtrees
        Arguments.of("A", null, false, false, "A"),
        Arguments.of("A", null, true, false, "ACD"),
        Arguments.of("AE", null, true, false, "ACDE"),
        // no org units: only the level filter
        Arguments.of("", 3, false, false, "CDE"),
        Arguments.of("", 2, false, true, "ABCDE"),
        // root above the level, exact match: the root is not at the level, so nothing
        Arguments.of("R", 2, false, false, ""),
        Arguments.of("R", 2, false, true, ""),
        // root above the level, subtree: predictor with SELECTED (DHIS2-21966) ...
        Arguments.of("R", 2, true, false, "AB"),
        // ... and with DESCENDANTS
        Arguments.of("R", 2, true, true, "ABCDE"),
        Arguments.of("A", 3, true, false, "CD"),
        Arguments.of("B", 2, true, true, "BE"),
        // root at the level, exact match
        Arguments.of("A", 2, false, false, "A"));
  }

  @ParameterizedTest(name = "roots={0} level={1} subtrees={2} includeDescendants={3} -> {4}")
  @MethodSource("orgUnitFilters")
  void testOrgUnitFilter(
      String roots, Integer level, boolean subtrees, boolean includeDescendants, String expected) {
    DeflatedDataValueParams params =
        new DeflatedDataValueParams()
            .setDataElements(Set.of(dataElement))
            // by dates, not by period: the period id cache outlives the rolled back test data
            .setStartDate(period.getStartDate())
            .setEndDate(period.getEndDate())
            .setOrganisationUnits(
                roots.chars().mapToObj(c -> orgUnits.get((char) c)).collect(Collectors.toSet()))
            .setOrgUnitLevel(level)
            .setOrganisationUnitSubtrees(subtrees)
            .setIncludeDescendants(includeDescendants);

    assertEquals(expected, codesOf(dataValueStore.getDeflatedDataValues(params)));
  }

  private void addOrgUnit(char code, Character parentCode) {
    OrganisationUnit ou =
        parentCode == null
            ? createOrganisationUnit(code)
            : createOrganisationUnit(code, orgUnits.get(parentCode));
    organisationUnitService.addOrganisationUnit(ou);
    orgUnits.put(code, ou);
  }

  /** The codes of the org units the data values are stored on, sorted. */
  private String codesOf(Iterable<DeflatedDataValue> dataValues) {
    Map<Long, Character> codeById = new HashMap<>();
    orgUnits.forEach((code, ou) -> codeById.put(ou.getId(), code));

    Set<Character> codes = new TreeSet<>();
    dataValues.forEach(dv -> codes.add(codeById.get(dv.getSourceId())));
    return codes.stream().map(String::valueOf).collect(joining());
  }
}
