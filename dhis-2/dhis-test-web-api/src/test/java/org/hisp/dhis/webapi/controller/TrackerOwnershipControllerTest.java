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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.event.TrackerOwnershipController} using (mocked)
 * REST requests.
 *
 * @author Jan Bernitt
 */
class TrackerOwnershipControllerTest extends DhisControllerConvenienceTest {

  private String ouId;

  private String teiId;

  private String pId;

  @BeforeEach
  void setUp() {
    ouId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
    String tetId = assertStatus(HttpStatus.CREATED, POST("/trackedEntityTypes/", "{'name': 'A'}"));
    teiId =
        assertStatus(
            HttpStatus.OK,
            POST(
                "/trackedEntityInstances",
                "{'name':'A', 'trackedEntityType':'" + tetId + "', 'orgUnit':'" + ouId + "'}"));
    pId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'P1', 'shortName':'P1', 'programType':'WITHOUT_REGISTRATION'}"));
  }

  @Test
  void shouldUpdateTrackerProgramOwnerWhenUsingDeprecateTrackedEntityInstanceParam() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Ownership transferred",
        PUT(
                "/tracker/ownership/transfer?trackedEntityInstance={tei}&program={prog}&ou={ou}",
                teiId,
                pId,
                ouId)
            .content(HttpStatus.OK));
  }

  @Test
  void shouldUpdateTrackerProgramOwner() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Ownership transferred",
        PUT(
                "/tracker/ownership/transfer?trackedEntity={tei}&program={prog}&ou={ou}",
                teiId,
                pId,
                ouId)
            .content(HttpStatus.OK));
  }

  @Test
  void shouldFailToUpdateWhenGivenTrackedEntityAndTrackedEntityInstanceParameters() {
    assertEquals(
        "Only one parameter of 'trackedEntityInstance' and 'trackedEntity' must be specified. "
            + "Prefer 'trackedEntity' as 'trackedEntityInstance' will be removed.",
        PUT(
                "/tracker/ownership/transfer?trackedEntity={tei}&"
                    + "trackedEntityInstance={tei}&program={prog}&ou={ou}",
                teiId,
                teiId,
                pId,
                ouId)
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldFailToUpdateWhenNoTrackedEntityOrTrackedEntityInstanceParametersArePresent() {
    assertEquals(
        "Required request parameter 'trackedEntity' is not present",
        PUT("/tracker/ownership/transfer?program={prog}&ou={ou}", pId, ouId)
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldOverrideOwnershipAccessWhenUsingDeprecateTrackedEntityInstanceParam() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Temporary Ownership granted",
        POST(
                "/tracker/ownership/override?trackedEntityInstance={tei}&program={prog}&reason=42",
                teiId,
                pId)
            .content(HttpStatus.OK));
  }

  @Test
  void shouldOverrideOwnershipAccess() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Temporary Ownership granted",
        POST("/tracker/ownership/override?trackedEntity={tei}&program={prog}&reason=42", teiId, pId)
            .content(HttpStatus.OK));
  }

  @Test
  void shouldFailToOverrideWhenGivenTrackedEntityAndTrackedEntityInstanceParameters() {
    assertEquals(
        "Only one parameter of 'trackedEntityInstance' and 'trackedEntity' must be specified. "
            + "Prefer 'trackedEntity' as 'trackedEntityInstance' will be removed.",
        POST(
                "/tracker/ownership/override?trackedEntity={tei}&"
                    + "trackedEntityInstance={tei}&program={prog}&&reason=42",
                teiId,
                teiId,
                pId)
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldFailToOverrideWhenNoTrackedEntityOrTrackedEntityInstanceParametersArePresent() {
    assertEquals(
        "Required request parameter 'trackedEntity' is not present",
        POST("/tracker/ownership/override?program=" + pId + "&reason=42")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }
}
