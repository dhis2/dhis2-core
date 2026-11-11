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

import static org.hisp.dhis.security.utils.CspConstants.LOCKED_DIRECTIVES;
import static org.hisp.dhis.security.utils.CspConstants.OVERRIDABLE_DIRECTIVES;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import org.hisp.dhis.webapi.security.csp.app.CspAppOverride.Draft;
import org.hisp.dhis.webapi.security.csp.app.CspAppOverride.EntrypointOverride;
import org.hisp.dhis.webapi.security.csp.app.CspAppOverride.SourceChanges;

/**
 * Validates and normalizes administrator CSP overrides. Rules:
 *
 * <ul>
 *   <li>Every value is one source token: {@code ;}, {@code ,}, whitespace and control characters
 *       (including CR/LF) are rejected, so a value can never break out of its directive.
 *   <li>Locked directives ({@code default-src}, {@code object-src}, {@code base-uri}, {@code
 *       frame-ancestors}, {@code upgrade-insecure-requests}, {@code report-uri}, {@code report-to})
 *       cannot be overridden.
 *   <li>{@code script-src} accepts every value: bare origins, {@code http://} origins while {@code
 *       server.https} is off, keywords ({@code 'self'}, {@code 'strict-dynamic'}, {@code
 *       'report-sample'}, the unsafe ones), hashes and the scheme or wildcard sources. Unsafe
 *       values ({@link #UNSAFE_KEYWORDS}, {@code data:}, {@code blob:}, {@code *}, {@code http:},
 *       {@code https:}, {@code http://} origins, wildcard hosts) are collected as unsafe tokens and
 *       need an explicit confirmation on save.
 *   <li>Other directives take bare origins {@code scheme://host[:port]} only (no path, query,
 *       fragment, userinfo, scheme-only source or bare {@code *}). {@code http://} and {@code
 *       ws://} are rejected while {@code server.https} is on. {@code https://*.<domain>} with at
 *       least two labels after {@code *.} is allowed in {@code img-src} and {@code connect-src}
 *       only; {@code ws(s)://} in {@code connect-src} only. {@code form-action} takes no wildcard.
 * </ul>
 *
 * @author Morten Svanæs
 */
public final class CspOverrideValidator {

  public static final Set<String> UNSAFE_KEYWORDS =
      Set.of("'unsafe-inline'", "'unsafe-eval'", "'wasm-unsafe-eval'", "'unsafe-hashes'");

  private static final Set<String> SAFE_SCRIPT_KEYWORDS =
      Set.of("'self'", "'strict-dynamic'", "'report-sample'");

  private static final Set<String> UNSAFE_SCRIPT_SCHEMES =
      Set.of("data:", "blob:", "*", "http:", "https:");

  private static final Pattern HASH =
      Pattern.compile("^'(sha256|sha384|sha512)-[A-Za-z0-9+/_-]+={0,2}'$");

  /**
   * An origin {@code scheme://host[:port]} followed by anything; the remainder must be empty. The
   * host is either a DNS name (each label {@code [a-z0-9]([a-z0-9-]*[a-z0-9])?}) optionally
   * prefixed by {@code *.}, or an IPv4 address (which matches the DNS form).
   */
  private static final Pattern ORIGIN =
      Pattern.compile(
          "^(?<scheme>[a-z][a-z0-9+.-]*)://(?<wild>\\*\\.)?"
              + "(?<host>[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)*)"
              + "(?::(?<port>\\d{1,5}))?(?<rest>.*)$");

  private static final int MAX_PORT = 65535;

  private static final int MAX_VALUES_PER_DIRECTIVE = 64;

  static final Duration MAX_DRAFT_TEST = Duration.ofHours(24);

  public static final String UNKNOWN_ENTRYPOINT = "UNKNOWN_ENTRYPOINT";
  public static final String LOCKED_DIRECTIVE = "LOCKED_DIRECTIVE";
  public static final String UNKNOWN_DIRECTIVE = "UNKNOWN_DIRECTIVE";
  public static final String INVALID_CHARACTERS = "INVALID_CHARACTERS";
  public static final String PATH_NOT_ALLOWED = "PATH_NOT_ALLOWED";
  public static final String INVALID_SOURCE = "INVALID_SOURCE";
  public static final String WILDCARD_NOT_ALLOWED = "WILDCARD_NOT_ALLOWED";
  public static final String INSECURE_SCHEME = "INSECURE_SCHEME";
  public static final String KEYWORD_NOT_ALLOWED = "KEYWORD_NOT_ALLOWED";
  public static final String UNKNOWN_BASE_SOURCE = "UNKNOWN_BASE_SOURCE";
  public static final String DRAFT_EXPIRY_INVALID = "DRAFT_EXPIRY_INVALID";
  public static final String APPROVAL_NOTE_REQUIRED = "APPROVAL_NOTE_REQUIRED";
  public static final String TOO_MANY_VALUES = "TOO_MANY_VALUES";
  public static final String CONFIRM_UNSAFE_REQUIRED = "CONFIRM_UNSAFE_REQUIRED";
  public static final String CONFIRM_EXEMPT_REQUIRED = "CONFIRM_EXEMPT_REQUIRED";

  private CspOverrideValidator() {}

  /** Outcome of a single source check. */
  enum Verdict {
    OK,
    UNSAFE,
    REJECTED
  }

  /** Result of {@link #checkSource}: the verdict, the normalized value and the error code. */
  record SourceCheck(Verdict verdict, String normalized, String code, String message) {
    static SourceCheck ok(String normalized) {
      return new SourceCheck(Verdict.OK, normalized, null, null);
    }

    static SourceCheck unsafe(String normalized) {
      return new SourceCheck(Verdict.UNSAFE, normalized, null, null);
    }

    static SourceCheck reject(String code, String message) {
      return new SourceCheck(Verdict.REJECTED, null, code, message);
    }
  }

  /** Validation result of a whole override. */
  public record Result(
      List<CspValidationError> errors,
      List<String> warnings,
      List<String> unsafeTokens,
      boolean exempt,
      Map<String, EntrypointOverride> normalized) {
    public boolean valid() {
      return errors.isEmpty();
    }
  }

  /**
   * Validates the entrypoint overrides of an app.
   *
   * @param entrypoints the overrides keyed by entrypoint
   * @param approvalNote the approval note (required for an exemption)
   * @param baseSources the {@code "<directive> <source>"} base-override sources of the app that may
   *     be disabled
   * @param https whether {@code server.https} is on
   * @param now the current time, for draft expiry checks
   */
  public static Result validate(
      Map<String, EntrypointOverride> entrypoints,
      @CheckForNull String approvalNote,
      Set<String> baseSources,
      boolean https,
      Instant now) {
    List<CspValidationError> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    Set<String> unsafe = new LinkedHashSet<>();
    Map<String, EntrypointOverride> normalized = new LinkedHashMap<>();
    boolean exempt = false;

    for (String key : orderedKeys(entrypoints)) {
      EntrypointOverride ep = entrypoints.get(key);
      if (!CspEntrypoints.isValid(key)) {
        errors.add(
            new CspValidationError(
                key,
                null,
                null,
                UNKNOWN_ENTRYPOINT,
                "Entrypoint must be one of *, app, plugin, worker"));
        continue;
      }
      if (ep == null) {
        continue;
      }
      CspEnforcementMode mode = ep.enforcementMode();
      exempt |= mode == CspEnforcementMode.REPORT_ONLY_EXEMPT;

      SourceChanges enforced = ep.enforcedOrEmpty();
      Map<String, List<String>> add =
          validateAdds(key, enforced.addOrEmpty(), https, errors, unsafe);
      List<String> disabled =
          validateDisabled(key, enforced.disabledOrEmpty(), baseSources, errors);

      Draft draft = ep.draftOrEmpty();
      Map<String, List<String>> draftAdd =
          validateAdds(key, draft.addOrEmpty(), https, errors, unsafe);
      List<String> draftDisabled =
          validateDisabled(key, draft.disabledOrEmpty(), baseSources, errors);
      String expiresAt = validateExpiry(key, draft.expiresAt(), now, errors);

      normalized.put(
          key,
          new EntrypointOverride(
              mode,
              new SourceChanges(add, disabled),
              new Draft(draft.enabled(), expiresAt, draftAdd, draftDisabled)));
    }

    if (exempt && (approvalNote == null || approvalNote.isBlank())) {
      errors.add(
          new CspValidationError(
              null,
              null,
              null,
              APPROVAL_NOTE_REQUIRED,
              "An approval note is required when an entrypoint uses REPORT_ONLY_EXEMPT"));
    }
    if (exempt) {
      warnings.add(
          "REPORT_ONLY_EXEMPT: the exempt entrypoints run without an enforced CSP (only"
              + " frame-ancestors); violations are reported but not blocked");
    }
    if (!unsafe.isEmpty()) {
      warnings.add("Unsafe sources weaken XSS protection: " + String.join(", ", unsafe));
    }
    return new Result(errors, warnings, new ArrayList<>(unsafe), exempt, normalized);
  }

  /**
   * Whether a stored value is still acceptable when composing a policy. Values that fail validation
   * now (for example {@code http://} origins after {@code server.https} was turned on, or entries
   * written directly into the datastore) are dropped.
   */
  public static boolean isAcceptableOnRead(String directive, String value, boolean https) {
    if (!OVERRIDABLE_DIRECTIVES.contains(directive)) {
      return false;
    }
    return checkSource(directive, value, https).verdict() != Verdict.REJECTED;
  }

  private static List<String> orderedKeys(Map<String, EntrypointOverride> entrypoints) {
    List<String> keys = new ArrayList<>();
    for (String key : CspEntrypoints.KEYS) {
      if (entrypoints.containsKey(key)) {
        keys.add(key);
      }
    }
    for (String key : entrypoints.keySet()) {
      if (!keys.contains(key)) {
        keys.add(key);
      }
    }
    return keys;
  }

  private static Map<String, List<String>> validateAdds(
      String entrypoint,
      Map<String, List<String>> add,
      boolean https,
      List<CspValidationError> errors,
      Set<String> unsafe) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    for (Map.Entry<String, List<String>> e : add.entrySet()) {
      String directive = e.getKey() == null ? "" : e.getKey().trim().toLowerCase(Locale.ROOT);
      List<String> values = e.getValue() == null ? List.of() : e.getValue();
      if (LOCKED_DIRECTIVES.contains(directive)) {
        errors.add(
            new CspValidationError(
                entrypoint,
                directive,
                null,
                LOCKED_DIRECTIVE,
                directive + " cannot be overridden"));
        continue;
      }
      if (!OVERRIDABLE_DIRECTIVES.contains(directive)) {
        errors.add(
            new CspValidationError(
                entrypoint,
                directive,
                null,
                UNKNOWN_DIRECTIVE,
                "Overridable directives: " + String.join(", ", OVERRIDABLE_DIRECTIVES)));
        continue;
      }
      if (values.size() > MAX_VALUES_PER_DIRECTIVE) {
        errors.add(
            new CspValidationError(
                entrypoint,
                directive,
                null,
                TOO_MANY_VALUES,
                "At most " + MAX_VALUES_PER_DIRECTIVE + " values per directive"));
        continue;
      }
      Set<String> accepted = new LinkedHashSet<>();
      for (String raw : values) {
        SourceCheck check = checkSource(directive, raw, https);
        switch (check.verdict()) {
          case REJECTED ->
              errors.add(
                  new CspValidationError(
                      entrypoint, directive, raw, check.code(), check.message()));
          case UNSAFE -> {
            accepted.add(check.normalized());
            unsafe.add(entrypoint + " " + directive + " " + check.normalized());
          }
          case OK -> accepted.add(check.normalized());
        }
      }
      if (!accepted.isEmpty()) {
        result.computeIfAbsent(directive, k -> new ArrayList<>()).addAll(accepted);
      }
    }
    return result;
  }

  private static List<String> validateDisabled(
      String entrypoint,
      List<String> disabled,
      Set<String> baseSources,
      List<CspValidationError> errors) {
    Set<String> result = new LinkedHashSet<>();
    for (String raw : disabled) {
      String value = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
      if (hasForbiddenCharacters(value.replace(" ", ""))) {
        errors.add(
            new CspValidationError(
                entrypoint, null, raw, INVALID_CHARACTERS, "Invalid characters in base source"));
      } else if (!baseSources.contains(value)) {
        errors.add(
            new CspValidationError(
                entrypoint,
                null,
                raw,
                UNKNOWN_BASE_SOURCE,
                "Not a base source of this app; expected '<directive> <source>' from baseOverrides"));
      } else {
        result.add(value);
      }
    }
    return new ArrayList<>(result);
  }

  @CheckForNull
  private static String validateExpiry(
      String entrypoint,
      @CheckForNull String expiresAt,
      Instant now,
      List<CspValidationError> errors) {
    if (expiresAt == null || expiresAt.isBlank()) {
      return null;
    }
    try {
      Instant expiry = Instant.parse(expiresAt.trim());
      if (!expiry.isAfter(now) || expiry.isAfter(now.plus(MAX_DRAFT_TEST))) {
        errors.add(
            new CspValidationError(
                entrypoint,
                null,
                expiresAt,
                DRAFT_EXPIRY_INVALID,
                "expiresAt must be in the future and at most 24 hours ahead"));
        return null;
      }
      return expiry.toString();
    } catch (DateTimeParseException e) {
      errors.add(
          new CspValidationError(
              entrypoint, null, expiresAt, DRAFT_EXPIRY_INVALID, "expiresAt must be ISO-8601"));
      return null;
    }
  }

  static SourceCheck checkSource(String directive, @CheckForNull String raw, boolean https) {
    String value = raw == null ? "" : raw.strip();
    if (raw != null && raw.chars().anyMatch(Character::isISOControl)) {
      return SourceCheck.reject(INVALID_CHARACTERS, "A source must not contain control characters");
    }
    if (value.isEmpty()) {
      return SourceCheck.reject(INVALID_SOURCE, "Empty source");
    }
    if (hasForbiddenCharacters(value)) {
      return SourceCheck.reject(
          INVALID_CHARACTERS,
          "A source must not contain ';', ',', whitespace or control characters");
    }
    return "script-src".equals(directive)
        ? checkScriptSource(value, https)
        : checkOriginSource(directive, value, https);
  }

  private static SourceCheck checkScriptSource(String value, boolean https) {
    String lower = value.toLowerCase(Locale.ROOT);
    if (lower.startsWith("'")) {
      if (SAFE_SCRIPT_KEYWORDS.contains(lower)) {
        return SourceCheck.ok(lower);
      }
      if (UNSAFE_KEYWORDS.contains(lower)) {
        return SourceCheck.unsafe(lower);
      }
      if (HASH.matcher(value).matches()) {
        // base64 is case-sensitive: keep the hash value as given, only lower the algorithm
        int dash = value.indexOf('-');
        return SourceCheck.ok(
            value.substring(0, dash).toLowerCase(Locale.ROOT) + value.substring(dash));
      }
      if (lower.startsWith("'nonce-")) {
        return SourceCheck.reject(
            KEYWORD_NOT_ALLOWED, "Nonces are generated per response and cannot be configured");
      }
      return SourceCheck.reject(KEYWORD_NOT_ALLOWED, "Unsupported keyword " + value);
    }
    if (UNSAFE_SCRIPT_SCHEMES.contains(lower)) {
      if (https && "http:".equals(lower)) {
        return SourceCheck.reject(INSECURE_SCHEME, "http: is not allowed while server.https is on");
      }
      return SourceCheck.unsafe(lower);
    }
    OriginCheck origin = parseOrigin(lower);
    if (origin.error() != null) {
      return origin.error();
    }
    if ("http".equals(origin.scheme())) {
      return https
          ? SourceCheck.reject(INSECURE_SCHEME, "http:// is not allowed while server.https is on")
          : SourceCheck.unsafe(origin.value());
    }
    if (!"https".equals(origin.scheme())) {
      return SourceCheck.reject(INVALID_SOURCE, "script-src origins must use https");
    }
    return origin.wildcard() ? SourceCheck.unsafe(origin.value()) : SourceCheck.ok(origin.value());
  }

  private static SourceCheck checkOriginSource(String directive, String value, boolean https) {
    String lower = value.toLowerCase(Locale.ROOT);
    if ("'self'".equals(lower)) {
      return SourceCheck.ok(lower);
    }
    if (lower.startsWith("'")) {
      return SourceCheck.reject(
          KEYWORD_NOT_ALLOWED,
          "Keywords are only allowed in script-src; " + directive + " takes origins");
    }
    if ("*".equals(lower)) {
      return SourceCheck.reject(WILDCARD_NOT_ALLOWED, "A bare * is not allowed");
    }
    if (lower.matches("^[a-z][a-z0-9+.-]*:$")) {
      return SourceCheck.reject(
          INVALID_SOURCE, "Scheme-only sources are not allowed; use scheme://host[:port]");
    }
    OriginCheck origin = parseOrigin(lower);
    if (origin.error() != null) {
      return origin.error();
    }
    boolean connect = "connect-src".equals(directive);
    switch (origin.scheme()) {
      case "https" -> {}
      case "wss" -> {
        if (!connect) {
          return SourceCheck.reject(INVALID_SOURCE, "wss:// is only allowed in connect-src");
        }
      }
      case "http", "ws" -> {
        if (!connect && "ws".equals(origin.scheme())) {
          return SourceCheck.reject(INVALID_SOURCE, "ws:// is only allowed in connect-src");
        }
        if (https) {
          return SourceCheck.reject(
              INSECURE_SCHEME, origin.scheme() + ":// is not allowed while server.https is on");
        }
      }
      default -> {
        return SourceCheck.reject(INVALID_SOURCE, "Unsupported scheme " + origin.scheme());
      }
    }
    if (origin.wildcard() && !(connect || "img-src".equals(directive))) {
      return SourceCheck.reject(
          WILDCARD_NOT_ALLOWED, "Wildcard hosts are only allowed in img-src and connect-src");
    }
    return SourceCheck.ok(origin.value());
  }

  private record OriginCheck(String scheme, String value, boolean wildcard, SourceCheck error) {
    static OriginCheck fail(String code, String message) {
      return new OriginCheck(null, null, false, SourceCheck.reject(code, message));
    }
  }

  private static OriginCheck parseOrigin(String lower) {
    if (!lower.contains("://")) {
      return OriginCheck.fail(INVALID_SOURCE, "Expected an origin scheme://host[:port]");
    }
    String afterScheme = lower.substring(lower.indexOf("://") + 3);
    int end = indexOfAny(afterScheme, "/?#");
    if (afterScheme.substring(0, end < 0 ? afterScheme.length() : end).contains("@")) {
      return OriginCheck.fail(PATH_NOT_ALLOWED, "Userinfo is not allowed in an origin");
    }
    Matcher m = ORIGIN.matcher(lower);
    if (!m.matches()) {
      return OriginCheck.fail(INVALID_SOURCE, "Expected an origin scheme://host[:port]");
    }
    if (!m.group("rest").isEmpty()) {
      return OriginCheck.fail(
          PATH_NOT_ALLOWED, "Only bare origins (scheme://host[:port]) are allowed");
    }
    String port = m.group("port");
    if (port != null) {
      int number = Integer.parseInt(port);
      if (number < 1 || number > MAX_PORT) {
        return OriginCheck.fail(INVALID_SOURCE, "Port is outside the range 1-" + MAX_PORT);
      }
    }
    boolean wildcard = m.group("wild") != null;
    if (wildcard && m.group("host").split("\\.").length < 2) {
      return OriginCheck.fail(
          WILDCARD_NOT_ALLOWED,
          "A wildcard needs at least two labels after *., e.g. https://*.example.org");
    }
    return new OriginCheck(m.group("scheme"), lower, wildcard, null);
  }

  private static int indexOfAny(String s, String chars) {
    for (int i = 0; i < s.length(); i++) {
      if (chars.indexOf(s.charAt(i)) >= 0) {
        return i;
      }
    }
    return -1;
  }

  private static boolean hasForbiddenCharacters(String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == ';'
          || c == ','
          || Character.isWhitespace(c)
          || Character.isISOControl(c)
          || Character.isSpaceChar(c)) {
        return true;
      }
    }
    return false;
  }
}
