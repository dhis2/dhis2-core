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
package org.hisp.dhis.program.notification;

import java.util.Date;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;

/**
 * @author Zubair Asghar
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ProgramNotificationInstanceParam extends NotificationPagingParam {
  @Builder
  public ProgramNotificationInstanceParam(
      Integer page,
      Integer pageSize,
      boolean paging,
      Enrollment enrollment,
      TrackerEvent trackerEvent,
      SingleEvent singleEvent,
      Date scheduledAt) {
    super(page, pageSize, paging);
    this.enrollment = enrollment;
    this.trackerEvent = trackerEvent;
    this.singleEvent = singleEvent;
    this.scheduledAt = scheduledAt;
  }

  private Enrollment enrollment;

  private TrackerEvent trackerEvent;

  private SingleEvent singleEvent;

  private Date scheduledAt;

  public boolean hasEnrollment() {
    return enrollment != null;
  }

  public boolean hasTrackerEvent() {
    return trackerEvent != null;
  }

  public boolean hasSingleEvent() {
    return singleEvent != null;
  }

  public boolean hasScheduledAt() {
    return scheduledAt != null;
  }
}
