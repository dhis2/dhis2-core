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
package org.hisp.dhis.period;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PeriodTypeEnum {
  BI_MONTHLY("BiMonthly"),
  BI_WEEKLY("BiWeekly"),
  DAILY("Daily"),
  FINANCIAL_APRIL("FinancialApril"),
  FINANCIAL_JULY("FinancialJuly"),
  FINANCIAL_NOV("FinancialNov"),
  FINANCIAL_SEP("FinancialSep"),
  FINANCIAL_OCT("FinancialOct"),
  MONTHLY("Monthly"),
  QUARTERLY("Quarterly"),
  QUARTERLY_NOV("QuarterlyNov"),
  SIX_MONTHLY_APRIL("SixMonthlyApril"),
  SIX_MONTHLY_NOV("SixMonthlyNov"),
  SIX_MONTHLY("SixMonthly"),
  TWO_YEARLY("TwoYearly"),
  WEEKLY("Weekly"),
  WEEKLY_SATURDAY("WeeklySaturday"),
  WEEKLY_SUNDAY("WeeklySunday"),
  WEEKLY_THURSDAY("WeeklyThursday"),
  WEEKLY_WEDNESDAY("WeeklyWednesday"),
  YEARLY("Yearly");

  @Getter private final String name;

  /**
   * Finds the {@link PeriodTypeEnum} by a ISO period value.
   *
   * <pre>
   * Daily: ~~~~~~~~ (8) or ~~~~-~~-~~ (10)
   * Weekly: ~~~~W~ (6) or ~~~~W~~ (7)
   * Weekly Wednesday: ~~~~WedW~ (9) or ~~~~WedW~~ (10)
   * Weekly Thursday: ~~~~ThuW~ (9) or ~~~~ThuW~~ (10)
   * Weekly Saturday: ~~~~SatW~ (9) or ~~~~SatW~~ (10)
   * Weekly Sunday: ~~~~SunW~ (9) or ~~~~SunW~~ (10)
   * Bi-weekly: ~~~~BiW~ (8) or ~~~~BiW~~ (9)
   * Monthly: ~~~~~~ (6) or ~~~~-~~ (7)
   * Bi-monthly: ~~~~~~B (7)
   * Quarterly: ~~~~Q~ (6)
   * Quarterly November: ~~~~NovQ~ (9)
   * Six-monthly: ~~~~S~ (6)
   * Six-monthly April: ~~~~AprilS~ (11)
   * Six-monthly November: ~~~~NovS~ (9)
   * Yearly: ~~~~ (4)
   * Yearly April: ~~~~April (9)
   * Yearly July: ~~~~July (8)
   * Yearly September: ~~~~Sep (7)
   * Yearly October: ~~~~Oct (7)
   * Yearly November: ~~~~Nov (7)
   * </pre>
   *
   * @param isoPeriod an ISO string
   * @return the period type for the given string or null if it does not match any of the expected
   *     patterns
   */
  public static PeriodTypeEnum ofIsoPeriod(String isoPeriod) {
    if (isoPeriod == null || isoPeriod.isEmpty()) return null;
    char[] chars = isoPeriod.toCharArray();
    int len = chars.length;
    if (len < 4 || len > 11) return null;
    if (!isDigit(chars[0])) return null;
    if (!isDigit(chars[1])) return null;
    if (!isDigit(chars[2])) return null;
    if (!isDigit(chars[3])) return null;
    if (len == 4) return YEARLY;
    if (len == 6) {
      if (!isDigit(chars[5])) return null;
      char c4 = chars[4];
      if (c4 == 'W') return WEEKLY;
      if (isDigit(c4)) return MONTHLY;
      if (c4 == 'Q') return QUARTERLY;
      if (c4 == 'S') return SIX_MONTHLY;
      return null;
    }
    if (len == 7) {
      if (isoPeriod.endsWith("Sep")) return FINANCIAL_SEP;
      if (isoPeriod.endsWith("Oct")) return FINANCIAL_OCT;
      if (isoPeriod.endsWith("Nov")) return FINANCIAL_NOV;
      if (isoPeriod.endsWith("B")) {
        if (!isDigit(chars[4])) return null;
        if (!isDigit(chars[5])) return null;
        return BI_MONTHLY;
      }
      if (!isDigit(chars[5])) return null;
      if (!isDigit(chars[6])) return null;
      char c4 = chars[4];
      if (c4 == 'W') return WEEKLY;
      if (c4 == '-') return MONTHLY;
      return null;
    }
    if (len == 8) {
      if (isoPeriod.endsWith("July")) return FINANCIAL_JULY;
      if (isoPeriod.indexOf("BiW") == 4) {
        if (!isDigit(chars[7])) return null;
        return BI_WEEKLY;
      }
      if (!isDigit(chars[4])) return null;
      if (!isDigit(chars[5])) return null;
      if (!isDigit(chars[6])) return null;
      if (!isDigit(chars[7])) return null;
      return DAILY;
    }
    if (len == 9) {
      if (isoPeriod.endsWith("April")) return FINANCIAL_APRIL;
      if (!isDigit(chars[8])) return null;
      if (isoPeriod.indexOf("NovQ") == 4) return QUARTERLY_NOV;
      if (isoPeriod.indexOf("BiW") == 4) {
        if (!isDigit(chars[7])) return null;
        return BI_WEEKLY;
      }
      if (isoPeriod.indexOf("WedW") == 4) return WEEKLY_WEDNESDAY;
      if (isoPeriod.indexOf("ThuW") == 4) return WEEKLY_THURSDAY;
      if (isoPeriod.indexOf("SatW") == 4) return WEEKLY_SATURDAY;
      if (isoPeriod.indexOf("SunW") == 4) return WEEKLY_SUNDAY;
      if (isoPeriod.indexOf("NovS") == 4) return SIX_MONTHLY_NOV;
      return null;
    }
    if (len == 10) {
      if (!isDigit(chars[8])) return null;
      if (!isDigit(chars[9])) return null;
      if (chars[4] == '-') {
        if (!isDigit(chars[5])) return null;
        if (!isDigit(chars[6])) return null;
        if (chars[7] != '-') return null;
        return DAILY;
      }
      if (isoPeriod.indexOf("WedW") == 4) return WEEKLY_WEDNESDAY;
      if (isoPeriod.indexOf("ThuW") == 4) return WEEKLY_THURSDAY;
      if (isoPeriod.indexOf("SatW") == 4) return WEEKLY_SATURDAY;
      if (isoPeriod.indexOf("SunW") == 4) return WEEKLY_SUNDAY;
      return null;
    }
    if (len == 11) {
      if (!isDigit(chars[10])) return null;
      if (isoPeriod.indexOf("AprilS") == 4) return SIX_MONTHLY_APRIL;
    }
    return null;
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }
}
