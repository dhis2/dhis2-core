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
package org.hisp.dhis.tracker.imports.notification;

import java.util.Set;
import javax.annotation.Nullable;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.TrackerEvent;

/**
 * Notifications to send after an entity is persisted. Contains the entity and a deduplicated set of
 * notifications from both lifecycle triggers (enrollment creation, completion) and rule engine
 * evaluation (SENDMESSAGE/SCHEDULEMESSAGE).
 *
 * @param entity the persisted entity (Enrollment, TrackerEvent, or SingleEvent)
 * @param notifications deduplicated set of notifications to send. Each identifies a template by UID
 *     and optionally a scheduled date. Notifications with {@code scheduledAt == null} are sent
 *     immediately.
 */
public record EntityNotifications(IdentifiableObject entity, Set<Notification> notifications) {

  /** Returns the enrollment UID for repeatable key generation, or null for SingleEvents. */
  @Nullable
  public String enrollmentUid() {
    if (entity instanceof Enrollment enrollment) {
      return enrollment.getUid();
    }
    if (entity instanceof TrackerEvent event && event.getEnrollment() != null) {
      return event.getEnrollment().getUid();
    }
    return null;
  }
}
