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
package org.hisp.dhis.tracker.export.trackedentity.aggregates;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityIdentifiers;
import org.hisp.dhis.user.AuthenticationService;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component("org.hisp.dhis.tracker.trackedentity.aggregates.EnrollmentAggregate")
@RequiredArgsConstructor
class EnrollmentAggregate {
  private final AuthenticationService authenticationService;
  private final EnrollmentService enrollmentService;

  /**
   * Key: te uid , value Enrollment
   *
   * @param ids a List of {@see TrackedEntity} Primary Keys
   * @return a MultiMap where key is a {@see TrackedEntity} uid and the key a List of {@see
   *     Enrollment} objects
   */
  Multimap<String, Enrollment> findByTrackedEntityIds(
      List<TrackedEntityIdentifiers> ids, Context ctx) {
    Multimap<String, Enrollment> result = ArrayListMultimap.create();

    try {
      authenticationService.obtainAuthentication(ctx.getUserUid());
      ids.forEach(
          id -> {
            EnrollmentOperationParams params =
                EnrollmentOperationParams.builder()
                    .enrollmentParams(ctx.getParams().getEnrollmentParams())
                    .trackedEntity(UID.of(id.uid()))
                    .includeDeleted(ctx.getQueryParams().isIncludeDeleted())
                    .program(ctx.getQueryParams().getEnrolledInTrackerProgram())
                    .build();
            try {
              result.putAll(id.uid(), enrollmentService.findEnrollments(params));
            } catch (BadRequestException e) {
              throw new IllegalArgumentException(
                  "this must be a bug in how the EnrollmentOperationParams are built");
            } catch (ForbiddenException e) {
              // ForbiddenExceptions are caused when mapping the EnrollmentOperationParams. These
              // params should already have been validated as they are coming from the
              // TrackedEntityQueryParams. Other reasons the user does not have access to data will
              // not be shown as such items are simply not returned in collections.
            }
          });
    } catch (NotFoundException e) {
      throw new IllegalArgumentException(
          "this must be called within a context where the user is known to exist");
    } finally {
      authenticationService.clearAuthentication();
    }
    return result;
  }
}
