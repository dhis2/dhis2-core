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
package org.hisp.dhis.dxf2.adx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.api.Test;

/**
 * @author bobj
 * @author Jim Grace
 */
class AdxPeriodTest {

  @Test
  void testParse() {
    // P1D - Daily
    assertEquals("20240101", parse("2024-01-01/P1D"));
    assertEquals("20240506", parse("2024-05-06/P1D"));
    // P7D - Weekly
    assertEquals("2024W1", parse("2024-01-01/P7D"));
    assertEquals("2024W2", parse("2024-01-08/P7D"));
    assertEquals("2024WedW1", parse("2024-01-03/P7D"));
    assertEquals("2024ThuW1", parse("2024-01-04/P7D"));
    assertEquals("2024SatW2", parse("2024-01-06/P7D"));
    assertEquals("2024SunW2", parse("2024-01-07/P7D"));
    // P14D - BiWeekly
    assertEquals("2024BiW1", parse("2024-01-01/P14D"));
    assertEquals("2024BiW2", parse("2024-01-15/P14D"));
    // P1M - Monthly
    assertEquals("202401", parse("2024-01-01/P1M"));
    assertEquals("202405", parse("2024-05-01/P1M"));
    // P2M - BiMonthly
    assertEquals("202401B", parse("2024-01-01/P2M"));
    assertEquals("202401B", parse("2024-01-01/P2M"));
    // P3M - Quarterly
    assertEquals("2024Q1", parse("2024-01-01/P3M"));
    assertEquals("2024Q2", parse("2024-04-01/P3M"));
    // P6M - SixMonthly
    assertEquals("2024S1", parse("2024-01-01/P6M"));
    assertEquals("2024S2", parse("2024-07-01/P6M"));
    assertEquals("2024AprilS1", parse("2024-04-01/P6M"));
    assertEquals("2024AprilS2", parse("2024-10-01/P6M"));
    assertEquals("2025NovS1", parse("2024-11-01/P6M"));
    assertEquals("2025NovS2", parse("2025-05-01/P6M"));
    // P1Y - Yearly
    assertEquals("2024", parse("2024-01-01/P1Y"));
    assertEquals("2024April", parse("2024-04-01/P1Y"));
    assertEquals("2024July", parse("2024-07-01/P1Y"));
    assertEquals("2024Oct", parse("2024-10-01/P1Y"));
    assertEquals("2025Nov", parse("2024-11-01/P1Y"));
  }

  @Test
  void testParseBadFormat() {
    failToParse("2024-01-01/P1D/P1D");
  }

  @Test
  void testParseBadDuration() {
    failToParse("2024-01-01/P1");
  }

  @Test
  void testParseBadWeekly() {
    failToParse("2024-01-02/P7D");
  }

  @Test
  void testParseBadSixMonthly() {
    failToParse("2014-02-01/P6M");
  }

  @Test
  void testParseBadYearly() {
    failToParse("2014-02-01/P1Y");
  }

  @Test
  void testSerialize() {
    // Daily - P1D
    assertEquals("2022-01-01/P1D", serialize("20220101"));
    assertEquals("2022-01-07/P1D", serialize("20220107"));
    // Weekly - P7D
    assertEquals("2022-01-03/P7D", serialize("2022W1"));
    assertEquals("2022-01-10/P7D", serialize("2022W2"));
    assertEquals("2021-12-29/P7D", serialize("2022WedW1"));
    assertEquals("2021-12-30/P7D", serialize("2022ThuW1"));
    assertEquals("2022-01-01/P7D", serialize("2022SatW1"));
    assertEquals("2022-01-02/P7D", serialize("2022SunW1"));
    // BiWeekly - P14D
    assertEquals("2022-01-03/P14D", serialize("2022BiW1"));
    assertEquals("2022-01-17/P14D", serialize("2022BiW2"));
    // Monthly - P1M
    assertEquals("2022-01-01/P1M", serialize("202201"));
    assertEquals("2022-05-01/P1M", serialize("202205"));
    // BiMonthly - P2M
    assertEquals("2022-01-01/P2M", serialize("202201B"));
    assertEquals("2022-03-01/P2M", serialize("202202B"));
    // Quarterly - P3M
    assertEquals("2022-01-01/P3M", serialize("2022Q1"));
    assertEquals("2022-04-01/P3M", serialize("2022Q2"));
    // SixMonthly - P6M
    assertEquals("2022-01-01/P6M", serialize("2022S1"));
    assertEquals("2022-07-01/P6M", serialize("2022S2"));
    assertEquals("2022-04-01/P6M", serialize("2022AprilS1"));
    assertEquals("2022-10-01/P6M", serialize("2022AprilS2"));
    assertEquals("2022-05-01/P6M", serialize("2022NovS2"));
    // Yearly - P1Y
    assertEquals("2022-01-01/P1Y", serialize("2022"));
    assertEquals("2022-04-01/P1Y", serialize("2022April"));
    assertEquals("2022-07-01/P1Y", serialize("2022July"));
    assertEquals("2022-10-01/P1Y", serialize("2022Oct"));
    assertEquals("2021-11-01/P1Y", serialize("2022Nov"));
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------
  private String parse(String adxPeriod) {
    try {
      return AdxPeriod.parse(adxPeriod).getIsoDate();
    } catch (AdxException ex) {
      fail(ex.getMessage());
    }
    return null;
  }

  private void failToParse(String adxPeriod) {
    try {
      AdxPeriod.parse(adxPeriod).getIsoDate();
      fail("Should have thrown exception parsing " + adxPeriod);
    } catch (Exception ex) {
      assertEquals(AdxException.class, ex.getClass());
    }
  }

  private String serialize(String period) {
    return AdxPeriod.serialize(PeriodType.getPeriodFromIsoString(period));
  }
}
