/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.acl;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service for fetching tracker programs (i.e., programs that require registration). ACL validations
 * are performed automatically based on the currently logged-in user.
 */
@Service("org.hisp.dhis.tracker.acl.TrackerProgramService")
@RequiredArgsConstructor
public class TrackerProgramService {

  @Nonnull private final ProgramService programService;
  @Nonnull private final AclService aclService;

  /** Retrieves the list of tracker programs accessible to the current user. */
  public @Nonnull List<Program> getAccessibleTrackerPrograms() {
    UserDetails user = CurrentUserUtil.getCurrentUserDetails();

    return programService.getAllPrograms().stream()
        .filter(p -> p.isRegistration() && aclService.canDataRead(user, p))
        .toList();
  }

  /**
   * Retrieves the list of tracker programs accessible to the current user that match the given
   * tracked entity type. It is assumed that the user has access to the supplied trackedEntityType.
   */
  public @Nonnull List<Program> getAccessibleTrackerPrograms(
      @Nonnull TrackedEntityType trackedEntityType) {
    UserDetails user = CurrentUserUtil.getCurrentUserDetails();

    return programService.getAllPrograms().stream()
        .filter(
            p ->
                p.isRegistration()
                    && Objects.equals(p.getTrackedEntityType().getUid(), trackedEntityType.getUid())
                    && aclService.canDataRead(user, p))
        .toList();
  }
}
