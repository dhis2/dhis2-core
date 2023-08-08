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
package org.hisp.dhis.dxf2.deprecated.tracker;

import lombok.Value;
import lombok.With;

/**
 * @deprecated this is a class related to "old" (deprecated) tracker which will be removed with
 *     "old" tracker. Make sure to plan migrating to new tracker.
 */
@With
@Value
@Deprecated(since = "2.41")
public class EnrollmentParams {
  public static final EnrollmentParams TRUE =
      new EnrollmentParams(EnrollmentEventsParams.TRUE, true, true, false, false);

  public static final EnrollmentParams FALSE =
      new EnrollmentParams(EnrollmentEventsParams.FALSE, false, false, false, false);

  private EnrollmentEventsParams enrollmentEventsParams;

  private boolean includeRelationships;

  private boolean includeAttributes;

  private boolean includeDeleted;

  private boolean dataSynchronizationQuery;

  public boolean isIncludeRelationships() {
    return includeRelationships;
  }

  public boolean isIncludeEvents() {
    return enrollmentEventsParams.isIncludeEvents();
  }

  public EnrollmentParams withIncludeEvents(boolean includeEvents) {
    return this.enrollmentEventsParams.isIncludeEvents() == includeEvents
        ? this
        : new EnrollmentParams(
            enrollmentEventsParams.withIncludeEvents(includeEvents),
            this.includeRelationships,
            this.includeAttributes,
            this.includeDeleted,
            this.dataSynchronizationQuery);
  }

  public boolean isIncludeAttributes() {
    return includeAttributes;
  }

  public boolean isIncludeDeleted() {
    return includeDeleted;
  }

  public boolean isDataSynchronizationQuery() {
    return dataSynchronizationQuery;
  }
}
