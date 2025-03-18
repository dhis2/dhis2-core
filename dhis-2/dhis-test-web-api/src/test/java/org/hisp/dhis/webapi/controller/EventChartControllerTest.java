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
package org.hisp.dhis.webapi.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpStatus.CREATED;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controller tests for {@link org.hisp.dhis.webapi.controller.event.EventChartController}.
 *
 * @author maikel arabori
 */
@Transactional
class EventChartControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;

  private Program mockProgram;

  @BeforeEach
  public void beforeEach() {
    mockProgram = createProgram('A');
    manager.save(mockProgram);
  }

  @Test
  void testThatGetEventVisualizationsContainsLegacyEventCharts() {
    // Given
    final String body =
        "{'name': 'Name Test', 'type':'GAUGE', 'program':{'id':'" + mockProgram.getUid() + "'}}";

    // When
    final String uid = assertStatus(CREATED, POST("/eventCharts/", body));

    // Then
    final JsonObject response = GET("/eventVisualizations/" + uid).content();

    assertThat(response.get("name").toString(), containsString("Name Test"));
    assertThat(response.get("type").toString(), containsString("GAUGE"));
  }

  @Test
  void testThatGetEventChartsDoesNotContainNewEventVisualizations() {
    // Given
    final String body =
        "{'name': 'Name Test', 'type':'GAUGE', 'program':{'id':'" + mockProgram.getUid() + "'}}";

    // When
    final String uid = assertStatus(CREATED, POST("/eventVisualizations/", body));

    // Then
    assertTrue(GET("/eventCharts/" + uid).content().isEmpty());
  }
}
