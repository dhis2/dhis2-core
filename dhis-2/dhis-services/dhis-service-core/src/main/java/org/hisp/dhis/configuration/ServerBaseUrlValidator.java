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
package org.hisp.dhis.configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Validates the {@code server.base.url} configured in {@code dhis.conf}. Shared by the startup log
 * warning and the {@code server_base_url_not_configured} data integrity check so both report the
 * same problems.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerBaseUrlValidator {

  public static final String HINT =
      "Expected an absolute URL without a trailing slash,"
          + " for example: 'https://dhis2.example.org/dhis'."
          + " This value is important: features including password recovery, OIDC/OAuth2"
          + " redirects, notification emails, and interpretation sharing will not work"
          + " correctly without it. See the 'Server base URL' section in the dhis.conf"
          + " reference documentation.";

  private static final Set<String> SCHEMES = Set.of("http", "https");

  /**
   * @param baseUrl the configured server base URL, may be null or blank when not set
   * @return a description of the problem, or empty if the base URL is valid
   */
  @Nonnull
  public static Optional<String> validate(@CheckForNull String baseUrl) {
    if (StringUtils.isBlank(baseUrl)) {
      return Optional.of("'server.base.url' is not set in dhis.conf.");
    }
    if (!isAbsoluteHttpUrl(baseUrl)) {
      return Optional.of("'server.base.url' is not a valid URL: '" + baseUrl + "'.");
    }
    if (baseUrl.endsWith("/")) {
      return Optional.of(
          "'server.base.url' must not end with a trailing slash: '" + baseUrl + "'.");
    }
    return Optional.empty();
  }

  /**
   * Requires an absolute http(s) URL with a host. Deliberately does not check the host against
   * public top-level domains, so intranet names such as {@code dhis2.moh.local}, {@code localhost}
   * or {@code dhis-server} are accepted. {@link URI} reports no host for names containing an
   * underscore (e.g. Docker service names like {@code dhis2_core}), so a non-empty authority is
   * accepted too.
   */
  private static boolean isAbsoluteHttpUrl(String baseUrl) {
    try {
      URI uri = new URI(baseUrl);
      return uri.getScheme() != null
          && SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT))
          && (StringUtils.isNotEmpty(uri.getHost()) || StringUtils.isNotEmpty(uri.getAuthority()));
    } catch (URISyntaxException ex) {
      return false;
    }
  }
}
