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
package org.hisp.dhis.security.oidc.provider;

/**
 * Abstract base for all built-in DHIS2 OIDC provider builders. Shared constants and configuration
 * keys declared here are consumed by the concrete built-in providers, {@link GoogleProvider},
 * {@link AzureAdProvider}, {@link Wso2Provider} and {@link Dhis2InternalOidcProvider}. Providers
 * declared with an arbitrary, non-reserved id (for example {@code oidc.provider.keycloak.*}) are
 * handled by {@link GenericOidcProviderBuilder} instead, which also extends this class to reuse the
 * same config-key vocabulary.
 *
 * <p>Every configured OIDC provider whose registration is visible on the login page renders as a
 * sign-in button on the DHIS2 web login page when {@code oidc.oauth2.login.enabled=on}. The
 * internal DHIS2-as-IdP provider ({@link Dhis2InternalOidcProvider}) is registered when the
 * embedded authorization server is enabled and is deliberately not shown on the login page.
 *
 * <p>The public string constants below fall into two groups: defaults used when a given
 * configuration value is absent, and the canonical property-key suffixes used to look up values
 * inside {@code oidc.provider.<id>.*} blocks. The keystore related constants carry the values used
 * by providers that authenticate with {@code private_key_jwt}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public abstract class AbstractOidcProvider {
  /**
   * Default Spring Security redirect URI template applied to every provider when no explicit {@code
   * redirect_url} is configured. The {@code baseUrl} and {@code registrationId} placeholders are
   * resolved by Spring Security at runtime.
   */
  public static final String DEFAULT_REDIRECT_TEMPLATE_URL =
      "{baseUrl}/oauth2/code/{registrationId}";

  /**
   * Default ID-token / user-info claim used to look up the local DHIS2 user when no explicit {@code
   * mapping_claim} is configured. The internal DHIS2-as-IdP provider overrides this with {@code
   * username}.
   */
  public static final String DEFAULT_MAPPING_CLAIM = "email";

  /** Default OAuth2 / OIDC scope requested by every built-in provider. */
  public static final String DEFAULT_SCOPE = "openid";

  public static final String PROVIDER_ID = "provider_id";

  public static final String CLIENT_ID = "client_id";

  public static final String CLIENT_SECRET = "client_secret";

  public static final String MAPPING_CLAIM = "mapping_claim";

  public static final String REDIRECT_URL = "redirect_url";

  public static final String AUTHORIZATION_URI = "authorization_uri";

  public static final String TOKEN_URI = "token_uri";

  public static final String USERINFO_URI = "user_info_uri";

  public static final String JWK_URI = "jwk_uri";

  public static final String ISSUER_URI = "issuer_uri";

  public static final String END_SESSION_ENDPOINT = "end_session_endpoint";

  public static final String DISPLAY_ALIAS = "display_alias";

  public static final String ENABLE_LOGOUT = "enable_logout";

  public static final String SCOPES = "scopes";

  public static final String LOGIN_IMAGE = "login_image";

  public static final String LOGIN_IMAGE_PADDING = "login_image_padding";

  public static final String ENABLE_PKCE = "enable_pkce";

  public static final String EXTRA_REQUEST_PARAMETERS = "extra_request_parameters";

  public static final String EXTERNAL_CLIENT_PREFIX = "ext_client";

  public static final String JWT_PRIVATE_KEY_KEYSTORE_PATH = "keystore_path";

  public static final String JWT_PRIVATE_KEY_KEYSTORE_PASSWORD = "keystore_password";

  public static final String JWT_PRIVATE_KEY_ALIAS = "key_alias";

  public static final String JWT_PRIVATE_KEY_PASSWORD = "key_password";

  public static final String AUTHORIZATION_GRANT_TYPE = "authorization_grant_type";

  public static final String CLIENT_AUTHENTICATION_METHOD = "client_authentication_method";

  public static final String JWK_SET_URL = "jwk_set_url";

  /**
   * Selects userinfo response handling: {@code json} (default; Spring Security's normal path) or
   * {@code jwt} (eSignet-style signed JWT). See {@link
   * org.hisp.dhis.security.oidc.UserInfoResponseType}.
   */
  public static final String USER_INFO_RESPONSE_TYPE = "user_info_response_type";

  /**
   * JWS algorithm used to verify the userinfo JWT when {@link #USER_INFO_RESPONSE_TYPE} is {@code
   * jwt}. Defaults to {@code RS256}. See {@link
   * org.hisp.dhis.security.oidc.SupportedJwsAlgorithms}.
   */
  public static final String USER_INFO_JWS_ALGORITHM = "user_info_jws_algorithm";
}
