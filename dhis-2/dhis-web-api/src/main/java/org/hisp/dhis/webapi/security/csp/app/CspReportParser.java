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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;

/**
 * Parses CSP violation reports into a fixed set of fields and redacts them. Accepts the {@code
 * report-uri} format ({@code application/csp-report}, one {@code csp-report} object) and the
 * Reporting API format ({@code application/reports+json}, an array of reports of type {@code
 * csp-violation}). Everything else of a payload (original policy, script sample, referrer, user
 * agent) is ignored.
 *
 * <p>Redaction: query and fragment are stripped from the document URI and source file; the blocked
 * URI is reduced to its origin or keyword. Reports caused by browser extensions ({@code
 * chrome-extension:}, {@code moz-extension:}, {@code safari-extension:}, {@code
 * safari-web-extension:}) or {@code about:} documents are dropped.
 *
 * @author Morten Svanæs
 */
final class CspReportParser {
  private CspReportParser() {}

  static final int MAX_REPORTS_PER_PAYLOAD = 20;

  private static final int MAX_URI_LENGTH = 512;

  private static final Set<String> NOISE_SCHEMES =
      Set.of(
          "chrome-extension",
          "moz-extension",
          "safari-extension",
          "safari-web-extension",
          "ms-browser-extension",
          "about");

  private static final Pattern ORIGIN =
      Pattern.compile("^([a-z][a-z0-9+.-]*)://([^/?#@\\s]+@)?([^/?#\\s]+)(.*)$");

  private static final Pattern DIRECTIVE = Pattern.compile("^[a-z-]{1,40}$");

  private static final Pattern KEYWORD = Pattern.compile("^[a-z-]{1,30}$");

  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  /** The fields read from a report payload, before redaction. */
  record Raw(
      String documentUri,
      String blockedUri,
      String effectiveDirective,
      String violatedDirective,
      String sourceFile,
      Integer line,
      Integer column,
      String disposition) {}

  /** Outcome of normalizing one report. */
  record Outcome(CspViolationService.Report report, String dropReason) {}

  static List<Raw> parse(byte[] body, ObjectMapper mapper) throws IOException {
    JsonNode root = mapper.readTree(body);
    List<Raw> result = new ArrayList<>();
    if (root == null) {
      return result;
    }
    if (root.isArray()) {
      for (JsonNode item : root) {
        if (result.size() >= MAX_REPORTS_PER_PAYLOAD) {
          break;
        }
        if ("csp-violation".equals(text(item, "type")) && item.path("body").isObject()) {
          JsonNode b = item.get("body");
          result.add(
              new Raw(
                  text(b, "documentURL"),
                  text(b, "blockedURL"),
                  text(b, "effectiveDirective"),
                  null,
                  text(b, "sourceFile"),
                  number(b, "lineNumber"),
                  number(b, "columnNumber"),
                  text(b, "disposition")));
        }
      }
    } else if (root.path("csp-report").isObject()) {
      JsonNode r = root.get("csp-report");
      result.add(
          new Raw(
              text(r, "document-uri"),
              text(r, "blocked-uri"),
              text(r, "effective-directive"),
              text(r, "violated-directive"),
              text(r, "source-file"),
              number(r, "line-number"),
              number(r, "column-number"),
              text(r, "disposition")));
    }
    return result;
  }

  static Outcome normalize(Raw raw, String appKey, String entrypoint) {
    String directive = directive(raw);
    if (directive == null) {
      return new Outcome(null, "invalid");
    }
    if (isNoise(raw.documentUri()) || isNoise(raw.sourceFile()) || isNoise(raw.blockedUri())) {
      return new Outcome(null, "extension");
    }
    Optional<String> blocked = blocked(raw.blockedUri());
    if (blocked.isEmpty()) {
      return new Outcome(null, "invalid");
    }
    String disposition = "report".equalsIgnoreCase(raw.disposition()) ? "report" : "enforce";
    return new Outcome(
        new CspViolationService.Report(
            appKey,
            entrypoint,
            disposition,
            directive,
            blocked.get(),
            stripUri(raw.documentUri()),
            stripUri(raw.sourceFile()),
            bounded(raw.line()),
            bounded(raw.column())),
        null);
  }

  @CheckForNull
  private static String directive(Raw raw) {
    String value = raw.effectiveDirective();
    if (value == null || value.isBlank()) {
      value =
          raw.violatedDirective() == null
              ? null
              : WHITESPACE.split(raw.violatedDirective().trim())[0];
    }
    if (value == null) {
      return null;
    }
    value = value.trim().toLowerCase(Locale.ROOT);
    if (!DIRECTIVE.matcher(value).matches()) {
      return null;
    }
    if (value.endsWith("-elem") || value.endsWith("-attr")) {
      value = value.substring(0, value.length() - 5);
    }
    return value;
  }

  /** The blocked origin ({@code scheme://host[:port]}) or keyword. Empty means inline. */
  static Optional<String> blocked(@CheckForNull String blockedUri) {
    if (blockedUri == null || blockedUri.isBlank()) {
      return Optional.of("inline");
    }
    String value = blockedUri.trim();
    Matcher m = ORIGIN.matcher(value);
    if (m.matches()) {
      String scheme = m.group(1).toLowerCase(Locale.ROOT);
      String host = m.group(3).toLowerCase(Locale.ROOT);
      if (host.length() > 255) {
        return Optional.empty();
      }
      return Optional.of(scheme + "://" + host);
    }
    String lower = value.toLowerCase(Locale.ROOT);
    if (lower.endsWith(":")) {
      lower = lower.substring(0, lower.length() - 1);
    }
    int colon = lower.indexOf(':');
    if (colon > 0) {
      // data:image/png;base64,... or blob:https://... keep the scheme only
      lower = lower.substring(0, colon);
    }
    return KEYWORD.matcher(lower).matches() ? Optional.of(lower) : Optional.of("other");
  }

  private static boolean isNoise(@CheckForNull String uri) {
    if (uri == null) {
      return false;
    }
    String lower = uri.trim().toLowerCase(Locale.ROOT);
    int colon = lower.indexOf(':');
    return colon > 0 && NOISE_SCHEMES.contains(lower.substring(0, colon))
        || NOISE_SCHEMES.contains(lower);
  }

  /** Keeps http(s) URIs without query and fragment; anything else becomes {@code null}. */
  @CheckForNull
  static String stripUri(@CheckForNull String uri) {
    if (uri == null) {
      return null;
    }
    String value = uri.trim();
    String lower = value.toLowerCase(Locale.ROOT);
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
      return null;
    }
    int cut = value.length();
    for (char c : new char[] {'?', '#'}) {
      int i = value.indexOf(c);
      if (i >= 0 && i < cut) {
        cut = i;
      }
    }
    value = value.substring(0, cut);
    Matcher m = ORIGIN.matcher(value);
    if (m.matches() && m.group(2) != null) {
      value = m.group(1) + "://" + m.group(3) + m.group(4);
    }
    if (value.length() > MAX_URI_LENGTH || value.chars().anyMatch(Character::isISOControl)) {
      return null;
    }
    return value;
  }

  @CheckForNull
  private static Integer bounded(@CheckForNull Integer value) {
    return value == null || value < 0 || value > 10_000_000 ? null : value;
  }

  @CheckForNull
  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value != null && value.isTextual() ? value.asText() : null;
  }

  @CheckForNull
  private static Integer number(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value != null && value.canConvertToInt() ? value.asInt() : null;
  }
}
