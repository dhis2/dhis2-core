/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link RelativePeriodsController} using (mocked) REST requests.
 *
 * @author Jason P. Pickering
 */
@Transactional
class RelativePeriodsControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testGetRelativePeriods_Last3Days() {
    JsonArray response = GET("/relativePeriods/LAST_3_DAYS?startDate=2001-07-04").content();

    assertEquals(List.of("20010701", "20010702", "20010703"), response.stringValues());
  }

  @Test
  void testGetRelativePeriods_WithFinancialYearStart() {
    JsonArray response =
        GET("/relativePeriods/THIS_FINANCIAL_YEAR"
                + "?startDate=2001-07-04"
                + "&financialYearStart=FINANCIAL_YEAR_APRIL")
            .content();

    assertEquals(List.of("2001April"), response.stringValues());
  }

  @Test
  void testGetRelativePeriods_Last12Months_MidYear() {
    JsonArray response = GET("/relativePeriods/LAST_12_MONTHS?startDate=2001-07-04").content();

    assertEquals(
        List.of(
            "200007", "200008", "200009", "200010", "200011", "200012", "200101", "200102",
            "200103", "200104", "200105", "200106"),
        response.stringValues());
  }

  @Test
  void testGetRelativePeriods_Last12Months_EarlyYear() {
    JsonArray response = GET("/relativePeriods/LAST_12_MONTHS?startDate=2001-01-15").content();

    assertEquals(
        List.of(
            "200001", "200002", "200003", "200004", "200005", "200006", "200007", "200008",
            "200009", "200010", "200011", "200012"),
        response.stringValues());
  }

  @Test
  void testGetRelativePeriods_InvalidRelativePeriod() {
    assertStatus(HttpStatus.BAD_REQUEST, GET("/relativePeriods/LAST_14_MONTHS"));
  }

  @Test
  void testGetRelativePeriods_InvalidFinancialYearStart() {
    assertStatus(
        HttpStatus.BAD_REQUEST,
        GET("/relativePeriods/LAST_3_DAYS?financialYearStart=NOT_A_FINANCIAL_YEAR"));
  }
}
