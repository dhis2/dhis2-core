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
package org.hisp.dhis.common;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.program.EnrollmentStatus;

/**
 * @author Jan Bernitt
 */
@Getter
@Setter
@NoArgsConstructor
public class EnrollmentAnalyticsQueryCriteria extends AnalyticsPagingCriteria {
  private Date startDate;

  private Date endDate;

  /** Date interval for enrollment date; */
  private String enrollmentDate;

  /**
   * Time interval for incident date;
   *
   * @deprecated use @see {@link #occurredDate} instead
   */
  @Deprecated(since = "2.42")
  private String incidentDate;

  private String occurredDate;

  /** Time interval for last updated date */
  private String lastUpdated;

  /** Time interval for created date */
  private String createdDate;

  /** Time interval for completed date */
  private String completedDate;

  /** Time interval for event date */
  private String eventDate;

  /** Time interval for scheduled date */
  private String scheduledDate;

  private String timeField;

  private Set<String> dimension = new HashSet<>();

  private Set<String> filter = new HashSet<>();

  /**
   * This parameter selects the headers to be returned as part of the response. The implementation
   * for this Set will be LinkedHashSet as the ordering is important.
   */
  private Set<String> headers = new HashSet<>();

  private OrganisationUnitSelectionMode ouMode;

  private Set<String> asc = new HashSet<>();

  private Set<String> desc = new HashSet<>();

  private boolean skipMeta;

  private boolean skipData;

  private boolean skipRounding;

  private boolean completedOnly;

  private boolean hierarchyMeta;

  private boolean showHierarchy;

  private boolean coordinatesOnly;

  private boolean includeMetadataDetails;

  private IdScheme dataIdScheme;

  /**
   * Identifier scheme to use for metadata items the query response, can be identifier, code or
   * attributes. ( options: UID | CODE | ATTRIBUTE:<ID> )
   */
  private IdScheme outputIdScheme;

  private Set<EnrollmentStatus> programStatus = new HashSet<>();

  private DisplayProperty displayProperty;

  private Date relativePeriodDate;

  private String userOrgUnit;

  private String coordinateField;

  private SortOrder sortOrder;

  private boolean totalPages;

  /** flag to enable enhanced OR conditions on queryItem dimensions/filters */
  private boolean enhancedConditions;

  /** flag to enable row context in grid response */
  private boolean rowContext;

  /** Returns true when parameters are incoming from analytics enrollments/aggregate endpoint. */
  public boolean isAggregatedEnrollments() {
    return isAggregateEndpoint() && isEnrollmentEndpointItem();
  }
}
