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
package org.hisp.dhis.analytics.cache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.analytics.cache.TimeToLive.DEFAULT_MULTIPLIER;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import org.hisp.dhis.test.junit.TestClock;
import org.junit.jupiter.api.Test;

/**
 * The TTL is the diff (in days) between an ending date and "now". Both the ending date and the
 * {@code now} read by {@link TimeToLive#compute()} are derived from the same fixed {@link Clock},
 * so a day-boundary crossing between the two reads can no longer shift the expected TTL.
 */
@TestClock(instant = "2026-06-15T10:00:00Z")
class TimeToLiveTest {

  /** Fixed by {@link TestClock}; passed into {@link TimeToLive} so test and code share "now". */
  private Clock clock;

  @Test
  void testComputeForCurrentDayWhenCacheFactorIsNegative() {
    int aNegativeCachingFactor = -1;
    Date endingDate = Date.from(clock.instant());

    assertThrows(
        IllegalArgumentException.class,
        () -> new TimeToLive(endingDate, aNegativeCachingFactor, clock));
  }

  @Test
  void testComputeForZeroDayDiffWhenCacheFactorIsPositive() {
    int aPositiveCachingFactor = 3;
    Date endingDate = Date.from(clock.instant());
    long expectedTtl = DEFAULT_MULTIPLIER * aPositiveCachingFactor;

    long actualTtl = new TimeToLive(endingDate, aPositiveCachingFactor, clock).compute();

    assertThat(actualTtl, is(equalTo(expectedTtl)));
  }

  @Test
  void testComputeForOneDayBeforeWhenCacheFactorIsPositive() {
    int oneDayDiff = 1;
    int aPositiveCachingFactor = 2;
    Date endingDate = Date.from(clock.instant().minus(Duration.ofDays(oneDayDiff)));
    long expectedTtl = (long) aPositiveCachingFactor * oneDayDiff;

    long actualTtl = new TimeToLive(endingDate, aPositiveCachingFactor, clock).compute();

    assertThat(actualTtl, is(equalTo(expectedTtl)));
  }

  @Test
  void testComputeWhenDateObjectIsOfTypeSqlDate() {
    int oneDayDiff = 1;
    int aPositiveCachingFactor = 2;
    java.sql.Date endingDate =
        new java.sql.Date(clock.instant().minus(Duration.ofDays(oneDayDiff)).toEpochMilli());
    long expectedTtl = (long) aPositiveCachingFactor * oneDayDiff;

    long actualTtl = new TimeToLive(endingDate, aPositiveCachingFactor, clock).compute();

    assertThat(actualTtl, is(equalTo(expectedTtl)));
  }

  @Test
  void testComputeEndingDateIsAheadOfNowAndCacheFactorIsPositive() {
    int tenDaysAhead = 10;
    int aPositiveCachingFactor = 1;
    Date endingDate = Date.from(clock.instant().plus(Duration.ofDays(tenDaysAhead)));
    long expectedTtl = DEFAULT_MULTIPLIER * aPositiveCachingFactor;

    long actualTtl = new TimeToLive(endingDate, aPositiveCachingFactor, clock).compute();

    assertThat(actualTtl, is(equalTo(expectedTtl)));
  }

  @Test
  void testComputeEndingDateIsTenDaysBeforeNowAndCacheFactorIsPositive() {
    int tenDays = 10;
    int aPositiveCachingFactor = 2;
    Date endingDate = Date.from(clock.instant().minus(Duration.ofDays(tenDays)));
    long expectedTtl = (long) aPositiveCachingFactor * tenDays;

    long actualTtl = new TimeToLive(endingDate, aPositiveCachingFactor, clock).compute();

    assertThat(actualTtl, is(equalTo(expectedTtl)));
  }
}
