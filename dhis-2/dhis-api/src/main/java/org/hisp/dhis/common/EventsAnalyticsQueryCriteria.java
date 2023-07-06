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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStatus;

/**
 * This class contains all the criteria that can be used to execute a DHIS2 Events analytics query
 * using the EventAnalyticsController
 */
@Getter
@Setter
@NoArgsConstructor
public class EventsAnalyticsQueryCriteria extends AnalyticsPagingCriteria {
  // -------------------------------------------------------------------------
  // Event and aggregate analytics
  // -------------------------------------------------------------------------

  /** Program stage identifier. */
  private String stage;

  /** Start date for events. This is a mandatory field. */
  private Date startDate;

  /** End date for events. This is a mandatory field. */
  private Date endDate;

  /** Time interval for event date; */
  private String eventDate;

  /** Time interval for enrollment date; */
  private String enrollmentDate;

  /** Time interval for scheduled date; */
  private String scheduledDate;

  /** Time interval for incident date; */
  private String incidentDate;

  /** Time interval for last updated date; */
  private String lastUpdated;

  /**
   * Dimension identifier including data elements, attributes, program indicators, periods,
   * organisation units and organisation unit group sets. Parameter can be repeated any number of
   * times.
   */
  private Set<String> dimension;

  /**
   * Filters to apply to the analytics: a Set of identifiers including data elements, attributes,
   * periods, organisation units and organisation unit group sets. Parameter can be repeated any
   * number of times.
   */
  private Set<String> filter;

  /**
   * This parameter selects the headers to be returned as part of the response. The implementation
   * for this Set will be LinkedHashSet as the ordering is important.
   */
  private Set<String> headers;

  /**
   * Whether to include names of organisation unit ancestors and hierarchy paths of organisation
   * units in the metadata.
   */
  private boolean hierarchyMeta;

  /** Specify ths status of events to include. */
  private Set<EventStatus> eventStatus;

  /** Specify the enrollment status of events to include. */
  private Set<ProgramStatus> programStatus;

  /** Overrides the start date of the relative period. */
  private Date relativePeriodDate;

  /** Dimensions to use as columns for table layout. */
  private String columns;

  /** Dimensions to use as rows for table layout. */
  private String rows;

  // -------------------------------------------------------------------------
  // Event aggregate analytics only
  // -------------------------------------------------------------------------

  /**
   * Value dimension identifier. Can be a data element or an attribute which must be of numeric
   * value type.
   *
   * <p>Valid for aggregate event analytics only.
   */
  private String value;

  /** Aggregation type for the value dimension. Default is AVERAGE. */
  private AggregationType aggregationType;

  /** Whether to exclude the meta data part of the response. */
  private boolean skipMeta;

  /** Whether to exclude the data part of the response. */
  private boolean skipData;

  /** Whether to skip rounding of aggregate data values. */
  private boolean skipRounding;

  /** Whether to only show completed events. */
  private boolean completedOnly;

  /** Display full org unit hierarchy path together with org unit name. */
  private boolean showHierarchy;

  /** Sort the records on the value column in ascending or descending order. */
  private SortOrder sortOrder;

  /** The maximum number of records to return. */
  private Integer limit;

  /**
   * Specify output type for analytical data which can be events, enrollments or tracked entity
   * instances. The two last options apply to programs with registration only.
   *
   * <p>(options: EVENT | ENROLLMENT | TRACKED_ENTITY_INSTANCE )
   */
  private EventOutputType outputType = EventOutputType.EVENT;

  /**
   * Whether to collapse all data dimensions (data elements and attributes) into a single dimension
   * in the response.
   */
  private boolean collapseDataDimensions;

  /** Produce aggregate values for the data dimensions (as opposed to dimension items). */
  private boolean aggregateData;

  /** Whether to include metadata details to the response. */
  private boolean includeMetadataDetails;

  /** Property to display for metadata. */
  private DisplayProperty displayProperty;

  /**
   * The time field to base event aggregation on. Applies to event data items only. Can be a
   * predefined option or the ID of an attribute or data element having a time-based value type.
   */
  private String timeField;

  /**
   * The organisation unit field to base event aggregation on. Applies to event data items only. Can
   * be the ID of an attribute or data element with the Organisation unit value type. The default
   * option is specified as omitting the query parameter.
   */
  private String orgUnitField;

  private String userOrgUnit;

  /**
   * Field to base geospatial event analytics on. Default is event. Can be set to identifiers of
   * attributes and data elements of value type coordinate.
   */
  private String coordinateField;

  // -------------------------------------------------------------------------
  // Event query analytics only
  // -------------------------------------------------------------------------

  /**
   * Field to base geospatial event analytics on. Default is event. Can be set to identifiers of
   * attributes and data elements of value type coordinate.
   */
  private String fallbackCoordinateField;

  /**
   * The mode of selecting organisation units. Default is DESCENDANTS, meaning all sub units in the
   * hierarchy. CHILDREN refers to immediate children in the hierarchy; SELECTED refers to the
   * selected organisation units only.
   */
  private OrganisationUnitSelectionMode ouMode = OrganisationUnitSelectionMode.DESCENDANTS;

  /**
   * Dimensions identifier to be sorted ascending, can reference event date, org unit name and code
   * and any item identifiers.
   */
  private Set<String> asc;

  /**
   * Dimensions identifier to be sorted descending, can reference event date, org unit name and code
   * and any item identifiers.
   */
  private Set<String> desc;

  /** Whether to only return events which have coordinates. */
  private boolean coordinatesOnly;

  /** Whether to return events which ou coordinates when primary coordinates do not exist. */
  private boolean coordinateOuFallback;

  /**
   * d scheme to be used for data, more specifically data elements and attributes which have an
   * option set or legend set, e.g. return the name of the option instead of the code, or the name
   * of the legend instead of the legend ID, in the data response.
   */
  private IdScheme dataIdScheme;

  /**
   * Identifier scheme to use for metadata items the query response, can be identifier, code or
   * attributes. ( options: UID | CODE | ATTRIBUTE:<ID> )
   */
  private IdScheme outputIdScheme;

  /** flag to enable enhanced OR conditions on queryItem dimensions/filters */
  private boolean enhancedConditions;
}
