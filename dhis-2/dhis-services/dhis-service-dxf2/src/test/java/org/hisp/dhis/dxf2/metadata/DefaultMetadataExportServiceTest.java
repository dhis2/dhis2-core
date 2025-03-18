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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionGroup;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.SystemUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultMetadataExportService}.
 *
 * @author Volker Schmidt
 */
@ExtendWith(MockitoExtension.class)
class DefaultMetadataExportServiceTest {
  @Mock private SchemaService schemaService;

  @Mock private ProgramRuleService programRuleService;

  @Mock private ProgramRuleVariableService programRuleVariableService;

  @Mock private QueryService queryService;

  @InjectMocks private DefaultMetadataExportService service;

  @Test
  void getParamsFromMapIncludedSecondary() {
    when(schemaService.getSchemaByPluralName(Mockito.eq("jobConfigurations")))
        .thenReturn(new Schema(JobConfiguration.class, "jobConfiguration", "jobConfigurations"));
    when(schemaService.getSchemaByPluralName(Mockito.eq("options")))
        .thenReturn(new Schema(Option.class, "option", "options"));

    final Map<String, List<String>> params = new HashMap<>();
    params.put("jobConfigurations", Collections.singletonList("true"));
    params.put("options", Collections.singletonList("true"));

    MetadataExportParams exportParams = service.getParamsFromMap(params);
    Assertions.assertTrue(exportParams.getClasses().contains(JobConfiguration.class));
    Assertions.assertTrue(exportParams.getClasses().contains(Option.class));
  }

  @Test
  void getParamsFromMapNotIncludedSecondary() {
    when(schemaService.getSchemaByPluralName(Mockito.eq("jobConfigurations")))
        .thenReturn(new Schema(JobConfiguration.class, "jobConfiguration", "jobConfigurations"));
    when(schemaService.getSchemaByPluralName(Mockito.eq("options")))
        .thenReturn(new Schema(Option.class, "option", "options"));

    final Map<String, List<String>> params = new HashMap<>();
    params.put("jobConfigurations", Arrays.asList("true", "false"));
    params.put("options", Collections.singletonList("true"));

    MetadataExportParams exportParams = service.getParamsFromMap(params);
    Assertions.assertFalse(exportParams.getClasses().contains(JobConfiguration.class));
    Assertions.assertTrue(exportParams.getClasses().contains(Option.class));
  }

  @Test
  void getParamsFromMapNoSecondary() {
    when(schemaService.getSchemaByPluralName(Mockito.eq("options")))
        .thenReturn(new Schema(Option.class, "option", "options"));

    final Map<String, List<String>> params = new HashMap<>();
    params.put("options", Collections.singletonList("true"));

    MetadataExportParams exportParams = service.getParamsFromMap(params);
    Assertions.assertFalse(exportParams.getClasses().contains(JobConfiguration.class));
    Assertions.assertTrue(exportParams.getClasses().contains(Option.class));
  }

  @Test
  void testGetMetadataWithDependenciesForDashboardWithMapView() {
    MapView mapView = new MapView();
    mapView.setName("mapViewA");

    org.hisp.dhis.mapping.Map map = new org.hisp.dhis.mapping.Map();
    map.setName("mapA");
    map.getMapViews().add(mapView);

    DashboardItem item = new DashboardItem();
    item.setName("itemA");
    item.setMap(map);

    Dashboard dashboard = new Dashboard("dashboardA");
    dashboard.getItems().add(item);

    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> result =
        service.getMetadataWithDependencies(dashboard);
    // MapView is embedded object, it must not be included at top level
    assertNull(result.get(MapView.class));
    assertNotNull(result.get(Dashboard.class));
    assertNotNull(result.get(org.hisp.dhis.mapping.Map.class));
    Set<IdentifiableObject> setMap = result.get(org.hisp.dhis.mapping.Map.class);
    assertEquals(1, setMap.size());
    org.hisp.dhis.mapping.Map mapResult = (org.hisp.dhis.mapping.Map) setMap.iterator().next();
    assertEquals(1, mapResult.getMapViews().size());
    assertEquals(mapView.getName(), mapResult.getMapViews().get(0).getName());
  }

  @Test
  void testExportProgramWithOptionGroup() {
    Program program = new Program();
    program.setName("programA");

    OptionSet optionSet = new OptionSet();
    optionSet.setName("optionSetA");

    OptionGroup optionGroup = new OptionGroup();
    optionGroup.setName("optionGroupA");
    optionGroup.setOptionSet(optionSet);

    ProgramRuleAction programRuleAction = new ProgramRuleAction();
    programRuleAction.setName("programRuleActionA");
    programRuleAction.setOptionGroup(optionGroup);

    ProgramRule programRule = new ProgramRule();
    programRule.setName("programRuleA");
    programRule.getProgramRuleActions().add(programRuleAction);
    programRule.setProgram(program);

    when(programRuleService.getProgramRule(program)).thenReturn(List.of(programRule));

    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> result =
        service.getMetadataWithDependencies(program);

    assertNotNull(result.get(ProgramRuleAction.class));
    assertNotNull(result.get(OptionGroup.class));
    assertNotNull(result.get(OptionSet.class));
  }

  @Test
  @DisplayName(
      "Deprecated Analytic classes (EventChart and EventReport) should have their schemas removed from metadata export")
  void deprecatedAnalyticClassesSchemaRemovedTest() {
    // given
    MetadataExportParams params = new MetadataExportParams();
    List<Schema> schemas =
        List.of(
            new Schema(EventReport.class, "eventReport", "eventReports"),
            new Schema(EventChart.class, "eventChart", "eventCharts"),
            new Schema(EventVisualization.class, "eventVisualization", "eventVisualizations"),
            new Schema(DataElement.class, "dataElement", "dataElements"),
            new Schema(Program.class, "program", "programs"));
    schemas.forEach(s -> s.setPersisted(true));

    when(queryService.getQueryFromUrl(any(), any())).thenReturn(Query.of(null));

    // return 5 schemas, including the 2 for the deprecated classes EventChart & EventReport
    when(schemaService.getMetadataSchemas()).thenReturn(schemas);

    // when
    params.setCurrentUserDetails(new SystemUser());
    service.getMetadata(params);

    // then
    assertEquals(
        3, params.getClasses().size(), "EventChart and EventReport classes should not be present");
  }

  @ParameterizedTest
  @MethodSource(value = "schemaSources")
  @DisplayName("Deprecated Analytic schema predicate returns the correct result")
  void deprecatedAnalyticSchemaPredicateTest(Schema schema, boolean expectedResult) {
    // when
    boolean actualResult = DefaultMetadataExportService.DEPRECATED_ANALYTICS_SCHEMAS.test(schema);

    // then
    assertEquals(expectedResult, actualResult, "expected result should match predicate result");
  }

  private static Stream<Arguments> schemaSources() {
    return Stream.of(
        arguments(new Schema(EventReport.class, "eventReport", "eventReports"), false),
        arguments(new Schema(EventChart.class, "eventChart", "eventCharts"), false),
        arguments(
            new Schema(EventVisualization.class, "eventVisualization", "eventVisualizations"),
            true),
        arguments(new Schema(DataElement.class, "dataElement", "dataElements"), true),
        arguments(new Schema(Program.class, "program", "programs"), true),
        arguments(new Schema(CategoryCombo.class, "categoryCombo", "categoryCombos"), true));
  }
}
