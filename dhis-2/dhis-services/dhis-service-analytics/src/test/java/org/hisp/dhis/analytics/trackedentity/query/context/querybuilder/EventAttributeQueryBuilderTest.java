/*
 * Copyright (c) 2004-2025, University of Oslo
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

import java.util.Collections;
import java.util.List;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventAttributeQueryBuilderTest {

  private EventAttributeQueryBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new EventAttributeQueryBuilder();
  }

  @Test
  void testHeaderFilterAcceptsEventDateAtEventLevel() {
    DimensionIdentifier<DimensionParam> eventDateDimension =
        createEventLevelDimension(StaticDimension.EVENT_DATE);

    boolean accepted =
        builder.getHeaderFilters().stream().allMatch(p -> p.test(eventDateDimension));

    assertTrue(accepted);
  }

  @Test
  void testFilterAcceptsEventDateAtEventLevel() {
    DimensionIdentifier<DimensionParam> eventDateDimension =
        createEventLevelDimension(StaticDimension.EVENT_DATE);

    boolean accepted =
        builder.getDimensionFilters().stream().allMatch(p -> p.test(eventDateDimension));

    assertTrue(accepted);
  }

  @Test
  void testFilterAcceptsScheduledDateAtEventLevel() {
    DimensionIdentifier<DimensionParam> scheduledDateDimension =
        createEventLevelDimension(StaticDimension.SCHEDULED_DATE);

    boolean accepted =
        builder.getDimensionFilters().stream().allMatch(p -> p.test(scheduledDateDimension));

    assertTrue(accepted);
  }

  @Test
  void testFilterAcceptsOuAtEventLevel() {
    DimensionIdentifier<DimensionParam> ouDimension = createEventLevelDimension(StaticDimension.OU);

    boolean accepted = builder.getDimensionFilters().stream().allMatch(p -> p.test(ouDimension));

    assertTrue(accepted);
  }

  @Test
  void testFilterAcceptsEventStatusAtEventLevel() {
    DimensionIdentifier<DimensionParam> eventStatusDimension =
        createEventLevelDimension(StaticDimension.EVENT_STATUS);

    boolean accepted =
        builder.getDimensionFilters().stream().allMatch(p -> p.test(eventStatusDimension));

    assertTrue(accepted);
  }

  @Test
  void testFilterRejectsEnrollmentStatusAtEventLevel() {
    DimensionIdentifier<DimensionParam> enrollmentStatusDimension =
        createEventLevelDimension(StaticDimension.ENROLLMENT_STATUS);

    boolean accepted =
        builder.getDimensionFilters().stream().allMatch(p -> p.test(enrollmentStatusDimension));

    assertFalse(accepted);
  }

  @Test
  void testFilterRejectsEventDateAtEnrollmentLevel() {
    DimensionIdentifier<DimensionParam> eventDateDimension =
        createEnrollmentLevelDimension(StaticDimension.EVENT_DATE);

    boolean accepted =
        builder.getDimensionFilters().stream().allMatch(p -> p.test(eventDateDimension));

    assertFalse(accepted);
  }

  @Test
  void testFilterRejectsEventDateAtTeLevel() {
    DimensionIdentifier<DimensionParam> eventDateDimension =
        createTeLevelDimension(StaticDimension.EVENT_DATE);

    boolean accepted =
        builder.getDimensionFilters().stream().allMatch(p -> p.test(eventDateDimension));

    assertFalse(accepted);
  }

  @Test
  void testHeaderOnlyEventDateGeneratesNonVirtualField() {
    DimensionIdentifier<DimensionParam> eventDateHeader =
        createEventLevelDimension(StaticDimension.EVENT_DATE);

    RenderableSqlQuery result =
        builder.buildSqlQuery(
            null, List.of(eventDateHeader), Collections.emptyList(), Collections.emptyList());

    List<Field> nonVirtualFields =
        result.getSelectFields().stream().filter(f -> !f.isVirtual()).toList();

    assertEquals(1, nonVirtualFields.size());
    String rendered = nonVirtualFields.get(0).render();
    assertTrue(rendered.contains("occurreddate"), "Expected 'occurreddate' in: " + rendered);
    assertTrue(rendered.contains("eventdate"), "Expected 'eventdate' in: " + rendered);
  }

  @Test
  void testHeaderOnlyScheduledDateGeneratesNonVirtualField() {
    DimensionIdentifier<DimensionParam> scheduledDateHeader =
        createEventLevelDimension(StaticDimension.SCHEDULED_DATE);

    RenderableSqlQuery result =
        builder.buildSqlQuery(
            null, List.of(scheduledDateHeader), Collections.emptyList(), Collections.emptyList());

    List<Field> nonVirtualFields =
        result.getSelectFields().stream().filter(f -> !f.isVirtual()).toList();

    assertEquals(1, nonVirtualFields.size());
    assertTrue(nonVirtualFields.get(0).render().contains("scheduleddate"));
    assertTrue(nonVirtualFields.get(0).render().contains("coalesce(occurreddate, scheduleddate)"));
  }

  @Test
  void testHeaderOnlyOuGeneratesNonVirtualField() {
    DimensionIdentifier<DimensionParam> ouHeader = createEventLevelDimension(StaticDimension.OU);

    RenderableSqlQuery result =
        builder.buildSqlQuery(
            null, List.of(ouHeader), Collections.emptyList(), Collections.emptyList());

    List<Field> nonVirtualFields =
        result.getSelectFields().stream().filter(f -> !f.isVirtual()).toList();

    assertEquals(1, nonVirtualFields.size());
    String rendered = nonVirtualFields.get(0).render();
    assertTrue(rendered.contains("ev.\"ou\""));
    assertTrue(rendered.contains("status != 'SCHEDULE'"));
  }

  @Test
  void testHeaderOnlyEventStatusGeneratesNonVirtualField() {
    DimensionIdentifier<DimensionParam> eventStatusHeader =
        createEventLevelDimension(StaticDimension.EVENT_STATUS);

    RenderableSqlQuery result =
        builder.buildSqlQuery(
            null, List.of(eventStatusHeader), Collections.emptyList(), Collections.emptyList());

    List<Field> nonVirtualFields =
        result.getSelectFields().stream().filter(f -> !f.isVirtual()).toList();

    assertEquals(1, nonVirtualFields.size());
    String rendered = nonVirtualFields.get(0).render();
    assertTrue(rendered.contains("ev.\"status\""));
    assertTrue(rendered.contains("coalesce(occurreddate, scheduleddate)"));
  }

  @Test
  void testDimensionWithFilterGeneratesVirtualField() {
    DimensionIdentifier<DimensionParam> eventDateDimension =
        createEventLevelDimensionWithRestrictions(StaticDimension.EVENT_DATE, List.of("2022"));

    RenderableSqlQuery result =
        builder.buildSqlQuery(
            null, Collections.emptyList(), List.of(eventDateDimension), Collections.emptyList());

    List<Field> virtualFields = result.getSelectFields().stream().filter(Field::isVirtual).toList();

    assertEquals(1, virtualFields.size());
  }

  @Test
  void testHeaderMatchingDimensionDoesNotDuplicateField() {
    DimensionIdentifier<DimensionParam> eventDateDimension =
        createEventLevelDimensionWithRestrictions(StaticDimension.EVENT_DATE, List.of("2022"));

    RenderableSqlQuery result =
        builder.buildSqlQuery(
            null,
            List.of(eventDateDimension),
            List.of(eventDateDimension),
            Collections.emptyList());

    assertEquals(1, result.getSelectFields().size());
    assertTrue(result.getSelectFields().get(0).isVirtual());
  }

  @Test
  void testSortingScheduledDateIncludesSchedule() {
    DimensionIdentifier<DimensionParam> scheduledDateDimension =
        createEventLevelDimension(StaticDimension.SCHEDULED_DATE);

    AnalyticsSortingParams sortingParams =
        AnalyticsSortingParams.builder()
            .index(0)
            .orderBy(scheduledDateDimension)
            .sortDirection(SortDirection.ASC)
            .build();

    RenderableSqlQuery result =
        builder.buildSqlQuery(
            null, Collections.emptyList(), Collections.emptyList(), List.of(sortingParams));

    assertEquals(1, result.getOrderClauses().size());
    String renderedOrder = result.getOrderClauses().get(0).getRenderable().render();
    assertTrue(
        renderedOrder.contains("coalesce(occurreddate, scheduleddate)"),
        "Expected schedule-inclusive ordering in: " + renderedOrder);
    assertFalse(
        renderedOrder.contains("status != 'SCHEDULE'"),
        "Did not expect schedule exclusion in: " + renderedOrder);
  }

  private DimensionIdentifier<DimensionParam> createEventLevelDimension(
      StaticDimension staticDimension) {
    return createEventLevelDimensionWithRestrictions(staticDimension, List.of());
  }

  private DimensionIdentifier<DimensionParam> createEventLevelDimensionWithRestrictions(
      StaticDimension staticDimension, List<String> restrictions) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            staticDimension.name(), DimensionParamType.DIMENSIONS, UID, restrictions);

    Program program = createProgram();
    ProgramStage programStage = createProgramStage();

    return DimensionIdentifier.of(
        ElementWithOffset.of(program, 0), ElementWithOffset.of(programStage, 0), dimensionParam);
  }

  private DimensionIdentifier<DimensionParam> createEnrollmentLevelDimension(
      StaticDimension staticDimension) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            staticDimension.name(), DimensionParamType.DIMENSIONS, UID, List.of());

    Program program = createProgram();

    return DimensionIdentifier.of(
        ElementWithOffset.of(program, 0),
        ElementWithOffset.emptyElementWithOffset(),
        dimensionParam);
  }

  private DimensionIdentifier<DimensionParam> createTeLevelDimension(
      StaticDimension staticDimension) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            staticDimension.name(), DimensionParamType.DIMENSIONS, UID, List.of());

    return DimensionIdentifier.of(
        ElementWithOffset.emptyElementWithOffset(),
        ElementWithOffset.emptyElementWithOffset(),
        dimensionParam);
  }

  private Program createProgram() {
    Program program = new Program();
    program.setUid("programUid");
    TrackedEntityType tet = new TrackedEntityType();
    tet.setUid("tetUid");
    program.setTrackedEntityType(tet);
    return program;
  }

  private ProgramStage createProgramStage() {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid("programStageUid");
    return programStage;
  }
}
