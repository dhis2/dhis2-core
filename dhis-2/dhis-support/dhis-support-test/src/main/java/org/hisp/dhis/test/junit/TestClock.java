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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Clock;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pins a test to a fixed instant so it never reads the wall clock. Tests must not depend on the
 * day, hour or timezone they happen to run on; annotating a test class with {@code @TestClock}
 * provides a deterministic {@link Clock} that the {@link FixedClockExtension} makes available to
 * the test.
 *
 * <p>The fixed {@link Clock} can be consumed in two ways:
 *
 * <ul>
 *   <li>declare a {@link Clock} field on the test class &ndash; the extension assigns the fixed
 *       clock to it before each test;
 *   <li>declare a {@link Clock} parameter on a test (or lifecycle) method &ndash; the extension
 *       resolves it.
 * </ul>
 *
 * <p>Pass the resolved clock into the code under test instead of letting that code call {@code
 * Instant.now()} / {@code new Date()} itself. For value objects this is a constructor overload that
 * defaults to {@link Clock#systemDefaultZone()} in production (see {@code
 * org.hisp.dhis.analytics.cache.TimeToLive}).
 *
 * <pre>{@code
 * @TestClock(instant = "2026-06-15T10:00:00Z")
 * class TimeToLiveTest {
 *   @Test
 *   void computesTtl(Clock clock) {
 *     ...
 *   }
 * }
 * }</pre>
 *
 * @see FixedClockExtension
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(FixedClockExtension.class)
public @interface TestClock {

  /** Instant the clock is fixed to, parsed with {@link java.time.Instant#parse}. */
  String instant() default "2026-06-15T10:00:00Z";

  /** Zone the clock reports, parsed with {@link java.time.ZoneId#of}. Defaults to UTC. */
  String zone() default "Z";
}
