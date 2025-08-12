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

import static org.hisp.dhis.security.acl.AccessStringHelper.DEFAULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.query.Filters;
import org.hisp.dhis.query.Junction;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
class MetadataExportServiceTest extends PostgresIntegrationTestBase {
  @Autowired private MetadataExportService metadataExportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private SchemaService schemaService;

  @Test
  void testValidate() {
    MetadataExportParams params = new MetadataExportParams();
    metadataExportService.validate(params);
  }

  @Test
  void testMetadataExport() {
    DataElementGroup deg1 = createDataElementGroup('A');
    DataElement de1 = createDataElement('A');
    DataElement de2 = createDataElement('B');
    DataElement de3 = createDataElement('C');
    manager.save(de1);
    manager.save(de2);
    manager.save(de3);
    User user = makeUser("A");
    manager.save(user);
    deg1.addDataElement(de1);
    deg1.addDataElement(de2);
    deg1.addDataElement(de3);
    deg1.setCreatedBy(user);
    manager.save(deg1);
    MetadataExportParams params = new MetadataExportParams();
    Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata =
        metadataExportService.getMetadata(params);
    assertEquals(2, metadata.get(User.class).size());
    assertEquals(1, metadata.get(DataElementGroup.class).size());
    assertEquals(3, metadata.get(DataElement.class).size());
  }

  @Test
  void testMetadataExportWithCustomClasses() {
    DataElementGroup deg1 = createDataElementGroup('A');
    DataElement de1 = createDataElement('A');
    DataElement de2 = createDataElement('B');
    DataElement de3 = createDataElement('C');
    manager.save(de1);
    manager.save(de2);
    manager.save(de3);
    User user = makeUser("A");
    manager.save(user);
    deg1.addDataElement(de1);
    deg1.addDataElement(de2);
    deg1.addDataElement(de3);
    deg1.setCreatedBy(user);
    manager.save(deg1);
    MetadataExportParams params = new MetadataExportParams();
    params.addClass(DataElement.class);
    Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata =
        metadataExportService.getMetadata(params);
    assertFalse(metadata.containsKey(User.class));
    assertFalse(metadata.containsKey(DataElementGroup.class));
    assertTrue(metadata.containsKey(DataElement.class));
    assertEquals(3, metadata.get(DataElement.class).size());
  }

  @Test
  void testMetadataExportWithCustomQueries() {
    DataElementGroup deg1 = createDataElementGroup('A');
    DataElement de1 = createDataElement('A');
    DataElement de2 = createDataElement('B');
    DataElement de3 = createDataElement('C');
    manager.save(de1);
    manager.save(de2);
    manager.save(de3);
    User user = makeUser("A");
    manager.save(user);
    deg1.addDataElement(de1);
    deg1.addDataElement(de2);
    deg1.addDataElement(de3);
    deg1.setCreatedBy(user);
    manager.save(deg1);
    Query<DataElement> deQuery = Query.of(DataElement.class, Junction.Type.OR);
    deQuery.add(Filters.eq("id", de1.getUid()));
    deQuery.add(Filters.eq("id", de2.getUid()));
    Query<DataElementGroup> degQuery = Query.of(DataElementGroup.class);
    degQuery.add(Filters.eq("id", "INVALID UID"));
    MetadataExportParams params = new MetadataExportParams();
    params.addQuery(deQuery);
    params.addQuery(degQuery);
    Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata =
        metadataExportService.getMetadata(params);
    assertFalse(metadata.containsKey(User.class));
    assertFalse(metadata.containsKey(DataElementGroup.class));
    assertTrue(metadata.containsKey(DataElement.class));
    assertEquals(2, metadata.get(DataElement.class).size());
  }

  @Test
  void testDashboardMetadataExportAsNodeStream() throws IOException {

    // Setup test data
    DataElement dataElementA = createDataElement('A');
    manager.save(dataElementA);
    DataElement dataElementB = createDataElement('B');
    manager.save(dataElementB);
    Visualization visualizationA = createVisualization('A');
    visualizationA.addDataDimensionItem(dataElementA);
    visualizationA.addDataDimensionItem(dataElementB);
    visualizationA.setSharing(Sharing.builder().publicAccess(DEFAULT).build());
    List<String> rawPeriods = new ArrayList<>();
    rawPeriods.add(RelativePeriodEnum.LAST_5_YEARS.name());
    rawPeriods.add(RelativePeriodEnum.THIS_BIWEEK.name());
    visualizationA.setRawPeriods(rawPeriods);
    manager.save(visualizationA, false);
    Dashboard dashboard =
        createDashboardWithItem("A", Sharing.builder().publicAccess(DEFAULT).build());
    dashboard.getItems().get(0).setVisualization(visualizationA);
    manager.save(dashboard, false);
    MetadataExportParams params = new MetadataExportParams();
    params.setClasses(Sets.newHashSet(Dashboard.class, DashboardItem.class, Visualization.class));

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    // method under test
    metadataExportService.getMetadataWithDependenciesAsNodeStream(dashboard, params, out);

    // assertions
    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(out.toByteArray());

    JsonNode systemNode = rootNode.get("system");
    assertNotNull(systemNode, "System node should exist");
    assertNull(systemNode.get("id").asText(null), "System id should be null");
    assertEquals("abc1234", systemNode.get("rev").asText(), "System revision should match");
    assertEquals("123", systemNode.get("version").asText(), "System version should match");
    assertNotNull(systemNode.get("date"), "System date should be present");
    assertFalse(systemNode.get("date").asText().isEmpty(), "System date should not be empty");

    // Assert Dashboards Array
    JsonNode dashboardsNode = rootNode.get("dashboards");
    assertNotNull(dashboardsNode, "Dashboards node should exist");
    assertTrue(dashboardsNode.isArray(), "Dashboards should be an array");
    assertEquals(1, dashboardsNode.size(), "Should have exactly 1 dashboard");

    // Assert Dashboard Properties
    JsonNode exportedDashboard = dashboardsNode.get(0);
    assertNotNull(exportedDashboard.get("created"), "Dashboard created date should be present");
    assertFalse(
        exportedDashboard.get("created").asText().isEmpty(),
        "Dashboard created date should not be empty");
    assertNotNull(exportedDashboard.get("lastUpdated"), "Dashboard lastUpdated should be present");
    assertFalse(
        exportedDashboard.get("lastUpdated").asText().isEmpty(),
        "Dashboard lastUpdated should not be empty");
    assertEquals(
        "dashboardA", exportedDashboard.get("name").asText(), "Dashboard name should match");
    assertFalse(
        exportedDashboard.get("restrictFilters").asBoolean(),
        "Dashboard restrictFilters should be false");
    assertNotNull(exportedDashboard.get("id"), "Dashboard id should be present");

    // Assert Dashboard CreatedBy User
    JsonNode createdBy = exportedDashboard.get("createdBy");
    assertNotNull(createdBy, "CreatedBy should exist");
    assertNotNull(createdBy.get("id"), "Created by id should be present");
    assertEquals("Codeadmin", createdBy.get("code").asText(), "CreatedBy code should match");
    assertEquals(
        "FirstNameadmin Surnameadmin",
        createdBy.get("name").asText(),
        "CreatedBy name should match");
    assertEquals(
        "FirstNameadmin Surnameadmin",
        createdBy.get("displayName").asText(),
        "CreatedBy displayName should match");
    assertEquals("admin", createdBy.get("username").asText(), "CreatedBy username should match");

    // Assert Dashboard LastUpdatedBy User
    JsonNode lastUpdatedBy = exportedDashboard.get("lastUpdatedBy");
    assertNotNull(lastUpdatedBy, "LastUpdatedBy should exist");
    assertNotNull(lastUpdatedBy.get("id"), "LastUpdatedBy id should be present");
    assertEquals(
        "Codeadmin", lastUpdatedBy.get("code").asText(), "LastUpdatedBy code should match");
    assertEquals(
        "FirstNameadmin Surnameadmin",
        lastUpdatedBy.get("name").asText(),
        "LastUpdatedBy name should match");
    assertEquals(
        "FirstNameadmin Surnameadmin",
        lastUpdatedBy.get("displayName").asText(),
        "LastUpdatedBy displayName should match");
    assertEquals(
        "admin", lastUpdatedBy.get("username").asText(), "LastUpdatedBy username should match");

    // Assert Dashboard Arrays
    assertTrue(
        exportedDashboard.get("allowedFilters").isArray(), "AllowedFilters should be an array");
    assertEquals(
        0, exportedDashboard.get("allowedFilters").size(), "AllowedFilters should be empty");
    assertTrue(exportedDashboard.get("favorites").isArray(), "Favorites should be an array");
    assertEquals(0, exportedDashboard.get("favorites").size(), "Favorites should be empty");
    assertTrue(exportedDashboard.get("translations").isArray(), "Translations should be an array");
    assertEquals(0, exportedDashboard.get("translations").size(), "Translations should be empty");

    // Assert Dashboard Sharing
    JsonNode sharing = exportedDashboard.get("sharing");
    assertNotNull(sharing, "Sharing should exist");
    assertNotNull(sharing.get("owner"), "Sharing owner id should be present");
    assertFalse(sharing.get("external").asBoolean(), "Sharing external should be false");
    assertEquals("--------", sharing.get("public").asText(), "Sharing public should match");
    assertTrue(sharing.get("users").isObject(), "Sharing users should be an object");
    assertTrue(sharing.get("userGroups").isObject(), "Sharing userGroups should be an object");

    // Assert Dashboard Items
    JsonNode dashboardItems = exportedDashboard.get("dashboardItems");
    assertNotNull(dashboardItems, "DashboardItems should exist");
    assertTrue(dashboardItems.isArray(), "DashboardItems should be an array");
    assertEquals(1, dashboardItems.size(), "Should have exactly 1 dashboard item");

    // Assert Dashboard Item Properties
    JsonNode dashboardItem = dashboardItems.get(0);
    assertNotNull(dashboardItem.get("created"), "DashboardItem created should be present");
    assertFalse(
        dashboardItem.get("created").asText().isEmpty(),
        "DashboardItem created should not be empty");
    assertNotNull(dashboardItem.get("lastUpdated"), "DashboardItem lastUpdated should be present");
    assertFalse(
        dashboardItem.get("lastUpdated").asText().isEmpty(),
        "DashboardItem lastUpdated should not be empty");
    assertEquals(
        "VISUALIZATION", dashboardItem.get("type").asText(), "DashboardItem type should match");
    assertNotNull(dashboardItem.get("id"), "DashboardItem id should be present");
    assertEquals(
        0,
        dashboardItem.get("interpretationCount").asInt(),
        "DashboardItem interpretationCount should be 0");
    assertEquals(
        0,
        dashboardItem.get("interpretationLikeCount").asInt(),
        "DashboardItem interpretationLikeCount should be 0");
    assertEquals(
        1, dashboardItem.get("contentCount").asInt(), "DashboardItem contentCount should be 1");

    // Assert Dashboard Item Visualization
    JsonNode visualization = dashboardItem.get("visualization");
    assertNotNull(visualization, "Visualization should exist");
    assertNotNull(visualization.get("id"), "Visualization id should be present");

    // Assert Dashboard Item Access
    JsonNode access = dashboardItem.get("access");
    assertNotNull(access, "Access should exist");
    assertTrue(access.get("manage").asBoolean(), "Access manage should be true");
    assertTrue(access.get("externalize").asBoolean(), "Access externalize should be true");
    assertTrue(access.get("write").asBoolean(), "Access write should be true");
    assertTrue(access.get("read").asBoolean(), "Access read should be true");
    assertTrue(access.get("update").asBoolean(), "Access update should be true");
    assertTrue(access.get("delete").asBoolean(), "Access delete should be true");

    // Assert Dashboard Item Arrays
    assertTrue(
        dashboardItem.get("translations").isArray(),
        "DashboardItem translations should be an array");
    assertEquals(
        0, dashboardItem.get("translations").size(), "DashboardItem translations should be empty");
    assertTrue(dashboardItem.get("users").isArray(), "DashboardItem users should be an array");
    assertEquals(0, dashboardItem.get("users").size(), "DashboardItem users should be empty");
    assertTrue(dashboardItem.get("reports").isArray(), "DashboardItem reports should be an array");
    assertEquals(0, dashboardItem.get("reports").size(), "DashboardItem reports should be empty");
    assertTrue(
        dashboardItem.get("resources").isArray(), "DashboardItem resources should be an array");
    assertEquals(
        0, dashboardItem.get("resources").size(), "DashboardItem resources should be empty");

    // Assert Visualizations Array
    JsonNode visualizationsNode = rootNode.get("visualizations");
    assertNotNull(visualizationsNode, "Visualizations node should exist");
    assertTrue(visualizationsNode.isArray(), "Visualizations should be an array");
    assertEquals(1, visualizationsNode.size(), "Should have exactly 1 visualization");

    // Assert Visualization Properties
    JsonNode viz = visualizationsNode.get(0);
    assertEquals("VisualizationA", viz.get("name").asText(), "Visualization name should match");
    assertNotNull(viz.get("created"), "Visualization created should be present");
    assertFalse(viz.get("created").asText().isEmpty(), "Visualization created should not be empty");
    assertNotNull(viz.get("lastUpdated"), "Visualization lastUpdated should be present");
    assertFalse(
        viz.get("lastUpdated").asText().isEmpty(), "Visualization lastUpdated should not be empty");
    assertEquals("PIVOT_TABLE", viz.get("type").asText(), "Visualization type should match");

    // Assert Visualization CreatedBy and LastUpdatedBy
    JsonNode vizCreatedBy = viz.get("createdBy");
    assertNotNull(vizCreatedBy, "Visualization createdBy should exist");
    assertEquals(
        "M5zQapPyTZI", vizCreatedBy.get("id").asText(), "Visualization createdBy id should match");
    assertEquals(
        "admin",
        vizCreatedBy.get("username").asText(),
        "Visualization createdBy username should match");

    JsonNode vizLastUpdatedBy = viz.get("lastUpdatedBy");
    assertNotNull(vizLastUpdatedBy, "Visualization lastUpdatedBy should exist");
    assertEquals(
        "admin",
        vizLastUpdatedBy.get("username").asText(),
        "Visualization lastUpdatedBy username should match");

    // Assert Visualization Sharing
    JsonNode vizSharing = viz.get("sharing");
    assertNotNull(vizSharing, "Visualization sharing should exist");
    assertFalse(
        vizSharing.get("external").asBoolean(), "Visualization sharing external should be false");
    assertEquals(
        "--------", vizSharing.get("public").asText(), "Visualization sharing public should match");

    // Assert Visualization Configuration Properties
    assertEquals("NONE", viz.get("regressionType").asText(), "RegressionType should match");
    assertEquals("NORMAL", viz.get("displayDensity").asText(), "DisplayDensity should match");
    assertEquals("NORMAL", viz.get("fontSize").asText(), "FontSize should match");
    assertEquals(0, viz.get("sortOrder").asInt(), "SortOrder should be 0");
    assertEquals(0, viz.get("topLimit").asInt(), "TopLimit should be 0");
    assertEquals(
        "SPACE", viz.get("digitGroupSeparator").asText(), "DigitGroupSeparator should match");
    assertEquals("NONE", viz.get("hideEmptyRowItems").asText(), "HideEmptyRowItems should match");

    // Assert Visualization Boolean Properties
    assertFalse(viz.get("hideEmptyRows").asBoolean(), "HideEmptyRows should be false");
    assertFalse(viz.get("showHierarchy").asBoolean(), "ShowHierarchy should be false");
    assertFalse(
        viz.get("userOrganisationUnit").asBoolean(), "UserOrganisationUnit should be false");
    assertFalse(
        viz.get("userOrganisationUnitChildren").asBoolean(),
        "UserOrganisationUnitChildren should be false");
    assertFalse(
        viz.get("userOrganisationUnitGrandChildren").asBoolean(),
        "UserOrganisationUnitGrandChildren should be false");
    assertFalse(viz.get("completedOnly").asBoolean(), "CompletedOnly should be false");
    assertFalse(viz.get("skipRounding").asBoolean(), "SkipRounding should be false");
    assertFalse(viz.get("hideLegend").asBoolean(), "HideLegend should be false");
    assertFalse(
        viz.get("noSpaceBetweenColumns").asBoolean(), "NoSpaceBetweenColumns should be false");
    assertFalse(viz.get("cumulativeValues").asBoolean(), "CumulativeValues should be false");
    assertFalse(
        viz.get("percentStackedValues").asBoolean(), "PercentStackedValues should be false");
    assertFalse(viz.get("showData").asBoolean(), "ShowData should be false");
    assertFalse(viz.get("colTotals").asBoolean(), "ColTotals should be false");
    assertFalse(viz.get("rowTotals").asBoolean(), "RowTotals should be false");
    assertFalse(viz.get("rowSubTotals").asBoolean(), "RowSubTotals should be false");
    assertFalse(viz.get("colSubTotals").asBoolean(), "ColSubTotals should be false");
    assertFalse(viz.get("hideTitle").asBoolean(), "HideTitle should be false");
    assertFalse(viz.get("hideSubtitle").asBoolean(), "HideSubtitle should be false");
    assertFalse(viz.get("showDimensionLabels").asBoolean(), "ShowDimensionLabels should be false");
    assertFalse(viz.get("regression").asBoolean(), "Regression should be false");
    assertFalse(viz.get("hideEmptyColumns").asBoolean(), "HideEmptyColumns should be false");
    assertFalse(viz.get("fixColumnHeaders").asBoolean(), "FixColumnHeaders should be false");
    assertFalse(viz.get("fixRowHeaders").asBoolean(), "FixRowHeaders should be false");

    // Assert Visualization rawPeriods Array
    JsonNode exportedRawPeriods = viz.get("rawPeriods");
    assertNotNull(exportedRawPeriods, "RawPeriods should exist");
    assertTrue(exportedRawPeriods.isArray(), "RawPeriods should be an array");
    assertEquals(2, exportedRawPeriods.size(), "RawPeriods should have 2 elements");
    assertEquals(
        "LAST_5_YEARS", exportedRawPeriods.get(0).asText(), "First rawPeriod should match");
    assertEquals(
        "THIS_BIWEEK", exportedRawPeriods.get(1).asText(), "Second rawPeriod should match");

    // Assert Visualization dataDimensionItems Array
    JsonNode dataDimensionItems = viz.get("dataDimensionItems");
    assertNotNull(dataDimensionItems, "DataDimensionItems should exist");
    assertTrue(dataDimensionItems.isArray(), "DataDimensionItems should be an array");
    assertEquals(2, dataDimensionItems.size(), "DataDimensionItems should have 2 elements");
    assertEquals(
        "DATA_ELEMENT",
        dataDimensionItems.get(0).get("dataDimensionItemType").asText(),
        "First dataDimensionItem type should match");
    assertEquals(
        "DATA_ELEMENT",
        dataDimensionItems.get(1).get("dataDimensionItemType").asText(),
        "Second dataDimensionItem type should match");

    // Assert Empty Arrays in Visualization
    String[] emptyArrayFields = {
      "translations",
      "favorites",
      "filterDimensions",
      "organisationUnits",
      "periods",
      "dataElementGroupSetDimensions",
      "organisationUnitGroupSetDimensions",
      "organisationUnitLevels",
      "categoryDimensions",
      "categoryOptionGroupSetDimensions",
      "itemOrganisationUnitGroups",
      "subscribers",
      "columnDimensions",
      "rowDimensions",
      "yearlySeries",
      "attributeValues",
      "sorting",
      "series",
      "optionalAxes",
      "icons",
      "axes"
    };

    for (String fieldName : emptyArrayFields) {
      JsonNode arrayField = viz.get(fieldName);
      assertNotNull(arrayField, fieldName + " should exist");
      assertTrue(arrayField.isArray(), fieldName + " should be an array");
      assertEquals(0, arrayField.size(), fieldName + " should be empty");
    }
  }

  protected Dashboard createDashboardWithItem(String name, Sharing sharing) {
    DashboardItem dashboardItem = createDashboardItem("A");
    Dashboard dashboard = new Dashboard();
    dashboard.setName("dashboard" + name);
    dashboard.setSharing(sharing);
    dashboard.getItems().add(dashboardItem);
    return dashboard;
  }

  protected DashboardItem createDashboardItem(String name) {
    DashboardItem dashboardItem = new DashboardItem();
    dashboardItem.setName("dashboardItem" + name);
    dashboardItem.setAutoFields();
    return dashboardItem;
  }

  // @Test
  // TODO Fix this
  public void testSkipSharing() {
    MetadataExportParams params = new MetadataExportParams();
    params.setSkipSharing(true);
    params.setClasses(Sets.newHashSet(DataElement.class));
    User user = makeUser("A");
    UserGroup group = createUserGroup('A', Sets.newHashSet(user));
    DataElement de1 = createDataElement('A');
    DataElement de2 = createDataElement('B');
    DataElement de3 = createDataElement('C');
    DataElement de4 = createDataElement('D');
    DataElement de5 = createDataElement('E');
    de1.getSharing().setUserAccesses(Sets.newHashSet(new UserAccess(user, "rwrwrwrw")));
    de2.setPublicAccess("rwrwrwrw");
    de3.setCreatedBy(user);
    de4.getSharing().setUserGroupAccess(Sets.newHashSet(new UserGroupAccess(group, "rwrwrwrw")));
    de5.setExternalAccess(true);
    manager.save(user);
    manager.save(group);
    manager.save(de1);
    manager.save(de2);
    manager.save(de3);
    manager.save(de4);
    manager.save(de5);
    Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata =
        metadataExportService.getMetadata(params);
    assertEquals(5, metadata.get(DataElement.class).size());
    metadata.get(DataElement.class).stream().forEach(element -> checkSharingFields(element));
  }

  private void checkSharingFields(IdentifiableObject object) {
    assertTrue(object.getSharing().getUsers().isEmpty());
    assertEquals("--------", object.getSharing().getPublicAccess());
    // assertNull( object.getUser() );
    assertTrue(object.getSharing().getUserGroups().isEmpty());
    // assertFalse( object.getExternalAccess() );
  }
}
