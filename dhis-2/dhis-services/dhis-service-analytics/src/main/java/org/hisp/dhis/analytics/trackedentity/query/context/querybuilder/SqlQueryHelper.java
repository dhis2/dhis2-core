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
package org.hisp.dhis.analytics.trackedentity.query.context.querybuilder;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.text.StringSubstitutor.replace;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.isDataElement;
import static org.hisp.dhis.analytics.trackedentity.query.context.QueryContextConstants.TRACKED_ENTITY_ALIAS;
import static org.hisp.dhis.common.collection.CollectionUtils.mergeMaps;

import java.util.Map;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.OffsetHelper.Offset;

/**
 * Helper class that contains methods used along with query generation. It's mainly referenced in
 * implementers of {@link org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryBuilder}.
 */
@NoArgsConstructor(access = PRIVATE)
class SqlQueryHelper {

  private static final String ENROLLMENT_ORDER_BY_SUBQUERY =
      """
          (select ${selectedEnrollmentField}
           from (select *,
                 row_number() over ( partition by trackedentity
                                     order by enrollmentdate ${programOffsetDirection} ) as rn
                 from analytics_te_enrollment_${trackedEntityTypeUid}
                 where program = '${programUid}'
                   and t_1.trackedentity = trackedentity) en
           where en.rn = ${programOffset})""";

  private static final String EVENT_ORDER_BY_SUBQUERY =
      """
          (select ${selectedEventField}
           from (select *,
                 row_number() over ( partition by enrollment
                                     order by occurreddate ${programStageOffsetDirection} ) as rn
                 from analytics_te_event_${trackedEntityTypeUid} events
                 where programstage = '${programStageUid}'
                   and enrollment = %s
                   and status != 'SCHEDULE') ev
           where ev.rn = ${programStageOffset})"""
          .formatted(ENROLLMENT_ORDER_BY_SUBQUERY);

  private static final String DATA_VALUES_ORDER_BY_SUBQUERY =
      """
          (select ${dataElementField}
           from analytics_te_event_${trackedEntityTypeUid}
           where event = %s)"""
          .formatted(EVENT_ORDER_BY_SUBQUERY);

  private static final String ENROLLMENT_EXISTS_SUBQUERY =
      """
          exists(select 1
                 from (select *
                       from (select *, row_number() over (partition by trackedentity order by enrollmentdate ${programOffsetDirection}) as rn
                             from analytics_te_enrollment_${trackedEntityTypeUid}
                             where program = '${programUid}'
                               and trackedentity = t_1.trackedentity) en
                       where en.rn = 1) as "${enrollmentSubqueryAlias}"
                 where ${enrollmentCondition})""";

  private static final String EVENT_EXISTS_SUBQUERY =
      replace(
          ENROLLMENT_EXISTS_SUBQUERY,
          Map.of(
              "enrollmentCondition",
              """
                  exists(select 1
                         from (select *
                               from (select *, row_number() over ( partition by enrollment order by occurreddate ${programStageOffsetDirection} ) as rn
                                     from analytics_te_event_${trackedEntityTypeUid}
                                     where "${enrollmentSubqueryAlias}".enrollment = enrollment
                                       and programstage = '${programStageUid}'
                                       and status != 'SCHEDULE') ev
                               where ev.rn = 1) as "${eventSubqueryAlias}"
                         where ${eventCondition})"""));

  private static final String DATA_VALUES_EXISTS_SUBQUERY =
      replace(
          EVENT_EXISTS_SUBQUERY,
          Map.of(
              "eventCondition",
              """
                  exists(select 1
                         from analytics_te_event_${trackedEntityTypeUid}
                         where "${eventSubqueryAlias}".event = event
                           and ${eventDataValueCondition})"""));

  /**
   * Builds the order by sub-query for the given dimension identifier and field.
   *
   * @param dimId the dimension identifier
   * @param field the renderable field on which to eventually sort by
   * @return the renderable order by sub-query
   */
  static Renderable buildOrderSubQuery(
      DimensionIdentifier<DimensionParam> dimId, Renderable field) {
    if (isDataElement(dimId)) {
      return () ->
          replace(
              DATA_VALUES_ORDER_BY_SUBQUERY,
              mergeMaps(
                  getEnrollmentPlaceholders(dimId),
                  getEventPlaceholders(dimId),
                  Map.of(
                      "selectedEnrollmentField", "enrollment",
                      "selectedEventField", "event",
                      "dataElementField", field.render())));
    }
    if (dimId.isEventDimension()) {
      return () ->
          replace(
              EVENT_ORDER_BY_SUBQUERY,
              mergeMaps(
                  getEnrollmentPlaceholders(dimId),
                  getEventPlaceholders(dimId),
                  Map.of(
                      "selectedEnrollmentField",
                      "enrollment",
                      "selectedEventField",
                      field.render())));
    }
    if (dimId.isEnrollmentDimension()) {
      return () ->
          replace(
              ENROLLMENT_ORDER_BY_SUBQUERY,
              mergeMaps(
                  getEnrollmentPlaceholders(dimId),
                  Map.of("selectedEnrollmentField", field.render())));
    }
    if (dimId.isTeDimension()) {
      return Field.of(TRACKED_ENTITY_ALIAS, field, StringUtils.EMPTY);
    }
    throw new IllegalArgumentException("Unsupported dimension type: " + dimId);
  }

  /**
   * Builds the exists value sub-query for the given dimension identifier and condition.
   *
   * @param dimId the dimension identifier
   * @param condition the condition to apply
   * @return the renderable exists value sub-query
   */
  public static Renderable buildExistsValueSubquery(
      DimensionIdentifier<DimensionParam> dimId, Renderable condition) {
    if (isDataElement(dimId)) {
      return () ->
          replace(
              DATA_VALUES_EXISTS_SUBQUERY,
              mergeMaps(
                  getEnrollmentPlaceholders(dimId),
                  getEventPlaceholders(dimId),
                  Map.of(
                      "eventSubqueryAlias", dimId.getPrefix(),
                      "enrollmentSubqueryAlias", "enrollmentSubqueryAlias",
                      "eventDataValueCondition", condition.render())));
    }
    if (dimId.isEventDimension() && !isDataElement(dimId)) {
      return () ->
          replace(
              EVENT_EXISTS_SUBQUERY,
              mergeMaps(
                  getEnrollmentPlaceholders(dimId),
                  getEventPlaceholders(dimId),
                  Map.of(
                      "eventSubqueryAlias", dimId.getPrefix(),
                      "enrollmentSubqueryAlias", "enrollmentSubqueryAlias",
                      "eventCondition", condition.render())));
    }
    if (dimId.isEnrollmentDimension()) {
      return () ->
          replace(
              ENROLLMENT_EXISTS_SUBQUERY,
              mergeMaps(
                  getEnrollmentPlaceholders(dimId),
                  Map.of(
                      "enrollmentSubqueryAlias", dimId.getPrefix(),
                      "enrollmentCondition", condition.render())));
    }
    if (dimId.isTeDimension()) {
      return condition;
    }
    throw new IllegalArgumentException("Unsupported dimension type: " + dimId);
  }

  /**
   * Returns the placeholders for the event.
   *
   * @param dimId the dimension identifier
   * @return the placeholders
   */
  private static Map<String, String> getEventPlaceholders(
      DimensionIdentifier<DimensionParam> dimId) {

    String programStageUid = dimId.getProgramStage().getElement().getUid();
    Offset programStageOffset =
        OffsetHelper.getOffset(dimId.getProgramStage().getOffsetWithDefault());

    return Map.of(
        "programStageUid",
        programStageUid,
        "programStageOffset",
        programStageOffset.offset(),
        "programStageOffsetDirection",
        programStageOffset.direction());
  }

  /**
   * Returns the placeholders for the enrollment.
   *
   * @param dimId the dimension identifier
   * @return the placeholders
   */
  private static Map<String, String> getEnrollmentPlaceholders(
      DimensionIdentifier<DimensionParam> dimId) {

    String trackedEntityTypeUid = dimId.getProgram().getElement().getTrackedEntityType().getUid();

    String programUid = dimId.getProgram().getElement().getUid();
    Offset programOffset = OffsetHelper.getOffset(dimId.getProgram().getOffsetWithDefault());

    return Map.of(
        "trackedEntityTypeUid", StringUtils.lowerCase(trackedEntityTypeUid),
        "programUid", programUid,
        "programOffset", programOffset.offset(),
        "programOffsetDirection", programOffset.direction());
  }
}
