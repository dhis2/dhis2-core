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
package org.hisp.dhis.tracker.imports.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.artemis.MessageType;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.junit.jupiter.api.Test;

/**
 * @author Zubair Asghar
 */
class TrackerSideValidationEffectDataBundleTest {

  @Test
  void testNotificationDataBundleForEnrollment() {
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        new org.hisp.dhis.tracker.imports.domain.Enrollment();
    enrollment.setEnrollment(UID.of("ja8NY4PW7Xm"));
    UID enrollmentUid = UID.generate();
    TrackerNotificationDataBundle bundle =
        TrackerNotificationDataBundle.builder()
            .enrollmentNotifications(List.of())
            .accessedBy("testUser")
            .importStrategy(TrackerImportStrategy.CREATE)
            .object(enrollmentUid.getValue())
            .klass(Enrollment.class)
            .build();
    assertEquals(enrollmentUid.getValue(), bundle.getObject());
    assertEquals(Enrollment.class, bundle.getKlass());
    assertTrue(bundle.getEnrollmentNotifications().isEmpty());
    assertTrue(bundle.getTrackerEventNotifications().isEmpty());
    assertEquals(TrackerImportStrategy.CREATE, bundle.getImportStrategy());
    assertEquals(MessageType.TRACKER_SIDE_EFFECT, bundle.getMessageType());
  }

  @Test
  void testNotificationDataBundleForEvent() {
    TrackerEvent expected = new TrackerEvent();
    expected.setAutoFields();
    TrackerNotificationDataBundle bundle =
        TrackerNotificationDataBundle.builder()
            .trackerEventNotifications(List.of())
            .object(expected.getUid())
            .klass(TrackerEvent.class)
            .build();
    assertEquals(expected.getUid(), bundle.getObject());
    assertEquals(TrackerEvent.class, bundle.getKlass());
    assertTrue(bundle.getTrackerEventNotifications().isEmpty());
    assertTrue(bundle.getEnrollmentNotifications().isEmpty());
  }
}
