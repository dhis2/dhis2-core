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
package org.hisp.dhis.tracker.export.trackedentity;

import lombok.Value;
import lombok.With;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentParams;

/**
 * @author Luca Camnbi
 *     <p>Class used to define inclusion in {@link TrackedEntityParams} of {@link EnrollmentParams}
 *     properties
 */
@With
@Value
public class TrackedEntityEnrollmentParams {
  public static final TrackedEntityEnrollmentParams FALSE =
      new TrackedEntityEnrollmentParams(false, EnrollmentParams.FALSE);

  public static final TrackedEntityEnrollmentParams TRUE =
      new TrackedEntityEnrollmentParams(true, EnrollmentParams.TRUE);

  boolean includeEnrollments;

  EnrollmentParams enrollmentParams;

  public TrackedEntityEnrollmentParams withIncludeEvents(boolean includeEvents) {
    return this.enrollmentParams.isIncludeEvents() == includeEvents
        ? this
        : new TrackedEntityEnrollmentParams(
            includeEnrollments, enrollmentParams.withIncludeEvents(includeEvents));
  }

  public TrackedEntityEnrollmentParams withIncludeRelationships(boolean includeRelationships) {
    return this.enrollmentParams.isIncludeRelationships() == includeRelationships
        ? this
        : new TrackedEntityEnrollmentParams(
            includeEnrollments, enrollmentParams.withIncludeRelationships(includeRelationships));
  }

  public TrackedEntityEnrollmentParams withIncludeAttributes(boolean includeAttributes) {
    return this.enrollmentParams.isIncludeAttributes() == includeAttributes
        ? this
        : new TrackedEntityEnrollmentParams(
            includeEnrollments, enrollmentParams.withIncludeAttributes(includeAttributes));
  }

  public boolean isIncludeEvents() {
    return enrollmentParams.isIncludeEvents();
  }

  public boolean isIncludeRelationships() {
    return enrollmentParams.isIncludeRelationships();
  }

  public boolean isIncludeAttributes() {
    return enrollmentParams.isIncludeAttributes();
  }
}
