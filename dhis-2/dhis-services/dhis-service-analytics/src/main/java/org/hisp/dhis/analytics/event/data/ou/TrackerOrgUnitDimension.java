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
package org.hisp.dhis.analytics.event.data.ou;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.Getter;
import org.hisp.dhis.analytics.common.ColumnHeader;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Org unit dimensions of tracker programs that are read from an org unit column of the event row.
 * Each is resolved like the standard {@code ou} dimension, but against its own column, by joining
 * the org unit structure table under its own alias.
 */
public enum TrackerOrgUnitDimension {
  ENROLLMENT_OU(
      "ENROLLMENT_OU",
      EventAnalyticsColumnName.ENROLLMENT_OU_COLUMN_NAME,
      "enrous",
      ColumnHeader.ENROLLMENT_OU,
      ColumnHeader.ENROLLMENT_OU_NAME,
      EventQueryParams::getEnrollmentOuDimensionItems,
      EventQueryParams::getEnrollmentOuFilterItems,
      EventQueryParams::hasEnrollmentOuDimension),
  REGISTRATION_OU(
      "REGISTRATION_OU",
      EventAnalyticsColumnName.REGISTRATION_OU_COLUMN_NAME,
      "regous",
      ColumnHeader.REGISTRATION_OU,
      ColumnHeader.REGISTRATION_OU_NAME,
      EventQueryParams::getRegistrationOuDimensionItems,
      EventQueryParams::getRegistrationOuFilterItems,
      EventQueryParams::hasRegistrationOuDimension);

  @Getter
  /* The dimension keyword as written in the request. */
  private final String dimensionName;

  @Getter
  /* The column of the event analytics table holding the org unit UID. */
  private final String analyticsColumn;

  @Getter
  /*
   * The alias of the org unit structure table joined for this dimension. Distinct from the {@code
   * ous} alias of the default org unit join and from the other dimensions, so all can appear in one
   * query.
   */
  private final String structAlias;

  @Getter
  /* The output column holding the org unit UID. */
  private final ColumnHeader uidHeader;

  @Getter
  /* The output column holding the org unit name. */
  private final ColumnHeader nameHeader;

  private final Function<EventQueryParams, List<OrganisationUnit>> dimensionItems;

  private final Function<EventQueryParams, List<OrganisationUnit>> filterItems;

  private final Predicate<EventQueryParams> dimensionRequested;

  TrackerOrgUnitDimension(
      String dimensionName,
      String analyticsColumn,
      String structAlias,
      ColumnHeader uidHeader,
      ColumnHeader nameHeader,
      Function<EventQueryParams, List<OrganisationUnit>> dimensionItems,
      Function<EventQueryParams, List<OrganisationUnit>> filterItems,
      Predicate<EventQueryParams> dimensionRequested) {
    this.dimensionName = dimensionName;
    this.analyticsColumn = analyticsColumn;
    this.structAlias = structAlias;
    this.uidHeader = uidHeader;
    this.nameHeader = nameHeader;
    this.dimensionItems = dimensionItems;
    this.filterItems = filterItems;
    this.dimensionRequested = dimensionRequested;
  }

  /** The output columns of the query endpoint, holding the org unit UID and name. */
  public Set<String> outputColumns() {
    return Set.of(uidHeader.getItem(), nameHeader.getItem());
  }

  /** The org units requested as a dimension, carrying their hierarchy level. */
  public List<OrganisationUnit> dimensionItems(EventQueryParams params) {
    return dimensionItems.apply(params);
  }

  /** The org units requested as a filter, carrying their hierarchy level. */
  public List<OrganisationUnit> filterItems(EventQueryParams params) {
    return filterItems.apply(params);
  }

  /** True if the dimension was requested, which may be without items on the query endpoint. */
  public boolean isDimensionRequested(EventQueryParams params) {
    return dimensionRequested.test(params);
  }

  /** True if the dimension was named at all, as a dimension or as a filter. */
  public boolean isRequested(EventQueryParams params) {
    return isDimensionRequested(params) || !filterItems(params).isEmpty();
  }

  /**
   * True if the dimension carries org units, which is the condition for the aggregate
   * disaggregation column to exist.
   */
  public boolean hasAggregateColumn(EventQueryParams params) {
    return !dimensionItems(params).isEmpty();
  }
}
