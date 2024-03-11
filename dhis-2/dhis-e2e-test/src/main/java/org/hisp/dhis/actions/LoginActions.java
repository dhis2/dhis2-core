/*
 * Copyright (c) 2004-2018, University of Oslo
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

package org.hisp.dhis.actions;

import io.restassured.RestAssured;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.ConfigurationHelper;

import static io.restassured.RestAssured.preemptive;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class LoginActions
{
    public void loginAsUser( final String username, final String password )
    {
        ApiResponse loggedInUser =  getLoggedInUserInfo();

        if ( loggedInUser.getContentType().contains( "json" ) && loggedInUser.extract( "userCredentials.username" ) != null && loggedInUser.extract( "userCredentials.username" ).equals( username ) ) {
            return;
        }

        RestAssured.authentication = preemptive().basic( username, password );

        getLoggedInUserInfo().validate().statusCode( 200 );
    }

    /**
     * Logs in with superuser configured in test run time.
     * If properties are not set default user will be used.
     */
    public void loginAsSuperUser()
    {
        loginAsUser( ConfigurationHelper.SUPER_USER_USERNAME, ConfigurationHelper.SUPER_USER_PASS );
    }

    /**
     * Logs in with default user created by dhis2 setup.
     * Username: admin
     */
    public void loginAsDefaultUser()
    {
        loginAsUser( "admin", "district" );
    }

    public ApiResponse getLoggedInUserInfo()
    {
        ApiResponse response = new RestApiActions( "/me" ).get();

        return response;
    }
}
