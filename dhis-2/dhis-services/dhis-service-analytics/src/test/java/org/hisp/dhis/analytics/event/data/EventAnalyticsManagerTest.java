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
package org.hisp.dhis.analytics.event.data;

import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.common.DimensionConstants.OPTION_SEP;
import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.common.QueryOperator.NEQ;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.AGGREGATE;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.QUERY;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_DATABASE;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createOrganisationUnitGroup;
import static org.hisp.dhis.test.TestBase.createPeriodDimensions;
import static org.hisp.dhis.test.TestBase.getDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.programindicator.DefaultProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagInfoInitializer;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageDatePeriodBucketSqlRenderer;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageOrgUnitSqlService;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageQueryItemClassifier;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageQuerySqlFacade;
import org.hisp.dhis.analytics.event.data.stage.StageQuerySqlFacade;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.RequestTypeAware.EndpointItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.ClickHouseAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.DorisAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.external.conf.DefaultDhisConfigurationProvider;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EventAnalyticsManagerTest extends EventAnalyticsTest {
  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private ExecutionPlanStore executionPlanStore;

  @Mock private OrganisationUnitResolver organisationUnitResolver;

  @Mock private DataElementService dataElementService;

  @Mock private PiDisagInfoInitializer piDisagInfoInitializer;

  @Mock private PiDisagQueryGenerator piDisagQueryGenerator;

  @Mock private ProgramIndicatorService programIndicatorService;

  private QueryItemFilterBuilder filterBuilder;

  @Spy private PostgreSqlAnalyticsSqlBuilder sqlBuilder = new PostgreSqlAnalyticsSqlBuilder();

  private JdbcEventAnalyticsManager subject;

  @Captor private ArgumentCaptor<String> sql;

  private static final String TABLE_NAME = "analytics_event";

  @Mock private SystemSettingsService systemSettingsService;

  @Mock private DefaultDhisConfigurationProvider config;

  @Mock private SystemSettings mockSettings;

  @Spy
  private PostgreSqlAnalyticsSqlBuilder analyticsSqlBuilder = new PostgreSqlAnalyticsSqlBuilder();

  private static final String BASE_COLUMNS =
      "ax.event, ax.ps, ax.occurreddate, "
          + "ax.createdbydisplayname, ax.lastupdatedbydisplayname, "
          + "ax.lastupdated, ax.created, ax.completeddate, ax.scheduleddate";

  private static final String REGISTRATION_COLUMNS =
      ", ax.enrollmentdate, ax.enrollmentoccurreddate, ax.trackedentity, ax.enrollment";

  private static final String GEO_AND_OU_COLUMNS =
      ", ST_AsGeoJSON(coalesce(ax.\"eventgeometry\", ax.\"enrollmentgeometry\", "
          + "ax.\"tegeometry\", ax.\"ougeometry\"), 6) as geometry, "
          + "ST_AsGeoJSON(coalesce(ax.enrollmentgeometry), 6) as enrollmentgeometry, "
          + "ax.longitude, ax.latitude, ax.ouname, ax.ounamehierarchy, ax.oucode, ax.enrollmentstatus, ax.eventstatus";

  private static final String DEFAULT_COLUMNS_WITH_REGISTRATION =
      BASE_COLUMNS + REGISTRATION_COLUMNS + GEO_AND_OU_COLUMNS;

  private static final String DEFAULT_COLUMNS_WITHOUT_REGISTRATION =
      BASE_COLUMNS + GEO_AND_OU_COLUMNS;

  @BeforeEach
  public void setUp() {
    filterBuilder = new QueryItemFilterBuilder(organisationUnitResolver, sqlBuilder);

    subject = createEventAnalyticsManager(sqlBuilder, "postgresql");

    when(jdbcTemplate.queryForRowSet(anyString())).thenReturn(this.rowSet);
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn("postgresql");
    when(systemSettingsService.getCurrentSettings()).thenReturn(mockSettings);
  }

  @Test
  void verifyGetEventSqlWithProgramWithNoRegistration() {
    mockEmptyRowSet();

    this.programA.setProgramType(ProgramType.WITHOUT_REGISTRATION);

    subject.getEvents(createRequestParams(), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "select "
            + DEFAULT_COLUMNS_WITHOUT_REGISTRATION
            + ", ax.\"quarterly\" as quarterly, ax.\"ou\" as ou from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and ax.\"uidlevel1\" in ('ouabcdefghA') limit 101";

    assertThat(sql.getValue(), is(expected));
  }

  @Test
  void verifyGetEventQueryWithUnlimitedAnalyticsPageSizeAndPageSize50() {
    mockGivenRowsRowSet(101);
    EventQueryParams requestParams =
        createRequestParamsBuilder().withEndpointAction(QUERY).withPageSize(100).build();
    Grid events = subject.getEvents(requestParams, createGrid(), 0);
    assertThat(events.getRows(), hasSize(100));
    assertThat(events.hasLastDataRow(), is(false));
  }

  @Test
  void verifyGetEventQueryWithUnlimitedAnalyticsPageSizeAndNoPageSize() {
    mockGivenRowsRowSet(101);
    EventQueryParams requestParams =
        createRequestParamsBuilder().withEndpointAction(QUERY).withPageSize(null).build();
    Grid events = subject.getEvents(requestParams, createGrid(), 0);
    assertThat(events.getRows(), hasSize(101));
    assertThat(events.hasLastDataRow(), is(true));
  }

  @Test
  void verifyGetEventAggregateIsNotPaginatedAndIsLastPageTrue() {
    mockGivenRowsRowSet(500);
    EventQueryParams requestParams =
        createRequestParamsBuilder().withEndpointAction(AGGREGATE).withPageSize(100).build();
    Grid events = subject.getEvents(requestParams, createGrid(), 0);
    assertThat(events.getRows(), hasSize(500));
    assertThat(events.hasLastDataRow(), is(true));
  }

  @Test
  void verifyGetEventSqlWithOrgUnitTypeDataElement() {
    mockEmptyRowSet();

    DataElement dataElement = createDataElement('a');
    QueryItem queryItem =
        new QueryItem(
            dataElement,
            this.programA,
            null,
            ValueType.ORGANISATION_UNIT,
            AggregationType.SUM,
            null);

    subject.getEvents(createRequestParams(queryItem), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "select "
            + DEFAULT_COLUMNS_WITH_REGISTRATION
            + ", ax.\"quarterly\" as quarterly, ax.\"ou\" as ou, ax.\""
            + dataElement.getUid()
            + "_name"
            + "\" from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and ax.\"uidlevel1\" in ('ouabcdefghA')"
            + " limit 101";

    assertThat(sql.getValue(), is(expected));
  }

  @Test
  void verifyGetEventSqlWithProgram() {
    Grid grid = createGrid();
    int unlimited = 0;

    mockEmptyRowSet();

    subject.getEvents(createRequestParams(), grid, unlimited);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\" as quarterly, ax.\"ou\" as ou from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and ax.\"uidlevel1\" in ('ouabcdefghA')";

    assertSql(expected, sql.getValue());
    assertThat(sql.getValue(), not(containsString("geometrySource")));
    assertTrue(grid.hasLastDataRow());
  }

  @Test
  void verifyGetEventSqlWithGeometrySourceWhenFallbackActive() {
    mockEmptyRowSet();

    EventQueryParams params =
        createRequestParamsBuilder()
            .withCoordinateFields(List.of("eventgeometry", "ougeometry"))
            .withGeometrySources(
                List.of(
                    new EventQueryParams.GeometrySource("eventgeometry", "psigeometry"),
                    new EventQueryParams.GeometrySource("ougeometry", "ougeometry")))
            .build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertThat(
        sql.getValue(),
        containsString(
            "ST_AsGeoJSON(coalesce(ax.\"eventgeometry\", ax.\"ougeometry\"), 6) as geometry, "
                + "(case when ax.\"eventgeometry\" IS not NULL then 'psigeometry' "
                + "when ax.\"ougeometry\" IS not NULL then 'ougeometry' end) as geometrySource, "
                + "ST_AsGeoJSON(coalesce(ax.enrollmentgeometry), 6) as enrollmentgeometry"));
  }

  @Test
  void verifyGetEventSqlKeepsNonStageTimeFiltersWithStageSpecificEventDate() {
    mockEmptyRowSet();

    QueryItem stageEventDateItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME),
            programA,
            null,
            ValueType.DATE,
            AggregationType.NONE,
            null);
    stageEventDateItem.setProgramStage(programStage);
    stageEventDateItem.addFilter(new QueryFilter(QueryOperator.GE, "2026-01-01"));
    stageEventDateItem.addFilter(new QueryFilter(QueryOperator.LT, "2027-01-01"));

    PeriodDimension createdPeriod = createPeriodDimensions("2017").get(0).setDateField("CREATED");
    PeriodDimension lastUpdatedPeriod =
        createPeriodDimensions("2022").get(0).setDateField("LAST_UPDATED");

    EventQueryParams params =
        createRequestParamsBuilder()
            .withEndpointItem(EndpointItem.EVENT)
            .withEndpointAction(QUERY)
            .withOutputType(EventOutputType.EVENT)
            .withPeriods(List.of(createdPeriod, lastUpdatedPeriod), "yearly")
            .addItem(stageEventDateItem)
            .withStartEndDatesForPeriods()
            .build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertThat(sql.getValue(), containsString("ax.\"created\" >= '2017-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"created\" < '2018-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"lastupdated\" >= '2022-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"lastupdated\" < '2023-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"occurreddate\" >= '2026-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"occurreddate\" < '2027-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"ps\" = '" + programStage.getUid() + "'"));
  }

  @Test
  void verifyGetEventSqlKeepsExplicitLastUpdatedTimeFieldWithStageSpecificEventDate() {
    mockEmptyRowSet();

    QueryItem stageEventDateItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME),
            programA,
            null,
            ValueType.DATE,
            AggregationType.NONE,
            null);
    stageEventDateItem.setProgramStage(programStage);
    stageEventDateItem.addFilter(new QueryFilter(QueryOperator.GE, "2026-01-01"));
    stageEventDateItem.addFilter(new QueryFilter(QueryOperator.LT, "2027-01-01"));

    EventQueryParams params =
        createRequestParamsBuilder()
            .withEndpointItem(EndpointItem.EVENT)
            .withEndpointAction(QUERY)
            .withOutputType(EventOutputType.EVENT)
            .withStartDate(getDate(2017, 1, 1))
            .withEndDate(getDate(2017, 12, 31))
            .withTimeField(TimeField.LAST_UPDATED.name())
            .addItem(stageEventDateItem)
            .build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertThat(sql.getValue(), containsString("ax.\"lastupdated\" >= '2017-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"lastupdated\" < '2018-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"occurreddate\" >= '2026-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"occurreddate\" < '2027-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"ps\" = '" + programStage.getUid() + "'"));
  }

  @Test
  void verifyGetEventSqlKeepsExplicitScheduledDateTimeFieldWithStageSpecificEventDate() {
    mockEmptyRowSet();

    QueryItem stageEventDateItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME),
            programA,
            null,
            ValueType.DATE,
            AggregationType.NONE,
            null);
    stageEventDateItem.setProgramStage(programStage);
    stageEventDateItem.addFilter(new QueryFilter(QueryOperator.GE, "2026-01-01"));
    stageEventDateItem.addFilter(new QueryFilter(QueryOperator.LT, "2027-01-01"));

    EventQueryParams params =
        createRequestParamsBuilder()
            .withEndpointItem(EndpointItem.EVENT)
            .withEndpointAction(QUERY)
            .withOutputType(EventOutputType.EVENT)
            .withStartDate(getDate(2017, 1, 1))
            .withEndDate(getDate(2017, 12, 31))
            .withTimeField(TimeField.SCHEDULED_DATE.name())
            .addItem(stageEventDateItem)
            .build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertThat(sql.getValue(), containsString("ax.\"scheduleddate\" >= '2017-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"scheduleddate\" < '2018-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"occurreddate\" >= '2026-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"occurreddate\" < '2027-01-01'"));
    assertThat(sql.getValue(), containsString("ax.\"ps\" = '" + programStage.getUid() + "'"));
  }

  @Test
  void verifyExperimentalQueryDoesNotDuplicateStageSpecificDateCondition() {
    mockEmptyRowSet();

    QueryItem stageEventDateItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME),
            programA,
            null,
            ValueType.DATE,
            AggregationType.NONE,
            null);
    stageEventDateItem.setProgramStage(programStage);
    stageEventDateItem.addFilter(new QueryFilter(QueryOperator.GE, "2026-01-01"));
    stageEventDateItem.addFilter(new QueryFilter(QueryOperator.LE, "2026-12-31"));

    PeriodDimension createdPeriod = createPeriodDimensions("2017").get(0).setDateField("CREATED");
    PeriodDimension lastUpdatedPeriod =
        createPeriodDimensions("2022").get(0).setDateField("LAST_UPDATED");

    EventQueryParams params =
        createRequestParamsBuilder()
            .withEndpointItem(EndpointItem.EVENT)
            .withEndpointAction(QUERY)
            .withOutputType(EventOutputType.EVENT)
            .withPeriods(List.of(createdPeriod, lastUpdatedPeriod), "yearly")
            .addItem(stageEventDateItem)
            .withStartEndDatesForPeriods()
            .build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertEquals(1, countMatches(sql.getValue(), "ax.\"created\" >= '2017-01-01'"));
    assertEquals(1, countMatches(sql.getValue(), "ax.\"lastupdated\" >= '2022-01-01'"));
    assertEquals(1, countMatches(sql.getValue(), "ax.\"occurreddate\" >= '2026-01-01'"));
    assertEquals(1, countMatches(sql.getValue(), "ax.\"occurreddate\" <= '2026-12-31'"));
    assertEquals(1, countMatches(sql.getValue(), "ax.\"ps\" = '" + programStage.getUid() + "'"));
  }

  @Test
  void verifyExperimentalQueryKeepsScheduledDateWithStageSpecificEventDate() {
    mockEmptyRowSet();

    QueryItem stageEventDateItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME),
            programA,
            null,
            ValueType.DATE,
            AggregationType.NONE,
            null);
    stageEventDateItem.setProgramStage(programStage);
    stageEventDateItem.addFilter(new QueryFilter(QueryOperator.GE, "2026-01-01"));
    stageEventDateItem.addFilter(new QueryFilter(QueryOperator.LE, "2026-12-31"));

    PeriodDimension scheduledDatePeriod =
        createPeriodDimensions("2022").get(0).setDateField(TimeField.SCHEDULED_DATE.name());

    EventQueryParams params =
        createRequestParamsBuilder()
            .withEndpointItem(EndpointItem.EVENT)
            .withEndpointAction(QUERY)
            .withOutputType(EventOutputType.EVENT)
            .withPeriods(List.of(scheduledDatePeriod), "yearly")
            .addItem(stageEventDateItem)
            .withStartEndDatesForPeriods()
            .build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertEquals(1, countMatches(sql.getValue(), "ax.\"scheduleddate\" >= '2022-01-01'"));
    assertEquals(1, countMatches(sql.getValue(), "ax.\"scheduleddate\" < '2023-01-01'"));
    assertEquals(1, countMatches(sql.getValue(), "ax.\"occurreddate\" >= '2026-01-01'"));
    assertEquals(1, countMatches(sql.getValue(), "ax.\"occurreddate\" <= '2026-12-31'"));
    assertEquals(1, countMatches(sql.getValue(), "ax.\"ps\" = '" + programStage.getUid() + "'"));
  }

  @Test
  void verifyGetEventsSqlWithProgramAndProgramStage() {
    mockEmptyRowSet();

    subject.getEvents(createRequestParams(programStage), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\" as quarterly, ax.\"ou\" as ou from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid()
            + "' limit 101";

    assertSql(expected, sql.getValue());
  }

  @Test
  void verifyGetEventsWithProgramStageAndNumericDataElement() {
    mockEmptyRowSet();

    subject.getEvents(createRequestParams(programStage, ValueType.INTEGER), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\" as quarterly, ax.\"ou\" as ou, ax.\"fWIAEtYVEGk\" from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid()
            + "' limit 101";

    assertSql(expected, sql.getValue());
  }

  @Test
  void verifyGetEventsWithProgramStageAndNumericDataElementAndFilter() {
    mockEmptyRowSet();

    subject.getEvents(
        createRequestParamsWithFilter(programStage, ValueType.INTEGER), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\" as quarterly, ax.\"ou\" as ou, ax.\"fWIAEtYVEGk\" from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid()
            + "' and ax.\"fWIAEtYVEGk\" > '10' limit 101";

    assertSql(expected, sql.getValue());
  }

  @Test
  void verifyGetEventsWithProgramStatusAndEventStatusParams() {
    mockEmptyRowSet();

    subject.getEvents(createRequestParamsWithStatuses(), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\" as quarterly, ax.\"ou\" as ou from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and ax.\"uidlevel1\" in ('ouabcdefghA')"
            + " and ax.\"enrollmentstatus\" in ('ACTIVE','COMPLETED') and eventstatus in ('SCHEDULE') limit 101";

    assertSql(expected, sql.getValue());
  }

  @Test
  void verifyGetEventsWithScheduledDateTimeFieldParam() {
    mockEmptyRowSet();

    subject.getEvents(createRequestParamsWithTimeField("SCHEDULED_DATE"), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ps.\"quarterly\" as quarterly, ax.\"ou\" as ou from "
            + getTable(programA.getUid())
            + " as ax "
            + "where (ps.\"quarterly\" in ('2000Q1') ) and ax.\"uidlevel1\" "
            + "in ('ouabcdefghA') and ax.\"enrollmentstatus\" in ('ACTIVE','COMPLETED') limit 101";

    assertSql(expected, sql.getValue());
  }

  @Test
  void verifyGetEventsWithLastUpdatedTimeFieldParam() {
    mockEmptyRowSet();

    subject.getEvents(createRequestParamsWithTimeField("LAST_UPDATED"), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\" as quarterly, ax.\"ou\" as ou from "
            + getTable(programA.getUid())
            + " as ax "
            + "where ((( ax.\"lastupdated\" >= '2000-01-01' and ax.\"lastupdated\" < '2000-04-01') )) and ax.\"uidlevel1\" "
            + "in ('ouabcdefghA') and ax.\"enrollmentstatus\" in ('ACTIVE','COMPLETED') limit 101";

    assertSql(expected, sql.getValue());
  }

  @Test
  void verifyGetEventsWithMissingValueEqFilter() {
    String expected = "ax.\"fWIAEtYVEGk\" is null";
    testIt(
        EQ,
        NV,
        Collections.singleton((capturedSql) -> assertThat(capturedSql, containsString(expected))));
  }

  @Test
  void verifyGetEventsWithMissingValueNeqFilter() {
    String expected = "ax.\"fWIAEtYVEGk\" is not null";
    testIt(
        NEQ,
        NV,
        Collections.singleton((capturedSql) -> assertThat(capturedSql, containsString(expected))));
  }

  @Test
  void verifyGetEventsWithMissingValueAndNumericValuesInFilter() {
    String numericValues = String.join(OPTION_SEP, "10", "11", "12");
    String expected =
        "(ax.\"fWIAEtYVEGk\" in ("
            + String.join(",", numericValues.split(OPTION_SEP))
            + ") or ax.\"fWIAEtYVEGk\" is null )";
    testIt(
        IN,
        numericValues + OPTION_SEP + NV,
        Collections.singleton((capturedSql) -> assertThat(capturedSql, containsString(expected))));
  }

  @Test
  void verifyGetEventsWithoutMissingValueAndNumericValuesInFilter() {
    String numericValues = String.join(OPTION_SEP, "10", "11", "12");
    String expected =
        "ax.\"fWIAEtYVEGk\" in (" + String.join(",", numericValues.split(OPTION_SEP)) + ")";
    testIt(
        IN,
        numericValues,
        Collections.singleton((capturedSql) -> assertThat(capturedSql, containsString(expected))));
  }

  @Test
  void verifyGetEventsWithOnlyMissingValueInFilter() {
    String expected = "ax.\"fWIAEtYVEGk\" is null";
    String unexpected = "(ax.\"fWIAEtYVEGk\" in (";
    testIt(
        IN,
        NV,
        List.of(
            (capturedSql) -> assertThat(capturedSql, containsString(expected)),
            (capturedSql) -> assertThat(capturedSql, not(containsString(unexpected)))));
  }

  private void testIt(
      QueryOperator operator, String filter, Collection<Consumer<String>> assertions) {
    mockEmptyRowSet();

    subject.getEvents(
        createRequestParamsWithFilter(programStage, ValueType.INTEGER, operator, filter),
        createGrid(),
        100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertions.forEach(consumer -> consumer.accept(sql.getValue()));
  }

  @Test
  void verifyGetEventsWithProgramStageAndTextDataElement() {
    mockEmptyRowSet();

    subject.getEvents(createRequestParams(programStage, ValueType.TEXT), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\" as quarterly, ax.\"ou\" as ou, ax.\"fWIAEtYVEGk\" from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid()
            + "' limit 101";

    assertSql(expected, sql.getValue());
  }

  @Test
  void verifyGetEventsWithProgramStageAndTextDataElementAndFilter() {
    mockEmptyRowSet();

    subject.getEvents(
        createRequestParamsWithFilter(programStage, ValueType.TEXT), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "ax.\"quarterly\" as quarterly, ax.\"ou\" as ou, ax.\"fWIAEtYVEGk\" from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid()
            + "' and ax.\"fWIAEtYVEGk\" > '10' limit 101";

    assertSql(expected, sql.getValue());
  }

  @Test
  void verifyGetAggregatedEventQuery() {
    mockRowSet();

    when(rowSet.getString("fWIAEtYVEGk")).thenReturn("2000");
    when(rowSet.getString("ax.\"fWIAEtYVEGk\"")).thenReturn("2000");
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    Grid resultGrid =
        subject.getAggregatedEventData(
            createRequestParams(programStage, ValueType.INTEGER), createGrid(), 200000);

    assertThat(resultGrid.getRows(), hasSize(1));
    assertThat(resultGrid.getRow(0), hasSize(4));
    assertThat(resultGrid.getRow(0).get(0), is("2000"));
    assertThat(resultGrid.getRow(0).get(1), is("2017Q1"));
    assertThat(resultGrid.getRow(0).get(2), is("Sierra Leone"));
    assertThat(resultGrid.getRow(0).get(3), is(100));

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String expected =
        "select count(ax.\"event\") as value,ax.\"quarterly\" as quarterly,ax.\"ou\" as ou,ax.\"fWIAEtYVEGk\" from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid()
            + "' group by ax.\"quarterly\", ax.\"ou\", ax.\"fWIAEtYVEGk\" limit 200001";

    assertThat(sql.getValue(), is(expected));
  }

  @Test
  void verifyGetAggregatedEventQueryWithFilter() {

    when(rowSet.getString("fWIAEtYVEGk")).thenReturn("2000");
    when(rowSet.getString("ax.\"fWIAEtYVEGk\"")).thenReturn("2000");
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    mockRowSet();

    Grid resultGrid =
        subject.getAggregatedEventData(
            createRequestParamsWithFilter(programStage, ValueType.TEXT), createGrid(), 200000);

    assertThat(resultGrid.getRows(), hasSize(1));
    assertThat(resultGrid.getRow(0), hasSize(4));
    assertThat(resultGrid.getRow(0).get(0), is("2000"));
    assertThat(resultGrid.getRow(0).get(1), is("2017Q1"));
    assertThat(resultGrid.getRow(0).get(2), is("Sierra Leone"));
    assertThat(resultGrid.getRow(0).get(3), is(100));

    verify(jdbcTemplate).queryForRowSet(sql.capture());
    String expected =
        "select count(ax.\"event\") as value,ax.\"quarterly\" as quarterly,ax.\"ou\" as ou,ax.\"fWIAEtYVEGk\" from "
            + getTable(programA.getUid())
            + " as ax where (ax.\"quarterly\" in ('2000Q1') ) and ax.\"uidlevel1\" in ('ouabcdefghA') and ax.\"ps\" = '"
            + programStage.getUid()
            + "' and ax.\"fWIAEtYVEGk\" > '10'"
            + " group by ax.\"quarterly\", ax.\"ou\", ax.\"fWIAEtYVEGk\" limit 200001";
    assertThat(sql.getValue(), is(expected));
  }

  @Test
  void verifyExperimentalAggregatedEventQueryIncludesStageDateFilters() {
    mockEmptyRowSet();
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    QueryItem stageEventDateItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME),
            programA,
            null,
            ValueType.DATE,
            AggregationType.NONE,
            null);
    stageEventDateItem.setProgramStage(programStage);
    stageEventDateItem.addFilter(new QueryFilter(QueryOperator.GE, "2021-03-01"));
    stageEventDateItem.addFilter(new QueryFilter(QueryOperator.LE, "2021-05-31"));

    EventQueryParams params =
        createRequestParamsBuilder()
            .withEndpointItem(EndpointItem.EVENT)
            .withEndpointAction(AGGREGATE)
            .withOutputType(EventOutputType.EVENT)
            .withAggregateData(true)
            .addItem(stageEventDateItem)
            .build();

    subject.getAggregatedEventData(params, createGrid(), 200000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertThat(sql.getValue(), containsString("ax.\"occurreddate\" >= '2021-03-01'"));
    assertThat(sql.getValue(), containsString("ax.\"occurreddate\" <= '2021-05-31'"));
    assertThat(sql.getValue(), containsString("ax.\"ps\" = '" + programStage.getUid() + "'"));
  }

  @Test
  void verifyFirstAggregationTypeSubquery() {
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);
    verifyFirstOrLastAggregationTypeSubquery(AnalyticsAggregationType.FIRST);
  }

  @Test
  void verifyLastAggregationTypeSubquery() {
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);
    verifyFirstOrLastAggregationTypeSubquery(AnalyticsAggregationType.LAST);
  }

  @Test
  void verifyLastLastOrgUnitAggregationTypeSubquery() {
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    DataElement deX = createDataElement('X');

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withOrganisationUnits(List.of(createOrganisationUnit('A')))
            .withPeriods(createPeriodDimensions("202201"), PeriodTypeEnum.MONTHLY.getName())
            .addDimension(
                new BaseDimensionalObject(
                    "jkYhtGth12t",
                    DimensionType.ORGANISATION_UNIT_GROUP_SET,
                    "Facility type",
                    List.of(createOrganisationUnitGroup('A'))))
            .withOrganisationUnitMode(OrganisationUnitSelectionMode.SELECTED)
            .withValue(deX)
            .withProgram(programA)
            .withTableName(getTable(programA.getUid()))
            .withAggregationType(
                new AnalyticsAggregationType(AggregationType.LAST, AggregationType.LAST))
            .withAggregateData(true)
            .build();

    subject.getAggregatedEventData(params, createGrid(), 200000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String subquery =
        "from (select \"event\",ax.\"deabcdefghX\",\"ou\","
            + "\"monthly\",\"jkYhtGth12t\","
            + "row_number() over (partition by ax.\"ou\",ax.\"jkYhtGth12t\" "
            + "order by ax.\"occurreddate\" desc, ax.\"created\" desc) as pe_rank "
            + "from analytics_event_prabcdefghA as ax "
            + "where ax.\"occurreddate\" >= '2012-01-31' and ax.\"occurreddate\" <= '2022-01-31' "
            + "and ax.\"deabcdefghX\" is not null)";

    assertThat(params.isAggregationType(AggregationType.LAST), is(true));
    assertThat(sql.getValue(), containsString(subquery));
  }

  @Test
  void verifyGetAggregatedEventQueryWithMeasureCriteria() {

    when(rowSet.getString("fWIAEtYVEGk")).thenReturn("2000");
    when(rowSet.getString("ax.\"fWIAEtYVEGk\"")).thenReturn("2000");
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    mockRowSet();

    Grid resultGrid =
        subject.getAggregatedEventData(
            createRequestParamsMeasureCriteria(programStage, ValueType.TEXT), createGrid(), 200000);

    assertThat(resultGrid.getRows(), hasSize(1));
    assertThat(resultGrid.getRow(0), hasSize(4));
    assertThat(resultGrid.getRow(0).get(0), is("2000"));
    assertThat(resultGrid.getRow(0).get(1), is("2017Q1"));
    assertThat(resultGrid.getRow(0).get(2), is("Sierra Leone"));
    assertThat(resultGrid.getRow(0).get(3), is(100));

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    // Verify that the Measure criteria is applied to the query
    assertThat(sql.getValue().trim(), containsString("having"));
    assertThat(
        sql.getValue().trim(),
        containsString("round(count(ax.\"event\"), 10) > ('10.0')::numeric(38,10)"));
    assertThat(
        sql.getValue().trim(),
        containsString("round(count(ax.\"event\"), 10) < ('20.0')::numeric(38,10)"));
  }

  @Test
  void verifyGetAggregatedEventQueryUsesDatePeriodStructureForNonDefaultMonthlyPeriodDimension() {
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    mockEmptyRowSet();

    List<PeriodDimension> periods = createPeriodDimensions("202001");
    periods.forEach(period -> period.setDateField(TimeField.ENROLLMENT_DATE.name()));

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams(programStage, ValueType.INTEGER))
            .withPeriods(periods, "monthly")
            .build();

    subject.getAggregatedEventData(params, createGrid(), 200000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertThat(
        sql.getValue(),
        containsString(
            "(select \"monthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('month', ax.\"enrollmentdate\")::date) as \"monthly\""));
    assertThat(
        sql.getValue(),
        containsString(
            "group by (select \"monthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('month', ax.\"enrollmentdate\")::date), ax.\"ou\", ax.\"fWIAEtYVEGk\""));
  }

  @Test
  void verifyGetAggregatedEventQueryProjectsMultipleStaticDatePeriodDimensions() {
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    mockEmptyRowSet();

    List<PeriodDimension> periods = createPeriodDimensions("202001");
    periods.get(0).setDateField(TimeField.SCHEDULED_DATE.name());
    PeriodDimension lastUpdatedPeriod = createPeriodDimensions("202001").get(0);
    lastUpdatedPeriod.setDateField(TimeField.LAST_UPDATED.name());

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams(programStage, ValueType.INTEGER))
            .withPeriods(List.of(periods.get(0), lastUpdatedPeriod), "monthly")
            .build();

    subject.getAggregatedEventData(params, createGrid(), 200000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    // Both bucket expressions in SELECT
    assertThat(
        sql.getValue(),
        containsString(
            "(select \"monthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('month', ax.\"scheduleddate\")::date) as \"scheduleddate\""));
    assertThat(
        sql.getValue(),
        containsString(
            "(select \"monthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('month', ax.\"lastupdated\")::date) as \"lastupdated\""));
    // Both in GROUP BY
    assertThat(sql.getValue(), containsString("group by"));
    assertThat(
        sql.getValue(),
        containsString(
            "(select \"monthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('month', ax.\"scheduleddate\")::date)"));
    // Both date field bucket expressions appear in GROUP BY
    assertThat(
        sql.getValue(),
        containsString(
            "(select \"monthly\" from analytics_rs_dateperiodstructure as dps_period where dps_period.\"dateperiod\" = date_trunc('month', ax.\"lastupdated\")::date)"));
  }

  @Test
  void verifyGetAggregatedEventQueryUsesJoinBasedPeriodLookupForDoris() {
    DorisAnalyticsSqlBuilder dorisBuilder =
        new DorisAnalyticsSqlBuilder("internal", "doris-jdbc.jar");
    JdbcEventAnalyticsManager dorisSubject = createEventAnalyticsManager(dorisBuilder, "doris");

    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    mockEmptyRowSet();

    List<PeriodDimension> periods = createPeriodDimensions("202001");
    periods.forEach(period -> period.setDateField(TimeField.ENROLLMENT_DATE.name()));

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams(programStage, ValueType.INTEGER))
            .withPeriods(periods, "monthly")
            .build();

    dorisSubject.getAggregatedEventData(params, createGrid(), 200000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertThat(
        sql.getValue(),
        containsString(
            "left join analytics_rs_dateperiodstructure as dps_period_ax_enrollmentdate on dps_period_ax_enrollmentdate.`dateperiod` = cast(date_trunc(cast(ax.`enrollmentdate` as date), 'month') as date)"));
    assertThat(
        sql.getValue(), containsString("dps_period_ax_enrollmentdate.`monthly` as `monthly`"));
    assertThat(
        sql.getValue(),
        containsString(
            "group by dps_period_ax_enrollmentdate.`monthly`, ax.`ou`, ax.`fWIAEtYVEGk`"));
  }

  @Test
  void verifyAggregatedTextDimensionWrapsEmptyStringAsNullForClickHouse() {
    ClickHouseAnalyticsSqlBuilder clickHouseBuilder = new ClickHouseAnalyticsSqlBuilder("dhis2");
    JdbcEventAnalyticsManager clickHouseSubject =
        createEventAnalyticsManager(clickHouseBuilder, "clickhouse");
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);
    mockEmptyRowSet();

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams(programStage, ValueType.TEXT)).build();

    clickHouseSubject.getAggregatedEventData(params, createGrid(), 200000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    // SELECT and GROUP BY must both reference the nullif-wrapped column so '' folds into NULL.
    assertThat(countMatches(generatedSql, "nullif(ax.\"fwiaetyvegk\", '')"), is(2));
    // The SELECT column must keep an alias matching the item name so the row builder can read it.
    assertThat(generatedSql, containsString("nullif(ax.\"fwiaetyvegk\", '') as \"fwiaetyvegk\""));
  }

  @Test
  void verifyAggregatedTextDimensionKeepsRawColumnForPostgres() {
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);
    mockEmptyRowSet();

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams(programStage, ValueType.TEXT)).build();

    subject.getAggregatedEventData(params, createGrid(), 200000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    assertThat(generatedSql, not(containsString("nullif(ax.\"fwiaetyvegk\"")));
  }

  @Test
  void verifyGetAggregatedEventQueryUsesJoinBasedPeriodLookupForClickHouse() {
    ClickHouseAnalyticsSqlBuilder clickHouseBuilder = new ClickHouseAnalyticsSqlBuilder("dhis2");
    JdbcEventAnalyticsManager clickHouseSubject =
        createEventAnalyticsManager(clickHouseBuilder, "clickhouse");

    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    mockEmptyRowSet();

    List<PeriodDimension> periods = createPeriodDimensions("202001");
    periods.forEach(period -> period.setDateField(TimeField.ENROLLMENT_DATE.name()));

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams(programStage, ValueType.INTEGER))
            .withPeriods(periods, "monthly")
            .build();

    clickHouseSubject.getAggregatedEventData(params, createGrid(), 200000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    // ClickHouse cannot resolve correlated scalar subqueries that reference non-constant outer
    // columns; the period-bucket lookup is therefore emitted as a LEFT JOIN, mirroring Doris but
    // using ClickHouse identifier quoting and date functions.
    assertThat(
        sql.getValue(),
        containsString(
            "left join analytics_rs_dateperiodstructure as dps_period_ax_enrollmentdate "
                + "on dps_period_ax_enrollmentdate.\"dateperiod\" = "
                + "toDate(date_trunc('month', toDate(ax.\"enrollmentdate\")))"));
    assertThat(
        sql.getValue(), containsString("dps_period_ax_enrollmentdate.\"monthly\" as \"monthly\""));
    assertThat(
        sql.getValue(),
        containsString(
            "group by dps_period_ax_enrollmentdate.\"monthly\", ax.\"ou\", ax.\"fWIAEtYVEGk\""));

    // Postgres-only constructs must not leak into the ClickHouse SQL.
    assertThat(sql.getValue(), not(containsString("::date")));
    assertThat(sql.getValue(), not(containsString(" interval ")));
    assertThat(sql.getValue(), not(containsString("make_date")));
  }

  @Test
  void verifyEventProgramIndicatorCountUsesEventKeyedCte() {
    when(sqlBuilder.supportsCorrelatedSubquery()).thenReturn(false);
    mockEmptyRowSet();
    ProgramIndicator programIndicator =
        createEventProgramIndicator("piEventCount", AggregationType.COUNT, "ou");
    stubProgramIndicatorExpression(programIndicator, "ou", "ou");
    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams(programIndicator, null))
            .withProgramStage(programStage)
            .build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    assertThat(generatedSql, containsString("with event_pi_candidates as ("));
    assertTrue(
        generatedSql.indexOf("event_pi_candidates as") < generatedSql.indexOf("pieventcount as"),
        "Candidate event CTE must be declared before EVENT PI CTEs: " + sql.getValue());
    assertThat(
        generatedSql,
        containsString(
            "pieventcount as ( select subax.event as event, count(ou) as value from "
                + "event_pi_candidates"
                + " as subax group by subax.event )"));
    String candidateSql = extractCte(generatedSql, "event_pi_candidates");
    assertThat(
        candidateSql,
        containsString("select ax.* from " + getTable(programA.getUid()).toLowerCase() + " as ax"));
    assertThat(candidateSql, containsString("ax.\"quarterly\" in ('2000q1')"));
    assertThat(candidateSql, containsString("ax.\"uidlevel1\" in ('ouabcdefgha')"));
    assertThat(
        candidateSql, containsString("and ax.\"ps\" = '" + programStage.getUid().toLowerCase()));
    assertThat(generatedSql, containsString(" left join pieventcount "));
    assertThat(generatedSql, containsString(".event = ax.event"));
    assertTrue(
        generatedSql.matches("(?s).*coalesce\\([a-z]{5}\\.value, 0\\) as pieventcount.*"),
        "EVENT count PI select should coalesce missing joined rows to zero: " + sql.getValue());
    assertThat(generatedSql, not(containsString("where event = ax.event")));
  }

  @Test
  void verifyEventProgramIndicatorFilterOnlyCountUsesEventKeyedCte() {
    when(sqlBuilder.supportsCorrelatedSubquery()).thenReturn(false);
    mockEmptyRowSet();
    ProgramIndicator programIndicator =
        createEventProgramIndicator("piEventFilter", AggregationType.COUNT, "ou");
    stubProgramIndicatorExpression(programIndicator, "ou", "ou");

    QueryItem filterItem = createProgramIndicatorQueryItem(programIndicator);
    filterItem.addFilter(new QueryFilter(QueryOperator.GE, "0"));
    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams()).addItemFilter(filterItem).build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    assertThat(generatedSql, containsString("with event_pi_candidates as"));
    assertThat(generatedSql, containsString("pieventfilter as"));
    assertThat(generatedSql, containsString("from event_pi_candidates as subax"));
    assertThat(
        extractCte(generatedSql, "event_pi_candidates"), not(containsString("pieventfilter")));
    assertThat(generatedSql, containsString(" left join pieventfilter "));
    assertTrue(
        generatedSql.matches("(?s).*coalesce\\([a-z]{5}\\.value, 0\\) >= 0.*"),
        "EVENT count PI filter should coalesce missing joined rows to zero: " + sql.getValue());
  }

  @Test
  void verifyEventProgramIndicatorCandidatesNotEmittedWithoutEligibleEventPiCte() {
    when(sqlBuilder.supportsCorrelatedSubquery()).thenReturn(false);
    mockEmptyRowSet();

    subject.getEvents(createRequestParams(), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertThat(sql.getValue().toLowerCase(), not(containsString("event_pi_candidates")));
  }

  @Test
  void verifyEventItemSelectColumnsKeepRequestOrderWhenMixingCteAndInlineItems() {
    // On ClickHouse (no correlated subquery support) program indicators are rendered as CTEs while
    // plain data elements stay inline. The SELECT must keep the items in request order so the
    // positionally-read rows align with the grid headers, which are built in items order.
    when(sqlBuilder.supportsCorrelatedSubquery()).thenReturn(false);
    mockEmptyRowSet();

    ProgramIndicator programIndicator =
        createEventProgramIndicator("piFirstItem", AggregationType.SUM, "ou");
    stubProgramIndicatorExpression(programIndicator, "ou", "ou");

    QueryItem inlineDataElement =
        new QueryItem(dataElementA, programA, null, ValueType.INTEGER, AggregationType.SUM, null);

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .addItem(createProgramIndicatorQueryItem(programIndicator)) // request item 0
            .addItem(inlineDataElement) // request item 1
            .build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    int piSelectPos = generatedSql.indexOf("as pifirstitem");
    int inlineSelectPos = generatedSql.indexOf(dataElementA.getUid().toLowerCase());

    assertTrue(
        piSelectPos >= 0 && inlineSelectPos >= 0,
        "Both item columns must be present: " + sql.getValue());
    assertTrue(
        piSelectPos < inlineSelectPos,
        "Program indicator column must keep its request-order position before the inline data "
            + "element column: "
            + sql.getValue());
  }

  @Test
  void verifyEventQueryEnrollmentProgramIndicatorUsesCtePathToExpandPlaceholders() {
    when(sqlBuilder.supportsCorrelatedSubquery()).thenReturn(true);
    mockEmptyRowSet();
    ProgramIndicator programIndicator =
        createEventProgramIndicator("piEnrollComplex", AggregationType.SUM, "complex");
    programIndicator.setAnalyticsType(AnalyticsType.ENROLLMENT);

    String variablePlaceholder =
        "FUNC_CTE_VAR( type='vCreationDate', column='created', piUid='piEnrollComplex', psUid='null', offset='0')";
    String psdePlaceholder =
        "__PSDE_CTE_PLACEHOLDER__(psUid='PgmStgUid1', deUid='DataElmUid1', offset='0', boundaryHash='noboundaries', piUid='piEnrollComplex')";
    String d2Placeholder =
        "__D2FUNC__(func='countIfValue', ps='PgmStgUid1', de='DataElmUid2', argType='val64', arg64='NQ==', hash='noboundaries', pi='piEnrollComplex')__";

    when(programIndicatorService.getAnalyticsSqlDeferRelationshipCount(
            anyString(), eq(NUMERIC), eq(programIndicator), any(), any(), eq("subax")))
        .thenReturn(variablePlaceholder + " + " + psdePlaceholder + " + " + d2Placeholder);

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .addItem(createProgramIndicatorQueryItem(programIndicator))
            .build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    assertThat(generatedSql, containsString("pienrollcomplex as ( select sum("));
    assertThat(
        generatedSql,
        containsString("from analytics_enrollment_" + programA.getUid().toLowerCase()));
    assertThat(generatedSql, containsString("cross join pienrollcomplex"));
    assertTrue(
        generatedSql.matches("(?s).*coalesce\\([a-z]{5}\\.value, 0\\) as pienrollcomplex.*"),
        "ENROLLMENT PI select should read the joined PI CTE value: " + sql.getValue());
    assertThat(generatedSql, containsString("row_number() over (partition by enrollment"));
    assertThat(generatedSql, containsString("count(\"dataelmuid2\") as value"));
    assertThat(generatedSql, not(containsString("func_cte_var(")));
    assertThat(generatedSql, not(containsString("__psde_cte_placeholder__")));
    assertThat(generatedSql, not(containsString("__d2func__(")));
  }

  @Test
  void verifyPostgresEventProgramIndicatorKeepsInlineCorrelatedSubquery() {
    when(sqlBuilder.supportsCorrelatedSubquery()).thenReturn(true);
    mockEmptyRowSet();
    ProgramIndicator programIndicator =
        createEventProgramIndicator("piEventInline", AggregationType.COUNT, "1");
    stubProgramIndicatorExpression(programIndicator, "1", "1");

    subject.getEvents(createRequestParams(programIndicator, null), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    assertThat(generatedSql, not(containsString("pieventinline as")));
    assertThat(generatedSql, not(containsString("event_pi_candidates")));
    assertThat(generatedSql, containsString("(select count(1) from analytics_event_"));
    assertThat(generatedSql, containsString("where event = ax.event"));
  }

  @Test
  void verifyEventProgramIndicatorWithInlineStageDataElementFilterUsesEventKeyedCte() {
    when(sqlBuilder.supportsCorrelatedSubquery()).thenReturn(false);
    mockEmptyRowSet();
    ProgramIndicator programIndicator =
        createEventProgramIndicator("rxNjqzJ7dkK", AggregationType.COUNT, "distinct ou");
    programIndicator.setFilter("#{edqlbukwRfQ.nhW3SZX9JaN} == 'Ongoing'");
    stubProgramIndicatorExpression(programIndicator, "distinct ou", "distinct ou");
    when(programIndicatorService.getAnalyticsSqlDeferRelationshipCount(
            eq("#{edqlbukwRfQ.nhW3SZX9JaN} == 'Ongoing'"),
            eq(org.hisp.dhis.analytics.DataType.BOOLEAN),
            eq(programIndicator),
            any(),
            any(),
            eq("subax")))
        .thenReturn(
            "coalesce(toString(case when subax.\"ps\" = 'edqlbukwRfQ' then \"nhW3SZX9JaN\" else null end), '') = 'Ongoing'");

    QueryItem queryItem = createProgramIndicatorQueryItem(programIndicator);
    queryItem.addFilter(new QueryFilter(QueryOperator.GE, "0"));
    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams()).addItem(queryItem).build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    assertThat(generatedSql, containsString("with event_pi_candidates as"));
    assertThat(generatedSql, containsString("rxnjqzj7dkk as"));
    assertThat(
        generatedSql, containsString("select subax.event as event, count(distinct ou) as value"));
    assertThat(generatedSql, containsString("from event_pi_candidates as subax"));
    assertThat(generatedSql, containsString("case when subax.\"ps\" = 'edqlbukwrfq'"));
    assertThat(generatedSql, containsString(" left join rxnjqzj7dkk "));
    assertThat(generatedSql, containsString(".event = ax.event"));
    assertTrue(
        generatedSql.matches("(?s).*coalesce\\([a-z]{5}\\.value, 0\\) >= 0.*"),
        "EVENT count PI query-item filter should use the joined CTE value: " + sql.getValue());
    assertThat(generatedSql, not(containsString("where event = ax.event")));
  }

  @Test
  void verifyEventProgramIndicatorAverageWithInlineStageDataElementFilterUsesEventKeyedCte() {
    when(sqlBuilder.supportsCorrelatedSubquery()).thenReturn(false);
    mockEmptyRowSet();
    ProgramIndicator programIndicator =
        createEventProgramIndicator(
            "GxdhnY5wmHq",
            AggregationType.AVERAGE,
            "(#{A03MvHHogjR.UXz7xuGCEhU} + #{ZzYYXq4fJie.GQY2lXrypjO}) / V{event_count}");
    programIndicator.setFilter("V{event_count} > 0");

    String renderedExpression =
        "(coalesce(toFloat64(case when subax.\"ps\" = 'A03MvHHogjR' then \"UXz7xuGCEhU\" else null end), 0) "
            + "+ coalesce(toFloat64(case when subax.\"ps\" = 'ZzYYXq4fJie' then \"GQY2lXrypjO\" else null end), 0)) "
            + "/ nullif(cast((case when \"GQY2lXrypjO\" is not null then 1 else 0 end "
            + "+ case when \"UXz7xuGCEhU\" is not null then 1 else 0 end) as Float64), 0)";
    String renderedFilter =
        "nullif(cast((case when \"GQY2lXrypjO\" is not null then 1 else 0 end "
            + "+ case when \"UXz7xuGCEhU\" is not null then 1 else 0 end) as Float64), 0) > toFloat64(0)";
    stubProgramIndicatorExpression(
        programIndicator, programIndicator.getExpression(), renderedExpression);
    when(programIndicatorService.getAnalyticsSqlDeferRelationshipCount(
            eq(programIndicator.getFilter()),
            eq(org.hisp.dhis.analytics.DataType.BOOLEAN),
            eq(programIndicator),
            any(),
            any(),
            eq("subax")))
        .thenReturn(renderedFilter);

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .addItem(createProgramIndicatorQueryItem(programIndicator))
            .build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    assertThat(generatedSql, containsString("with event_pi_candidates as"));
    assertThat(generatedSql, containsString("gxdhny5wmhq as"));
    assertThat(
        generatedSql,
        containsString("select subax.event as event, avg((coalesce(tofloat64(case when subax."));
    assertThat(generatedSql, containsString("from event_pi_candidates as subax"));
    assertThat(generatedSql, containsString(" where nullif(cast((case when \"gqy2lxrypjo\""));
    assertThat(generatedSql, containsString(" group by subax.event"));
    assertThat(generatedSql, containsString(" left join gxdhny5wmhq "));
    assertThat(generatedSql, containsString(".event = ax.event"));
    assertThat(generatedSql, containsString(".value as gxdhny5wmhq"));
    assertFalse(
        generatedSql.matches("(?s).*coalesce\\([a-z]{5}\\.value, 0\\).*"),
        "EVENT avg PI select should not coalesce the joined CTE value: " + sql.getValue());
    assertThat(generatedSql, not(containsString("where event = ax.event")));
  }

  @Test
  void verifyEventProgramIndicatorNonCountFilterDoesNotCoalesce() {
    when(sqlBuilder.supportsCorrelatedSubquery()).thenReturn(false);
    mockEmptyRowSet();
    ProgramIndicator programIndicator =
        createEventProgramIndicator("piEventSum", AggregationType.SUM, "1");
    stubProgramIndicatorExpression(programIndicator, "1", "1");

    QueryItem filterItem = createProgramIndicatorQueryItem(programIndicator);
    filterItem.addFilter(new QueryFilter(QueryOperator.GE, "0"));
    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams()).addItemFilter(filterItem).build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    assertThat(generatedSql, containsString("with event_pi_candidates as"));
    assertThat(generatedSql, containsString("pieventsum as"));
    assertThat(generatedSql, containsString("from event_pi_candidates as subax"));
    assertThat(generatedSql, containsString(".value >= 0"));
    assertFalse(
        generatedSql.matches("(?s).*coalesce\\([a-z]{5}\\.value, 0\\).*"),
        "Non-count EVENT PI filter should not coalesce its CTE value: " + sql.getValue());
  }

  @Test
  void verifyCoordinatesOnlyFilterTreatsEmptyStringAsNullForClickHouse() {
    // ClickHouse stores a missing coordinate as '' (not NULL), so a plain
    // "coalesce(col) is not null" filter matches every row. The column must be wrapped in
    // nullif(col, '') so empty strings are treated as NULL, matching Postgres.
    ClickHouseAnalyticsSqlBuilder clickHouseBuilder = new ClickHouseAnalyticsSqlBuilder("dhis2");
    JdbcEventAnalyticsManager clickHouseSubject =
        createEventAnalyticsManager(clickHouseBuilder, "clickhouse");
    mockEmptyRowSet();

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .withCoordinatesOnly(true)
            .withCoordinateFields(List.of("eventgeometry"))
            .build();

    clickHouseSubject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    assertThat(
        generatedSql, containsString("nullif(coalesce(ax.\"eventgeometry\"), '') is not null"));
  }

  @Test
  void verifyCoordinatesOnlyFilterKeepsPlainCoalesceForPostgres() {
    mockEmptyRowSet();

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParams())
            .withCoordinatesOnly(true)
            .withCoordinateFields(List.of("eventgeometry"))
            .build();

    subject.getEvents(params, createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue().toLowerCase();
    assertThat(generatedSql, containsString("coalesce(ax.\"eventgeometry\") is not null"));
    assertThat(generatedSql, not(containsString("nullif(coalesce(")));
  }

  @Test
  void verifyRelationshipEventProgramIndicatorKeepsInlineRelationshipPredicate() {
    mockEmptyRowSet();
    ProgramIndicator programIndicator =
        createEventProgramIndicator("piRelEvent", AggregationType.COUNT, "1");
    stubProgramIndicatorExpression(programIndicator, "1", "1");
    RelationshipType relationshipType =
        createRelationshipType(
            RelationshipEntity.PROGRAM_STAGE_INSTANCE, RelationshipEntity.PROGRAM_STAGE_INSTANCE);

    subject.getEvents(createRequestParams(programIndicator, relationshipType), createGrid(), 100);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String generatedSql = sql.getValue();
    assertThat(generatedSql, not(containsString("with pirelevent as")));
    assertThat(generatedSql, containsString("select count(1) from"));
    assertThat(
        generatedSql, containsString("where rty.relationshiptypeid = " + relationshipType.getId()));
    assertThat(generatedSql, containsString("ev2.uid = ax.event"));
  }

  private JdbcEventAnalyticsManager createEventAnalyticsManager(
      AnalyticsSqlBuilder builder, String analyticsDatabase) {
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn(analyticsDatabase);

    EventTimeFieldSqlRenderer timeCoordinateSelector = new EventTimeFieldSqlRenderer(builder);
    DefaultProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder =
        new DefaultProgramIndicatorSubqueryBuilder(
            programIndicatorService, systemSettingsService, builder, dataElementService);
    programIndicatorSubqueryBuilder.init();
    ColumnMapper columnMapper = new ColumnMapper(builder, systemSettingsService);
    filterBuilder = new QueryItemFilterBuilder(organisationUnitResolver, builder);
    StageQuerySqlFacade stageQuerySqlFacade =
        new DefaultStageQuerySqlFacade(
            new DefaultStageQueryItemClassifier(),
            new DefaultStageDatePeriodBucketSqlRenderer(builder),
            new DefaultStageOrgUnitSqlService(organisationUnitResolver, builder),
            builder);

    return new JdbcEventAnalyticsManager(
        jdbcTemplate,
        programIndicatorService,
        programIndicatorSubqueryBuilder,
        piDisagInfoInitializer,
        piDisagQueryGenerator,
        timeCoordinateSelector,
        executionPlanStore,
        systemSettingsService,
        config,
        builder,
        organisationUnitResolver,
        columnMapper,
        filterBuilder,
        stageQuerySqlFacade,
        new DateFieldPeriodBucketColumnResolver(builder),
        new FirstOrLastValueSubqueryRenderer(
            builder, timeCoordinateSelector, programIndicatorService));
  }

  private ProgramIndicator createEventProgramIndicator(
      String uid, AggregationType aggregationType, String expression) {
    ProgramIndicator programIndicator = new ProgramIndicator();
    programIndicator.setUid(uid);
    programIndicator.setProgram(programA);
    programIndicator.setAnalyticsType(AnalyticsType.EVENT);
    programIndicator.setAggregationType(aggregationType);
    programIndicator.setExpression(expression);
    return programIndicator;
  }

  private QueryItem createProgramIndicatorQueryItem(ProgramIndicator programIndicator) {
    return new QueryItem(
        programIndicator,
        programIndicator.getProgram(),
        null,
        ValueType.NUMBER,
        programIndicator.getAggregationType(),
        null);
  }

  private void stubProgramIndicatorExpression(
      ProgramIndicator programIndicator, String expression, String renderedSql) {
    when(programIndicatorService.getAnalyticsSqlDeferRelationshipCount(
            eq(expression), eq(NUMERIC), eq(programIndicator), any(), any(), eq("subax")))
        .thenReturn(renderedSql);
    when(programIndicatorService.getAnalyticsSql(
            eq(expression), eq(NUMERIC), eq(programIndicator), any(), any(), eq("subax")))
        .thenReturn(renderedSql);
  }

  private RelationshipType createRelationshipType(
      RelationshipEntity fromEntity, RelationshipEntity toEntity) {
    RelationshipType relationshipType = new RelationshipType();
    relationshipType.setId(12L);

    RelationshipConstraint from = new RelationshipConstraint();
    from.setRelationshipEntity(fromEntity);
    RelationshipConstraint to = new RelationshipConstraint();
    to.setRelationshipEntity(toEntity);

    relationshipType.setFromConstraint(from);
    relationshipType.setToConstraint(to);
    return relationshipType;
  }

  private String extractCte(String sql, String cteName) {
    String marker = cteName.toLowerCase() + " as (";
    int start = sql.indexOf(marker);
    assertTrue(start >= 0, "Expected CTE not found: " + cteName + " in " + sql);

    int openParen = sql.indexOf('(', start);
    int depth = 0;
    for (int i = openParen; i < sql.length(); i++) {
      char current = sql.charAt(i);
      if (current == '(') {
        depth++;
      } else if (current == ')') {
        depth--;
        if (depth == 0) {
          return sql.substring(start, i + 1);
        }
      }
    }

    throw new AssertionError("Could not parse CTE: " + cteName + " in " + sql);
  }

  private void verifyFirstOrLastAggregationTypeSubquery(
      AnalyticsAggregationType analyticsAggregationType) {
    DataElement deU = createDataElement('U');

    EventQueryParams params =
        new EventQueryParams.Builder(createRequestParamsWithFilter(ValueType.TEXT))
            .withValue(deU)
            .withAggregationType(analyticsAggregationType)
            .withAggregateData(true)
            .build();

    subject.getAggregatedEventData(params, createGrid(), 200000);

    verify(jdbcTemplate).queryForRowSet(sql.capture());

    String order = (analyticsAggregationType == AnalyticsAggregationType.LAST) ? "desc" : "asc";

    String expectedFirstOrLastSubquery =
        "from (select \"event\",ax.\""
            + deU.getUid()
            + "\",\"quarterly\",\"ou\","
            + "row_number() over (partition by ax.\"ou\",ax.\"ao\" order by ax.\"occurreddate\" "
            + order
            + ", ax.\"created\" "
            + order
            + ") as pe_rank "
            + "from "
            + getTable(programA.getUid())
            + " as ax where ax.\"occurreddate\" >= '1990-03-31' "
            + "and ax.\"occurreddate\" <= '2000-03-31' and ax.\""
            + deU.getUid()
            + "\" is not null)";

    assertThat(sql.getValue(), containsString(expectedFirstOrLastSubquery));
  }

  @Test
  void verifyGetAggregatedEventDataRendersTimestampForDateQueryItem() {
    // Given: a QueryItem with DATE value type
    DataElement dateElement = createDataElement('D');
    dateElement.setUid("dateElement1");
    dateElement.setValueType(ValueType.DATE);

    QueryItem dateQueryItem =
        new QueryItem(dateElement, programA, null, ValueType.DATE, AggregationType.NONE, null);
    dateQueryItem.setProgramStage(programStage);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withPeriods(createPeriodDimensions("2000Q1"), "quarterly")
            .withOrganisationUnits(List.of(createOrganisationUnit('A')))
            .withProgram(programA)
            .withProgramStage(programStage)
            .withTableName(getTable(programA.getUid()))
            .addItem(dateQueryItem)
            .build();

    // Mock row set with a date value
    when(rowSet.next()).thenReturn(true).thenReturn(false);
    when(rowSet.getString("dateElement1")).thenReturn("2024-01-15");
    when(rowSet.getString("quarterly")).thenReturn("2024Q1");
    when(rowSet.getString("ou")).thenReturn("OrgUnit");
    when(rowSet.getInt("value")).thenReturn(1);
    when(piDisagInfoInitializer.getParamsWithDisaggregationInfo(any(EventQueryParams.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    // When
    subject.getAggregatedEventData(params, createGrid(), 200000);

    // Then: verify renderTimestamp was called for the date value
    verify(sqlBuilder).renderTimestamp("2024-01-15");
  }

  private EventQueryParams createRequestParamsWithFilter(ValueType queryItemValueType) {
    EventQueryParams.Builder params =
        new EventQueryParams.Builder(createRequestParams(queryItemValueType));
    QueryItem queryItem = params.build().getItems().get(0);
    queryItem.addFilter(new QueryFilter(QueryOperator.GT, "10"));

    return params.build();
  }

  private Grid createGrid() {
    return new ListGrid()
        .addHeader(new GridHeader("fWIAEtYVEGk", "Mode of discharge", ValueType.TEXT, false, true))
        .addHeader(new GridHeader("pe", "Period", ValueType.TEXT, false, true))
        .addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, true));
  }

  private void mockRowSet() {
    // Simulate one row only
    when(rowSet.next()).thenReturn(true).thenReturn(false);

    when(rowSet.getString("quarterly")).thenReturn("2017Q1");
    when(rowSet.getString("ou")).thenReturn("Sierra Leone");
    when(rowSet.getInt("value")).thenReturn(100);
  }

  private void assertSql(String expected, String actual) {
    expected = "select " + DEFAULT_COLUMNS_WITH_REGISTRATION + ", " + expected;

    assertThat(actual, is(expected));
  }

  @Override
  String getTableName() {
    return TABLE_NAME;
  }
}
