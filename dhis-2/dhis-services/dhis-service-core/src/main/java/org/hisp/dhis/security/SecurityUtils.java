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
package org.hisp.dhis.security;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.user.User;
import org.jboss.aerogear.security.otp.Totp;
import org.springframework.util.Assert;

/**
 * @author Henning Håkonsen
 */
public class SecurityUtils
{
    private static final String APP_NAME_PREFIX = "DHIS 2 ";

    private static final String QR_PREFIX = "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=";

    /**
     * Generates a QR URL using Google chart API.
     *
     * @param appName the name of the DHIS 2 instance.
     * @param user the user to generate the URL for.
     * @return a QR URL.
     */
    public static String generateQrUrl( String appName, User user )
    {
        Assert.notNull( user.getSecret(), "User must have a secret" );

        String app = (APP_NAME_PREFIX + StringUtils.stripToEmpty( appName )).replace( " ", "%20" );
        String url = String.format( "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            app, user.getUsername(), user.getSecret(), app );

        try
        {
            return QR_PREFIX + URLEncoder.encode( url, StandardCharsets.UTF_8.name() );
        }
        catch ( UnsupportedEncodingException ex )
        {
            throw new RuntimeException( "Failed to encode QR URL", ex );
        }
    }

    /**
     * Verifies that the secret for the given user matches the given code.
     *
     * @param user the users.
     * @param code the code.
     * @return true if the user secret matches the given code, false if not.
     */
    public static boolean verify( User user, String code )
    {
        Assert.notNull( user.getSecret(), "User must have a secret" );

        Totp totp = new Totp( user.getSecret() );
        try
        {
            return totp.verify( code );
        }
        catch ( NumberFormatException ex )
        {
            return false;
        }
    }
}
