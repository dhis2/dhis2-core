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
package org.hisp.dhis.dxf2.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.function.Consumer;
import org.hisp.dhis.dxf2.csv.CsvImportClass;
import org.hisp.dhis.dxf2.csv.CsvImportOptions;
import org.hisp.dhis.dxf2.csv.CsvImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * Tests special validation for {@link org.hisp.dhis.organisationunit.OrganisationUnit} move during
 * metadata import.
 *
 * @author Jan Bernitt
 */
class CsvMetadataImportIntegrationTest extends TransactionalIntegrationTest {

  @Autowired private CsvImportService csvImportService;

  @Autowired private SchemaService schemaService;

  @Autowired private MetadataImportService importService;

  @Autowired private UserService _userService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @BeforeEach
  void setUp() throws IOException {
    userService = _userService;
    ImportReport report =
        runImport(
            "dxf2/metadata/organisationUnits.csv",
            CsvImportClass.ORGANISATION_UNIT,
            metadata -> assertEquals(6, metadata.getOrganisationUnits().size()));
    assertEquals(6, report.getStats().getCreated());
  }

  @Test
  void testOrgUnitImport_MoveLacksMoveAuthority() throws Exception {
    createAndInjectAdminUser(new String[0]);
    ImportReport report =
        runImport(
            "dxf2/metadata/organisationUnits_move.csv",
            CsvImportClass.ORGANISATION_UNIT,
            null,
            params -> params.setImportStrategy(ImportStrategy.UPDATE));
    assertEquals(Status.ERROR, report.getStatus());
    assertTrue(report.hasErrorReport(error -> error.getErrorCode() == ErrorCode.E1520));
  }

  @Test
  void testOrgUnitImport_MoveLacksWriteAuthority() throws Exception {
    createAndInjectAdminUser("F_ORGANISATIONUNIT_MOVE");
    ImportReport report =
        runImport(
            "dxf2/metadata/organisationUnits_move.csv",
            CsvImportClass.ORGANISATION_UNIT,
            null,
            params -> params.setImportStrategy(ImportStrategy.UPDATE));
    assertEquals(Status.ERROR, report.getStatus());
    assertTrue(report.hasErrorReport(error -> error.getErrorCode() == ErrorCode.E1521));
  }

  @Test
  void testOrgUnitImport_MoveFromParentNotInHierarchy() throws Exception {
    User user = createAndInjectAdminUser("F_ORGANISATIONUNIT_MOVE", "F_ORGANISATIONUNIT_ADD");
    HashSet<OrganisationUnit> orgUnits = new HashSet<>();
    orgUnits.add(organisationUnitService.getOrganisationUnitByCode("L2b"));
    user.setOrganisationUnits(orgUnits);
    userService.updateUser(user);
    ImportReport report =
        runImport(
            "dxf2/metadata/organisationUnits_move.csv",
            CsvImportClass.ORGANISATION_UNIT,
            null,
            params -> params.setImportStrategy(ImportStrategy.UPDATE));
    assertEquals(Status.ERROR, report.getStatus());
    assertEquals(1, report.getErrorReportsCount());
    assertTrue(report.hasErrorReport(error -> error.getErrorCode() == ErrorCode.E1522));
  }

  @Test
  void testOrgUnitImport_MoveToParentNotInHierarchy() throws Exception {
    User user = createAndInjectAdminUser("F_ORGANISATIONUNIT_MOVE", "F_ORGANISATIONUNIT_ADD");
    HashSet<OrganisationUnit> orgUnits = new HashSet<>();
    orgUnits.add(organisationUnitService.getOrganisationUnitByCode("L2a"));
    user.setOrganisationUnits(orgUnits);
    userService.updateUser(user);
    ImportReport report =
        runImport(
            "dxf2/metadata/organisationUnits_move.csv",
            CsvImportClass.ORGANISATION_UNIT,
            null,
            params -> params.setImportStrategy(ImportStrategy.UPDATE));
    assertEquals(Status.ERROR, report.getStatus());
    assertEquals(1, report.getErrorReportsCount());
    assertTrue(report.hasErrorReport(error -> error.getErrorCode() == ErrorCode.E1523));
  }

  @Test
  void testOrgUnitImport_Success() throws Exception {
    User user = createAndInjectAdminUser("F_ORGANISATIONUNIT_MOVE", "F_ORGANISATIONUNIT_ADD");
    HashSet<OrganisationUnit> orgUnits = new HashSet<>();
    orgUnits.add(organisationUnitService.getOrganisationUnitByCode("L1"));
    user.setOrganisationUnits(orgUnits);
    userService.updateUser(user);
    ImportReport importReport =
        runImport(
            "dxf2/metadata/organisationUnits_move.csv",
            CsvImportClass.ORGANISATION_UNIT,
            null,
            params -> params.setImportStrategy(ImportStrategy.UPDATE));
    assertEquals(Status.OK, importReport.getStatus());
    assertEquals(1, importReport.getStats().getUpdated());
  }

  private ImportReport runImport(
      String csvFile, CsvImportClass importClass, Consumer<Metadata> preCondition)
      throws IOException {
    return runImport(csvFile, importClass, preCondition, null);
  }

  private ImportReport runImport(
      String csvFile,
      CsvImportClass importClass,
      Consumer<Metadata> preCondition,
      Consumer<MetadataImportParams> modifier)
      throws IOException {
    InputStream input = new ClassPathResource(csvFile).getInputStream();
    Metadata metadata =
        csvImportService.fromCsv(
            input, new CsvImportOptions().setImportClass(importClass).setFirstRowIsHeader(true));
    if (preCondition != null) {
      preCondition.accept(metadata);
    }
    MetadataImportParams params = new MetadataImportParams();
    params.addMetadata(schemaService.getMetadataSchemas(), metadata);
    if (modifier != null) {
      modifier.accept(params);
    }
    return importService.importMetadata(params);
  }
}
