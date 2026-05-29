/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.test.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link FixedClockExtension} provides the {@link Clock} described by {@link TestClock}
 * through both field injection and parameter resolution, and that the same instant is observed on
 * every read.
 */
@TestClock(instant = "2026-06-15T10:00:00Z")
class FixedClockExtensionTest {

  private Clock injectedClock;

  @Test
  void shouldInjectFixedClockIntoField() {
    assertNotNull(injectedClock, "clock field should have been assigned before the test");
    assertEquals(Instant.parse("2026-06-15T10:00:00Z"), injectedClock.instant());
    assertEquals(ZoneOffset.UTC, injectedClock.getZone());
  }

  @Test
  void shouldResolveFixedClockAsParameter(Clock clock) {
    assertEquals(Instant.parse("2026-06-15T10:00:00Z"), clock.instant());
    assertSame(injectedClock, clock, "field and parameter should be the same cached clock");
  }

  @Test
  void shouldReturnTheSameInstantOnRepeatedReads() {
    assertEquals(injectedClock.instant(), injectedClock.instant());
  }
}
