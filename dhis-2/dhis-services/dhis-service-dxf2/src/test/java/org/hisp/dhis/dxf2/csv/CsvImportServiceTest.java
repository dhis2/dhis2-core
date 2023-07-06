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
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

class CsvImportServiceTest extends DhisSpringTest {

  @Autowired private CsvImportService csvImportService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private OrganisationUnitGroupService organisationUnitGroupService;

  private InputStream inputBasicObjects;

  private OrganisationUnit organisationUnit_A;

  private OrganisationUnit organisationUnit_B;

  private OrganisationUnit organisationUnit_C;

  private OrganisationUnit organisationUnit_D;

  private OrganisationUnitGroup organisationUnitGroup_A;

  private OrganisationUnitGroup organisationUnitGroup_B;

  @Override
  protected void setUpTest() throws Exception {
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
    inputBasicObjects = new ClassPathResource("csv/basic_objects_no_header.csv").getInputStream();
    Metadata metadata =
        csvImportService.fromCsv(
            inputBasicObjects,
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
}
