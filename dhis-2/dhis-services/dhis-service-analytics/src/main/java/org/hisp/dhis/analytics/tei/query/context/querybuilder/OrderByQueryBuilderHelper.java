/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.apache.commons.text.StringSubstitutor.replace;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.isDataElement;
import static org.hisp.dhis.analytics.tei.query.context.QueryContextConstants.TEI_ALIAS;

import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.Renderable;

/** This class is responsible for building the order by sub-query for the different dimensions. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderByQueryBuilderHelper {

  private static final String ENROLLMENT_QUERY =
      """
      (select ${selectedEnrollmentField}
       from (select *, row_number() over (partition by trackedentityinstanceuid order by enrollmentdate desc) as rn
             from analytics_tei_enrollments_${trackedEntityTypeUid}
             where programuid = '${programUid}'
               and t_1.trackedentityinstanceuid = trackedentityinstanceuid) en
       where en.rn = ${programOffset})""";

  private static final String EVENT_QUERY =
      """
      (select ${selectedEventField}
       from (select *, row_number() over ( partition by programinstanceuid order by occurreddate desc ) as rn
             from analytics_tei_events_${trackedEntityTypeUid} events
             where programstageuid = '${programStageUid}'
               and programinstanceuid = %s) ev
       where ev.rn = ${programStageOffset})"""
          .formatted(ENROLLMENT_QUERY);

  private static final String DATA_VALUES_QUERY =
      """
      (select ${dataElementField}
       from analytics_tei_events_${trackedEntityTypeUid}
       where programstageinstanceuid = %s)"""
          .formatted(EVENT_QUERY);

  /**
   * Builds the order by sub-query for the given dimension identifier and field.
   *
   * @param dimId the dimension identifier
   * @param field the renderable field on which to eventually sort by
   * @return the renderable order by sub-query
   */
  public static Renderable buildOrderSubQuery(
      DimensionIdentifier<DimensionParam> dimId, Renderable field) {
    String rendered = field.render();
    if (isDataElement(dimId)) {
      return () ->
          replace(
              DATA_VALUES_QUERY,
              MiscHelper.merge(
                  getEnrollmentPlaceholders(dimId, rendered),
                  getEventPlaceholders(dimId, rendered),
                  getDataElementPlaceholders(rendered)));
    }
    if (dimId.isEventDimension() && !isDataElement(dimId)) {
      return () ->
          replace(
              EVENT_QUERY,
              MiscHelper.merge(
                  getEnrollmentPlaceholders(dimId, rendered),
                  getEventPlaceholders(dimId, rendered)));
    }
    if (dimId.isEnrollmentDimension()) {
      return () -> replace(ENROLLMENT_QUERY, getEnrollmentPlaceholders(dimId, rendered));
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
    String programStageOffset =
        OffsetHelper.getOffset(dimId.getProgramStage().getOffsetWithDefault());

    return Map.of(
        "selectedEnrollmentField", "programInstanceUid",
        "selectedEventField", field,
        "programStageUid", programStageUid,
        "programStageOffset", programStageOffset);
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
    String programOffset = OffsetHelper.getOffset(dimId.getProgram().getOffsetWithDefault());

    return Map.of(
        "selectedEnrollmentField", field,
        "trackedEntityTypeUid", trackedEntityTypeUid,
        "programUid", programUid,
        "programOffset", programOffset);
  }
}
