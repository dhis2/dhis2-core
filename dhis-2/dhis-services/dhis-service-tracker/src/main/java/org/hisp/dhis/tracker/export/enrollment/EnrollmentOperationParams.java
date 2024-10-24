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
package org.hisp.dhis.tracker.export.enrollment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.tracker.export.Order;

@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EnrollmentOperationParams {
  static final EnrollmentOperationParams EMPTY = EnrollmentOperationParams.builder().build();

  @Builder.Default private final EnrollmentParams enrollmentParams = EnrollmentParams.FALSE;

  /** Set of te uids to explicitly select. */
  @Builder.Default private final Set<UID> enrollments = new HashSet<>();

  /** Last updated for enrollment. */
  private final Date lastUpdated;

  /** The last updated duration filter. */
  private final String lastUpdatedDuration;

  /**
   * Organisation units for which instances in the response were registered at. Is related to the
   * specified OrganisationUnitMode.
   */
  @Builder.Default private final Set<UID> orgUnits = new HashSet<>();

  /** Selection mode for the specified organisation units. */
  private final OrganisationUnitSelectionMode orgUnitMode;

  /** Enrollments must be enrolled into this program. */
  private final UID program;

  /** Status of a tracked entities enrollment into a given program. */
  private final EnrollmentStatus enrollmentStatus;

  /** Indicates whether tracked entity is marked for follow up for the specified program. */
  private final Boolean followUp;

  /** Start date for enrollment in the given program. */
  private final Date programStartDate;

  /** End date for enrollment in the given program. */
  private final Date programEndDate;

  /** Tracked entity type of the tracked entity in the response. */
  private final UID trackedEntityType;

  /** Tracked entity. */
  private final UID trackedEntity;

  /** Indicates whether to include soft-deleted enrollments */
  private final boolean includeDeleted;

  private final List<Order> order;

  public static class EnrollmentOperationParamsBuilder {

    private final List<Order> order = new ArrayList<>();

    // Do not remove this unused method. This hides the order field from the builder which Lombok
    // does not support. The repeated order field and private order method prevent access to order
    // via the builder.
    // Order should be added via the orderBy builder methods.
    private EnrollmentOperationParamsBuilder order(List<Order> order) {
      return this;
    }

    public EnrollmentOperationParamsBuilder orderBy(String field, SortDirection direction) {
      this.order.add(new Order(field, direction));
      return this;
    }
  }
}
