/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.trackedentity;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeTableManager;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ameen
 */
class TrackedEntityAttributeStoreIntegrationTest extends PostgresIntegrationTestBase {
  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private TrackedEntityAttributeTableManager trackedEntityAttributeTableManager;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private ProgramService programService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityAttributeValueService entityAttributeValueService;

  @Autowired private OrganisationUnitService organisationUnitService;

  private static final int A = 65;

  private static final int T = 85;

  private Program programB;

  private TrackedEntityAttribute attributeW;

  private TrackedEntityAttribute attributeY;

  private TrackedEntityAttribute attributeZ;

  @BeforeEach
  void setUp() {
    attributeW = createTrackedEntityAttribute('W');
    attributeW.setUnique(true);
    attributeY = createTrackedEntityAttribute('Y');
    attributeY.setUnique(true);
    attributeZ = createTrackedEntityAttribute('Z', ValueType.NUMBER);

    Program program = createProgram('A');
    programService.addProgram(program);

    TrackedEntityType trackedEntityTypeA = createTrackedEntityType('A');
    trackedEntityTypeA.setPublicAccess(AccessStringHelper.FULL);
    trackedEntityTypeService.addTrackedEntityType(trackedEntityTypeA);

    TrackedEntityType trackedEntityTypeB = createTrackedEntityType('B');
    trackedEntityTypeB.setPublicAccess(AccessStringHelper.FULL);
    trackedEntityTypeService.addTrackedEntityType(trackedEntityTypeB);

    // Create 20 Tracked Entity Attributes (named A .. O)
    IntStream.range(A, T)
        .mapToObj(i -> Character.toString((char) i))
        .forEach(
            c ->
                attributeService.addTrackedEntityAttribute(
                    createTrackedEntityAttribute(c.charAt(0), ValueType.TEXT)));

    // Transform the Tracked Entity Attributes into a List of
    // TrackedEntityTypeAttribute
    List<TrackedEntityTypeAttribute> teatList =
        IntStream.range(A, T)
            .mapToObj(i -> Character.toString((char) i))
            .map(
                s ->
                    new TrackedEntityTypeAttribute(
                        trackedEntityTypeA,
                        attributeService.getTrackedEntityAttributeByName("Attribute" + s)))
            .collect(Collectors.toList());

    // Setting searchable to true for 5 random tracked entity type
    // attributes
    TrackedEntityTypeAttribute teta = teatList.get(0);
    teta.setSearchable(true);
    teta = teatList.get(4);
    teta.setSearchable(true);
    teta = teatList.get(9);
    teta.setSearchable(true);
    teta = teatList.get(14);
    teta.setSearchable(true);
    teta = teatList.get(19);
    teta.setSearchable(true);

    // Assign 10 TrackedEntityTypeAttribute to Tracked Entity Type A
    trackedEntityTypeA.getTrackedEntityTypeAttributes().addAll(teatList.subList(0, 10));
    trackedEntityTypeService.updateTrackedEntityType(trackedEntityTypeA);

    // Assign 10 TrackedEntityTypeAttribute to Tracked Entity Type B
    trackedEntityTypeB.getTrackedEntityTypeAttributes().addAll(teatList.subList(10, 20));
    trackedEntityTypeService.updateTrackedEntityType(trackedEntityTypeB);

    programB = createProgram('B');
    programService.addProgram(programB);

    List<ProgramTrackedEntityAttribute> pteaList =
        IntStream.range(A, T)
            .mapToObj(i -> Character.toString((char) i))
            .map(
                s ->
                    new ProgramTrackedEntityAttribute(
                        programB,
                        attributeService.getTrackedEntityAttributeByName("Attribute" + s)))
            .collect(Collectors.toList());

    // Setting searchable to true for 5 random program tracked entity
    // attributes
    ProgramTrackedEntityAttribute ptea = pteaList.get(0);
    ptea.setSearchable(true);
    ptea = pteaList.get(4);
    ptea.setSearchable(true);
    ptea = pteaList.get(9);
    ptea.setSearchable(true);
    ptea = pteaList.get(13);
    ptea.setSearchable(true);
    ptea = pteaList.get(18);
    ptea.setSearchable(true);

    programB.getProgramAttributes().addAll(pteaList);
    programService.updateProgram(programB);
  }

  @AfterEach
  void dropTrigramIndexes() {
    List<String> indexNames =
        jdbcTemplate.queryForList(
            "SELECT indexname FROM pg_indexes WHERE indexname LIKE 'in_gin_teavalue_%'",
            String.class);

    indexNames.forEach(name -> jdbcTemplate.execute("DROP INDEX IF EXISTS " + name));
  }

  @Test
  void testGetAllIndexableAttributes() {
    attributeW.setTrigramIndexable(true);
    attributeService.addTrackedEntityAttribute(attributeW);
    attributeY.setTrigramIndexable(true);
    attributeService.addTrackedEntityAttribute(attributeY);
    attributeZ.setTrigramIndexable(true);
    attributeService.addTrackedEntityAttribute(attributeZ);

    Set<TrackedEntityAttribute> indexableAttributes =
        attributeService.getAllTrigramIndexableAttributes();

    assertContainsOnly(Set.of(attributeW, attributeY, attributeZ), indexableAttributes);
    assertTrue(indexableAttributes.contains(attributeW));
    assertTrue(indexableAttributes.contains(attributeY));
  }

  @Test
  void testCreateTrigramIndex() {
    attributeService.addTrackedEntityAttribute(attributeW);
    trackedEntityAttributeTableManager.createTrigramIndex(attributeW);
    assertFalse(
        jdbcTemplate
            .queryForList(
                "select * "
                    + "from pg_indexes "
                    + "where tablename= 'trackedentityattributevalue' and indexname like 'in_gin_teavalue_%'; ")
            .isEmpty());
  }

  @Test
  void shouldReturnAllTrigramIndexedAttributes() {
    attributeService.addTrackedEntityAttribute(attributeY);
    trackedEntityAttributeTableManager.createTrigramIndex(attributeY);
    attributeService.addTrackedEntityAttribute(attributeZ);
    createTrigramIndexWithCustomNaming(attributeZ, "trigram_index_name");

    List<Long> attributeIds = trackedEntityAttributeTableManager.getAttributesWithTrigramIndex();

    assertContainsOnly(List.of(attributeY.getId(), attributeZ.getId()), attributeIds);
  }

  private void createTrigramIndexWithCustomNaming(
      TrackedEntityAttribute attribute, String indexName) {
    attributeService.addTrackedEntityAttribute(attributeY);

    String query =
        String.format(
            """
                CREATE INDEX CONCURRENTLY IF NOT EXISTS %s ON trackedentityattributevalue
                USING gin (trackedentityid,lower(value) gin_trgm_ops) where trackedentityattributeid = %d
            """,
            indexName, attribute.getId());

    jdbcTemplate.execute(query);
  }
}
