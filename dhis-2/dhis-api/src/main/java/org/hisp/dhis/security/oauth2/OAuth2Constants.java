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
package org.hisp.dhis.security.oauth2;

import java.util.List;
import java.util.Set;

/**
 * Shared constants for the DHIS2 OAuth2 authorization server integration.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class OAuth2Constants {

  private OAuth2Constants() {}

  /**
   * Reserved clientId for the bootstrap client that the DCR flow uses to mint initial access tokens
   * for new device registrations. Created and managed by {@code OAuth2DcrService.init()}; the
   * REST/metadata pipelines must never allow an end user to create or rename a client to this
   * value.
   */
  public static final String SYSTEM_REGISTRAR_CLIENTID = "system-dcr-registrar-client";

  /** OpenID Connect scope. */
  public static final String SCOPE_OPENID = "openid";

  /** OIDC profile scope. */
  public static final String SCOPE_PROFILE = "profile";

  /** DHIS2-specific scope that maps the token to a DHIS2 username claim. */
  public static final String SCOPE_USERNAME = "username";

  /** OIDC email scope. */
  public static final String SCOPE_EMAIL = "email";

  /**
   * Reserved scope that authorizes minting new clients through the DCR endpoint {@code
   * /connect/register}. Only the system DCR registrar's Initial Access Tokens may carry it.
   */
  public static final String SCOPE_CLIENT_CREATE = "client.create";

  /** Prefix shared by all reserved client-management scopes. */
  public static final String RESERVED_SCOPE_PREFIX = "client.";

  /** Scopes admin/metadata/DCR-assigned clients may carry. */
  public static final Set<String> ALLOWED_CLIENT_SCOPES =
      Set.of(SCOPE_OPENID, SCOPE_PROFILE, SCOPE_USERNAME, SCOPE_EMAIL);

  /** Reserved for the system DCR registrar / IAT path only. */
  public static final Set<String> RESERVED_SCOPES = Set.of(SCOPE_CLIENT_CREATE);

  /**
   * Server-side default scopes assigned to DCR-registered clients when the registration omits
   * scopes. Spring Authorization Server 7 forbids client-supplied {@code scope} on DCR requests, so
   * these are the only scopes a DCR client gets. Matches the Android reference client's authorize
   * scopes; deliberately excludes {@code email} (PR-H decision D3).
   */
  public static final List<String> DCR_DEFAULT_SCOPES =
      List.of(SCOPE_OPENID, SCOPE_PROFILE, SCOPE_USERNAME);

  /**
   * Returns true when the scope is reserved for server-managed clients: an exact member of {@link
   * #RESERVED_SCOPES} or any scope under the {@code client.} prefix.
   */
  public static boolean isReservedScope(String scope) {
    return RESERVED_SCOPES.contains(scope) || scope.startsWith(RESERVED_SCOPE_PREFIX);
  }

  /** Returns true when the scope may be assigned to admin/metadata/DCR-created clients. */
  public static boolean isAllowedClientScope(String scope) {
    return ALLOWED_CLIENT_SCOPES.contains(scope);
  }

  /**
   * Max length of the persisted {@code oauth2_client.name} column; kept in sync with the HBM
   * mapping.
   */
  public static final int CLIENT_NAME_MAX_LENGTH = 230;
}
