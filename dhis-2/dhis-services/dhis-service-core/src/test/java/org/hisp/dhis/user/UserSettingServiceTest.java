package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.setting.SettingKey;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * @author Kiran Prakash
 */
public class UserSettingServiceTest
    extends DhisSpringTest
{
    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private UserService userService;

    private User userA;

    @Override
    protected void setUpTest()
    {
        userSettingService.invalidateCache();
        
        userA = createUser( 'A' );
        userService.addUser( userA );
        UserCredentials userCredentialsA = userA.getUserCredentials();
        userCredentialsA.setUsername( "usernameA" );
        userCredentialsA.setUserInfo( userA );
        userService.addUserCredentials( userCredentialsA );
    }

    @Test
    public void testSaveGetDeleteUserSetting()
    {
        assertEquals( SettingKey.ANALYSIS_DISPLAY_PROPERTY.getDefaultValue(), userSettingService.getUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA ) );
        assertEquals( SettingKey.STYLE.getDefaultValue(), userSettingService.getUserSetting( UserSettingKey.STYLE, userA ) );
        
        userSettingService.saveUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, "shortName", "usernameA" );
        userSettingService.saveUserSetting( UserSettingKey.STYLE, "blue", "usernameA" );

        assertEquals( "shortName", userSettingService.getUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA ) );
        assertEquals( "blue", userSettingService.getUserSetting( UserSettingKey.STYLE, userA ) );
        
        userSettingService.deleteUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA );

        assertEquals( SettingKey.ANALYSIS_DISPLAY_PROPERTY.getDefaultValue(), userSettingService.getUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA ) );
        assertEquals( "blue", userSettingService.getUserSetting( UserSettingKey.STYLE, userA ) );

        userSettingService.deleteUserSetting( UserSettingKey.STYLE, userA );

        assertEquals( SettingKey.ANALYSIS_DISPLAY_PROPERTY.getDefaultValue(), userSettingService.getUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA ) );
        assertEquals( SettingKey.STYLE.getDefaultValue(), userSettingService.getUserSetting( UserSettingKey.STYLE, userA ) );
    }

    @Test
    public void testSaveOrUpdateUserSetting()
    {
        userSettingService.saveUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, "name", "usernameA" );
        userSettingService.saveUserSetting( UserSettingKey.STYLE, "blue", "usernameA" );

        assertEquals( "name", userSettingService.getUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA ) );
        assertEquals( "blue", userSettingService.getUserSetting( UserSettingKey.STYLE, userA ) );

        userSettingService.saveUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, "shortName", "usernameA" );
        userSettingService.saveUserSetting( UserSettingKey.STYLE, "green", "usernameA" );

        assertEquals( "shortName", userSettingService.getUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, userA ) );
        assertEquals( "green", userSettingService.getUserSetting( UserSettingKey.STYLE, userA ) );
    }

    @Test
    public void testGetUserSettingsByUser()
    {
        userSettingService.saveUserSetting( UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, "name", "usernameA" );
        userSettingService.saveUserSetting( UserSettingKey.STYLE, "blue", "usernameA" );
        
        assertEquals( 2, userSettingService.getUserSettings( userA ).size() );
    }
}
