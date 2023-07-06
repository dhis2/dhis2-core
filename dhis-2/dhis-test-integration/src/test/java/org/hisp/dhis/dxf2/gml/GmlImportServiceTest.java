/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.dxf2.gml;

import static org.hisp.dhis.common.coordinate.CoordinateUtils.getCoordinatesAsList;
import static org.hisp.dhis.system.util.GeoUtils.getCoordinatesFromGeometry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.CoordinatesTuple;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Halvdan Hoem Grelland
 */
class GmlImportServiceTest extends TransactionalIntegrationTest {

  private InputStream inputStream;

  private User user;

  private OrganisationUnit boOrgUnit, bontheOrgUnit, ojdOrgUnit, bliOrgUnit, forskOrgUnit;

  private ImportOptions importOptions;

  private JobConfiguration id;

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------
  @Autowired private GmlImportService gmlImportService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private UserService _userService;

  @Override
  public void setUpTest() throws IOException {
    inputStream = new ClassPathResource("dxf2/gml/testGmlPayload.gml").getInputStream();
    /*
     * Create orgunits present in testGmlPayload.gml and set ID properties.
     * Name - FeatureType - ID property Bo - Poly - Name Bonthe - Multi -
     * Code Ole Johan Dahls Hus - Point - Uid Blindern - Point (pos) - Name
     * Forskningsparken - Poly (list) - Name
     *
     * Note: some of these are included to cover different coordinate
     * element schemes such as <posList>, <coordinates> and <pos>.
     */
    userService = _userService;
    boOrgUnit = createOrganisationUnit('A');
    boOrgUnit.setName("Bo");
    organisationUnitService.addOrganisationUnit(boOrgUnit);
    bontheOrgUnit = createOrganisationUnit('B');
    // Match on Code, therefore wrong
    bontheOrgUnit.setName("AA Bonthe");
    // name
    bontheOrgUnit.setCode("CODE_BONTHE");
    organisationUnitService.addOrganisationUnit(bontheOrgUnit);
    ojdOrgUnit = createOrganisationUnit('C');
    ojdOrgUnit.setUid("ImspTQPwCqd");
    // Match on UID,
    ojdOrgUnit.setName("AA Ole Johan Dahls Hus");
    // therefore wrong name
    organisationUnitService.addOrganisationUnit(ojdOrgUnit);
    bliOrgUnit = createOrganisationUnit('D');
    bliOrgUnit.setName("Blindern");
    organisationUnitService.addOrganisationUnit(bliOrgUnit);
    forskOrgUnit = createOrganisationUnit('E');
    forskOrgUnit.setName("Forskningsparken");
    organisationUnitService.addOrganisationUnit(forskOrgUnit);
    user = createAndInjectAdminUser();
    id = new JobConfiguration("gmlImportTest", JobType.METADATA_IMPORT, user.getUid(), true);
    importOptions = new ImportOptions().setImportStrategy(ImportStrategy.UPDATE);
    importOptions.setDryRun(false);
    importOptions.setPreheatCache(true);
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------
  @Test
  void testImportGml() {
    MetadataImportParams importParams = new MetadataImportParams();
    importParams.setId(id);
    importParams.setUser(user);
    gmlImportService.importGml(inputStream, importParams);
    assertNotNull(boOrgUnit.getGeometry());
    assertNotNull(bontheOrgUnit.getGeometry());
    assertNotNull(ojdOrgUnit.getGeometry());
    assertNotNull(bliOrgUnit.getGeometry());
    assertNotNull(forskOrgUnit.getGeometry());
    // Check if data is correct
    assertEquals(1, getCoordinates(boOrgUnit).size());
    assertEquals(18, getCoordinates(bontheOrgUnit).size());
    assertEquals(1, getCoordinates(ojdOrgUnit).size());
    assertEquals(1, getCoordinates(bliOrgUnit).size());
    assertEquals(1, getCoordinates(forskOrgUnit).size());
    assertEquals(76, getCoordinates(boOrgUnit).get(0).getNumberOfCoordinates());
    assertEquals(189, getCoordinates(bontheOrgUnit).get(1).getNumberOfCoordinates());
    assertEquals(1, getCoordinates(ojdOrgUnit).get(0).getNumberOfCoordinates());
    assertEquals(1, getCoordinates(bliOrgUnit).get(0).getNumberOfCoordinates());
    assertEquals(76, getCoordinates(forskOrgUnit).get(0).getNumberOfCoordinates());
  }

  private List<CoordinatesTuple> getCoordinates(OrganisationUnit orgUnit) {
    return getCoordinatesAsList(
        getCoordinatesFromGeometry(orgUnit.getGeometry()),
        FeatureType.getTypeFromName(orgUnit.getGeometry().getGeometryType()));
  }
}
