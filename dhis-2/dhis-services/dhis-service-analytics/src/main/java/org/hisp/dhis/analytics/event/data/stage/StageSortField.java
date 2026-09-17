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
package org.hisp.dhis.analytics.event.data.stage;

import static org.hisp.dhis.analytics.event.data.OrganisationUnitResolver.STAGE_OU_CODE_COLUMN;
import static org.hisp.dhis.analytics.event.data.OrganisationUnitResolver.STAGE_OU_NAME_COLUMN;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.OU_CODE_COLUMN_NAME;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.OU_COLUMN_NAME;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.OU_NAME_COLUMN_NAME;
import static org.hisp.dhis.analytics.table.EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.common.ColumnHeader;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.QueryItem;

/**
 * Stage-prefixed fields accepted by {@code asc}/{@code desc} on the Event and Enrollment query
 * endpoints, and the columns each field orders by.
 *
 * <p>Both the request parser and the SQL layer read this table, so the accepted suffixes and the
 * ordering columns cannot drift apart. A field is requested as {@code <stageUid>.<suffix>} where
 * the suffix is either the output name the same API uses for headers ({@code ouname}, {@code
 * eventdate}, ...) or the canonical dimension token ({@code ou}, {@code EVENT_DATE}, ...).
 */
@Getter
@RequiredArgsConstructor
public enum StageSortField {
  OU(OU_COLUMN_NAME, OU_COLUMN_NAME, OU_COLUMN_NAME, OU_COLUMN_NAME, Cte.VALUE_COLUMN),
  OU_NAME(
      ColumnHeader.ORG_UNIT_NAME.getItem(),
      OU_COLUMN_NAME,
      OU_NAME_COLUMN_NAME,
      OU_COLUMN_NAME,
      STAGE_OU_NAME_COLUMN),
  OU_CODE(
      ColumnHeader.ORG_UNIT_CODE.getItem(),
      OU_COLUMN_NAME,
      OU_CODE_COLUMN_NAME,
      OU_COLUMN_NAME,
      STAGE_OU_CODE_COLUMN),
  EVENT_DATE(
      ColumnHeader.EVENT_DATE.getItem(),
      TimeField.EVENT_DATE.name(),
      OCCURRED_DATE_COLUMN_NAME,
      OCCURRED_DATE_COLUMN_NAME,
      Cte.VALUE_COLUMN),
  SCHEDULED_DATE(
      ColumnHeader.SCHEDULED_DATE.getItem(),
      TimeField.SCHEDULED_DATE.name(),
      SCHEDULED_DATE_COLUMN_NAME,
      SCHEDULED_DATE_COLUMN_NAME,
      Cte.VALUE_COLUMN);

  /** Output name of the field, as used by {@code headers=} (e.g. {@code eventdate}). */
  private final String outputName;

  /** Dimension token resolved to validate the stage (e.g. {@code EVENT_DATE}). */
  private final String canonicalDimension;

  /** Item id carried by the sort item. On the Event endpoint this is also the column ordered by. */
  private final String itemId;

  /** Item id of the stage CTE the field is read from. Several fields can share one CTE. */
  private final String canonicalItemId;

  /** Column of the stage CTE the Enrollment endpoint orders by. */
  private final String enrollmentCteColumn;

  /**
   * Returns the field requested by a stage-prefixed sort suffix, matching the output name or the
   * canonical dimension token case-insensitively.
   */
  public static Optional<StageSortField> forRequestedSuffix(String suffix) {
    return Arrays.stream(values())
        .filter(
            field ->
                field.outputName.equalsIgnoreCase(suffix)
                    || field.canonicalDimension.equalsIgnoreCase(suffix))
        .findFirst();
  }

  /** Returns the field whose sort item carries the given item id. */
  public static Optional<StageSortField> forItemId(String itemId) {
    return Arrays.stream(values()).filter(field -> field.itemId.equals(itemId)).findFirst();
  }

  /**
   * Returns the stage-scoped item whose CTE this field is read from, keeping the sort item's stage,
   * program and offset so both resolve to the same CTE key.
   */
  public QueryItem toCanonicalItem(QueryItem sortItem) {
    if (canonicalItemId.equals(sortItem.getItemId())) {
      return sortItem;
    }
    QueryItem canonical =
        new QueryItem(
            new BaseDimensionalItemObject(canonicalItemId),
            sortItem.getProgram(),
            sortItem.getLegendSet(),
            sortItem.getValueType(),
            sortItem.getAggregationType(),
            sortItem.getOptionSet());
    canonical.setProgramStage(sortItem.getProgramStage());
    canonical.setRepeatableStageParams(sortItem.getRepeatableStageParams());
    return canonical;
  }

  /** Holds the CTE column name so enum constants can reference it during initialisation. */
  private static final class Cte {
    private static final String VALUE_COLUMN = "value";

    private Cte() {}
  }
}
