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
package org.hisp.dhis.webapi.security.csp.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.webapi.security.csp.app.CspAppOverride.Draft;
import org.hisp.dhis.webapi.security.csp.app.CspAppOverride.EntrypointOverride;
import org.hisp.dhis.webapi.security.csp.app.CspAppOverride.SourceChanges;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Morten Svanæs
 */
class CspOverrideValidatorTest {
  private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

  private static CspOverrideValidator.Result validate(
      String entrypoint, String directive, List<String> values, boolean https) {
    return CspOverrideValidator.validate(
        Map.of(entrypoint, ep(directive, values)), "note", Set.of(), https, NOW);
  }

  private static EntrypointOverride ep(String directive, List<String> values) {
    return new EntrypointOverride(
        null, new SourceChanges(Map.of(directive, values), List.of()), null);
  }

  private static String singleError(CspOverrideValidator.Result result) {
    assertEquals(1, result.errors().size(), result.errors().toString());
    return result.errors().get(0).code();
  }

  @Test
  void scriptSrc_acceptsEveryValueKind_andFlagsTheUnsafeOnes() {
    List<String> values =
        List.of(
            "https://cdn.example.org",
            "http://intranet.example.org:8080",
            "'self'",
            "'strict-dynamic'",
            "'unsafe-inline'",
            "'unsafe-eval'",
            "'wasm-unsafe-eval'",
            "'unsafe-hashes'",
            "'sha256-4cxJscbRcE7YwGipNEvlqtPCMkWLwJXchmXYyVWHinI='",
            "data:",
            "blob:",
            "*",
            "http:",
            "https://*.example.org");

    CspOverrideValidator.Result result = validate("*", "script-src", values, false);

    assertTrue(result.valid(), result.errors().toString());
    assertEquals(
        List.of(
            "* script-src http://intranet.example.org:8080",
            "* script-src 'unsafe-inline'",
            "* script-src 'unsafe-eval'",
            "* script-src 'wasm-unsafe-eval'",
            "* script-src 'unsafe-hashes'",
            "* script-src data:",
            "* script-src blob:",
            "* script-src *",
            "* script-src http:",
            "* script-src https://*.example.org"),
        result.unsafeTokens());
    // the hash keeps its case-sensitive base64 value
    assertTrue(
        result
            .normalized()
            .get("*")
            .enforced()
            .add()
            .get("script-src")
            .contains("'sha256-4cxJscbRcE7YwGipNEvlqtPCMkWLwJXchmXYyVWHinI='"));
  }

  @Test
  void scriptSrc_emptyListIsValidAndSafe() {
    CspOverrideValidator.Result result = validate("app", "script-src", List.of(), true);

    assertTrue(result.valid());
    assertTrue(result.unsafeTokens().isEmpty());
  }

  @ParameterizedTest
  @CsvSource(
      quoteCharacter = '"',
      value = {
        "'nonce-abc',KEYWORD_NOT_ALLOWED",
        "'none',KEYWORD_NOT_ALLOWED",
        "http:,INSECURE_SCHEME",
        "http://intranet.example.org,INSECURE_SCHEME",
        "https://cdn.example.org/lib.js,PATH_NOT_ALLOWED",
        "javascript:alert(1),INVALID_SOURCE"
      })
  void scriptSrc_rejects(String value, String code) {
    assertEquals(code, singleError(validate("*", "script-src", List.of(value), true)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://x.org; script-src 'unsafe-inline'",
        "https://x.org,https://y.org",
        "https://x.org\r\nX-Injected: 1",
        "https://x.org\u0000",
        "'self' https://x.org"
      })
  void everyDirective_rejectsSeparatorsWhitespaceAndControlCharacters(String value) {
    for (String directive : List.of("script-src", "img-src", "connect-src", "form-action")) {
      assertEquals(
          CspOverrideValidator.INVALID_CHARACTERS,
          singleError(validate("*", directive, List.of(value), false)),
          directive + " " + value);
    }
  }

  @ParameterizedTest
  @CsvSource(
      quoteCharacter = '"',
      value = {
        "img-src,https://tiles.example.org,",
        "img-src,https://tiles.example.org:8443,",
        "img-src,https://*.tiles.example.org,",
        "connect-src,https://*.example.org,",
        "connect-src,wss://socket.example.org,",
        "frame-src,https://embed.example.org,",
        "font-src,'self',",
        "img-src,https://*.org,WILDCARD_NOT_ALLOWED",
        "frame-src,https://*.example.org,WILDCARD_NOT_ALLOWED",
        "form-action,https://*.example.org,WILDCARD_NOT_ALLOWED",
        "img-src,*,WILDCARD_NOT_ALLOWED",
        "img-src,https:,INVALID_SOURCE",
        "img-src,data:,INVALID_SOURCE",
        "img-src,'unsafe-inline',KEYWORD_NOT_ALLOWED",
        "style-src,'unsafe-eval',KEYWORD_NOT_ALLOWED",
        "img-src,https://tiles.example.org/,PATH_NOT_ALLOWED",
        "img-src,https://tiles.example.org/x?y#z,PATH_NOT_ALLOWED",
        "img-src,https://user:pw@tiles.example.org,PATH_NOT_ALLOWED",
        "img-src,https://tiles.example.org:70000,INVALID_SOURCE",
        "img-src,wss://socket.example.org,INVALID_SOURCE",
        "img-src,http://tiles.example.org,INSECURE_SCHEME",
        "connect-src,ws://socket.example.org,INSECURE_SCHEME"
      })
  void originDirectives_onHttps(String directive, String value, String code) {
    CspOverrideValidator.Result result = validate("plugin", directive, List.of(value), true);
    if (code == null) {
      assertTrue(result.valid(), result.errors().toString());
      assertTrue(result.unsafeTokens().isEmpty());
    } else {
      assertEquals(code, singleError(result));
    }
  }

  @Test
  void originDirectives_allowPlainHttpWhileHttpsIsOff() {
    assertTrue(validate("*", "img-src", List.of("http://tiles.example.org"), false).valid());
    assertTrue(validate("*", "connect-src", List.of("ws://socket.example.org"), false).valid());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "default-src",
        "object-src",
        "base-uri",
        "frame-ancestors",
        "upgrade-insecure-requests",
        "report-uri",
        "report-to"
      })
  void lockedDirectives_cannotBeOverridden(String directive) {
    assertEquals(
        CspOverrideValidator.LOCKED_DIRECTIVE,
        singleError(validate("*", directive, List.of("https://x.org"), false)));
  }

  @Test
  void unknownEntrypointAndDirective_areRejected() {
    CspOverrideValidator.Result result =
        CspOverrideValidator.validate(
            Map.of(
                "index.html", ep("img-src", List.of("https://x.org")),
                "*", ep("sandbox", List.of("allow-scripts"))),
            null,
            Set.of(),
            true,
            NOW);

    assertEquals(
        Set.of(CspOverrideValidator.UNKNOWN_ENTRYPOINT, CspOverrideValidator.UNKNOWN_DIRECTIVE),
        Set.copyOf(result.errors().stream().map(CspValidationError::code).toList()));
  }

  @Test
  void disabledBaseSources_mustNameABaseSourceOfTheApp() {
    Set<String> base = CspBaseOverrides.sourceKeys("maps", true);
    EntrypointOverride ep =
        new EntrypointOverride(
            null,
            new SourceChanges(
                Map.of(),
                List.of("img-src https://tiles.maps.eox.at", "img-src https://evil.example.org")),
            null);

    CspOverrideValidator.Result result =
        CspOverrideValidator.validate(Map.of("plugin", ep), null, base, true, NOW);

    assertEquals(CspOverrideValidator.UNKNOWN_BASE_SOURCE, singleError(result));
    assertEquals(
        List.of("img-src https://tiles.maps.eox.at"),
        result.normalized().get("plugin").enforced().disabledBaseSources());
  }

  @Test
  void exemption_needsAnApprovalNote_andIsFlagged() {
    EntrypointOverride exempt =
        new EntrypointOverride(CspEnforcementMode.REPORT_ONLY_EXEMPT, null, null);

    CspOverrideValidator.Result noNote =
        CspOverrideValidator.validate(Map.of("app", exempt), " ", Set.of(), true, NOW);
    CspOverrideValidator.Result withNote =
        CspOverrideValidator.validate(Map.of("app", exempt), "OPS-1", Set.of(), true, NOW);

    assertEquals(CspOverrideValidator.APPROVAL_NOTE_REQUIRED, singleError(noNote));
    assertTrue(withNote.valid());
    assertTrue(withNote.exempt());
  }

  @Test
  void draftExpiry_mustBeWithinTheNext24Hours() {
    for (String expiry :
        List.of(
            NOW.minusSeconds(1).toString(),
            NOW.plus(Duration.ofHours(25)).toString(),
            "tomorrow")) {
      EntrypointOverride ep =
          new EntrypointOverride(null, null, new Draft(true, expiry, Map.of(), List.of()));
      assertEquals(
          CspOverrideValidator.DRAFT_EXPIRY_INVALID,
          singleError(CspOverrideValidator.validate(Map.of("*", ep), null, Set.of(), true, NOW)));
    }
    EntrypointOverride ok =
        new EntrypointOverride(
            null,
            null,
            new Draft(true, NOW.plus(Duration.ofHours(2)).toString(), Map.of(), List.of()));
    assertTrue(CspOverrideValidator.validate(Map.of("*", ok), null, Set.of(), true, NOW).valid());
  }

  @Test
  void draftAdditions_countAsUnsafeToo() {
    EntrypointOverride ep =
        new EntrypointOverride(
            null,
            null,
            new Draft(false, null, Map.of("script-src", List.of("'unsafe-eval'")), List.of()));

    CspOverrideValidator.Result result =
        CspOverrideValidator.validate(Map.of("app", ep), null, Set.of(), true, NOW);

    assertEquals(List.of("app script-src 'unsafe-eval'"), result.unsafeTokens());
  }

  @Test
  void isAcceptableOnRead_dropsHttpAfterHttpsIsTurnedOn_andLockedDirectives() {
    assertTrue(CspOverrideValidator.isAcceptableOnRead("img-src", "http://x.example.org", false));
    assertFalse(CspOverrideValidator.isAcceptableOnRead("img-src", "http://x.example.org", true));
    assertFalse(CspOverrideValidator.isAcceptableOnRead("frame-ancestors", "https://x.org", false));
    assertFalse(CspOverrideValidator.isAcceptableOnRead("default-src", "*", false));
  }
}
