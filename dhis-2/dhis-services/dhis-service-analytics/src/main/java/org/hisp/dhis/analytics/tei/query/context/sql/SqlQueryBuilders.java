/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.tei.query.context.sql;

import static org.apache.commons.text.StringSubstitutor.replace;

import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamObjectType;

/** Utility class of common methods used in the SQL query builders. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SqlQueryBuilders {

  private static final String EVENT_QUERY =
      """
      select json_agg(json_build_object(
          'programStageUid', ev.programstageuid,
          'eventUid', ev.programstageinstanceuid,
          'occurredDate', ev.occurreddate,
          'dueDate', ev.scheduleddate,
          'orgUnitUid', ev.ou,
          'orgUnitName', ev.ouname,
          'orgUnitCode', ev.oucode,
          'orgUnitNameHierarchy', ev.ounamehierarchy,
          'eventStatus', ev.status,
          'eventDataValues', ev.eventdatavalues))
      from analytics_tei_events_${trackedEntityType} ev
      where ev.programinstanceuid = en.programinstanceuid""";

  private static final String ENROLLMENT_QUERY =
      replace(
          """
              select json_agg(
                         json_build_object(
                             'programUid', en.programuid,
                             'enrollmentUid', en.programinstanceuid,
                             'enrollmentDate', en.enrollmentdate,
                             'incidentDate', en.incidentdate,
                             'endDate', en.enddate,
                             'orgUnitUid', en.ou,
                             'orgUnitName', en.ouname,
                             'orgUnitCode', en.oucode,
                             'orgUnitNameHierarchy', en.ounamehierarchy,
                             'enrollmentStatus', en.enrollmentstatus,
                             'events', ${eventQuery}))
                    from analytics_tei_enrollments_${trackedEntityType} en
                    where en.trackedentityinstanceuid = t_1.trackedentityinstanceuid""",
          Map.of("eventQuery", coalesceToEmptyArray(EVENT_QUERY)));

  private static final String JSON_AGGREGATION_QUERY = coalesceToEmptyArray(ENROLLMENT_QUERY);

  private static String coalesceToEmptyArray(String query) {
    return replace("coalesce((${query}), '[]'::json)", Map.of("query", query));
  }

  public static boolean isNotPeriodDimension(
      DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    return !dimensionIdentifier.getDimension().isPeriodDimension();
  }

  public static boolean hasRestrictions(DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    return dimensionIdentifier.getDimension().hasRestrictions();
  }

  public static boolean isOfType(
      DimensionIdentifier<DimensionParam> dimensionParamDimensionIdentifier,
      DimensionParamObjectType type) {
    return dimensionParamDimensionIdentifier.getDimension().isOfType(type);
  }

  public static String getJsonAggregationQuery(String tetTableSuffix) {
    return replace(JSON_AGGREGATION_QUERY, Map.of("trackedEntityType", tetTableSuffix));
  }
}
