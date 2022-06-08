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
package org.hisp.dhis.dataexchange.client;

import java.net.URI;
import java.util.Base64;

import lombok.Getter;
import lombok.NonNull;

import org.apache.commons.lang3.Validate;
import org.springframework.web.util.UriComponentsBuilder;

@Getter
public class Dhis2Config
{
    /**
     * Base URL to the target DHIS 2 instance, excluding the <code>/api</code>
     * path.
     */
    @NonNull
    private final String url;

    /**
     * Valid access token for the target DHIS 2 instance.
     */
    private final String accessToken;

    /**
     * Username for the target DHIS 2 instance.
     */
    private final String username;

    /**
     * Password for the target DHIS 2 instance.
     */
    private final String password;

    /**
     * Value to use for authentication for the Authorization HTTP header.
     */
    @NonNull
    private final String authHeaderValue;

    /**
     * Constructor for access token.
     *
     * @param url the base URL of the DHIS 2 instance.
     * @param accessToken the access token.
     */
    public Dhis2Config( String url, String accessToken )
    {
        this.url = url;
        this.accessToken = accessToken;
        this.username = null;
        this.password = null;
        this.authHeaderValue = getAccessTokenHeaderValue();
        Validate.notBlank( accessToken );
    }

    /**
     * Constructor for username and password.
     *
     * @param url the base URL of the DHIS 2 instance.
     * @param username the username of the DHIS 2 account.
     * @param password the password of the DHIS 2 account.
     */
    public Dhis2Config( String url, String username, String password )
    {
        this.url = url;
        this.accessToken = null;
        this.username = username;
        this.password = password;
        this.authHeaderValue = getBasicAuthHeaderValue();
        Validate.notBlank( username );
        Validate.notBlank( password );
    }

    public URI getResolvedUri( String path )
    {
        return UriComponentsBuilder.fromHttpUrl( url )
            .pathSegment( "api" )
            .path( path )
            .build()
            .toUri();
    }

    private String getAccessTokenHeaderValue()
    {
        return String.format( "ApiToken %s", accessToken );
    }

    private String getBasicAuthHeaderValue()
    {
        return "Basic " + Base64.getEncoder().encodeToString( (username + ":" + password).getBytes() );
    }
}
