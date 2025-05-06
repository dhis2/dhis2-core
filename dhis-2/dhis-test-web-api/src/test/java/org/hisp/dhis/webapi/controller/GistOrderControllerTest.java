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
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link org.hisp.dhis.gist.GistQuery.Order} related features of the Gist API.
 *
 * @author Jan Bernitt
 */
class GistOrderControllerTest extends AbstractGistControllerTest {

  @Test
  void testCustomSortOrder() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/api/optionSets",
            """
                {
            "id": "I2W4ujmlGYu",
            "code": "OS1",
            "name": "OS1",
            "valueType": "TEXT"
            }"""));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/api/options",
            """
              { "code": "a", "name": "a", "optionSet": {"id": "I2W4ujmlGYu" }}
          """));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/api/options",
            """
              { "code": "b", "name": "b", "optionSet": {"id": "I2W4ujmlGYu" }}
          """));
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/api/options",
            """
              { "code": "c", "name": "c", "optionSet": {"id": "I2W4ujmlGYu" }}
          """));
    JsonObject page = GET("/api/options/gist?filter=optionSet.name:eq:OS1").content();
    JsonList<JsonObject> options = page.getArray("options").asList(JsonObject.class);
    assertEquals(3, options.size());
    assertEquals(
        List.of("a", "b", "c"),
        options.toList(o -> o.getString("code").string()),
        "Should be sorted by code since @Gist(order=x) is used on code");
  }
}
