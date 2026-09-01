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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpStatus.FORBIDDEN;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link DataElementInsightsController} endpoints.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class DataElementInsightsControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void shouldReturnSummaryForAllDataElements() {
    DataElement numeric = createDataElement('A');
    DataElement text = createDataElement('B');
    text.setValueType(ValueType.TEXT);
    manager.save(numeric);
    manager.save(text);

    JsonMixed summary = GET("/dataElementInsights").content(OK);

    assertEquals(2, summary.getNumber("total").intValue());
    assertEquals(1, summary.getObject("byCategory").getNumber("numeric").intValue());
    assertEquals(1, summary.getObject("byCategory").getNumber("text").intValue());
    assertFalse(summary.getBoolean("monitoringActive").booleanValue());
  }

  @ParameterizedTest
  @CsvSource({"NUMBER,numeric", "INTEGER,numeric", "TEXT,text", "DATE,date", "BOOLEAN,boolean"})
  void shouldClassifyValueTypeIntoCategory(ValueType valueType, String category) {
    DataElement dataElement = createDataElement('A');
    dataElement.setValueType(valueType);
    manager.save(dataElement);

    JsonMixed summary = GET("/dataElementInsights").content(OK);

    assertEquals(1, summary.getObject("byCategory").getNumber(category).intValue());
  }

  @Test
  void shouldDenySummaryWithoutMaintenanceAuthority() {
    switchToNewUser("guest");

    assertEquals(FORBIDDEN, GET("/dataElementInsights").status());
  }
}
