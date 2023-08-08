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
import org.hisp.dhis.tracker.export.event.EventParams;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@With
@Value
public class TrackedEntityParams {
  public static final TrackedEntityParams TRUE =
      new TrackedEntityParams(true, TrackedEntityEnrollmentParams.TRUE, true, true);

  public static final TrackedEntityParams FALSE =
      new TrackedEntityParams(false, TrackedEntityEnrollmentParams.FALSE, false, false);

  boolean includeRelationships;

  TrackedEntityEnrollmentParams teEnrollmentParams;

  boolean includeProgramOwners;

  boolean includeAttributes;

  public boolean isIncludeEnrollments() {
    return teEnrollmentParams.isIncludeEnrollments();
  }

  public EnrollmentParams getEnrollmentParams() {
    return this.teEnrollmentParams.getEnrollmentParams();
  }

  public EventParams getEventParams() {
    return getEnrollmentParams().getEnrollmentEventsParams().getEventParams();
  }
}
