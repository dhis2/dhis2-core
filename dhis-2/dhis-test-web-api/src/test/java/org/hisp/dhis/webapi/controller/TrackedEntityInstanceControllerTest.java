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

import static org.hisp.dhis.webapi.WebClient.Accept;
import static org.hisp.dhis.webapi.WebClient.Body;
import static org.hisp.dhis.webapi.WebClient.ContentType;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.event.TrackedEntityInstanceController} using
 * (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class TrackedEntityInstanceControllerTest extends DhisControllerConvenienceTest {

  private String ouId;

  private String tetId;

  @BeforeEach
  void setUp() {
    ouId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
    tetId = assertStatus(HttpStatus.CREATED, POST("/trackedEntityTypes/", "{'name': 'A'}"));
  }

  @Test
  void testPostTrackedEntityInstanceJson() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Import was successful.",
        POST(
                "/trackedEntityInstances",
                "{'name':'A', 'trackedEntityType':'" + tetId + "', 'orgUnit':'" + ouId + "'}")
            .content(HttpStatus.OK));
  }

  @Test
  void testPostTrackedEntityInstanceJson_Async() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Initiated inMemoryEventImport",
        POST(
                "/trackedEntityInstances?async=true",
                "{'name':'A', 'trackedEntityType':'" + tetId + "', 'orgUnit':'" + ouId + "'}")
            .content(HttpStatus.OK));
  }

  @Test
  void testPostTrackedEntityInstanceXml() {
    HttpResponse response =
        POST(
            "/trackedEntityInstances",
            Body(
                "<trackedEntityInstance><name>A</name><trackedEntityType>"
                    + tetId
                    + "</trackedEntityType><orgUnit>"
                    + ouId
                    + "</orgUnit></trackedEntityInstance>"),
            ContentType(MediaType.APPLICATION_XML),
            Accept(MediaType.APPLICATION_XML));
    assertEquals(HttpStatus.OK, response.status());
    assertTrue(response.content(MediaType.APPLICATION_XML).startsWith("<webMessage"));
  }

  @Test
  void testPostTrackedEntityInstanceXml_Async() {
    HttpResponse response =
        POST(
            "/trackedEntityInstances?async=true",
            Body(
                "<trackedEntityInstance><name>A</name><trackedEntityType>"
                    + tetId
                    + "</trackedEntityType><orgUnit>"
                    + ouId
                    + "</orgUnit></trackedEntityInstance>"),
            ContentType(MediaType.APPLICATION_XML),
            Accept(MediaType.APPLICATION_XML));
    assertEquals(HttpStatus.OK, response.status());
    assertTrue(response.content(MediaType.APPLICATION_XML).startsWith("<webMessage"));
  }

  @Test
  void testUpdateTrackedEntityInstanceXml() {
    String uid =
        assertStatus(
            HttpStatus.OK,
            POST(
                "/trackedEntityInstances",
                "{'name':'A', 'trackedEntityType':'" + tetId + "', 'orgUnit':'" + ouId + "'}"));
    HttpResponse response =
        PUT(
            "/trackedEntityInstances/" + uid,
            Body(
                "<trackedEntityInstance><name>A</name><trackedEntityType>"
                    + tetId
                    + "</trackedEntityType><orgUnit>"
                    + ouId
                    + "</orgUnit></trackedEntityInstance>"),
            ContentType(MediaType.APPLICATION_XML),
            Accept(MediaType.APPLICATION_XML));
    assertEquals(HttpStatus.OK, response.status());
    assertTrue(response.content(MediaType.APPLICATION_XML).startsWith("<webMessage"));
  }

  @Test
  void testUpdateTrackedEntityInstanceJson() {
    String uid =
        assertStatus(
            HttpStatus.OK,
            POST(
                "/trackedEntityInstances",
                "{'name':'A', 'trackedEntityType':'" + tetId + "', 'orgUnit':'" + ouId + "'}"));
    JsonObject tei = GET("/trackedEntityInstances/" + uid).content();
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Import was successful.",
        PUT("/trackedEntityInstances/" + uid, tei.toString()).content(HttpStatus.OK));
  }

  @Test
  void testDeleteTrackedEntityInstance() {
    String uid =
        assertStatus(
            HttpStatus.OK,
            POST(
                "/trackedEntityInstances",
                "{'name':'A', 'trackedEntityType':'" + tetId + "', 'orgUnit':'" + ouId + "'}"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Import was successful.",
        DELETE("/trackedEntityInstances/" + uid).content(HttpStatus.OK));
  }

  @Test
  void testDeleteTrackedEntityInstance_NoSuchObject() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Import was successful.",
        DELETE("/trackedEntityInstances/xyz").content(HttpStatus.OK));
  }
}
