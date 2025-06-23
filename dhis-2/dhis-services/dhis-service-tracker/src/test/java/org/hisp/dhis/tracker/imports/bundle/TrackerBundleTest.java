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
package org.hisp.dhis.tracker.imports.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Collections;
import org.hisp.dhis.tracker.imports.AtomicMode;
import org.hisp.dhis.tracker.imports.ValidationMode;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class TrackerBundleTest {

  @Test
  void testBasicSetup1() {
    TrackerBundle trackerBundle =
        TrackerBundle.builder()
            .atomicMode(AtomicMode.ALL)
            .validationMode(ValidationMode.SKIP)
            .trackedEntities(Collections.singletonList(new TrackedEntity()))
            .enrollments(Collections.singletonList(new Enrollment()))
            .trackerEvents(Collections.singletonList(new TrackerEvent()))
            .build();
    assertEquals(AtomicMode.ALL, trackerBundle.getAtomicMode());
    assertEquals(ValidationMode.SKIP, trackerBundle.getValidationMode());
    assertFalse(trackerBundle.getTrackedEntities().isEmpty());
    assertFalse(trackerBundle.getEnrollments().isEmpty());
    assertFalse(trackerBundle.getEvents().isEmpty());
  }

  @Test
  void testBasicSetup2() {
    TrackerBundle trackerBundle =
        TrackerBundle.builder()
            .atomicMode(AtomicMode.ALL)
            .validationMode(ValidationMode.SKIP)
            .trackedEntities(Arrays.asList(new TrackedEntity(), new TrackedEntity()))
            .enrollments(Arrays.asList(new Enrollment(), new Enrollment()))
            .trackerEvents(Arrays.asList(new TrackerEvent(), new TrackerEvent()))
            .build();
    assertEquals(AtomicMode.ALL, trackerBundle.getAtomicMode());
    assertEquals(ValidationMode.SKIP, trackerBundle.getValidationMode());
    assertEquals(2, trackerBundle.getTrackedEntities().size());
    assertEquals(2, trackerBundle.getEnrollments().size());
    assertEquals(2, trackerBundle.getEvents().size());
  }
}
