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
package org.hisp.dhis.dxf2.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.csv.CsvImportClass;
import org.hisp.dhis.dxf2.csv.CsvImportOptions;
import org.hisp.dhis.dxf2.csv.CsvImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.option.OptionGroup;
import org.hisp.dhis.option.OptionGroupSet;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class CsvMetadataImportTest extends PostgresIntegrationTestBase {
  @Autowired private DataElementService dataElementService;

  @Autowired private OptionService optionService;

  @Autowired private CsvImportService csvImportService;

  @Autowired private MetadataImportService importService;

  @Autowired private SchemaService schemaService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private OrganisationUnitService organisationUnitService;

  // TODO(DHIS2-17768 platform) this import is run as a super user, create a different user if you
  // need to
  @Test
  void testOrgUnitImport() throws Exception {
    ImportReport importReport =
        runImport(
            "metadata/organisationUnits.csv",
            CsvImportClass.ORGANISATION_UNIT,
            metadata -> assertEquals(6, metadata.getOrganisationUnits().size()));
    assertEquals(6, importReport.getStats().created());
    assertEquals(6, organisationUnitService.getAllOrganisationUnits().size());
  }

  @Test
  void testOrgUnitImport_SuperUser() throws Exception {
    ImportReport importReport =
        runImport(
            "metadata/organisationUnits.csv",
            CsvImportClass.ORGANISATION_UNIT,
            metadata -> assertEquals(6, metadata.getOrganisationUnits().size()));
    assertEquals(6, importReport.getStats().created());
    assertEquals(6, organisationUnitService.getAllOrganisationUnits().size());
  }

  @Test
  void testDataElementImport() throws Exception {
    ImportReport importReport =
        runImport(
            "metadata/dataElements.csv",
            CsvImportClass.DATA_ELEMENT,
            metadata -> assertEquals(2, metadata.getDataElements().size()));
    assertEquals(2, importReport.getStats().created());
    assertEquals(2, dataElementService.getAllDataElements().size());
  }

  @Test
  void testOptionSetImport() throws Exception {
    ImportReport importReport =
        runImport(
            "metadata/optionSets.csv",
            CsvImportClass.OPTION_SET,
            metadata -> {
              assertEquals(4, metadata.getOptionSets().size());
              assertEquals(3, metadata.getOptionSets().get(0).getOptions().size());
              assertEquals(3, metadata.getOptionSets().get(1).getOptions().size());
              assertEquals(3, metadata.getOptionSets().get(2).getOptions().size());
              assertEquals(3, metadata.getOptionSets().get(3).getOptions().size());
            });
    assertEquals(16, importReport.getStats().created());
    List<OptionSet> optionSets = new ArrayList<>(optionService.getAllOptionSets());
    assertEquals(4, optionSets.size());
    assertEquals(3, optionSets.get(0).getOptions().size());
    assertEquals(3, optionSets.get(1).getOptions().size());
    assertEquals(3, optionSets.get(2).getOptions().size());
    assertEquals(3, optionSets.get(3).getOptions().size());
  }

  @Test
  void testOptionSetReplace() throws IOException {
    // Import 1 OptionSet with 3 Options
    ImportReport importReport = runImport("metadata/optionSet_add.csv", CsvImportClass.OPTION_SET);
    assertEquals(4, importReport.getStats().created());
    // Send payload with 2 new Options
    importReport = runImport("metadata/optionSet_update.csv", CsvImportClass.OPTION_SET);
    assertEquals(2, importReport.getStats().created());
    OptionSet optionSet = optionService.getOptionSetByCode("COLOR");
    // 3 old Options are replaced by 2 new added Options
    assertEquals(2, optionSet.getOptions().size());
  }

  @Test
  void testImportOptionGroupSet() throws IOException {
    ImportReport importReport = runImport("metadata/option_set.csv", CsvImportClass.OPTION_SET);
    assertEquals(5, importReport.getStats().created());
    OptionSet optionSetA = manager.get(OptionSet.class, "xmRubJIhmaK");
    assertNotNull(optionSetA);
    assertEquals(2, optionSetA.getOptions().size());
    OptionSet optionSetB = manager.get(OptionSet.class, "QYDAByFgTr1");
    assertNotNull(optionSetB);
    assertEquals(1, optionSetB.getOptions().size());
    importReport = runImport("metadata/option_groups.csv", CsvImportClass.OPTION_GROUP);
    assertEquals(2, importReport.getStats().created());
    OptionGroup optionGroupA = manager.get(OptionGroup.class, "UeHtizvXbx6");
    assertNotNull(optionGroupA);
    assertEquals(2, optionGroupA.getMembers().size());
    OptionGroup optionGroupB = manager.get(OptionGroup.class, "EVYjFX6Ez0a");
    assertNotNull(optionGroupB);
    assertEquals(1, optionGroupB.getMembers().size());
    importReport = runImport("metadata/option_group_set.csv", CsvImportClass.OPTION_GROUP_SET);
    assertEquals(2, importReport.getStats().created());
    OptionGroupSet optionGroupSetA = manager.get(OptionGroupSet.class, "FB9i0Jl2R80");
    assertNotNull(optionGroupSetA);
    OptionGroupSet optionGroupSetB = manager.get(OptionGroupSet.class, "K30djctzUtN");
    assertNotNull(optionGroupSetB);
    importReport =
        runImport(
            "metadata/option_group_set_members.csv", CsvImportClass.OPTION_GROUP_SET_MEMBERSHIP);
    assertEquals(2, importReport.getStats().updated());
    OptionGroupSet ogsA = optionService.getOptionGroupSet("FB9i0Jl2R80");
    OptionGroupSet ogsB = optionService.getOptionGroupSet("K30djctzUtN");
    assertEquals(1, ogsA.getMembers().size());
    assertEquals(1, ogsB.getMembers().size());
    assertEquals(2, ogsA.getMembers().get(0).getMembers().size());
  }

  @Test
  void testImportOrganisationUnitGroupMembers() throws IOException {
    ImportReport importReport =
        runImport(
            "metadata/organisationUnitMembers.csv",
            CsvImportClass.ORGANISATION_UNIT,
            metadata -> assertEquals(4, metadata.getOrganisationUnits().size()));
    assertEquals(4, importReport.getStats().created());
    assertEquals(4, organisationUnitService.getAllOrganisationUnits().size());

    importReport =
        runImport(
            "metadata/organisationUnitGroup.csv",
            CsvImportClass.ORGANISATION_UNIT_GROUP,
            metadata -> assertEquals(2, metadata.getOrganisationUnitGroups().size()));
    assertEquals(2, importReport.getStats().created());

    importReport =
        runImport(
            "metadata/organisationUnitGroup_members.csv",
            CsvImportClass.ORGANISATION_UNIT_GROUP_MEMBERSHIP,
            metadata -> assertEquals(2, metadata.getOrganisationUnitGroups().size()));
    assertEquals(2, importReport.getStats().updated());
    OrganisationUnitGroup orgUnitA = manager.get(OrganisationUnitGroup.class, "a1234567890");
    assertEquals(2, orgUnitA.getMembers().size());
    OrganisationUnitGroup orgUnitB = manager.get(OrganisationUnitGroup.class, "b1234567890");
    assertEquals(2, orgUnitB.getMembers().size());
  }

  private ImportReport runImport(String csvFile, CsvImportClass importClass) throws IOException {
    return runImport(csvFile, importClass, null);
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
    if (modifier != null) {
      modifier.accept(params);
    }
    return importService.importMetadata(
        params, new MetadataObjects().addMetadata(schemaService.getMetadataSchemas(), metadata));
  }
}
