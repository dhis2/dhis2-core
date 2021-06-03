package org.hisp.dhis.helpers.extensions;

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

import io.restassured.authentication.BasicAuthScheme;
import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class AuthFilter
    implements Filter
{
    HashMap<String, Cookie> auth = new HashMap<>();

    @Override
    public Response filter( FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
        FilterContext ctx )
    {
        String username = "";
        String password = "";
        requestSpec.removeCookies();

        if ( requestSpec.getAuthenticationScheme() instanceof BasicAuthScheme ||
            requestSpec.getAuthenticationScheme() instanceof PreemptiveBasicAuthScheme )
        {
            if ( requestSpec.getAuthenticationScheme() instanceof BasicAuthScheme )
            {
                username = ((BasicAuthScheme) requestSpec.getAuthenticationScheme()).getUserName();
                password = ((BasicAuthScheme) requestSpec.getAuthenticationScheme()).getPassword();
            }
            else
            {
                username = ((PreemptiveBasicAuthScheme) requestSpec.getAuthenticationScheme()).getUserName();
                password = ((PreemptiveBasicAuthScheme) requestSpec.getAuthenticationScheme()).getPassword();
            }

            if ( getMap( username ) != null )
            {
                String finalUsername = username;

                Cookie cookie = auth.entrySet().stream().filter( p -> p.getKey().equalsIgnoreCase( finalUsername ) )
                    .findFirst().orElse( null ).getValue();

                requestSpec.cookie( cookie.getType(), cookie.getValue() );

            }

            else
            {
                requestSpec.auth().preemptive().basic( username, password );
            }
        }

        final Response response = ctx.next( requestSpec, responseSpec );

        if ( getMap( username ) == null && getSessionCookie( response ) != null )
        {
            auth.put( username,
                getSessionCookie( response ) );
        }
        return response;
    }

    private Cookie getSessionCookie( Response response )
    {
        if ( response.getCookie( "JSESSIONID" ) != null )
        {
            return new Cookie( "JSESSIONID", response.getCookie( "JSESSIONID" ) );
        }
        else if ( response.getCookie( "SESSION" ) != null )
        {
            return new Cookie( "SESSION", response.getCookie( "SESSION" ) );
        }

        return null;
    }

    private Map.Entry<String, Cookie> getMap( String username )
    {
        return auth.entrySet().stream().filter( p -> p.getKey().equalsIgnoreCase( username ) ).findFirst().orElse(
            null
        );
    }

    public class Cookie
    {
        private String type;

        private String value;

        public Cookie( String type, String value )
        {
            this.type = type;
            this.value = value;
        }

        public String getType()
        {
            return type;
        }

        public void setType( String type )
        {
            this.type = type;
        }

        public String getValue()
        {
            return value;
        }

        public void setValue( String value )
        {
            this.value = value;
        }
    }
}
