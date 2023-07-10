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
package org.hisp.dhis.helpers;

import static java.time.LocalDate.now;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Helper class to assist with period and relative period manipulation.
 *
 * @author maikel arabori
 */
public class PeriodHelper {
  public enum Period {
    LAST_YEAR("Last year"),
    THIS_YEAR("This year"),
    LAST_6_MONTHS("Last 6 months"),
    LAST_12_MONTHS("Last 12 months"),
    LAST_5_YEARS("Last 5 years"),
    WEEKS_THIS_YEAR("Weeks this year"),
    TODAY("Today");

    private final String label;

    Period(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }

  private PeriodHelper() {
    throw new UnsupportedOperationException("helper");
  }

  public static String getRelativePeriodDate(Period relativePeriod) {
    switch (relativePeriod) {
      case LAST_YEAR:
        return now().plusYears(1).format(BASIC_ISO_DATE);
      case LAST_5_YEARS:
        return now().plusYears(5).format(BASIC_ISO_DATE);
      case LAST_12_MONTHS:
        return now().plusMonths(12).format(BASIC_ISO_DATE);
      case LAST_6_MONTHS:
        return now().plusMonths(6).format(BASIC_ISO_DATE);
      default:
        return EMPTY;
    }
  }
}
