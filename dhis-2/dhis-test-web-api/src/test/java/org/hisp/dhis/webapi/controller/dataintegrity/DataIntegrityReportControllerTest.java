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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.web.WebClientUtils.objectReference;
import static org.hisp.dhis.web.WebClientUtils.objectReferences;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.jsontree.JsonNodeType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.controller.DataIntegrityController;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link DataIntegrityController} API with focus API returning {@link
 * org.hisp.dhis.dataintegrity.FlattenedDataIntegrityReport}.
 *
 * @author Jan Bernitt
 */
class DataIntegrityReportControllerTest extends AbstractDataIntegrityIntegrationTest {
  /** Needed to create cyclic references for org units */
  @Autowired private OrganisationUnitStore organisationUnitStore;

  @Test
  void testNoViolations() {
    // if the report does not have any strings in the JSON tree there are no
    // errors since all collection/maps have string values
    JsonDataIntegrityReport report = getDataIntegrityReport();
    assertEquals(0, report.node().count(JsonNodeType.STRING));
  }

  @Test
  void testDataElementChecksOnly() {
    JsonDataIntegrityReport report = getDataIntegrityReport("/dataIntegrity?checks=data-element*");
    assertEquals(5, report.size());
    assertTrue(
        report.has(
            "dataElementsWithoutDataSet",
            "dataElementsWithoutGroups",
            "dataElementsAssignedToDataSetsWithDifferentPeriodTypes",
            "dataElementsViolatingExclusiveGroupSets",
            "dataElementsInDataSetNotInForm"));
  }

  @Test
  void testExclusiveGroupsChecksOnly() {
    JsonDataIntegrityReport report =
        getDataIntegrityReport("/dataIntegrity?checks=*exclusive-group*");
    assertEquals(3, report.size());
    assertTrue(
        report.has(
            "dataElementsViolatingExclusiveGroupSets",
            "indicatorsViolatingExclusiveGroupSets",
            "organisationUnitsViolatingExclusiveGroupSets"));
  }

  @Test
  void testPeriodsDuplicatesOnly() {
    JsonDataIntegrityReport report =
        getDataIntegrityReport("/dataIntegrity?checks=periods_same_start_date_period_type");
    assertEquals(1, report.size());
    assertTrue(report.getArray("duplicatePeriods").exists());
  }

  @Test
  void testOrphanedOrganisationUnits() {
    // should match:
    String ouId = addOrganisationUnit("OrphanedUnit");
    // should not match:
    String ouRootId = addOrganisationUnit("root");
    addOrganisationUnit("leaf", ouRootId);
    assertEquals(
        singletonList("OrphanedUnit:" + ouId),
        getDataIntegrityReport().getOrphanedOrganisationUnits().toList(JsonString::string));
  }

  @Test
  void testOrganisationUnitsWithoutGroups() {
    // should match:
    String ouId = addOrganisationUnit("noGroupSet");
    // should not match:
    addOrganisationUnitGroup("group", addOrganisationUnit("hasGroupSet"));
    List<String> results =
        getDataIntegrityReport().getOrganisationUnitsWithoutGroups().toList(JsonString::string);
    assertEquals(singletonList("noGroupSet:" + ouId), results);
  }

  @Test
  void testOrganisationUnitsWithCyclicReferences() {
    String ouIdA = addOrganisationUnit("A");
    String ouIdB = addOrganisationUnit("B", ouIdA);
    // create cyclic references (seemingly not possible via REST API)
    OrganisationUnit ouA = organisationUnitStore.getByUid(ouIdA);
    OrganisationUnit ouB = organisationUnitStore.getByUid(ouIdB);
    ouA.setParent(ouB);
    ouB.getChildren().add(ouA);
    organisationUnitStore.save(ouB);
    assertContainsOnly(
        List.of("A:" + ouIdA, "B:" + ouIdB),
        getDataIntegrityReport()
            .getOrganisationUnitsWithCyclicReferences()
            .toList(JsonString::string));
  }

  @Test
  void testOrganisationUnitsViolatingExclusiveGroupSets() {
    String ouIdA = addOrganisationUnit("A");
    String ouIdB = addOrganisationUnit("B");
    addOrganisationUnit("C");
    // all groups created are compulsory
    String groupA0Id = addOrganisationUnitGroup("A0", ouIdA);
    String groupB1Id = addOrganisationUnitGroup("B1", ouIdB);
    String groupB2Id = addOrganisationUnitGroup("B2", ouIdB);
    addOrganisationUnitGroupSet("K", groupA0Id);
    addOrganisationUnitGroupSet("X", groupB1Id, groupB2Id);
    assertEquals(
        Map.of("B", asList("B1:" + groupB1Id, "B2:" + groupB2Id)),
        getDataIntegrityReport()
            .getOrganisationUnitsViolatingExclusiveGroupSets()
            .toMap(JsonString::string, String::compareTo));
  }

  @Test
  void testDataElementsInDatasetsWithDifferentFrequencies() {
    String defaultCatCombo = getDefaultCatCombo();

    String dataElementA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    String dataset1_uid = generateUid();

    String dataset1 =
        "{ 'id':'"
            + dataset1_uid
            + "', 'name': 'Test Monthly', 'shortName': 'Test Monthly', 'periodType' : 'Monthly',"
            + "'categoryCombo' : {'id': '"
            + defaultCatCombo
            + "'}, "
            + "'dataSetElements' : [{'dataSet' : {'id':'"
            + dataset1_uid
            + "'}, 'id':'"
            + generateUid()
            + "', 'dataElement': {'id' : '"
            + dataElementA
            + "'}}]}";

    assertStatus(HttpStatus.CREATED, POST("/dataSets", dataset1));

    String dataset2_uid = generateUid();
    String dataset2 =
        "{ 'id':'"
            + dataset2_uid
            + "', 'name': 'Test Quarterly', 'shortName': 'Test Quarterly', 'periodType' : 'Quarterly',"
            + "'categoryCombo' : {'id': '"
            + defaultCatCombo
            + "'}, "
            + "'dataSetElements' : [{'dataSet' : {'id':'"
            + dataset2_uid
            + "'}, 'id':'"
            + generateUid()
            + "', 'dataElement': {'id' : '"
            + dataElementA
            + "'}}]}";

    assertStatus(HttpStatus.CREATED, POST("/dataSets", dataset2));

    Map<String, List<String>> expected =
        Map.of("ANC1", List.of(dataset1_uid, dataset2_uid).stream().sorted().toList());

    Map<String, List<String>> actual =
        getDataIntegrityReport()
            .getDataElementsAssignedToDataSetsWithDifferentPeriodTypes()
            .toMap(JsonString::string, String::compareTo);

    assertEquals(expected, actual);
  }

  private String addOrganisationUnit(String name) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            "{'name':'" + name + "', 'shortName':'" + name + "', 'openingDate':'2021'}"));
  }

  private String addOrganisationUnit(String name, String parentId) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            "{'name':'"
                + name
                + "', 'shortName':'"
                + name
                + "', 'openingDate':'2021', 'parent': "
                + objectReference(parentId)
                + " }"));
  }

  private String addOrganisationUnitGroup(String name, String... memberIds) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnitGroups",
            "{'name':'"
                + name
                + "', 'shortName':'"
                + name
                + "', 'organisationUnits': "
                + objectReferences(memberIds)
                + "}"));
  }

  private String addOrganisationUnitGroupSet(String name, String... groupIds) {
    // OBS! note that we make them compulsory
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnitGroupSets",
            "{'name':'"
                + name
                + "', 'shortName':'"
                + name
                + "', 'compulsory': true, 'organisationUnitGroups': "
                + objectReferences(groupIds)
                + "}"));
  }

  @Test
  void testDataElementsNoGroups() {

    String dataElementA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    String dataElementB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC2', 'shortName': 'ANC2', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataElementGroups",
            "{ 'name': 'ANC', 'shortName': 'ANC' , 'dataElements' : [{'id' : '"
                + dataElementA
                + "'}]}"));
    List<String> results =
        getDataIntegrityReport().getDataElementsWithoutGroups().toList(JsonString::string);
    assertEquals(singletonList("ANC2" + ":" + dataElementB), results);
  }

  @Test
  void testDataElementsNoDatasets() {

    String defaultCatCombo = getDefaultCatCombo();
    String dataSetA = generateUid();

    String dataElementA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));
    String dataElementB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC2', 'shortName': 'ANC2', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));
    String datasetMetadata =
        "{ 'id':'"
            + dataSetA
            + "', 'name': 'Test Monthly', 'shortName': 'Test Monthly', 'periodType' : 'Monthly',"
            + "'categoryCombo' : {'id': '"
            + defaultCatCombo
            + "'}, "
            + "'dataSetElements' : [{'dataSet' : {'id':'"
            + dataSetA
            + "'}, 'id':'"
            + generateUid()
            + "', 'dataElement': {'id' : '"
            + dataElementA
            + "'}}]}";

    assertStatus(HttpStatus.CREATED, POST("/dataSets", datasetMetadata));

    List<String> results =
        getDataIntegrityReport().getDataElementsWithoutDataSet().toList(JsonString::string);
    assertEquals(List.of("ANC2" + ":" + dataElementB), results);
  }

  @Test
  void testDatasetsNotAssignedToOrgUnits() {
    String defaultCatCombo = getDefaultCatCombo();
    String dataSetUID = generateUid();
    String dataElementA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataSets",
            "{ 'id' : '"
                + dataSetUID
                + "', 'name': 'Test', 'shortName': 'Test', 'periodType' : 'Monthly', 'categoryCombo' : {'id': '"
                + defaultCatCombo
                + "'}, "
                + " 'dataSetElements': [{ 'dataSet': { 'id': '"
                + dataSetUID
                + "'}, 'dataElement': { 'id': '"
                + dataElementA
                + "'}}]}"));
    List<String> results =
        getDataIntegrityReport()
            .getDataSetsNotAssignedToOrganisationUnits()
            .toList(JsonString::string);
    assertEquals(List.of("Test"), results);
  }

  @Test
  void testInvalidCategoryCombos() {
    String categoryTaste =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                "{ 'name': 'Taste', 'shortName': 'Taste', 'dataDimensionType': 'DISAGGREGATION' }"));

    String testCatCombo =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryCombos",
                "{ 'name' : 'Tasteless', "
                    + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                    + " {'id' : '"
                    + categoryTaste
                    + "'}]} "));
    List<String> results =
        getDataIntegrityReport().getInvalidCategoryCombos().toList(JsonString::string);
    assertEquals(List.of("Tasteless" + ":" + testCatCombo), results);
  }

  @Test
  void testIndicatorsNotInAnyGroups() {
    String indicatorTypeA =
        assertStatus(
            HttpStatus.CREATED,
            POST("/indicatorTypes", "{ 'name': 'Per cent', 'factor' : 100, 'number' : false }"));

    String indicatorA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators",
                "{ 'name': 'Indicator A', 'shortName': 'Indicator A',  'indicatorType' : {'id' : '"
                    + indicatorTypeA
                    + "'},"
                    + " 'numerator' : 'abc123', 'numeratorDescription' : 'One', 'denominator' : 'abc123', "
                    + "'denominatorDescription' : 'Zero'} }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/indicators",
            "{ 'name': 'Indicator B', 'shortName': 'Indicator B', 'indicatorType' : {'id' : '"
                + indicatorTypeA
                + "'},"
                + " 'numerator' : 'abc123', 'numeratorDescription' : 'One', 'denominator' : 'abc123', "
                + "'denominatorDescription' : 'Zero'}"));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/indicatorGroups",
            "{ 'name' : 'An indicator group', 'indicators' : [{'id' : '" + indicatorA + "'}]}"));

    List<String> results =
        getDataIntegrityReport().getIndicatorsWithoutGroups().toList(JsonString::string);
    assertEquals(List.of("Indicator B"), results);
  }

  @Test
  void testIndicatorsViolateExclusiveGroups()  {
    String indicatorTypeA =
        assertStatus(
            HttpStatus.CREATED,POST(
                "/indicatorTypes",
                """
                        {
                          "name": "Per cent",
                          "factor": 100,
                          "number": false
                        }
                        """
                    .formatted()));

    String indicatorA = createSimpleIndicator( "Indicator A", indicatorTypeA);
    String indicatorB = createSimpleIndicator( "Indicator B", indicatorTypeA);
    String indicatorGroupA =
        assertStatus(
            HttpStatus.CREATED,
            POST("/indicatorGroups",
                // language=JSON
                """
                            {
                            "name": "Group A",
                             "shortName" : "Group A",
                            "indicators": [
                                {
                                "id": "%s"
                                },
                                {
                                "id": "%s"
                                }
                            ]
                            }
                            """
                    .formatted(indicatorA, indicatorB)));

    String indicatorGroupB =
        assertStatus(
            HttpStatus.CREATED,
            POST("/indicatorGroups",
                // language=JSON
                """
                            {
                            "name": "Group B",
                             "shortName" : "Group B",
                            "indicators": [
                                {
                                "id": "%s"
                                }
                            ]
                            }
                            """
                    .formatted(indicatorB)));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/indicatorGroupSets",
            // language=JSON
            """
                    {
                    "name": "Indicator Group Set",
                    "shortName": "Indicator Group Set",
                    "indicatorGroups": [
                        {
                        "id": "%s"
                        },
                        {
                        "id": "%s"
                        }
                    ]
                    }
                    """
                .formatted(indicatorGroupA, indicatorGroupB)));
    final Map<String, List<String>> expectedResults =
        Map.of("Indicator B", List.of("Group A:" + indicatorGroupA, "Group B:" + indicatorGroupB));
    // Test for program rules with no condition
    Map<String, List<String>> results =
        getDataIntegrityReport()
            .getIndicatorsViolatingExclusiveGroupSets()
            .toMap(JsonString::string, String::compareTo);
    assertEquals(expectedResults, results);
  }

  @Test
  void testValidationRulesWithoutGroups() {
    String validationRule1 =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/validationRules",
                "{'importance':'MEDIUM','operator':'not_equal_to','leftSide':{'missingValueStrategy':'NEVER_SKIP', "
                    + ""
                    + "'description':'Test','expression':'#{FTRrcoaog83.qk6n4eMAdtK}'},"
                    + "'rightSide':{'missingValueStrategy': 'NEVER_SKIP', 'description':'Test1',"
                    + "'expression':'#{FTRrcoaog83.sqGRzCziswD}'},'periodType':'Monthly','name':'Test rule 1'}"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/validationRules",
            "{'importance':'MEDIUM','operator':'not_equal_to','leftSide':{'missingValueStrategy':'NEVER_SKIP', "
                + ""
                + "'description':'Test','expression':'#{FTRrcoaog83.qk6n4eMAdtK}'},"
                + "'rightSide':{'missingValueStrategy': 'NEVER_SKIP', 'description':'Test2',"
                + "'expression':'#{FTRrcoaog83.sqGRzCziswD}'},'periodType':'Monthly','name':'Test rule 2'}"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/validationRuleGroups",
            "{'name':'Test group', 'description':'Test group', 'validationRules': [{'id': '"
                + validationRule1
                + "'}]}"));

    List<String> results =
        getDataIntegrityReport().getValidationRulesWithoutGroups().toList(JsonString::string);
    assertEquals(List.of("Test rule 2"), results);
  }

  @Test
  void testProgramRulesWithoutCondition() {
    String program =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs",
                "{'name':'Test program', 'shortName': 'Test program', 'programType': 'WITHOUT_REGISTRATION'}"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/programRules",
            "{'name':'Test rule 1', 'description':'Test rule 1', 'program': {'id': '"
                + program
                + "'}}"));

    final Map<String, List<String>> expectedResults =
        Map.of("Test rule 1", List.of("Test program:" + program));
    // Test for program rules with no condition
    Map<String, List<String>> results =
        getDataIntegrityReport()
            .getProgramRulesWithNoCondition()
            .toMap(JsonString::string, String::compareTo);
    assertEquals(expectedResults, results);

    // Test for program rules with no action
    Map<String, List<String>> results2 =
        getDataIntegrityReport()
            .getProgramRulesWithNoAction()
            .toMap(JsonString::string, String::compareTo);
    assertEquals(expectedResults, results2);

    // Test for program rules with no priority
    Map<String, List<String>> results3 =
        getDataIntegrityReport()
            .getProgramRulesWithNoPriority()
            .toMap(JsonString::string, String::compareTo);
    assertEquals(expectedResults, results3);
  }

  @Test
  void testProgramIndicatorsWithoutExpression() {
    String program =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs",
                "{'name':'Test program', 'shortName': 'Test program', 'programType': 'WITHOUT_REGISTRATION'}"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/programIndicators",
            "{'name':'Test program indicator A', 'shortName': 'Test program indicator A', 'program': {'id': '"
                + program
                + "'}}"));
    // Test for program of indicators with no expression
    List<String> results =
        getDataIntegrityReport().getProgramIndicatorsWithNoExpression().toList(JsonString::string);
    assertEquals(List.of("Test program indicator A"), results);
  }

  private JsonDataIntegrityReport getDataIntegrityReport() {
    return getDataIntegrityReport("/dataIntegrity");
  }

  private JsonDataIntegrityReport getDataIntegrityReport(String url) {
    HttpResponse httpResponse = POST(url);
    assertTrue(
        httpResponse.location().startsWith("http://localhost/dataIntegrity/details?checks="));
    JsonObject response = httpResponse.content().getObject("response");
    String id = response.getString("id").string();
    String jobType = response.getString("jobType").string();
    return GET("/system/taskSummaries/{type}/{id}", jobType, id)
        .content()
        .as(JsonDataIntegrityReport.class);
  }
}
