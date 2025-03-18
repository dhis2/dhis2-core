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
package org.hisp.dhis.organisationunit;

import static org.hisp.dhis.organisationunit.FeatureType.POINT;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luciano Fiandesio
 */
@Transactional
class OrganisationUnitStoreIntegrationTest extends PostgresIntegrationTestBase {

  private static final long _150KM = 150_000;

  private static final long _190KM = 190_000;

  private static final long _250KM = 250_000;

  @Autowired private OrganisationUnitStore organisationUnitStore;

  @Autowired private IdentifiableObjectManager manager;

  @Test
  void testGetOrganisationUnitsByProgramNoResults() {
    // When
    List<OrganisationUnit> ous = organisationUnitStore.getOrganisationUnitsByProgram("programUid");

    // Then
    assertTrue(ous.isEmpty());
  }

  @Test
  void testGetOrganisationUnitsByProgram() {
    // Given
    Program program = createProgram('A');
    program.setUid("FQ2o8UBlcrS");
    manager.save(program);

    program = manager.load(Program.class, program.getUid());
    OrganisationUnit ouA = createOrganisationUnit('A');
    organisationUnitStore.save(ouA);

    program.addOrganisationUnit(ouA);
    manager.update(program);

    // When
    List<OrganisationUnit> ous =
        organisationUnitStore.getOrganisationUnitsByProgram(program.getUid());

    // Then
    assertEquals(1, ous.size());
  }

  @Test
  void testGetOrganisationUnitsByDataSet() {
    // Given
    DataSet dataSet = createDataSet('A', PeriodType.getByIndex(1));
    dataSet.setUid("FQ2o8UBlcrS");
    manager.save(dataSet);

    dataSet = manager.load(DataSet.class, dataSet.getUid());
    OrganisationUnit ouA = createOrganisationUnit('A');
    organisationUnitStore.save(ouA);

    dataSet.addOrganisationUnit(ouA);
    manager.update(dataSet);

    // When
    List<OrganisationUnit> ous =
        organisationUnitStore.getOrganisationUnitsByDataSet(dataSet.getUid());

    // Then
    assertEquals(1, ous.size());
  }

  @Test
  void testGetOrganisationUnitsByDataSetNoResults() {
    // When
    List<OrganisationUnit> ous = organisationUnitStore.getOrganisationUnitsByDataSet("programUid");

    // Then
    assertTrue(ous.isEmpty());
  }

  @Test
  void verifyGetOrgUnitsWithinAGeoBox() throws IOException {
    // https://gist.github.com/luciano-fiandesio/ea682cd4b9a37c5b4bef93e3918b8cda
    OrganisationUnit ouA =
        createOrganisationUnit(
            'A',
            GeoUtils.getGeometryFromCoordinatesAndType(POINT, "[27.421875, 22.49225722008518]"));
    OrganisationUnit ouB =
        createOrganisationUnit(
            'B',
            GeoUtils.getGeometryFromCoordinatesAndType(
                POINT, "[29.860839843749996, 20.035289711352377]"));
    OrganisationUnit ouC =
        createOrganisationUnit(
            'C',
            GeoUtils.getGeometryFromCoordinatesAndType(
                POINT, "[26.103515625, 20.879342971957897]"));
    OrganisationUnit ouD =
        createOrganisationUnit(
            'D',
            GeoUtils.getGeometryFromCoordinatesAndType(
                POINT, "[26.982421875, 19.476950206488414]"));
    Geometry point =
        GeoUtils.getGeometryFromCoordinatesAndType(POINT, "[27.83935546875, 21.207458730482642]");
    manager.save(ouA);
    manager.save(ouB);
    manager.save(ouC);
    manager.save(ouD);
    List<OrganisationUnit> ous = getOUsFromPointToDistance(point, _150KM);
    assertContainsOnly(List.of(ouA), ous);
    ous = getOUsFromPointToDistance(point, _190KM);
    assertContainsOnly(List.of(ouA, ouC), ous);
    ous = getOUsFromPointToDistance(point, _250KM);
    assertContainsOnly(List.of(ouA, ouB, ouC, ouD), ous);
  }

  @Test
  @DisplayName("Getting OrgUnits by CategoryOption returns the correct result set")
  void getOrgUnitsByCategoryOptionTest() {
    // given 2 org units have refs to category options
    CategoryOption co1 = createCategoryOption('1');
    CategoryOption co2 = createCategoryOption('2');
    CategoryOption co3 = createCategoryOption('3');
    manager.save(List.of(co1, co2, co3));

    OrganisationUnit ou1 = createOrganisationUnit('x');
    ou1.addCategoryOption(co1);
    ou1.addCategoryOption(co2);

    OrganisationUnit ou2 = createOrganisationUnit('y');
    ou2.addCategoryOption(co3);

    // no cat option refs
    OrganisationUnit ou3 = createOrganisationUnit('z');

    manager.save(List.of(ou1, ou2, ou3));

    // when
    List<OrganisationUnit> organisationUnits =
        organisationUnitStore.getByCategoryOption(
            List.of(co1.getUid(), co2.getUid(), co3.getUid()));

    // then
    assertEquals(2, organisationUnits.size());
    assertTrue(
        organisationUnits.stream()
            .flatMap(ou -> ou.getCategoryOptions().stream())
            .toList()
            .containsAll(List.of(co1, co2, co3)));
    assertFalse(organisationUnits.contains(ou3));
  }

  private List<OrganisationUnit> getOUsFromPointToDistance(Geometry point, long distance) {
    double[] box = GeoUtils.getBoxShape(point.getCoordinate().x, point.getCoordinate().y, distance);
    return organisationUnitStore.getWithinCoordinateArea(box);
  }
}
