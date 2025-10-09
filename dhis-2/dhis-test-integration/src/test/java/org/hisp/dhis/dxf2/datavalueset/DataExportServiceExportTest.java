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
package org.hisp.dhis.dxf2.datavalueset;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataDumpService;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataExportPipeline;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class DataExportServiceExportTest extends PostgresIntegrationTestBase {
  @Autowired private CategoryService categoryService;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private DataSetService dataSetService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private DataElementService dataElementService;

  @Autowired private DataExportPipeline dataExportPipeline;

  @Autowired private DataDumpService dataDumpService;

  @Autowired private AttributeService attributeService;

  @Autowired private PeriodService periodService;

  @Autowired private ObjectMapper jsonMapper;

  private DataElement deA;

  private DataElement deB;

  private DataElement deC;

  private CategoryCombo ccA;

  private CategoryOptionCombo cocA;

  private CategoryOptionCombo cocB;

  private Attribute atA;

  private DataSet dsA;

  private DataSet dsB;

  private Period peA;

  private Period peB;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  private OrganisationUnitGroup ogA;

  private User user;

  private String peAIso;

  private String peBIso;

  @BeforeEach
  void setUp() {
    peA =
        createPeriod(
            PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY),
            getDate(2016, 3, 1),
            getDate(2016, 3, 31));
    peB =
        createPeriod(
            PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY),
            getDate(2016, 4, 1),
            getDate(2016, 4, 30));
    periodService.addPeriod(peA);
    periodService.addPeriod(peB);
    peAIso = peA.getUid();
    peBIso = peB.getUid();
    deA = createDataElement('A');
    deB = createDataElement('B');
    deC = createDataElement('C');
    dataElementService.addDataElement(deA);
    dataElementService.addDataElement(deB);
    dataElementService.addDataElement(deC);
    ccA = createCategoryCombo('A');
    categoryService.addCategoryCombo(ccA);
    cocA = createCategoryOptionCombo('A');
    cocB = createCategoryOptionCombo('B');
    cocA.setCategoryCombo(ccA);
    cocB.setCategoryCombo(ccA);
    categoryService.addCategoryOptionCombo(cocA);
    categoryService.addCategoryOptionCombo(cocB);
    atA = createAttribute('A');
    atA.setDataElementAttribute(true);
    atA.setOrganisationUnitAttribute(true);
    atA.setCategoryOptionComboAttribute(true);
    idObjectManager.save(atA);
    dsA = createDataSet('A');
    dsA.addDataSetElement(deA);
    dsA.addDataSetElement(deB);
    dsB = createDataSet('B');
    dsB.addDataSetElement(deA);
    dataSetService.addDataSet(dsA);
    dataSetService.addDataSet(dsB);
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B', ouA);
    // Not in hierarchy of A
    ouC = createOrganisationUnit('C');
    organisationUnitService.addOrganisationUnit(ouA);
    organisationUnitService.addOrganisationUnit(ouB);
    organisationUnitService.addOrganisationUnit(ouC);
    ogA = createOrganisationUnitGroup('A');
    idObjectManager.save(ogA);
    attributeService.addAttributeValue(deA, atA.getUid(), "AttributeValueA");
    attributeService.addAttributeValue(ouA, atA.getUid(), "AttributeValueB");
    attributeService.addAttributeValue(cocA, atA.getUid(), "AttributeValueC");
    attributeService.addAttributeValue(cocB, atA.getUid(), "AttributeValueD");
    // Data values
    addDataValues(
        new DataValue(deA, peA, ouA, cocA, cocA, "1"),
        new DataValue(deA, peA, ouA, cocB, cocB, "1"),
        new DataValue(deA, peA, ouB, cocA, cocA, "1"),
        new DataValue(deA, peA, ouB, cocB, cocB, "1"),
        new DataValue(deA, peB, ouA, cocA, cocA, "1"),
        new DataValue(deA, peB, ouA, cocB, cocB, "1"),
        new DataValue(deA, peB, ouB, cocA, cocA, "1"),
        new DataValue(deA, peB, ouB, cocB, cocB, "1"),
        new DataValue(deB, peA, ouA, cocA, cocA, "1"),
        new DataValue(deB, peA, ouA, cocB, cocB, "1"),
        new DataValue(deB, peA, ouB, cocA, cocA, "1"),
        new DataValue(deB, peA, ouB, cocB, cocB, "1"));

    dataSetService.updateDataSet(dsA);
    dataSetService.updateDataSet(dsB);

    user = makeUser("A");
    user.setOrganisationUnits(Sets.newHashSet(ouA, ouB));
    userService.addUser(user);
    injectSecurityContextUser(user);

    enableDataSharing(user, dsA, AccessStringHelper.DATA_READ_WRITE);
    enableDataSharing(user, dsB, AccessStringHelper.DATA_READ_WRITE);
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------
  @Test
  void testExportBasic() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(Set.of(dsA.getUid()))
            .orgUnit(Set.of(ouA.getUid()))
            .period(Set.of(peA.getIsoDate()))
            .build();
    dataExportPipeline.exportAsJson(params, out);
    DataValueSet dvs = jsonMapper.readValue(out.toByteArray(), DataValueSet.class);
    assertNotNull(dvs);
    assertNotNull(dvs.getDataSet());
    assertEquals(dsA.getUid(), dvs.getDataSet());
    assertEquals(ouA.getUid(), dvs.getOrgUnit());
    assertEquals(peAIso, dvs.getPeriod());
    assertEquals(4, dvs.getDataValues().size());
  }

  @Test
  void testExportBasic_FilterAoc() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataElement(Set.of(deA.getUid()))
            .orgUnit(Set.of(ouA.getUid(), ouB.getUid()))
            .period(Set.of(peA.getIsoDate(), peB.getIsoDate()))
            .attributeOptionCombo(Set.of(cocA.getUid()))
            .build();
    dataExportPipeline.exportAsJson(params, out);
    DataValueSet dvs = jsonMapper.readValue(out.toByteArray(), DataValueSet.class);
    assertNotNull(dvs);
    assertEquals(cocA.getUid(), dvs.getAttributeOptionCombo());
    assertEquals(4, dvs.getDataValues().size());
    for (org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues())
      assertNull(dv.getAttributeOptionCombo());
  }

  @Test
  void testExportBasic_FilterMultiAoc() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataElement(Set.of(deA.getUid()))
            .orgUnit(Set.of(ouA.getUid(), ouB.getUid()))
            .period(Set.of(peA.getIsoDate(), peB.getIsoDate()))
            .attributeOptionCombo(Set.of(cocA.getUid(), cocB.getUid()))
            .build();
    dataExportPipeline.exportAsJson(params, out);
    DataValueSet dvs = jsonMapper.readValue(out.toByteArray(), DataValueSet.class);
    assertNotNull(dvs);
    assertNull(dvs.getAttributeOptionCombo());
    assertEquals(8, dvs.getDataValues().size());
    for (org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues())
      assertTrue(Set.of(cocA.getUid(), cocB.getUid()).contains(dv.getAttributeOptionCombo()));
  }

  @Test
  void testExportBasic_FromUrlParamsWithDataElementIds() throws Exception {
    DataExportParams params =
        DataExportParams.builder()
            .dataElement(Set.of(deA.getCode(), deB.getCode()))
            .inputDataElementIdScheme(IdentifiableProperty.CODE)
            .orgUnit(singleton(ouA.getCode()))
            .inputOrgUnitIdScheme(IdentifiableProperty.CODE)
            .period(singleton(peAIso))
            .idScheme(IdentifiableProperty.CODE.name())
            .build();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataExportPipeline.exportAsJson(params, out);
    DataValueSet dvs = jsonMapper.readValue(out.toByteArray(), DataValueSet.class);
    assertNotNull(dvs);
    assertEquals(4, dvs.getDataValues().size());
    assertEquals(
        Set.of("DataElementCodeA", "DataElementCodeB"),
        dvs.getDataValues().stream()
            .map(org.hisp.dhis.dxf2.datavalue.DataValue::getDataElement)
            .collect(toUnmodifiableSet()));
  }

  @Test
  void testExportBasic_FromUrlParamsWithCodes() throws Exception {
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(singleton(dsA.getCode()))
            .inputOrgUnitIdScheme(IdentifiableProperty.CODE)
            .orgUnit(singleton(ouA.getCode()))
            .inputDataSetIdScheme(IdentifiableProperty.CODE)
            .period(singleton(peAIso))
            .dataSetIdScheme(IdentifiableProperty.CODE.name())
            .orgUnitIdScheme(IdentifiableProperty.CODE.name())
            .build();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataExportPipeline.exportAsJson(params, out);
    DataValueSet dvs = jsonMapper.readValue(out.toByteArray(), DataValueSet.class);
    assertNotNull(dvs);
    assertNotNull(dvs.getDataSet());
    assertEquals(dsA.getCode(), dvs.getDataSet());
    assertEquals(ouA.getCode(), dvs.getOrgUnit());
    assertEquals(peAIso, dvs.getPeriod());
    assertEquals(4, dvs.getDataValues().size());
  }

  @Test
  void testExportAttributeOptionCombo() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(Set.of(dsA.getUid()))
            .orgUnit(Set.of(ouB.getUid()))
            .period(Set.of(peA.getIsoDate()))
            .attributeOptionCombo(Set.of(cocA.getUid()))
            .build();
    dataExportPipeline.exportAsJson(params, out);
    DataValueSet dvs = jsonMapper.readValue(out.toByteArray(), DataValueSet.class);
    assertNotNull(dvs);
    assertNotNull(dvs.getDataSet());
    assertEquals(ouB.getUid(), dvs.getOrgUnit());
    assertEquals(peAIso, dvs.getPeriod());
    assertEquals(2, dvs.getDataValues().size());
  }

  @Test
  void testExportAttributeOptionCombo_FromUrlParamsWithCodes() throws Exception {
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(singleton(dsA.getCode()))
            .inputOrgUnitIdScheme(IdentifiableProperty.CODE)
            .orgUnit(singleton(ouB.getCode()))
            .inputDataSetIdScheme(IdentifiableProperty.CODE)
            .period(singleton(peAIso))
            .attributeOptionCombo(singleton(cocA.getCode()))
            .inputIdScheme(IdentifiableProperty.CODE)
            .dataSetIdScheme(IdentifiableProperty.CODE.name())
            .orgUnitIdScheme(IdentifiableProperty.CODE.name())
            .attributeOptionComboIdScheme(IdentifiableProperty.CODE.name())
            .build();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataExportPipeline.exportAsJson(params, out);
    JsonObject dvs = JsonMixed.of(out.toString(StandardCharsets.UTF_8));
    JsonArray values = dvs.getArray("dataValues");
    assertEquals(2, values.size());
    // since COC, PE, OU are all common for all values
    // these should be in the set's header
    assertEquals(cocA.getCode(), dvs.getString("attributeOptionCombo").string());
    assertEquals(peAIso, dvs.getString("period").string());
    assertEquals(ouB.getCode(), dvs.getString("orgUnit").string());
  }

  @Test
  void testExportOrgUnitChildren() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(Set.of(dsA.getUid()))
            .orgUnit(Set.of(ouA.getUid()))
            .children(true)
            .period(Set.of(peA.getIsoDate()))
            .build();
    dataExportPipeline.exportAsJson(params, out);
    JsonObject dvs = JsonMixed.of(out.toString(StandardCharsets.UTF_8));
    assertEquals(dsA.getUid(), dvs.getString("dataSet").string());
    assertEquals(peAIso, dvs.getString("period").string());
    assertEquals(8, dvs.getArray("dataValues").size());
  }

  @Test
  void testExportOutputSingleDataValueSetIdSchemeCode() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(Set.of(dsB.getUid()))
            .orgUnit(Set.of(ouA.getUid()))
            .period(Set.of(peB.getIsoDate()))
            .orgUnitIdScheme(IdentifiableProperty.CODE.name())
            .dataElementIdScheme(IdentifiableProperty.CODE.name())
            .dataSetIdScheme(IdentifiableProperty.CODE.name())
            .build();
    dataExportPipeline.exportAsJson(params, out);
    DataValueSet dvs = jsonMapper.readValue(out.toByteArray(), DataValueSet.class);
    assertNotNull(dvs);
    assertNotNull(dvs.getDataSet());
    assertNotNull(dvs.getOrgUnit());
    assertEquals(dsB.getCode(), dvs.getDataSet());
    assertEquals(ouA.getCode(), dvs.getOrgUnit());
    assertEquals(2, dvs.getDataValues().size());
    for (org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues()) {
      assertNotNull(dv);
      assertEquals(deA.getCode(), dv.getDataElement());
    }
  }

  @Test
  void testExportOutputSingleDataValueSetIdSchemeCode_FromUrlParamsWithCodes() throws Exception {
    DataExportParams params =
        DataExportParams.builder()
            .orgUnitIdScheme(IdentifiableProperty.CODE.name())
            .dataElementIdScheme(IdentifiableProperty.CODE.name())
            .dataSetIdScheme(IdentifiableProperty.CODE.name())
            . //
            dataSet(singleton(dsB.getCode()))
            .orgUnit(singleton(ouA.getCode()))
            .period(singleton(peBIso))
            .inputIdScheme(IdentifiableProperty.CODE)
            .build();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    dataExportPipeline.exportAsJson(params, out);
    DataValueSet dvs = jsonMapper.readValue(out.toByteArray(), DataValueSet.class);
    assertNotNull(dvs);
    assertNotNull(dvs.getDataSet());
    assertNotNull(dvs.getOrgUnit());
    assertEquals(dsB.getCode(), dvs.getDataSet());
    assertEquals(ouA.getCode(), dvs.getOrgUnit());
    assertEquals(2, dvs.getDataValues().size());
    for (org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues()) {
      assertNotNull(dv);
      assertEquals(deA.getCode(), dv.getDataElement());
    }
  }

  @Test
  void testExportOutputIdSchemeAttribute() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    String attributeIdScheme = IdScheme.ATTR_ID_SCHEME_PREFIX + atA.getUid();
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(Set.of(dsB.getUid()))
            .orgUnit(Set.of(ouA.getUid()))
            .period(Set.of(peB.getIsoDate()))
            .dataElementIdScheme(attributeIdScheme)
            .orgUnitIdScheme(attributeIdScheme)
            .categoryOptionComboIdScheme(attributeIdScheme)
            .build();
    dataExportPipeline.exportAsJson(params, out);
    DataValueSet dvs = jsonMapper.readValue(out.toByteArray(), DataValueSet.class);
    assertNotNull(dvs);
    assertNotNull(dvs.getDataSet());
    assertEquals(dsB.getUid(), dvs.getDataSet());
    assertEquals("AttributeValueB", dvs.getOrgUnit());
    assertEquals(2, dvs.getDataValues().size());
    for (org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues()) {
      assertNotNull(dv);
      assertEquals("AttributeValueA", dv.getDataElement());
    }
  }

  @Test
  void testExportLastUpdated() throws Exception {
    Date lastUpdated = getDate(1970, 1, 1);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params = DataExportParams.builder().lastUpdated(lastUpdated).build();
    dataExportPipeline.exportAsJsonSync(params, out);
    DataValueSet dvs = jsonMapper.readValue(out.toByteArray(), DataValueSet.class);
    assertNotNull(dvs);
    assertEquals(12, dvs.getDataValues().size());
    for (org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues()) {
      assertNotNull(dv);
    }
  }

  @Test
  void testExportLastUpdatedWithDeletedValues() throws Exception {
    DataValue dvA = new DataValue(deC, peA, ouA, cocA, cocA, "1");
    DataValue dvB = new DataValue(deC, peB, ouA, cocA, cocA, "2");
    addDataValues(dvA, dvB);
    Date lastUpdated = getDate(1970, 1, 1);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params = DataExportParams.builder().lastUpdated(lastUpdated).build();
    dataExportPipeline.exportAsJsonSync(params, out);
    JsonObject json = JsonMixed.of(out.toString(StandardCharsets.UTF_8));
    assertEquals(14, json.getArray("dataValues").size());
    deleteDataValue(dvA);
    deleteDataValue(dvB);
    out = new ByteArrayOutputStream();
    dataExportPipeline.exportAsJsonSync(params, out);
    json = JsonMixed.of(out.toString(StandardCharsets.UTF_8));
    JsonArray values = json.getArray("dataValues");
    assertEquals(14, values.size());
    assertEquals(
        2, values.count(JsonValue::asObject, dv -> dv.getBoolean("deleted").booleanValue(false)));
  }

  @Test
  void testMissingDataSetElementGroup() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .orgUnit(Set.of(ouB.getUid()))
            .period(Set.of(peA.getUid()))
            .build();
    ConflictException ex =
        assertThrows(ConflictException.class, () -> dataExportPipeline.exportAsJson(params, out));
    assertEquals(ErrorCode.E2001, ex.getCode());
  }

  @Test
  void testMissingPeriodStartEndDate() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(Set.of(dsA.getUid()))
            .orgUnit(Set.of(ouA.getUid()))
            .build();
    ConflictException ex =
        assertThrows(ConflictException.class, () -> dataExportPipeline.exportAsJson(params, out));
    assertEquals(ErrorCode.E2002, ex.getCode());
  }

  @Test
  void testPeriodAndStartEndDate() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(Set.of(dsA.getUid()))
            .orgUnit(Set.of(ouB.getUid()))
            .period(Set.of(peA.getIsoDate()))
            .startDate(getDate(2019, 1, 1))
            .endDate(getDate(2019, 1, 31))
            .build();
    ConflictException ex =
        assertThrows(ConflictException.class, () -> dataExportPipeline.exportAsJson(params, out));
    assertEquals(ErrorCode.E2003, ex.getCode());
  }

  @Test
  void testStartDateAfterEndDate() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(Set.of(dsA.getUid()))
            .orgUnit(Set.of(ouB.getUid()))
            .startDate(getDate(2019, 3, 1))
            .endDate(getDate(2019, 1, 31))
            .build();
    ConflictException ex =
        assertThrows(ConflictException.class, () -> dataExportPipeline.exportAsJson(params, out));
    assertEquals(ErrorCode.E2004, ex.getCode());
  }

  @Test
  void testMissingOrgUnit() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(Set.of(dsA.getUid()))
            .period(Set.of(peA.getIsoDate()))
            .build();
    ConflictException ex =
        assertThrows(ConflictException.class, () -> dataExportPipeline.exportAsJson(params, out));
    assertEquals(ErrorCode.E2006, ex.getCode());
  }

  @Test
  void testAtLestOneOrgUnitWithChildren() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(Set.of(dsA.getUid()))
            .period(Set.of(peA.getIsoDate(), peB.getIsoDate()))
            .orgUnitGroup(Set.of(ogA.getUid()))
            .children(true)
            .build();
    ConflictException ex =
        assertThrows(ConflictException.class, () -> dataExportPipeline.exportAsJson(params, out));
    assertEquals(ErrorCode.E2007, ex.getCode());
  }

  @Test
  void testLimitLimitNotLessThanZero() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(Set.of(dsA.getUid()))
            .period(Set.of(peA.getIsoDate(), peB.getIsoDate()))
            .orgUnit(Set.of(ouB.getUid()))
            .limit(-2)
            .build();
    ConflictException ex =
        assertThrows(ConflictException.class, () -> dataExportPipeline.exportAsJson(params, out));
    assertEquals(ErrorCode.E2009, ex.getCode());
  }

  @Test
  void testAccessOutsideOrgUnitHierarchy() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataExportParams params =
        DataExportParams.builder()
            .dataSet(Set.of(dsA.getUid()))
            .orgUnit(Set.of(ouC.getUid()))
            .period(Set.of(peA.getIsoDate()))
            .build();
    ConflictException ex =
        assertThrows(ConflictException.class, () -> dataExportPipeline.exportAsJson(params, out));
    assertEquals(ErrorCode.E2012, ex.getCode());
  }

  private void addDataValues(DataValue... values) {
    if (dataDumpService.upsertValues(values) < values.length) fail("Failed to upsert test data");
  }

  private void deleteDataValue(DataValue dv) {
    dv.setDeleted(true);
    addDataValues(dv);
  }
}
