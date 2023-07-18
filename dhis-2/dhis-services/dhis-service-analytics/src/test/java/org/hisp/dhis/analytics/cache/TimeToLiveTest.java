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
package org.hisp.dhis.analytics.cache;

import static java.util.Calendar.DATE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.analytics.cache.TimeToLive.DEFAULT_MULTIPLIER;
import static org.hisp.dhis.util.DateUtils.calculateDateFrom;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import org.junit.jupiter.api.Test;

class TimeToLiveTest {
  @Test
  void testComputeForCurrentDayWhenCacheFactorIsNegative() {
    int aNegativeCachingFactor = -1;
    Date endingDate = new Date();

    assertThrows(
        IllegalArgumentException.class, () -> new TimeToLive(endingDate, aNegativeCachingFactor));
  }

  @Test
  void testComputeForZeroDayDiffWhenCacheFactorIsPositive() {
    int aPositiveCachingFactor = 3;
    Date endingDate = new Date();
    long expectedTtl = DEFAULT_MULTIPLIER * aPositiveCachingFactor;

    long actualTtl = new TimeToLive(endingDate, aPositiveCachingFactor).compute();

    assertThat(actualTtl, is(equalTo(expectedTtl)));
  }

  @Test
  void testComputeForOneDayBeforeWhenCacheFactorIsPositive() {
    int oneDayDiff = 1;
    int aPositiveCachingFactor = 2;
    Date endingDate = calculateDateFrom(new Date(), minus(oneDayDiff), DATE);
    long expectedTtl = aPositiveCachingFactor * oneDayDiff;

    long actualTtl = new TimeToLive(endingDate, aPositiveCachingFactor).compute();

    assertThat(actualTtl, is(equalTo(expectedTtl)));
  }

  @Test
  void testComputeWhenDateObjectIsOfTypeSqlDate() {
    int oneDayDiff = 1;
    int aPositiveCachingFactor = 2;
    java.sql.Date endingDate =
        new java.sql.Date(calculateDateFrom(new Date(), minus(oneDayDiff), DATE).getTime());
    long expectedTtl = aPositiveCachingFactor * oneDayDiff;

    long actualTtl = new TimeToLive(endingDate, aPositiveCachingFactor).compute();

    assertThat(actualTtl, is(equalTo(expectedTtl)));
  }

  @Test
  void testComputeEndingDateIsAheadOfNowAndCacheFactorIsPositive() {
    int tenDaysAhead = 10;
    int aPositiveCachingFactor = 1;
    Date beginningDate = new Date();
    Date endingDate = calculateDateFrom(beginningDate, plus(tenDaysAhead), DATE);
    long expectedTtl = DEFAULT_MULTIPLIER * aPositiveCachingFactor;

    long actualTtl = new TimeToLive(endingDate, aPositiveCachingFactor).compute();

    assertThat(actualTtl, is(equalTo(expectedTtl)));
  }

  @Test
  void testComputeEndingDateIsTenDaysBeforeNowAndCacheFactorIsPositive() {
    int tenDays = 10;
    int aPositiveCachingFactor = 2;
    Date now = new Date();
    Date endingDate = calculateDateFrom(now, minus(tenDays), DATE);
    long expectedTtl = aPositiveCachingFactor * tenDays;

    long actualTtl = new TimeToLive(endingDate, aPositiveCachingFactor).compute();

    assertThat(actualTtl, is(equalTo(expectedTtl)));
  }

  private int minus(int value) {
    return -value;
  }

  private int plus(int value) {
    return value;
  }
}
