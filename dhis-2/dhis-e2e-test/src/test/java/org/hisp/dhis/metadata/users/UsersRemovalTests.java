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
package org.hisp.dhis.metadata.users;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.OptionActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class UsersRemovalTests
    extends ApiTest
{
    private UserActions userActions;

    private OptionActions optionActions;

    private LoginActions loginActions;

    private String userId;

    private String userName;

    private String password = "!XPTOqwerty1";

    @BeforeEach
    public void beforeEach()
    {
        userActions = new UserActions();
        optionActions = new OptionActions();
        loginActions = new LoginActions();

        userName = (DataGenerator.randomString()).toLowerCase();

        loginActions.loginAsSuperUser();

        userId = userActions.addUserFull( "johnny", "bravo", userName, password );
    }

    @Test
    public void shouldRemoveWhenUserWasLoggedIn()
    {
        loginActions.loginAsUser( userName, password );
        loginActions.loginAsSuperUser();

        ApiResponse response = userActions.delete( userId );

        ResponseValidationHelper.validateObjectRemoval( response, "User was not removed" );
    }

    @Test
    @Disabled
    // jira issue 5573
    public void shouldRemoveWhenUserWasGrantedAccessToMetadata()
    {
        // arrange
        String id = optionActions.createOptionSet();

        optionActions.grantUserAccessToOptionSet( id, userId );

        // act
        ApiResponse response = userActions.delete( userId );

        // assert
        ResponseValidationHelper.validateObjectRemoval( response, "User was not removed" );
    }
}
