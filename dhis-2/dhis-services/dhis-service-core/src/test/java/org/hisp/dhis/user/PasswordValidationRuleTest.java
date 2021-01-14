package org.hisp.dhis.user;

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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * @author Zubair Asghar.
 */
public class PasswordValidationRuleTest
{
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 40;

    private static final String USERNAME = "alex";
    private static final String EMAIL = "alex@open.org";

    private static final String PASSWORD_WITHOUT_SPECIAL_CHAR = "Xman123";
    private static final String STRONG_PASSWORD = "XmanClassic-123";
    private static final String WEAK_PASSWORD = "abc";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<UserCredentials> userCredentialsArgumentCaptor;

    private SpecialCharacterValidationRule specialCharValidationRule;
    private DigitPatternValidationRule digitValidationRule;
    private PasswordLengthValidationRule lengthValidationRule;
    private UpperCasePatternValidationRule upperCasePatternValidationRule;
    private UserParameterValidationRule parameterValidationRule;
    private PasswordDictionaryValidationRule dictionaryValidationRule;
    private PasswordHistoryValidationRule historyValidationRule;

    @Before
    public void init()
    {
        specialCharValidationRule = new SpecialCharacterValidationRule();
        digitValidationRule = new DigitPatternValidationRule();
        dictionaryValidationRule = new PasswordDictionaryValidationRule();
        lengthValidationRule = new PasswordLengthValidationRule( systemSettingManager );
        upperCasePatternValidationRule = new UpperCasePatternValidationRule();
        parameterValidationRule = new UserParameterValidationRule();
        historyValidationRule = new PasswordHistoryValidationRule( passwordEncoder, userService, currentUserService );
    }

    @Test
    public void testWhenPasswordIsNullOrEmpty()
    {
        CredentialsInfo credentialsInfoNoPassword = new CredentialsInfo( USERNAME, "", EMAIL, true );

        assertThat( specialCharValidationRule.validate( credentialsInfoNoPassword ).isValid(), is( false ) );
        assertThat( digitValidationRule.validate( credentialsInfoNoPassword ).isValid(), is( false ) );
        assertThat( dictionaryValidationRule.validate( credentialsInfoNoPassword ).isValid(), is( false ) );
        assertThat( lengthValidationRule.validate( credentialsInfoNoPassword ).isValid(), is( false ) );
        assertThat( upperCasePatternValidationRule.validate( credentialsInfoNoPassword ).isValid(), is( false ) );
        assertThat( parameterValidationRule.validate( credentialsInfoNoPassword ).isValid(), is( false ) );
        assertThat( historyValidationRule.validate( credentialsInfoNoPassword ).isValid(), is( false ) );

        assertThat( parameterValidationRule.validate( new CredentialsInfo( USERNAME, STRONG_PASSWORD, "", true ) ).isValid(), is( true ) );
        assertThat( parameterValidationRule.validate( new CredentialsInfo( USERNAME, "", "", true ) ).isValid(), is( false ) );
        assertThat( parameterValidationRule.validate( new CredentialsInfo( "", STRONG_PASSWORD, "", false ) ).isValid(), is( false ) );
    }

    @Test
    public void testSpecialCharValidationRule()
    {
        CredentialsInfo credentialsInfo = new CredentialsInfo( USERNAME, STRONG_PASSWORD, EMAIL, true );

        assertThat( specialCharValidationRule.validate( credentialsInfo ).isValid(), is( true ) );

        credentialsInfo = new CredentialsInfo( USERNAME, PASSWORD_WITHOUT_SPECIAL_CHAR, EMAIL, true );

        assertThat( specialCharValidationRule.validate( credentialsInfo ).isValid(), is( false ) );
        assertThat( specialCharValidationRule.validate( credentialsInfo ).getErrorMessage(), is( SpecialCharacterValidationRule.ERROR ) );
    }

    @Test
    public void testDigitValidationRule()
    {
        CredentialsInfo credentialsInfo = new CredentialsInfo( USERNAME, STRONG_PASSWORD, EMAIL, true );

        assertThat( digitValidationRule.validate( credentialsInfo ).isValid(), is( true ) );

        credentialsInfo = new CredentialsInfo( USERNAME, WEAK_PASSWORD, EMAIL, true );

        assertThat( digitValidationRule.validate( credentialsInfo ).isValid(), is( false ) );
        assertThat( digitValidationRule.validate( credentialsInfo ).getErrorMessage(), is( DigitPatternValidationRule.ERROR ) );
    }

    @Test
    public void testDictionaryValidationRule()
    {
        CredentialsInfo credentialsInfo = new CredentialsInfo( USERNAME, STRONG_PASSWORD, EMAIL, true );

        assertThat( dictionaryValidationRule.validate( credentialsInfo ).isValid(), is( true ) );

        credentialsInfo = new CredentialsInfo( USERNAME, WEAK_PASSWORD + "admin", EMAIL, true );

        assertThat( dictionaryValidationRule.validate( credentialsInfo ).isValid(), is( false ) );
        assertThat( dictionaryValidationRule.validate( credentialsInfo ).getErrorMessage(), is( PasswordDictionaryValidationRule.ERROR ) );
    }

    @Test
    public void testLengthValidationRule()
    {
        Mockito.when( systemSettingManager.getSystemSetting( SettingKey.MIN_PASSWORD_LENGTH ) ).thenReturn( MIN_LENGTH );
        Mockito.when( systemSettingManager.getSystemSetting( SettingKey.MAX_PASSWORD_LENGTH ) ).thenReturn( MAX_LENGTH );

        CredentialsInfo credentialsInfo = new CredentialsInfo( USERNAME, STRONG_PASSWORD, EMAIL, true );

        assertThat( lengthValidationRule.validate( credentialsInfo ).isValid(), is( true ) );

        credentialsInfo = new CredentialsInfo( USERNAME, WEAK_PASSWORD, EMAIL, true );

        assertThat( lengthValidationRule.validate( credentialsInfo ).isValid(), is( false ) );
        assertThat( lengthValidationRule.validate( credentialsInfo ).getErrorMessage(),
            is( String.format( PasswordLengthValidationRule.ERROR, MIN_LENGTH, MAX_LENGTH ) ) );
    }

    @Test
    public void testUpperCaseValidationRule()
    {
        CredentialsInfo credentialsInfo = new CredentialsInfo( USERNAME, STRONG_PASSWORD, EMAIL, true );

        assertThat( upperCasePatternValidationRule.validate( credentialsInfo ).isValid(), is( true ) );

        credentialsInfo = new CredentialsInfo( USERNAME, WEAK_PASSWORD, EMAIL, true );

        assertThat( upperCasePatternValidationRule.validate( credentialsInfo ).isValid(), is( false ) );
        assertThat( upperCasePatternValidationRule.validate( credentialsInfo ).getErrorMessage(), is( UpperCasePatternValidationRule.ERROR ) );
    }

    @Test
    public void testUserParameterValidationRule()
    {
        CredentialsInfo credentialsInfo = new CredentialsInfo( USERNAME, STRONG_PASSWORD, EMAIL, true );

        assertThat( parameterValidationRule.validate( credentialsInfo ).isValid(), is( true ) );

        credentialsInfo = new CredentialsInfo( USERNAME, WEAK_PASSWORD + EMAIL, EMAIL, true );

        assertThat( parameterValidationRule.validate( credentialsInfo ).isValid(), is( false ) );
        assertThat( parameterValidationRule.validate( credentialsInfo ).getErrorMessage(), is( UserParameterValidationRule.ERROR ) );
    }

    @Test
    public void testPasswordHistoryValidationRule()
    {
        List<String> history = ListUtils.newList( STRONG_PASSWORD, STRONG_PASSWORD + "1", STRONG_PASSWORD + "2", STRONG_PASSWORD + "2"
            , STRONG_PASSWORD + "4", STRONG_PASSWORD + "5", STRONG_PASSWORD + "6", STRONG_PASSWORD + "7", STRONG_PASSWORD + "8", STRONG_PASSWORD + "9"
            , STRONG_PASSWORD + "10", STRONG_PASSWORD + "11", STRONG_PASSWORD + "12", STRONG_PASSWORD + "13", STRONG_PASSWORD + "14", STRONG_PASSWORD + "15"
            , STRONG_PASSWORD + "16", STRONG_PASSWORD + "17", STRONG_PASSWORD + "18", STRONG_PASSWORD + "19", STRONG_PASSWORD + "20", STRONG_PASSWORD + "21"
            , STRONG_PASSWORD + "22" );

        CredentialsInfo credentialsInfo = new CredentialsInfo( USERNAME, STRONG_PASSWORD + "23", EMAIL, true );
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setPreviousPasswords( history );

        Mockito.when( userService.getUserCredentialsByUsername( credentialsInfo.getUsername() ) ).thenReturn( userCredentials );
        Mockito.when( passwordEncoder.matches( Mockito.any( String.class ), Mockito.any( String.class ) ) ).thenReturn( false );

        assertThat( historyValidationRule.validate( credentialsInfo ).isValid(), is( true ) );

        history.add( STRONG_PASSWORD + "23" );

        credentialsInfo = new CredentialsInfo( USERNAME, STRONG_PASSWORD + "23", EMAIL, true );
        userCredentials = new UserCredentials();
        userCredentials.setPreviousPasswords( history );

        Mockito.when( userService.getUserCredentialsByUsername( credentialsInfo.getUsername() ) ).thenReturn( userCredentials );
        Mockito.when( passwordEncoder.matches( Mockito.any( String.class ), Mockito.any( String.class ) ) ).thenReturn( false );
        Mockito.doAnswer( invocation -> null ).when( userService ).updateUserCredentials( Mockito.any( UserCredentials.class ) );

        assertThat( historyValidationRule.validate( credentialsInfo ).isValid(), is( true ) );

        Mockito.verify( userService ).updateUserCredentials( userCredentialsArgumentCaptor.capture() );

        Assert.assertNotNull( userCredentialsArgumentCaptor.getValue() );
        Assert.assertEquals( 23, userCredentialsArgumentCaptor.getValue().getPreviousPasswords().size() );
        Assert.assertFalse( userCredentialsArgumentCaptor.getValue().getPreviousPasswords().contains( STRONG_PASSWORD ) );
    }
}
