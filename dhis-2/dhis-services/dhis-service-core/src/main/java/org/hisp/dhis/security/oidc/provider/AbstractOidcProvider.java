/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.security.oidc.provider;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public abstract class AbstractOidcProvider
{
    protected static final String DEFAULT_REDIRECT_TEMPLATE_URL = "{baseUrl}/oauth2/code/{registrationId}";

    public static final String DEFAULT_MAPPING_CLAIM = "email";

    public static final String DEFAULT_SCOPE = "openid";

    public final static String PROVIDER_ID = "provider_id";

    public final static String CLIENT_ID = "client_id";

    public final static String CLIENT_SECRET = "client_secret";

    public final static String MAPPING_CLAIM = "mapping_claim";

    public final static String REDIRECT_URL = "redirect_url";

    public final static String AUTHORIZATION_URI = "authorization_uri";

    public final static String TOKEN_URI = "token_uri";

    public final static String USERINFO_URI = "user_info_uri";

    public final static String JWK_URI = "jwk_uri";

    public final static String END_SESSION_ENDPOINT = "end_session_endpoint";

    public final static String DISPLAY_ALIAS = "display_alias";

    public final static String ENABLE_LOGOUT = "enable_logout";

    public final static String SCOPES = "scopes";

    public final static String LOGO_IMAGE = "logo_image";

    public final static String LOGO_IMAGE_PADDING = "logo_image_padding";

    public final static String ENABLE_PKCE = "enable_pkce";

    public final static String EXTRA_REQUEST_PARAMETERS = "extra_request_parameters";
}