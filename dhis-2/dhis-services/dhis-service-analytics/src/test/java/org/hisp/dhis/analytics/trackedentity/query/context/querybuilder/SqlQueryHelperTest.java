/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.common.UidObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SqlQueryHelperTest {

  @Mock private DimensionParam dimensionParam;

  @Mock private DimensionIdentifier<DimensionParam> testedDimension;

  @BeforeEach
  void beforeEach() {
    when(testedDimension.getDimension()).thenReturn(dimensionParam);
  }

  @Test
  void test_throws_when_undetected_type() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SqlQueryHelper.buildOrderSubQuery(testedDimension, () -> "field"));

    assertThrows(
        IllegalArgumentException.class,
        () -> SqlQueryHelper.buildExistsValueSubquery(testedDimension, () -> "field"));
  }

  @Test
  void test_subQuery_TE() {
    when(testedDimension.isTeDimension()).thenReturn(true);

    assertEquals(
        "t_1.\"field\"",
        SqlQueryHelper.buildOrderSubQuery(testedDimension, () -> "field").render());

    assertEquals(
        "field", SqlQueryHelper.buildExistsValueSubquery(testedDimension, () -> "field").render());
  }

  @Test
  void test_subQuery_enrollment() {
    when(testedDimension.getPrefix()).thenReturn("prefix");

    TrackedEntityType trackedEntityType = mock(TrackedEntityType.class);
    when(trackedEntityType.getUid()).thenReturn("trackedEntityType");

    ElementWithOffset<Program> program =
        mockElementWithOffset(
            Program.class,
            "programUid",
            p -> when(p.getTrackedEntityType()).thenReturn(trackedEntityType));

    when(testedDimension.getProgram()).thenReturn(program);

    when(testedDimension.isEnrollmentDimension()).thenReturn(true);

    assertEquals(
        """
        (select field
         from (select *,
               row_number() over ( partition by trackedentity
                                   order by enrollmentdate desc ) as rn
               from analytics_te_enrollment_trackedentitytype
               where program = 'programUid'
                 and t_1.trackedentity = trackedentity) en
         where en.rn = 1)""",
        SqlQueryHelper.buildOrderSubQuery(testedDimension, () -> "field").render());

    assertEquals(
        """
        exists(select 1
               from (select *
                     from (select *, row_number() over (partition by trackedentity order by enrollmentdate desc) as rn
                           from analytics_te_enrollment_trackedentitytype
                           where program = 'programUid'
                             and trackedentity = t_1.trackedentity) en
                     where en.rn = 1) as "prefix"
               where field)""",
        SqlQueryHelper.buildExistsValueSubquery(testedDimension, () -> "field").render());
  }

  @Test
  void test_subQuery_event() {
    when(testedDimension.getPrefix()).thenReturn("prefix");

    TrackedEntityType trackedEntityType = mock(TrackedEntityType.class);
    when(trackedEntityType.getUid()).thenReturn("trackedEntityType");

    ElementWithOffset<Program> program =
        mockElementWithOffset(
            Program.class,
            "programUid",
            p -> when(p.getTrackedEntityType()).thenReturn(trackedEntityType));

    ElementWithOffset<ProgramStage> programStage =
        mockElementWithOffset(ProgramStage.class, "programStageUid");

    when(testedDimension.getProgram()).thenReturn(program);
    when(testedDimension.getProgramStage()).thenReturn(programStage);

    when(testedDimension.isEventDimension()).thenReturn(true);

    assertEquals(
        """
        (select field
         from (select *,
               row_number() over ( partition by enrollment
                                   order by occurreddate desc ) as rn
               from analytics_te_event_trackedentitytype events
               where programstage = 'programStageUid'
                 and enrollment = (select enrollment
         from (select *,
               row_number() over ( partition by trackedentity
                                   order by enrollmentdate desc ) as rn
               from analytics_te_enrollment_trackedentitytype
               where program = 'programUid'
                 and t_1.trackedentity = trackedentity) en
         where en.rn = 1)
                 and status != 'SCHEDULE') ev
         where ev.rn = 1)""",
        SqlQueryHelper.buildOrderSubQuery(testedDimension, () -> "field").render());

    assertEquals(
        """
        exists(select 1
               from (select *
                     from (select *, row_number() over (partition by trackedentity order by enrollmentdate desc) as rn
                           from analytics_te_enrollment_trackedentitytype
                           where program = 'programUid'
                             and trackedentity = t_1.trackedentity) en
                     where en.rn = 1) as "enrollmentSubqueryAlias"
               where exists(select 1
               from (select *
                     from (select *, row_number() over ( partition by enrollment order by occurreddate desc ) as rn
                           from analytics_te_event_trackedentitytype
                           where "enrollmentSubqueryAlias".enrollment = enrollment
                             and programstage = 'programStageUid'
                             and status != 'SCHEDULE') ev
                     where ev.rn = 1) as "prefix"
               where field))""",
        SqlQueryHelper.buildExistsValueSubquery(testedDimension, () -> "field").render());
  }

  @Test
  void test_subQuery_data_element() {
    when(dimensionParam.isOfType(any())).thenReturn(true);
    when(testedDimension.getPrefix()).thenReturn("prefix");

    TrackedEntityType trackedEntityType = mock(TrackedEntityType.class);
    when(trackedEntityType.getUid()).thenReturn("trackedEntityType");

    ElementWithOffset<Program> program =
        mockElementWithOffset(
            Program.class,
            "programUid",
            p -> when(p.getTrackedEntityType()).thenReturn(trackedEntityType));

    ElementWithOffset<ProgramStage> programStage =
        mockElementWithOffset(ProgramStage.class, "programStageUid");

    when(testedDimension.getProgram()).thenReturn(program);
    when(testedDimension.getProgramStage()).thenReturn(programStage);

    when(testedDimension.isEventDimension()).thenReturn(true);

    assertEquals(
        """
        (select field
         from analytics_te_event_trackedentitytype
         where event = (select event
         from (select *,
               row_number() over ( partition by enrollment
                                   order by occurreddate desc ) as rn
               from analytics_te_event_trackedentitytype events
               where programstage = 'programStageUid'
                 and enrollment = (select enrollment
         from (select *,
               row_number() over ( partition by trackedentity
                                   order by enrollmentdate desc ) as rn
               from analytics_te_enrollment_trackedentitytype
               where program = 'programUid'
                 and t_1.trackedentity = trackedentity) en
         where en.rn = 1)
                 and status != 'SCHEDULE') ev
         where ev.rn = 1))""",
        SqlQueryHelper.buildOrderSubQuery(testedDimension, () -> "field").render());

    assertEquals(
        """
        exists(select 1
               from (select *
                     from (select *, row_number() over (partition by trackedentity order by enrollmentdate desc) as rn
                           from analytics_te_enrollment_trackedentitytype
                           where program = 'programUid'
                             and trackedentity = t_1.trackedentity) en
                     where en.rn = 1) as "enrollmentSubqueryAlias"
               where exists(select 1
               from (select *
                     from (select *, row_number() over ( partition by enrollment order by occurreddate desc ) as rn
                           from analytics_te_event_trackedentitytype
                           where "enrollmentSubqueryAlias".enrollment = enrollment
                             and programstage = 'programStageUid'
                             and status != 'SCHEDULE') ev
                     where ev.rn = 1) as "prefix"
               where exists(select 1
               from analytics_te_event_trackedentitytype
               where "prefix".event = event
                 and field)))""",
        SqlQueryHelper.buildExistsValueSubquery(testedDimension, () -> "field").render());
  }

  private <T extends UidObject> ElementWithOffset<T> mockElementWithOffset(
      Class<T> clazz, String uid) {
    return mockElementWithOffset(clazz, uid, element -> {});
  }

  private <T extends UidObject> ElementWithOffset<T> mockElementWithOffset(
      Class<T> clazz, String uid, Consumer<T> consumer) {
    ElementWithOffset<T> elementWithOffset = mock(ElementWithOffset.class);
    T element = mock(clazz);
    consumer.accept(element);
    when(elementWithOffset.getElement()).thenReturn(element);
    when(element.getUid()).thenReturn(uid);
    return elementWithOffset;
  }
}
