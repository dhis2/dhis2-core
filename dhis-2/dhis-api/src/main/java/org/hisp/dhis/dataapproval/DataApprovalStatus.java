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
package org.hisp.dhis.dataapproval;

import java.util.Date;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.user.User;

/**
 * Current status of data approval for a given selection of data from a data set. Returns the
 * approval state and, if approved for this particular selection, approval information.
 *
 * @author Jim Grace
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DataApprovalStatus {
  /** State of data approval for a given selection of data from a data set. */
  @Setter private DataApprovalState state;

  /**
   * If the selection of data is approved, the data approval level object at which it is approved.
   * If the selection is approved at more than one level, this is for the highest level of approval.
   */
  private final DataApprovalLevel approvedLevel;

  /**
   * If the selection of data is approved, the ID of the highest organisation unit at which there is
   * approval.
   */
  private final int approvedOrgUnitId;

  /**
   * If the selection of data is approved, the approval level (same as above) but if the selection
   * is not approved, the level for this orgUnit at which it could be approved (if any).
   */
  private final DataApprovalLevel actionLevel;

  /** If the selection is approved, the OrganisationUnit UID. */
  private final String organisationUnitUid;

  /** If the selection is approved, the OrganisationUnit name. */
  private final String organisationUnitName;

  /** If the selection is approved, the attribute category option combo UID. */
  private final String attributeOptionComboUid;

  /** If the selection is approved, whether or not it is accepted at the highest level approved. */
  private final boolean accepted;

  /** Permissions granted for current user for the this approval state. */
  @Setter private DataApprovalPermissions permissions;

  /**
   * If the selection is approved, and if present (not always needed), the date at which the highest
   * level of approval was created.
   */
  @Setter private Date created;

  /**
   * If the selection is approved, and if present (not always needed), The user who made this
   * approval.
   */
  @Setter private User creator;

  /**
   * If the selection is approved, and if present (not always needed), the date at which the highest
   * level of approval was last updated.
   */
  @Setter private Date lastUpdated;

  /**
   * If the selection is approved, and if present (not always needed), The user who made the last
   * update.
   */
  @Setter private User lastUpdatedBy;
}
