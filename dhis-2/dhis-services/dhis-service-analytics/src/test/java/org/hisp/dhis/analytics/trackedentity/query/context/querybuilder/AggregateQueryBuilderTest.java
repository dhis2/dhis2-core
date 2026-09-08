/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.query.context.querybuilder;

import static org.hisp.dhis.common.IdScheme.UID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.RootConditionRenderer;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageDatePeriodBucketSqlRenderer;
import org.hisp.dhis.analytics.trackedentity.EventValue;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlParameterManager;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests the grouping of event scoped dimensions by {@link AggregateQueryBuilder}. */
class AggregateQueryBuilderTest {

  private static final String PROGRAM_UID = "IpHINAT79UW";

  private static final String PROGRAM_STAGE_UID = "A03MvHHogjR";

  private static final String TET_UID = "nEenWmSyUEp";

  private static final String DATA_ELEMENT_UID = "UXz7xuGCEhU";

  private AggregateQueryBuilder builder;

  private TrackedEntityType trackedEntityType;

  @BeforeEach
  void setUp() {
    builder =
        new AggregateQueryBuilder(
            new DefaultStageDatePeriodBucketSqlRenderer(new PostgreSqlAnalyticsSqlBuilder()));
    trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid(TET_UID);
  }

  @Test
  void groupedKeysIncludeEventScopedOrgUnit() {
    DimensionIdentifier<DimensionParam> eventOu = eventScopedDimension(StaticDimension.OU);

    Set<String> groupedKeys = AggregateQueryBuilder.getGroupedDimensionKeys(contextParams(eventOu));

    assertTrue(
        groupedKeys.contains(eventOu.getKey()),
        "expected " + eventOu.getKey() + " to be grouped, got " + groupedKeys);
  }

  @Test
  void groupedKeysIncludeEventScopedStatus() {
    DimensionIdentifier<DimensionParam> eventStatus =
        eventScopedDimension(StaticDimension.EVENT_STATUS);

    Set<String> groupedKeys =
        AggregateQueryBuilder.getGroupedDimensionKeys(contextParams(eventStatus));

    assertTrue(groupedKeys.contains(eventStatus.getKey()));
  }

  /**
   * The select column is aliased to the header name, because the grid reads each row by header
   * name, while the group by repeats the bare expression, since an alias is not a valid group by
   * key.
   */
  @Test
  void eventScopedOrgUnitIsSelectedUnderItsHeaderNameAndGroupedByExpression() {
    DimensionIdentifier<DimensionParam> eventOu = eventScopedDimension(StaticDimension.OU);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParams(eventOu);

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(eventOu),
            List.of());

    String expectedExpression = SqlQueryHelper.buildCollapsedEventSubquery(eventOu, "ou").render();

    assertEquals(
        List.of(
            expectedExpression + " as \"" + PROGRAM_STAGE_UID + ".ou\"", "count(1) as \"value\""),
        query.getSelectFields().stream().map(Field::render).toList());
    assertEquals(
        List.of(expectedExpression), query.getGroupByFields().stream().map(Field::render).toList());
  }

  /**
   * Every grouped field of one program stage reads the same event, so the status is read from the
   * event the collapse chooses for the whole scope. That event is never a scheduled one, which is
   * the same row {@code value=} aggregates over.
   */
  @Test
  void eventScopedStatusReadsTheSameEventAsEveryOtherFieldOfTheStage() {
    DimensionIdentifier<DimensionParam> eventStatus =
        eventScopedDimension(StaticDimension.EVENT_STATUS);
    DimensionIdentifier<DimensionParam> eventOu = eventScopedDimension(StaticDimension.OU);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParams(eventStatus);

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(eventStatus),
            List.of());

    assertEquals(
        List.of(SqlQueryHelper.buildCollapsedEventSubquery(eventStatus, "status").render()),
        query.getGroupByFields().stream().map(Field::render).toList());

    // The two fields differ only in the selected column, never in the chosen row.
    String status = SqlQueryHelper.buildCollapsedEventSubquery(eventStatus, "status").render();
    String ou = SqlQueryHelper.buildCollapsedEventSubquery(eventOu, "ou").render();
    assertEquals(
        status.replace("ev.\"status\"", "ev.\"ou\""),
        ou,
        "the collapse must choose the same event for every field of the stage");
  }

  /**
   * A request addresses an event scoped dimension by its stage, while the parsed dimension also
   * carries the program the stage belongs to, so the two forms must be matched to each other.
   */
  @Test
  void groupedKeysMatchTheStageScopedRequestForm() {
    DimensionIdentifier<DimensionParam> eventOu = eventScopedDimension(StaticDimension.OU);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(
                TrackedEntityQueryParams.builder()
                    .aggregate(true)
                    .trackedEntityType(trackedEntityType)
                    .build())
            .commonRaw(
                new CommonRequestParams()
                    .withDimension(Set.of(PROGRAM_STAGE_UID + ".ou:USER_ORGUNIT")))
            .commonParsed(
                CommonParsedParams.builder().dimensionIdentifiers(List.of(eventOu)).build())
            .build();

    assertEquals(Set.of(eventOu.getKey()), AggregateQueryBuilder.getGroupedDimensionKeys(ctx));
    assertEquals(
        Set.of(PROGRAM_STAGE_UID + ".ou"), AggregateQueryBuilder.getGroupedRequestKeys(ctx));
  }

  /**
   * A date dimension is grouped on the period the request asked for, not on the raw timestamp, so
   * {@code EVENT_DATE:THIS_YEAR} answers with a year.
   */
  @Test
  void eventScopedDateIsGroupedByItsPeriodBucket() {
    DimensionIdentifier<DimensionParam> eventDate =
        eventScopedDimension(StaticDimension.EVENT_DATE, "THIS_YEAR");
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParams(eventDate);

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(eventDate),
            List.of());

    String dateExpression =
        SqlQueryHelper.buildCollapsedEventSubquery(eventDate, "occurreddate").render();
    String expected =
        new DefaultStageDatePeriodBucketSqlRenderer(new PostgreSqlAnalyticsSqlBuilder())
            .renderPeriodBucketExpression(dateExpression, "yearly");

    assertEquals(List.of(expected), query.getGroupByFields().stream().map(Field::render).toList());
    assertEquals(
        List.of(expected + " as \"" + PROGRAM_STAGE_UID + ".eventdate\"", "count(1) as \"value\""),
        query.getSelectFields().stream().map(Field::render).toList());
  }

  /** Without a resolvable period the date column is grouped as it is, matching the reference. */
  @Test
  void eventScopedDateWithoutPeriodItemsIsGroupedRaw() {
    DimensionIdentifier<DimensionParam> eventDate =
        eventScopedDimension(StaticDimension.EVENT_DATE);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParams(eventDate);

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(eventDate),
            List.of());

    assertEquals(
        List.of(SqlQueryHelper.buildCollapsedEventSubquery(eventDate, "occurreddate").render()),
        query.getGroupByFields().stream().map(Field::render).toList());
  }

  /**
   * An enrollment scoped dimension is read from the enrollment chosen for each tracked entity by
   * the offset, the same enrollment the row level endpoint sorts and filters on.
   */
  @Test
  void enrollmentScopedOrgUnitIsGroupedByTheChosenEnrollment() {
    DimensionIdentifier<DimensionParam> enrollmentOu =
        enrollmentScopedDimension(StaticDimension.OU);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParams(enrollmentOu);

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(enrollmentOu),
            List.of());

    String expected = SqlQueryHelper.buildOrderSubQuery(enrollmentOu, () -> "ou").render();

    assertEquals(List.of(expected), query.getGroupByFields().stream().map(Field::render).toList());
    assertEquals(
        List.of(expected + " as \"" + PROGRAM_UID + ".ou\"", "count(1) as \"value\""),
        query.getSelectFields().stream().map(Field::render).toList());
  }

  @Test
  void enrollmentScopedStatusIsGroupedOnTheEnrollmentStatusColumn() {
    DimensionIdentifier<DimensionParam> programStatus =
        enrollmentScopedDimension(StaticDimension.PROGRAM_STATUS);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParams(programStatus);

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(programStatus),
            List.of());

    assertEquals(
        List.of(
            SqlQueryHelper.buildOrderSubQuery(programStatus, () -> "enrollmentstatus").render()),
        query.getGroupByFields().stream().map(Field::render).toList());
  }

  @Test
  void enrollmentScopedDateIsGroupedByItsPeriodBucket() {
    DimensionIdentifier<DimensionParam> enrollmentDate =
        enrollmentScopedDimension(StaticDimension.ENROLLMENTDATE, "THIS_YEAR");
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParams(enrollmentDate);

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(enrollmentDate),
            List.of());

    String expected =
        new DefaultStageDatePeriodBucketSqlRenderer(new PostgreSqlAnalyticsSqlBuilder())
            .renderPeriodBucketExpression(
                SqlQueryHelper.buildOrderSubQuery(enrollmentDate, () -> "enrollmentdate").render(),
                "yearly");

    assertEquals(List.of(expected), query.getGroupByFields().stream().map(Field::render).toList());
  }

  /**
   * The end date has no column of that name on the enrollment table, so it is not groupable and
   * must be rejected rather than reaching the database.
   */
  @Test
  void enrollmentScopedEndDateIsNotGroupable() {
    DimensionIdentifier<DimensionParam> endDate =
        enrollmentScopedDimension(StaticDimension.ENDDATE, "THIS_YEAR");

    assertEquals(Set.of(), AggregateQueryBuilder.getGroupedDimensionKeys(contextParams(endDate)));
  }

  /** An event status has no column on the enrollment table, so a program scoped one is rejected. */
  @Test
  void enrollmentScopedEventStatusIsNotGroupable() {
    DimensionIdentifier<DimensionParam> eventStatus =
        enrollmentScopedDimension(StaticDimension.EVENT_STATUS, "ACTIVE");

    assertEquals(
        Set.of(), AggregateQueryBuilder.getGroupedDimensionKeys(contextParams(eventStatus)));
  }

  /**
   * A grouped dimension carrying items filters on the same row it groups on, so that the rows come
   * back restricted to what was asked for. Without this the restriction would run over a separately
   * chosen event and could answer with a group outside the requested items.
   */
  @Test
  void groupedEventScopedOrgUnitWithItemsIsRestrictedOnTheGroupedExpression() {
    DimensionIdentifier<DimensionParam> stageOu = stageScopedOuDimensionWithItems();
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParams(stageOu);

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(stageOu),
            List.of());

    assertEquals(1, query.getGroupableConditions().size());
    String condition = query.getGroupableConditions().get(0).getRenderable().render();
    assertTrue(
        condition.contains("partition by trackedentity"),
        "expected the restriction to read the collapsed event row, got " + condition);
  }

  /** A grouped dimension without items adds no restriction. */
  @Test
  void groupedEventScopedOrgUnitWithoutItemsAddsNoRestriction() {
    DimensionIdentifier<DimensionParam> eventOu = eventScopedDimension(StaticDimension.OU);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParams(eventOu);

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(eventOu),
            List.of());

    assertEquals(List.of(), query.getGroupableConditions());
  }

  /**
   * A stage data element is grouped on the value it holds in the single event chosen for each
   * tracked entity, read out of that event's data values.
   */
  @Test
  void stageDataElementIsGroupedByItsValueInTheChosenEvent() {
    DimensionIdentifier<DimensionParam> dataElement = stageDataElementDimension();
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParams(dataElement);

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(dataElement),
            List.of());

    List<String> groupBy = query.getGroupByFields().stream().map(Field::render).toList();

    assertEquals(1, groupBy.size());
    assertTrue(
        groupBy.get(0).contains("partition by trackedentity"),
        "expected the collapsed event row, got " + groupBy.get(0));
    assertTrue(
        groupBy.get(0).contains("-> '" + DATA_ELEMENT_UID + "' ->> 'value'"),
        "expected the data element value extraction, got " + groupBy.get(0));
    assertEquals(
        List.of(
            groupBy.get(0) + " as \"" + PROGRAM_STAGE_UID + "." + DATA_ELEMENT_UID + "\"",
            "count(1) as \"value\""),
        query.getSelectFields().stream().map(Field::render).toList());
  }

  private DimensionIdentifier<DimensionParam> stageDataElementDimension() {
    DataElement dataElement = new DataElement();
    dataElement.setUid(DATA_ELEMENT_UID);
    dataElement.setValueType(ValueType.TEXT);

    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new QueryItem(dataElement), DimensionParamType.DIMENSIONS, UID, List.of());

    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setTrackedEntityType(trackedEntityType);

    ProgramStage programStage = new ProgramStage();
    programStage.setUid(PROGRAM_STAGE_UID);
    programStage.setProgram(program);

    return DimensionIdentifier.of(
        ElementWithOffset.of(program), ElementWithOffset.of(programStage), dimensionParam);
  }

  private DimensionIdentifier<DimensionParam> stageScopedOuDimensionWithItems() {
    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid("ou1");

    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(orgUnit)),
            DimensionParamType.DIMENSIONS,
            UID,
            List.of("ou1"));

    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setTrackedEntityType(trackedEntityType);

    ProgramStage programStage = new ProgramStage();
    programStage.setUid(PROGRAM_STAGE_UID);
    programStage.setProgram(program);

    return DimensionIdentifier.of(
            ElementWithOffset.of(program), ElementWithOffset.of(programStage), dimensionParam)
        .withDefaultGroupId();
  }

  private DimensionIdentifier<DimensionParam> enrollmentScopedDimension(
      StaticDimension staticDimension, String... items) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            staticDimension.name(), DimensionParamType.DIMENSIONS, UID, List.of(items));

    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setTrackedEntityType(trackedEntityType);

    return DimensionIdentifier.of(
        ElementWithOffset.of(program), ElementWithOffset.emptyElementWithOffset(), dimensionParam);
  }

  /**
   * A stage offset is part of how a request addresses a dimension, so the short form carrying one
   * has to match the resolved dimension, which carries the offset on both the program and the
   * stage.
   */
  @Test
  void groupedKeysMatchTheStageScopedRequestFormWithAnOffset() {
    DimensionIdentifier<DimensionParam> eventOu =
        eventScopedDimensionWithOffset(StaticDimension.OU, 1);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        rawContextParams(eventOu, PROGRAM_STAGE_UID + "[1].ou");

    assertEquals(Set.of(eventOu.getKey()), AggregateQueryBuilder.getGroupedDimensionKeys(ctx));
    assertEquals(
        Set.of(PROGRAM_STAGE_UID + "[1].ou"), AggregateQueryBuilder.getGroupedRequestKeys(ctx));
  }

  /** The fully qualified form carrying offsets keeps matching. */
  @Test
  void groupedKeysMatchTheFullyQualifiedRequestFormWithAnOffset() {
    DimensionIdentifier<DimensionParam> eventOu =
        eventScopedDimensionWithOffset(StaticDimension.OU, 1);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        rawContextParams(eventOu, PROGRAM_UID + "[1]." + PROGRAM_STAGE_UID + "[1].ou");

    assertEquals(Set.of(eventOu.getKey()), AggregateQueryBuilder.getGroupedDimensionKeys(ctx));
  }

  /** A request without an offset must not match a dimension parsed with one. */
  @Test
  void groupedKeysDoNotMatchAnOffsetlessRequestAgainstAnOffsetDimension() {
    DimensionIdentifier<DimensionParam> eventOu =
        eventScopedDimensionWithOffset(StaticDimension.OU, 1);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        rawContextParams(eventOu, PROGRAM_STAGE_UID + ".ou");

    assertEquals(Set.of(), AggregateQueryBuilder.getGroupedDimensionKeys(ctx));
  }

  /**
   * Sorting on a grouped scoped dimension must order by the expression the query groups on.
   * Ordering by anything else is invalid grouped SQL, because the other expressions reference the
   * tracked entity table columns that the grouping replaced.
   */
  @Test
  void sortingOnAGroupedScopedDimensionOrdersByTheGroupedExpression() {
    DimensionIdentifier<DimensionParam> eventOu = eventScopedDimension(StaticDimension.OU);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParams(eventOu);

    AnalyticsSortingParams sort =
        AnalyticsSortingParams.builder()
            .index(0)
            .orderBy(eventOu)
            .sortDirection(SortDirection.ASC)
            .build();

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(eventOu),
            List.of(sort));

    assertEquals(1, query.getOrderClauses().size());
    String rendered = query.getOrderClauses().get(0).getRenderable().render();
    String groupedExpression = query.getGroupByFields().get(0).render();
    assertTrue(
        rendered.contains(groupedExpression),
        "expected the order clause to reuse the grouped expression, got " + rendered);
  }

  /**
   * A sorting parameter carries no items, so it cannot say which period a date is bucketed into.
   * The order clause has to come from the grouped dimension's own expression, or it orders by the
   * raw date while the query groups by the bucket, which is invalid grouped SQL.
   */
  @Test
  void sortingOnAGroupedBucketedDateOrdersByTheBucketExpression() {
    DimensionIdentifier<DimensionParam> groupedDate =
        eventScopedDimension(StaticDimension.EVENT_DATE, "202107", "202108");
    DimensionIdentifier<DimensionParam> sortDate =
        eventScopedSortingDimension(StaticDimension.EVENT_DATE);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParams(groupedDate);

    RenderableSqlQuery query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(groupedDate),
            List.of(
                AnalyticsSortingParams.builder()
                    .index(0)
                    .orderBy(sortDate)
                    .sortDirection(SortDirection.DESC)
                    .build()));

    assertEquals(1, query.getOrderClauses().size());
    String groupedExpression = query.getGroupByFields().get(0).render();
    assertTrue(
        groupedExpression.contains("dateperiodstructure"),
        "the grouped date must be bucketed, got " + groupedExpression);
    assertTrue(
        query.getOrderClauses().get(0).getRenderable().render().contains(groupedExpression),
        "the order clause must reuse the bucketed grouped expression, got "
            + query.getOrderClauses().get(0).getRenderable().render());
  }

  private DimensionIdentifier<DimensionParam> eventScopedSortingDimension(
      StaticDimension staticDimension) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(staticDimension.name(), DimensionParamType.SORTING, UID, List.of());

    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setTrackedEntityType(trackedEntityType);

    ProgramStage programStage = new ProgramStage();
    programStage.setUid(PROGRAM_STAGE_UID);
    programStage.setProgram(program);

    return DimensionIdentifier.of(
        ElementWithOffset.of(program), ElementWithOffset.of(programStage), dimensionParam);
  }

  /**
   * A tracked entity dimension's restriction stays with the builder that owns it. This builder only
   * takes over the restriction of a scoped dimension, whose expression it alone can produce, so
   * claiming a tracked entity dimension too would drop its restriction entirely and answer over
   * every item instead of the requested ones.
   */
  @Test
  void trackedEntityDimensionsAreNotClaimedFromTheirOwnBuilder() {
    DimensionIdentifier<DimensionParam> registrationOu = trackedEntityOuDimensionWithItems();
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        rawContextParams(registrationOu, "ou");
    QueryContext queryContext = QueryContext.of(ctx, new SqlParameterManager());

    assertTrue(
        AggregateQueryBuilder.getGroupedDimensionKeys(ctx).contains(registrationOu.getKey()),
        "the registration org unit is grouped");
    assertFalse(
        AggregateQueryBuilder.isGroupedInAggregate(queryContext, registrationOu),
        "a tracked entity dimension must keep its own builder's restriction");

    RenderableSqlQuery query =
        builder.buildSqlQuery(queryContext, List.of(), List.of(registrationOu), List.of());

    assertEquals(
        List.of(),
        query.getGroupableConditions(),
        "this builder must not contribute a restriction for a tracked entity dimension");
  }

  private DimensionIdentifier<DimensionParam> trackedEntityOuDimensionWithItems() {
    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid("a04CZxe0PSe");

    return DimensionIdentifier.of(
            ElementWithOffset.emptyElementWithOffset(),
            ElementWithOffset.emptyElementWithOffset(),
            DimensionParam.ofObject(
                new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(orgUnit)),
                DimensionParamType.DIMENSIONS,
                UID,
                List.of("a04CZxe0PSe")))
        .withDefaultGroupId();
  }

  /** The builder claims a sort on a groupable scoped dimension so it can own the order clause. */
  @Test
  void sortingFilterClaimsGroupableScopedDimensions() {
    AnalyticsSortingParams scoped =
        AnalyticsSortingParams.builder()
            .index(0)
            .orderBy(eventScopedDimension(StaticDimension.OU))
            .sortDirection(SortDirection.ASC)
            .build();

    assertTrue(builder.getSortingFilters().stream().allMatch(f -> f.test(scoped)));
  }

  @Test
  void filterOnGroupedDateDoesNotAddAGroupOrChangeItsBucket() {
    DimensionIdentifier<DimensionParam> date =
        eventScopedDimension(StaticDimension.EVENT_DATE, "2021").withDefaultGroupId();
    DimensionIdentifier<DimensionParam> filter =
        DimensionIdentifier.of(
                date.getProgram(),
                date.getProgramStage(),
                DimensionParam.ofObject(
                    "EVENT_DATE", DimensionParamType.FILTERS, UID, List.of("GE:2021-07-01")))
            .withDefaultGroupId();
    var ctx =
        contextParams(date).toBuilder()
            .commonParsed(
                CommonParsedParams.builder().dimensionIdentifiers(List.of(filter, date)).build())
            .build();
    var query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(filter, date),
            List.of());

    assertEquals(1, query.getGroupByFields().size());
    assertEquals(2, query.getSelectFields().size());
    assertTrue(query.getGroupByFields().get(0).render().contains("\"yearly\""));
    assertEquals(2, query.getGroupableConditions().size());
    assertFalse(
        RootConditionRenderer.of(query.getGroupableConditions()).render().contains(" or "),
        "dimension and filter restrictions must intersect");
  }

  @Test
  void groupedStageAndValueReadTheSameJoinedEventEvenWhenItsValueIsMissing() {
    var dimension = eventScopedDimension(StaticDimension.OU);
    DataElement dataElement = new DataElement();
    dataElement.setUid(DATA_ELEMENT_UID);
    dataElement.setValueType(ValueType.NUMBER);
    var ctx = contextParams(dimension);
    ctx =
        ctx.toBuilder()
            .typedParsed(
                ctx.getTypedParsed().toBuilder()
                    .eventValue(
                        new EventValue(dimension.getProgramStage().getElement(), dataElement, 0))
                    .aggregationType(AggregationType.AVERAGE)
                    .build())
            .build();
    var query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(dimension),
            List.of());

    assertEquals(1, query.getLeftJoins().size());
    assertFalse(query.getLeftJoins().get(0).render().contains("jsonb_exists"));
    assertEquals("ev.\"ou\"", query.getGroupByFields().get(0).render());
    assertTrue(query.getSelectFields().get(1).render().contains("ev.\"eventdatavalues\""));
  }

  /**
   * A snapshot cannot choose which tied event wins. Instead, verify that all coordinates, the
   * restriction, the sort and the value reference one joined selection, with no independent
   * subquery that could choose a different tied row.
   */
  @Test
  void matchingGroupedFieldsRestrictionsAndSortingShareOneEventSelection() {
    var ou = eventScopedDimension(StaticDimension.OU);
    var status = eventScopedDimension(StaticDimension.EVENT_STATUS, "ACTIVE");
    var date = eventScopedDimension(StaticDimension.EVENT_DATE);
    DataElement dataElement = new DataElement();
    dataElement.setUid(DATA_ELEMENT_UID);
    dataElement.setValueType(ValueType.NUMBER);
    var valueDimension =
        DimensionIdentifier.of(
            ou.getProgram(),
            ou.getProgramStage(),
            DimensionParam.ofObject(
                new QueryItem(dataElement), DimensionParamType.DIMENSIONS, UID, List.of()));
    var dimensions = List.of(ou, status, date, valueDimension);
    var ctx = contextParams(ou);
    ctx =
        ctx.toBuilder()
            .typedParsed(
                ctx.getTypedParsed().toBuilder()
                    .eventValue(new EventValue(ou.getProgramStage().getElement(), dataElement, 0))
                    .aggregationType(AggregationType.AVERAGE)
                    .build())
            .commonRaw(
                new CommonRequestParams()
                    .withDimension(
                        Set.of(
                            ou.getKey(), status.getKey(), date.getKey(), valueDimension.getKey())))
            .commonParsed(CommonParsedParams.builder().dimensionIdentifiers(dimensions).build())
            .build();
    var sort =
        AnalyticsSortingParams.builder()
            .index(0)
            .orderBy(status)
            .sortDirection(SortDirection.ASC)
            .build();

    var query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()), List.of(), dimensions, List.of(sort));

    assertEquals(1, query.getLeftJoins().size());
    var groups = query.getGroupByFields().stream().map(Field::render).toList();
    assertEquals(4, groups.size());
    assertEquals(
        List.of("ev.\"ou\"", "ev.\"status\"", "ev.\"occurreddate\""), groups.subList(0, 3));
    assertTrue(groups.get(3).contains("ev.\"eventdatavalues\""));
    assertTrue(groups.stream().noneMatch(expression -> expression.contains("select")));
    assertEquals(5, query.getSelectFields().size());
    assertTrue(query.getSelectFields().get(4).render().contains("ev.\"eventdatavalues\""));
    assertEquals(
        "ev.\"status\" in (:1)", RootConditionRenderer.of(query.getGroupableConditions()).render());
    assertEquals(1, query.getOrderClauses().size());
    assertTrue(query.getOrderClauses().get(0).getRenderable().render().contains("ev.\"status\""));
    assertFalse(query.getOrderClauses().get(0).getRenderable().render().contains("select"));
  }

  @ParameterizedTest
  @CsvSource({"0,0,true", "-1,-1,true", "1,1,true", "0,-1,false", "1,0,false"})
  void eventValueSharingRespectsEffectiveOffset(
      int groupedOffset, int valueOffset, boolean shared) {
    var dimension = eventScopedDimensionWithOffset(StaticDimension.OU, groupedOffset);
    DataElement dataElement = new DataElement();
    dataElement.setUid(DATA_ELEMENT_UID);
    dataElement.setValueType(ValueType.NUMBER);
    var ctx = contextParams(dimension);
    ctx =
        ctx.toBuilder()
            .typedParsed(
                ctx.getTypedParsed().toBuilder()
                    .eventValue(
                        new EventValue(
                            dimension.getProgramStage().getElement(), dataElement, valueOffset))
                    .aggregationType(AggregationType.COUNT)
                    .build())
            .build();
    var query =
        builder.buildSqlQuery(
            QueryContext.of(ctx, new SqlParameterManager()),
            List.of(),
            List.of(dimension),
            List.of());
    assertEquals(!shared, query.getLeftJoins().get(0).render().contains("jsonb_exists"));
    assertEquals(shared, query.getGroupByFields().get(0).render().equals("ev.\"ou\""));
  }

  private ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> rawContextParams(
      DimensionIdentifier<DimensionParam> dimension, String rawDimension) {
    return ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
        .typedParsed(
            TrackedEntityQueryParams.builder()
                .aggregate(true)
                .trackedEntityType(trackedEntityType)
                .build())
        .commonRaw(new CommonRequestParams().withDimension(Set.of(rawDimension)))
        .commonParsed(CommonParsedParams.builder().dimensionIdentifiers(List.of(dimension)).build())
        .build();
  }

  private DimensionIdentifier<DimensionParam> eventScopedDimensionWithOffset(
      StaticDimension staticDimension, int offset) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            staticDimension.name(), DimensionParamType.DIMENSIONS, UID, List.of());

    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setTrackedEntityType(trackedEntityType);

    ProgramStage programStage = new ProgramStage();
    programStage.setUid(PROGRAM_STAGE_UID);
    programStage.setProgram(program);

    // The parser applies a stage offset to both the program and the stage.
    return DimensionIdentifier.of(
        ElementWithOffset.of(program, offset),
        ElementWithOffset.of(programStage, offset),
        dimensionParam);
  }

  private ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams(
      DimensionIdentifier<DimensionParam> dimension) {
    return ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
        .typedParsed(
            TrackedEntityQueryParams.builder()
                .aggregate(true)
                .trackedEntityType(trackedEntityType)
                .build())
        .commonRaw(new CommonRequestParams().withDimension(Set.of(dimension.getKey())))
        .commonParsed(CommonParsedParams.builder().dimensionIdentifiers(List.of(dimension)).build())
        .build();
  }

  private DimensionIdentifier<DimensionParam> eventScopedDimension(
      StaticDimension staticDimension, String... items) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            staticDimension.name(), DimensionParamType.DIMENSIONS, UID, List.of(items));

    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setTrackedEntityType(trackedEntityType);

    ProgramStage programStage = new ProgramStage();
    programStage.setUid(PROGRAM_STAGE_UID);
    programStage.setProgram(program);

    // A request without an explicit offset parses to no offset, not to offset zero.
    return DimensionIdentifier.of(
        ElementWithOffset.of(program), ElementWithOffset.of(programStage), dimensionParam);
  }
}
