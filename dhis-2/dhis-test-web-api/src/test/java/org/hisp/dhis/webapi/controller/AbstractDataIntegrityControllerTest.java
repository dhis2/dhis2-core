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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;

/**
 * Base class for all test classes for the {@link DataIntegrityController}.
 *
 * @author Jan Bernitt
 */
abstract class AbstractDataIntegrityControllerTest extends DhisControllerConvenienceTest {
  protected final JsonDataIntegrityDetails getDetails(String check) {
    JsonObject content = GET("/dataIntegrity/details?checks={check}&timeout=1000", check).content();
    JsonDataIntegrityDetails details =
        content.get(check.replace('-', '_'), JsonDataIntegrityDetails.class);
    assertTrue(
        details.exists(), "check " + check + " did not complete in time or threw an exception");
    assertTrue(details.isObject());
    return details;
  }

  /**
   * Triggers the single check provided
   *
   * @param check name of the check to perform
   */
  protected final void postDetails(String check) {
    HttpResponse trigger = POST("/dataIntegrity/details?checks=" + check);
    assertEquals("http://localhost/dataIntegrity/details?checks=" + check, trigger.location());
    assertTrue(trigger.content().isA(JsonWebMessage.class));
  }

  protected final JsonDataIntegritySummary getSummary(String check) {
    JsonObject content = GET("/dataIntegrity/summary?checks={check}&timeout=1000", check).content();
    JsonDataIntegritySummary summary =
        content.get(check.replace('-', '_'), JsonDataIntegritySummary.class);
    assertTrue(summary.exists());
    assertTrue(summary.isObject());
    return summary;
  }

  protected final void postSummary(String check) {
    HttpResponse trigger = POST("/dataIntegrity/summary?checks=" + check);
    assertEquals("http://localhost/dataIntegrity/summary?checks=" + check, trigger.location());
    assertTrue(trigger.content().isA(JsonWebMessage.class));
  }
}
