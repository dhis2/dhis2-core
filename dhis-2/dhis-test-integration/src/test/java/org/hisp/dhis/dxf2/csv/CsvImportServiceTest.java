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
package org.hisp.dhis.dxf2.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.MetadataObjects;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class CsvImportServiceTest extends PostgresIntegrationTestBase {

  @Autowired private CsvImportService csvImportService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private OrganisationUnitGroupService organisationUnitGroupService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private MetadataImportService importService;

  @Autowired private SchemaService schemaService;

  private InputStream inputBasicObjects;

  private OrganisationUnit organisationUnit_A;

  private OrganisationUnit organisationUnit_B;

  private OrganisationUnit organisationUnit_C;

  private OrganisationUnit organisationUnit_D;

  private OrganisationUnitGroup organisationUnitGroup_A;

  private OrganisationUnitGroup organisationUnitGroup_B;

  @BeforeAll
  void setUp() throws Exception {
    inputBasicObjects = new ClassPathResource("csv/basic_objects.csv").getInputStream();
    organisationUnitGroup_A = createOrganisationUnitGroup('A');
    organisationUnitGroup_B = createOrganisationUnitGroup('B');
    organisationUnitGroupService.addOrganisationUnitGroup(organisationUnitGroup_A);
    organisationUnitGroupService.addOrganisationUnitGroup(organisationUnitGroup_B);
    organisationUnit_A = createOrganisationUnit('A');
    organisationUnit_B = createOrganisationUnit('B');
    organisationUnit_C = createOrganisationUnit('C');
    organisationUnit_D = createOrganisationUnit('D');
    organisationUnitService.addOrganisationUnit(organisationUnit_A);
    organisationUnitService.addOrganisationUnit(organisationUnit_B);
    organisationUnitService.addOrganisationUnit(organisationUnit_C);
    organisationUnitService.addOrganisationUnit(organisationUnit_D);
  }

  @Test
  void testCategoryOptionImport() throws IOException {
    Metadata metadata =
        csvImportService.fromCsv(
            inputBasicObjects,
            new CsvImportOptions()
                .setImportClass(CsvImportClass.CATEGORY_OPTION)
                .setFirstRowIsHeader(true));
    assertEquals(3, metadata.getCategoryOptions().size());
    for (CategoryOption categoryOption : metadata.getCategoryOptions()) {
      assertNotNull(categoryOption.getUid());
      assertNotNull(categoryOption.getName());
      assertNotNull(categoryOption.getShortName());
    }
  }

  @Test
  void testCategoryOptionImportNoHeader() throws IOException {
    Metadata metadata =
        csvImportService.fromCsv(
            new ClassPathResource("csv/basic_objects_no_header.csv").getInputStream(),
            new CsvImportOptions()
                .setImportClass(CsvImportClass.CATEGORY_OPTION)
                .setFirstRowIsHeader(false));
    assertEquals(3, metadata.getCategoryOptions().size());
    for (CategoryOption categoryOption : metadata.getCategoryOptions()) {
      assertNotNull(categoryOption.getUid());
      assertNotNull(categoryOption.getName());
      assertNotNull(categoryOption.getShortName());
    }
  }

  @Test
  void testOrganisationUnitGroupMembershipImport() throws IOException {
    InputStream in = new ClassPathResource("csv/org_unit_group_memberships.csv").getInputStream();
    Metadata metadata =
        csvImportService.fromCsv(
            in,
            new CsvImportOptions()
                .setImportClass(CsvImportClass.ORGANISATION_UNIT_GROUP_MEMBERSHIP)
                .setFirstRowIsHeader(true));
    assertEquals(2, metadata.getOrganisationUnitGroups().size());
  }

  @Test
  void testImportCategories() throws IOException {
    InputStream in = new ClassPathResource("csv/categories.csv").getInputStream();
    Metadata metadata =
        csvImportService.fromCsv(
            in,
            new CsvImportOptions()
                .setImportClass(CsvImportClass.CATEGORY)
                .setFirstRowIsHeader(true));
    assertEquals(3, metadata.getCategories().size());
    Category gender = metadata.getCategories().get(0);
    Category ageGroup = metadata.getCategories().get(1);
    Category partner = metadata.getCategories().get(2);
    assertEquals("Gender", gender.getName());
    assertEquals(DataDimensionType.DISAGGREGATION, gender.getDataDimensionType());
    assertEquals("AGEGROUP", ageGroup.getCode());
    assertEquals(DataDimensionType.ATTRIBUTE, partner.getDataDimensionType());
  }

  @Test
  void testImportCategoryCombos() throws IOException {
    InputStream in = new ClassPathResource("csv/category_combos.csv").getInputStream();
    Metadata metadata =
        csvImportService.fromCsv(
            in,
            new CsvImportOptions()
                .setImportClass(CsvImportClass.CATEGORY_COMBO)
                .setFirstRowIsHeader(true));
    assertEquals(2, metadata.getCategoryCombos().size());
    CategoryCombo genderAge = metadata.getCategoryCombos().get(0);
    CategoryCombo partner = metadata.getCategoryCombos().get(1);
    assertEquals("Gender and Age", genderAge.getName());
    assertEquals(DataDimensionType.DISAGGREGATION, genderAge.getDataDimensionType());
    assertEquals(DataDimensionType.ATTRIBUTE, partner.getDataDimensionType());
  }

  @Test
  void testImportIndicator() throws IOException {
    IndicatorType indicatorType = createIndicatorType('A');
    indicatorType.setUid("sqGRzCziswD");
    manager.save(indicatorType);
    InputStream in = new ClassPathResource("csv/indicators.csv").getInputStream();
    Metadata metadata =
        csvImportService.fromCsv(
            in,
            new CsvImportOptions()
                .setImportClass(CsvImportClass.INDICATOR)
                .setFirstRowIsHeader(true));

    assertEquals(2, metadata.getIndicators().size());
    MetadataImportParams params = new MetadataImportParams();
    ImportReport report =
        importService.importMetadata(
            params,
            new MetadataObjects().addMetadata(schemaService.getMetadataSchemas(), metadata));
    assertEquals(Status.OK, report.getStatus());

    Indicator indicatorA = manager.get(Indicator.class, "yiAKjiZVoOU");
    assertNotNull(indicatorA);
    assertEquals("Indicator A", indicatorA.getName());
    assertEquals("CodeA", indicatorA.getCode());
    assertEquals("Indicator A description", indicatorA.getDescription());
    assertEquals(
        "#{fbfJHSPpUQD.pq2XI5kz2BY}+#{fbfJHSPpUQD.PT59n8BQbqM}", indicatorA.getDenominator());
    assertEquals(
        "#{fbfJHSPpUQD.pq2XI5kz2BY}+#{fbfJHSPpUQD.PT59n8BQbqM}-#{Jtf34kNZhzP.pq2XI5kz2BY}-#{Jtf34kNZhzP.PT59n8BQbqM}",
        indicatorA.getNumerator());

    assertEquals(indicatorType.getUid(), indicatorA.getIndicatorType().getUid());
    Indicator indicatorB = manager.get(Indicator.class, "Uvn6LCg7dVU");
    assertNotNull(indicatorB);
    assertEquals("Indicator B", indicatorB.getName());
    assertEquals("CodeB", indicatorB.getCode());
    assertEquals("Indicator B description", indicatorB.getDescription());
    assertEquals("#{fbfJHSPpUQD}", indicatorB.getDenominator());
    assertEquals("#{h0xKKjijTdI}", indicatorB.getNumerator());
    assertEquals(indicatorType.getUid(), indicatorB.getIndicatorType().getUid());
  }

  @Test
  void testImportCategoryOptionGroup() throws IOException {
    InputStream in = new ClassPathResource("csv/category_option_group.csv").getInputStream();
    Metadata metadata =
        csvImportService.fromCsv(
            in,
            new CsvImportOptions()
                .setImportClass(CsvImportClass.CATEGORY_OPTION_GROUP)
                .setFirstRowIsHeader(true));
    assertEquals(3, metadata.getCategoryOptionGroups().size());
    // make sure all groups have data dimension type set
    for (CategoryOptionGroup group : metadata.getCategoryOptionGroups()) {
      assertNotNull(group.getDataDimensionType());
    }

    ImportReport report =
        importService.importMetadata(
            new MetadataImportParams(),
            new MetadataObjects().addMetadata(schemaService.getMetadataSchemas(), metadata));

    assertEquals(Status.OK, report.getStatus());
    List<CategoryOptionGroup> allGroups = manager.getAll(CategoryOptionGroup.class);
    assertEquals(3, allGroups.size());
    CategoryOptionGroup groupA =
        allGroups.stream().filter(g -> g.getName().equals("GroupA")).findFirst().orElse(null);
    assertNotNull(groupA);
    assertEquals(DataDimensionType.DISAGGREGATION, groupA.getDataDimensionType());
    CategoryOptionGroup groupB =
        allGroups.stream().filter(g -> g.getName().equals("GroupB")).findFirst().orElse(null);
    assertNotNull(groupB);
    assertEquals(DataDimensionType.DISAGGREGATION, groupB.getDataDimensionType());
    CategoryOptionGroup groupC =
        allGroups.stream().filter(g -> g.getName().equals("GroupC")).findFirst().orElse(null);
    assertNotNull(groupC);
    assertEquals(DataDimensionType.ATTRIBUTE, groupC.getDataDimensionType());
  }
}
