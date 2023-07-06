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
package org.hisp.dhis.common;

import java.util.Date;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.analytics.UserOrgUnitType;

/**
 * This class contains all the criteria that can be used to execute a DHIS2 analytics query using
 * the AnalyticsController
 */
@Data
@NoArgsConstructor
public class AggregateAnalyticsQueryCriteria {
  /** The analytics dimensions. */
  private Set<String> dimension;

  /** Filters to apply to the analytics query. */
  private Set<String> filter;

  /** The {@link AggregationType} */
  private AggregationType aggregationType;

  /** Filters for the data/measures (options: EQ | GT | GE | LT | LE). */
  private String measureCriteria;

  /**
   * Filters for the data/measure, applied before aggregation is performed. (options: EQ | GT | GE |
   * LT | LE)
   */
  private String preAggregationMeasureCriteria;

  /**
   * Start date for a date range. Will be applied as a filter. Can not be used together with a
   * period dimension or filter.
   */
  private Date startDate;

  /**
   * End date for date range. Will be applied as a filter. Can not be used together with a period
   * dimension or filter.
   */
  private Date endDate;

  /** The type of user org unit to use. */
  private UserOrgUnitType userOrgUnitType;

  /** The {@link SortOrder}. */
  private SortOrder order;

  /**
   * The time field to base event aggregation on. Applies to event data items only. Can be a
   * predefined option or the ID of an attribute or data element with a time-based value type.
   */
  private String timeField;

  /**
   * The organisation unit field to base event aggregation on. Applies to event data items only. Can
   * be the ID of an attribute or data element with the organisation unit value type. The default
   * option is specified as omitting the query parameter.
   */
  private String orgUnitField;

  /** Whether meta-data should be excluded from the response. */
  private boolean skipMeta;

  /** Whether data should be excluded from the response. */
  private boolean skipData;

  /** Skip rounding of data values, i.e. provide full precision. */
  private boolean skipRounding;

  /** Whether to only show completed events. */
  private boolean completedOnly;

  /**
   * Whether to include names of organisation unit ancestors and hierarchy paths of organisation
   * units in the metadata.
   */
  private boolean hierarchyMeta;

  /** Whether to ignore the limit on max 50 000 records in response. */
  private boolean ignoreLimit;

  /** Whether to hide empty rows in response, applicable when table layout is true. */
  private boolean hideEmptyRows;

  /** Whether to hide empty columns in response, applicable when table layout is true. */
  private boolean hideEmptyColumns;

  /** Whether to display full org unit hierarchy path together with org unit name. */
  private boolean showHierarchy;

  /**
   * Whether to include the numerator and denominator used to calculate the value in the response.
   */
  private boolean includeNumDen;

  /** Whether to include metadata details to raw data response. */
  private boolean includeMetadataDetails;

  /** Property to display for metadata. */
  private DisplayProperty displayProperty;

  /**
   * Identifier scheme to use for metadata items the query response, can be identifier, code or
   * attributes. ( options: UID | CODE | ATTRIBUTE:<ID> )
   */
  private IdScheme outputIdScheme;

  /**
   * Identifier scheme to use for metadata items the query response. Specific to org units. See
   * {@link IdScheme} for valid values.
   */
  private IdScheme outputOrgUnitIdScheme;

  /**
   * Identifier scheme to use for metadata items the query response. Specific to data elements. See
   * {@link IdScheme} for valid values.
   */
  private IdScheme outputDataElementIdScheme;

  /**
   * Identifier scheme to use for metadata items in the query request, can be an identifier, code or
   * attributes. (options: UID | CODE | ATTRIBUTE:<ID>) )
   */
  private IdScheme inputIdScheme;

  /**
   * Include data which has been approved at least up to the given approval level, refers to
   * identifier of approval level.
   */
  private String approvalLevel;

  /** Date identifier e.g: "2016-01-01". Overrides the start date of the relative period. */
  private Date relativePeriodDate;

  /**
   * Organisation unit identifiers, overrides organisation units associated with current user,
   * single or array.
   */
  private String userOrgUnit;

  /** Data dimensions to include in table as columns. */
  private String columns;

  /** Data dimensions to include in table as rows. */
  private String rows;
}
