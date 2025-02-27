/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramControllerTest extends DhisControllerConvenienceTest {

  @Autowired private ObjectMapper jsonMapper;

  public static final String PROGRAM_UID = "PrZMWi7rBga";

  @BeforeEach
  public void testSetup() throws JsonProcessingException {
    DataElement dataElement1 = createDataElement('a');
    DataElement dataElement2 = createDataElement('b');
    dataElement1.setUid("deabcdefgha");
    dataElement2.setUid("deabcdefghb");
    TrackedEntityAttribute tea1 = createTrackedEntityAttribute('a');
    TrackedEntityAttribute tea2 = createTrackedEntityAttribute('b');
    tea1.setUid("TEA1nnnnnaa");
    tea2.setUid("TEA1nnnnnab");
    POST("/dataElements", jsonMapper.writeValueAsString(dataElement1)).content(HttpStatus.CREATED);
    POST("/dataElements", jsonMapper.writeValueAsString(dataElement2)).content(HttpStatus.CREATED);
    POST("/trackedEntityAttributes", jsonMapper.writeValueAsString(tea1))
        .content(HttpStatus.CREATED);
    POST("/trackedEntityAttributes", jsonMapper.writeValueAsString(tea2))
        .content(HttpStatus.CREATED);

    POST("/metadata", org.hisp.dhis.web.WebClient.Body("program/create_program.json"))
        .content(HttpStatus.OK);
  }

  @Test
  void testDeleteWithMapView() {
    String mapViewJson =
        "{"
            + "\"name\": \"test mapview\","
            + "\"id\": \"mVIVRd23Jm9\","
            + "\"organisationUnitLevels\": [],"
            + "\"maps\": [],"
            + "\"layer\": \"event\","
            + "\"program\": {"
            + "\"id\": \"PrZMWi7rBga\""
            + "},"
            + "\"programStage\": {"
            + "\"id\": \"PSzMWi7rBga\""
            + "}"
            + "}";
    POST("/mapViews", mapViewJson).content(HttpStatus.CREATED);
    assertStatus(HttpStatus.OK, DELETE(String.format("/programs/%s", PROGRAM_UID)));
    assertStatus(HttpStatus.NOT_FOUND, GET(String.format("/programs/%s", PROGRAM_UID)));
    JsonResponse mapview = GET("/mapViews/mVIVRd23Jm9").content();
    assertFalse(mapview.has("program"));
  }
}
