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

import java.util.Collections;
import java.util.Set;
import lombok.Value;

/**
 * Query parameter to select events based on their assigned users. See {@link
 * AssignedUserSelectionMode} for the different selection modes.
 */
@Value
public class AssignedUserQueryParam {

  public static final AssignedUserQueryParam ALL =
      new AssignedUserQueryParam(AssignedUserSelectionMode.ALL, Collections.emptySet(), null);

  AssignedUserSelectionMode mode;

  Set<UID> assignedUsers;

  /**
   * Non-empty assigned users are only allowed with mode PROVIDED (or null).
   *
   * @param mode assigned user mode
   * @param assignedUsers assigned user uids
   */
  public AssignedUserQueryParam(AssignedUserSelectionMode mode, Set<UID> assignedUsers, UID user) {
    if (mode == AssignedUserSelectionMode.PROVIDED
        && (assignedUsers == null || assignedUsers.isEmpty())) {
      throw new IllegalQueryException(
          "Assigned User uid(s) must be specified if selectionMode is PROVIDED");
    }
    // we default the mode to PROVIDED in case mode is null but users are
    // given
    if (mode != null
        && mode != AssignedUserSelectionMode.PROVIDED
        && (assignedUsers != null && !assignedUsers.isEmpty())) {
      throw new IllegalQueryException(
          "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED");
    }

    if (mode == AssignedUserSelectionMode.CURRENT) {
      this.mode = AssignedUserSelectionMode.PROVIDED;
      this.assignedUsers = Collections.singleton(user);
    } else if ((mode == null || mode == AssignedUserSelectionMode.PROVIDED)
        && (assignedUsers != null && !assignedUsers.isEmpty())) {
      this.mode = AssignedUserSelectionMode.PROVIDED;
      this.assignedUsers = assignedUsers;
    } else if (mode == null) {
      this.mode = AssignedUserSelectionMode.ALL;
      this.assignedUsers = Collections.emptySet();
    } else {
      this.mode = mode;
      this.assignedUsers = Collections.emptySet();
    }
  }

  public boolean hasAssignedUsers() {
    return !this.getAssignedUsers().isEmpty();
  }
}
