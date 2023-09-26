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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClient.Accept;
import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClient.ContentType;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonImportSummary;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Tests the {@link CompleteDataSetRegistrationController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class CompleteDataSetRegistrationControllerTest extends DhisControllerConvenienceTest {

  @Test
  void testPostCompleteRegistrationsJson() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "An error occurred, please check import summary.",
        POST("/38/completeDataSetRegistrations", "{}").content(HttpStatus.CONFLICT));
  }

  @Test
  void testPostCompleteRegistrationsJson_Pre38() {
    JsonImportSummary summary =
        POST("/37/completeDataSetRegistrations", "{}")
            .content(HttpStatus.OK)
            .as(JsonImportSummary.class);
    assertEquals("ImportSummary", summary.getResponseType());
    assertEquals("ERROR", summary.getStatus());
  }

  @Test
  void testPostCompleteRegistrationsXml() {
    HttpResponse response =
        POST(
            "/38/completeDataSetRegistrations",
            Body("<completeDataSetRegistrations></completeDataSetRegistrations>"),
            ContentType(CONTENT_TYPE_XML),
            Accept(CONTENT_TYPE_XML));
    assertEquals(HttpStatus.CONFLICT, response.status());
    String content = response.content(MediaType.APPLICATION_XML.toString());
    assertTrue(content.startsWith("<webMessage "));
  }

  @Test
  void testPostCompleteRegistrationsXml_Pre38() {
    HttpResponse response =
        POST(
            "/37/completeDataSetRegistrations",
            Body("<completeDataSetRegistrations></completeDataSetRegistrations>"),
            ContentType(CONTENT_TYPE_XML),
            Accept(CONTENT_TYPE_XML));
    assertEquals(HttpStatus.OK, response.status());
    String content = response.content(MediaType.APPLICATION_XML.toString());
    assertTrue(content.startsWith("<importSummary "));
  }
}
