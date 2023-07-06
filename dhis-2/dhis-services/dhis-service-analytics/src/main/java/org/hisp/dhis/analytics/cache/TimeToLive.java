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

import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.ofInstant;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

import java.time.Instant;
import java.util.Date;

/**
 * This class is responsible for computing a time to live value based on the given date before today
 * and a TTL factor. The calculation of the TTL will be done by the method compute() - check this
 * method for the calculation details.
 */
public class TimeToLive implements Computable {
  static final long DEFAULT_MULTIPLIER = 1;

  private final Date dateBeforeToday;

  private final int ttlFactor;

  public TimeToLive(final Date dateBeforeToday, final int ttlFactor) {
    notNull(dateBeforeToday, "Param dateBeforeToday must not be null");
    isTrue(ttlFactor > 0, "Param ttlFactor must be greater than zero");

    // This ensures we always work with java.util.Date type, avoiding issues
    // with java.sql.Date.
    this.dateBeforeToday = new Date(dateBeforeToday.getTime());
    this.ttlFactor = ttlFactor;
  }

  /**
   * Execute the internal rules in order to calculate a TTL for the given parameters. The current
   * rules are based on a configurable timeout "ttlFactor" (through SettingKey) which will be used
   * in the calculation of this time to live. Basically:
   *
   * <p>Older the "dateBeforeToday", higher the "ttlFactor", longer the TTL. The formula is
   * basically: TTL = "ttlFactor" * (diff between now and the "dateBeforeToday")
   *
   * @return the computed TTL value.
   */
  @Override
  public long compute() {
    /*
     * If the difference between the most recent date and NOW is 0 (zero) it
     * means the current day, so set the days multiplier to 1 (one) avoiding
     * multiplying by 0 (zero).
     */
    final long daysDiff = daysBetweenDateBeforeTodayAndNow(dateBeforeToday.toInstant());
    final long daysMultiplier = daysDiff > 0 ? daysDiff : DEFAULT_MULTIPLIER;

    return ttlFactor * daysMultiplier;
  }

  /**
   * Calculates the difference between now and the given date. It has a the particularity of
   * returning ZERO (0) if the diff is negative (because it means that the input date is ahead of
   * now).
   *
   * @param dateBeforeToday the date to subtract from now
   * @return the difference of days
   */
  private long daysBetweenDateBeforeTodayAndNow(final Instant dateBeforeToday) {
    final long diff = DAYS.between(ofInstant(dateBeforeToday, systemDefault()), now());
    return diff >= 0 ? diff : 0;
  }
}
