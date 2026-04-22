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
package org.hisp.dhis.webapi.controller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.security.oauth2.authorization.Dhis2OAuth2Authorization;
import org.hisp.dhis.security.oauth2.consent.Dhis2OAuth2AuthorizationConsent;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.test.webapi.json.domain.JsonImportSummary;
import org.hisp.dhis.test.webapi.json.domain.JsonTypeReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that {@code /api/metadata} refuses to create or update {@link Dhis2OAuth2Authorization}
 * or {@link Dhis2OAuth2AuthorizationConsent}. These are runtime OAuth2 state (access tokens,
 * refresh tokens, device codes, principal consent grants) and must only be written by Spring
 * Authorization Server's own save path — never by an admin-supplied metadata payload.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class OAuth2MetadataImportRejectionTest extends H2ControllerIntegrationTestBase {

  private static Stream<Arguments> rejectedTypes() {
    return Stream.of(
        Arguments.of(
            "oAuth2Authorizations",
            Dhis2OAuth2Authorization.class,
            """
            {"oAuth2Authorizations":[{"name":"a","registeredClientId":"c1",\
            "principalName":"admin","authorizationGrantType":"authorization_code"}]}"""),
        Arguments.of(
            "oAuth2AuthorizationConsents",
            Dhis2OAuth2AuthorizationConsent.class,
            """
            {"oAuth2AuthorizationConsents":[{"name":"a","registeredClientId":"c1",\
            "principalName":"admin","authorities":"read"}]}"""));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedTypes")
  @DisplayName("POST /api/metadata is rejected with E6023 for runtime OAuth2 state types")
  void metadataImportIsRejected(
      String plural, Class<? extends IdentifiableObject> entity, String body) {
    JsonImportSummary report =
        POST("/metadata", body)
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals("ERROR", report.getStatus());
    assertEquals(0, report.getStats().getCreated());
    assertEquals(1, report.getStats().getIgnored());

    JsonTypeReport typeReport = report.getTypeReport(entity);
    JsonErrorReport errorReport =
        typeReport.getObjectReports().stream()
            .flatMap(or -> or.getErrorReports().stream())
            .filter(e -> e.getErrorCode() == ErrorCode.E6023)
            .findFirst()
            .orElse(null);
    assertNotNull(
        errorReport, "Expected E6023 error report for " + entity.getSimpleName() + " import");
    assertTrue(
        errorReport.getMessage().contains(entity.getSimpleName()),
        "Error message should name the rejected type");
  }
}
