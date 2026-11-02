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
package org.hisp.dhis.configuration;

import com.google.common.net.InternetDomainName;
import java.net.IDN;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.commons.util.TextUtils;

/**
 * A validated and normalised entry of the CORS allowlist.
 *
 * <p>The allowlist decides which foreign origins are trusted, so an entry must describe an origin:
 * {@code scheme://host[:port]} with scheme {@code http} or {@code https} and no user info, path,
 * query or fragment. A single trailing {@code /} is tolerated and removed. Scheme and host are
 * lower-cased, internationalised host names are converted to their ASCII form and default ports
 * ({@code 80} for http, {@code 443} for https) are dropped, so the normalised entry has the same
 * form as a browser {@code Origin} header.
 *
 * <p>The only wildcard allowed is {@code *} as the complete leftmost host label, for example {@code
 * https://*.example.org}, which trusts every subdomain of {@code example.org}. The part after the
 * wildcard must have at least two labels and must not be a public suffix ({@code *.org}, {@code
 * *.co.uk} and {@code *.github.io} are rejected). Wildcard entries keep the matching semantics of
 * {@link TextUtils#createRegexFromGlob(String)}; all other entries match by exact comparison.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class CorsAllowlistEntry {

  private static final String SCHEME_SEPARATOR = "://";

  private static final String WILDCARD_LABEL = "*";

  private static final List<String> SCHEMES = List.of("http", "https");

  private static final Pattern HOST_LABEL =
      Pattern.compile("[a-z0-9_](?:[a-z0-9_-]{0,61}[a-z0-9_])?");

  private static final Pattern IPV6_LITERAL = Pattern.compile("\\[[0-9a-f:.]+]");

  private static final Pattern PORT = Pattern.compile("[0-9]{1,5}");

  private static final Pattern DIGITS = Pattern.compile("[0-9]+");

  private final String origin;

  @CheckForNull private final Pattern wildcardPattern;

  private CorsAllowlistEntry(String origin, boolean wildcard) {
    this.origin = origin;
    this.wildcardPattern = wildcard ? Pattern.compile(TextUtils.createRegexFromGlob(origin)) : null;
  }

  /**
   * Parses and validates an allowlist entry.
   *
   * @param entry the entry as entered by an administrator or as stored.
   * @return the validated, normalised entry.
   * @throws IllegalArgumentException if the entry is not a valid allowlist entry; the message
   *     explains why and does not repeat the entry.
   */
  @Nonnull
  public static CorsAllowlistEntry parse(@CheckForNull String entry) {
    String value = entry == null ? "" : entry.strip();
    if (value.isEmpty()) {
      throw invalid("the entry is empty");
    }
    if (value.equalsIgnoreCase("null")) {
      throw invalid("the opaque origin `null` cannot be trusted");
    }
    if (value.endsWith("/")) {
      value = value.substring(0, value.length() - 1);
    }

    int schemeEnd = value.indexOf(SCHEME_SEPARATOR);
    if (schemeEnd < 0) {
      throw invalid("an origin must start with http:// or https://");
    }
    String scheme = value.substring(0, schemeEnd).toLowerCase(Locale.ROOT);
    if (!SCHEMES.contains(scheme)) {
      throw invalid("the scheme must be http or https");
    }

    String authority = value.substring(schemeEnd + SCHEME_SEPARATOR.length());
    if (authority.isEmpty()) {
      throw invalid("the host is missing");
    }
    if (authority.contains("/") || authority.contains("?") || authority.contains("#")) {
      throw invalid("an origin must not contain a path, query or fragment");
    }
    if (authority.contains("@")) {
      throw invalid("an origin must not contain user info");
    }

    String host;
    String port;
    if (authority.startsWith("[")) {
      int ipv6End = authority.indexOf(']');
      host = ipv6End < 0 ? authority : authority.substring(0, ipv6End + 1);
      port = portAfter(authority.substring(host.length()));
      host = host.toLowerCase(Locale.ROOT);
      if (!IPV6_LITERAL.matcher(host).matches()) {
        throw invalid("the IPv6 address is malformed");
      }
    } else {
      int portStart = authority.lastIndexOf(':');
      host = portStart < 0 ? authority : authority.substring(0, portStart);
      port = portStart < 0 ? null : portAfter(authority.substring(portStart));
      host = toAsciiHost(host);
    }

    boolean wildcard = host.startsWith(WILDCARD_LABEL + ".");
    validateHost(wildcard ? host.substring(2) : host, wildcard);

    StringBuilder origin = new StringBuilder(scheme).append(SCHEME_SEPARATOR).append(host);
    if (port != null && !isDefaultPort(scheme, port)) {
      origin.append(':').append(port);
    }
    return new CorsAllowlistEntry(origin.toString(), wildcard);
  }

  /**
   * @return the normalised origin, for example {@code https://*.example.org:8443}.
   */
  @Nonnull
  public String getOrigin() {
    return origin;
  }

  /**
   * @return true if this entry trusts every subdomain of a domain.
   */
  public boolean isSubdomainWildcard() {
    return wildcardPattern != null;
  }

  /**
   * @param requestOrigin the value of an {@code Origin} request header.
   * @return true if the given origin is trusted by this entry.
   */
  public boolean matches(@CheckForNull String requestOrigin) {
    if (requestOrigin == null) {
      return false;
    }
    return wildcardPattern == null
        ? origin.equals(requestOrigin)
        : wildcardPattern.matcher(requestOrigin).matches();
  }

  @Override
  public String toString() {
    return origin;
  }

  @CheckForNull
  private static String portAfter(String rest) {
    if (rest.isEmpty()) {
      return null;
    }
    if (!rest.startsWith(":")) {
      throw invalid("the host is malformed");
    }
    String port = rest.substring(1);
    if (!PORT.matcher(port).matches()) {
      throw invalid("the port must be a number");
    }
    int number = Integer.parseInt(port);
    if (number < 1 || number > 65535) {
      throw invalid("the port must be between 1 and 65535");
    }
    return String.valueOf(number);
  }

  private static String toAsciiHost(String host) {
    if (host.isEmpty()) {
      throw invalid("the host is missing");
    }
    try {
      return IDN.toASCII(host, IDN.ALLOW_UNASSIGNED).toLowerCase(Locale.ROOT);
    } catch (IllegalArgumentException ex) {
      throw invalid("the host is not a valid domain name");
    }
  }

  private static void validateHost(String host, boolean wildcard) {
    if (host.contains(WILDCARD_LABEL)) {
      throw invalid(
          "a wildcard is only allowed as the leftmost part of the host, as in https://*.example.org");
    }
    if (host.startsWith("[")) {
      return; // IPv6 literal, validated by the caller
    }
    String[] labels = host.split("\\.", -1);
    for (String label : labels) {
      if (!HOST_LABEL.matcher(label).matches()) {
        throw invalid("the host is not a valid domain name");
      }
    }
    if (wildcard) {
      validateWildcardDomain(host, labels);
    }
  }

  private static void validateWildcardDomain(String domain, String[] labels) {
    if (labels.length < 2 || DIGITS.matcher(labels[labels.length - 1]).matches()) {
      throw invalid("a wildcard entry must name a registered domain, as in https://*.example.org");
    }
    boolean publicSuffix;
    try {
      publicSuffix = InternetDomainName.from(domain).isPublicSuffix();
    } catch (IllegalArgumentException ex) {
      throw invalid("the host is not a valid domain name");
    }
    if (publicSuffix) {
      throw invalid("a wildcard entry must not cover a public suffix such as *.co.uk");
    }
  }

  private static boolean isDefaultPort(String scheme, String port) {
    return switch (scheme) {
      case "http" -> port.equals("80");
      case "https" -> port.equals("443");
      default -> false;
    };
  }

  private static IllegalArgumentException invalid(String reason) {
    return new IllegalArgumentException(reason);
  }
}
