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
package org.hisp.dhis.analytics.tei.query.context.querybuilder;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.text.StringSubstitutor.replace;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.isDataElement;
import static org.hisp.dhis.analytics.tei.query.context.QueryContextConstants.ANALYTICS_TEI_ENR;
import static org.hisp.dhis.analytics.tei.query.context.QueryContextConstants.ANALYTICS_TEI_EVT;
import static org.hisp.dhis.analytics.tei.query.context.QueryContextConstants.PS_UID;
import static org.hisp.dhis.analytics.tei.query.context.QueryContextConstants.P_UID;
import static org.hisp.dhis.analytics.tei.query.context.QueryContextConstants.TEI_ALIAS;

import java.util.Map;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.tei.query.context.querybuilder.OffsetHelper.Offset;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlParameterManager;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;

/**
 * Helper class that contains methods used along with query generation. It's mainly referenced in
 * implementers of {@link org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilder}.
 */
@NoArgsConstructor(access = PRIVATE)
class SqlQueryHelper {

  private static final String ENROLLMENT_ORDER_BY_SUBQUERY =
      """
          (select ${selectedEnrollmentField}
           from (select *,
                 row_number() over ( partition by trackedentityinstanceuid
                                     order by enrollmentdate ${programOffsetDirection} ) as rn
                 from analytics_tei_enrollments_${trackedEntityTypeUid}
                 where programuid = '${programUid}'
                   and t_1.trackedentityinstanceuid = trackedentityinstanceuid) en
           where en.rn = ${programOffset})""";

  private static final String EVENT_ORDER_BY_SUBQUERY =
      """
          (select ${selectedEventField}
           from (select *,
                 row_number() over ( partition by programinstanceuid
                                     order by occurreddate ${programStageOffsetDirection} ) as rn
                 from analytics_tei_events_${trackedEntityTypeUid} events
                 where programstageuid = '${programStageUid}'
                   and programinstanceuid = %s) ev
           where ev.rn = ${programStageOffset})"""
          .formatted(ENROLLMENT_ORDER_BY_SUBQUERY);

  private static final String DATA_VALUES_ORDER_BY_SUBQUERY =
      """
          (select ${dataElementField}
           from analytics_tei_events_${trackedEntityTypeUid}
           where programstageinstanceuid = %s)"""
          .formatted(EVENT_ORDER_BY_SUBQUERY);

  static String enrollmentSelect(
      ElementWithOffset<Program> program,
      TrackedEntityType trackedEntityType,
      SqlParameterManager sqlParameterManager) {
    Offset offset = OffsetHelper.getOffset(program.getOffsetWithDefault());

    return "select innermost_enr.*"
        + " from (select *,"
        + " row_number() over (partition by trackedentityinstanceuid order by enrollmentdate "
        + offset.direction()
        + ") as rn "
        + " from "
        + ANALYTICS_TEI_ENR
        + trackedEntityType.getUid().toLowerCase()
        + " where "
        + P_UID
        + " = "
        + sqlParameterManager.bindParamAndGetIndex(program.getElement().getUid())
        + ") innermost_enr"
        + " where innermost_enr.rn = "
        + offset.offset();
  }

  static String eventSelect(
      ElementWithOffset<Program> program,
      ElementWithOffset<ProgramStage> programStage,
      TrackedEntityType trackedEntityType,
      SqlParameterManager sqlParameterManager) {
    Offset offset = OffsetHelper.getOffset(programStage.getOffsetWithDefault());

    return "select innermost_evt.*"
        + " from (select *,"
        + " row_number() over (partition by programinstanceuid order by occurreddate "
        + offset.direction()
        + ", created "
        + offset.direction()
        + " ) as rn"
        + " from "
        + ANALYTICS_TEI_EVT
        + trackedEntityType.getUid().toLowerCase()
        + " where status != '"
        + EventStatus.SCHEDULE
        + "' and "
        + P_UID
        + " = "
        + sqlParameterManager.bindParamAndGetIndex(program.getElement().getUid())
        + " and "
        + PS_UID
        + " = "
        + sqlParameterManager.bindParamAndGetIndex(programStage.getElement().getUid())
        + ") innermost_evt"
        + " where innermost_evt.rn = "
        + offset.offset();
  }

  /**
   * Builds the order by sub-query for the given dimension identifier and field.
   *
   * @param dimId the dimension identifier
   * @param field the renderable field on which to eventually sort by
   * @return the renderable order by sub-query
   */
  static Renderable buildOrderSubQuery(
      DimensionIdentifier<DimensionParam> dimId, Renderable field) {
    String rendered = field.render();
    if (isDataElement(dimId)) {
      return () ->
          replace(
              DATA_VALUES_ORDER_BY_SUBQUERY,
              CollectionUtils.merge(
                  getEnrollmentPlaceholders(dimId, rendered),
                  getEventPlaceholders(dimId, rendered),
                  getDataElementPlaceholders(rendered)));
    }
    if (dimId.isEventDimension() && !isDataElement(dimId)) {
      return () ->
          replace(
              EVENT_ORDER_BY_SUBQUERY,
              CollectionUtils.merge(
                  getEnrollmentPlaceholders(dimId, rendered),
                  getEventPlaceholders(dimId, rendered)));
    }
    if (dimId.isEnrollmentDimension()) {
      return () ->
          replace(ENROLLMENT_ORDER_BY_SUBQUERY, getEnrollmentPlaceholders(dimId, rendered));
    }
    if (dimId.isTeiDimension()) {
      return Field.of(TEI_ALIAS, field, StringUtils.EMPTY);
    }
    throw new IllegalArgumentException("Unsupported dimension type: " + dimId);
  }

  /**
   * Returns the placeholders for the data element field.
   *
   * @param field the field to render
   * @return the placeholders
   */
  private static Map<String, String> getDataElementPlaceholders(String field) {
    return Map.of(
        "selectedEnrollmentField", "programInstanceUid",
        "selectedEventField", "programStageInstanceUid",
        "dataElementField", field);
  }

  /**
   * Returns the placeholders for the event field.
   *
   * @param dimId the dimension identifier
   * @param field the field to render
   * @return the placeholders
   */
  private static Map<String, String> getEventPlaceholders(
      DimensionIdentifier<DimensionParam> dimId, String field) {

    String programStageUid = dimId.getProgramStage().getElement().getUid();
    Offset programStageOffset =
        OffsetHelper.getOffset(dimId.getProgramStage().getOffsetWithDefault());

    return Map.of(
        "selectedEnrollmentField",
        "programInstanceUid",
        "selectedEventField",
        field,
        "programStageUid",
        programStageUid,
        "programStageOffset",
        programStageOffset.offset(),
        "programStageOffsetDirection",
        programStageOffset.direction());
  }

  /**
   * Returns the placeholders for the enrollment field.
   *
   * @param dimId the dimension identifier
   * @param field the field to render
   * @return the placeholders
   */
  private static Map<String, String> getEnrollmentPlaceholders(
      DimensionIdentifier<DimensionParam> dimId, String field) {

    String trackedEntityTypeUid = dimId.getProgram().getElement().getTrackedEntityType().getUid();

    String programUid = dimId.getProgram().getElement().getUid();
    Offset programOffset = OffsetHelper.getOffset(dimId.getProgram().getOffsetWithDefault());

    return Map.of(
        "selectedEnrollmentField", field,
        "trackedEntityTypeUid", StringUtils.lowerCase(trackedEntityTypeUid),
        "programUid", programUid,
        "programOffset", programOffset.offset(),
        "programOffsetDirection", programOffset.direction());
  }
}
