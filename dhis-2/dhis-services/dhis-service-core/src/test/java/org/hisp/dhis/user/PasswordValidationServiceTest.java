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
package org.hisp.dhis.user;

import static java.util.Arrays.asList;
import static org.hisp.dhis.user.PasswordValidationError.PASSWORD_ALREADY_USED_BEFORE;
import static org.hisp.dhis.user.PasswordValidationError.PASSWORD_CONTAINS_NAME_OR_EMAIL;
import static org.hisp.dhis.user.PasswordValidationError.PASSWORD_CONTAINS_RESERVED_WORD;
import static org.hisp.dhis.user.PasswordValidationError.PASSWORD_IS_MANDATORY;
import static org.hisp.dhis.user.PasswordValidationError.PASSWORD_MUST_HAVE_DIGIT;
import static org.hisp.dhis.user.PasswordValidationError.PASSWORD_MUST_HAVE_SPECIAL;
import static org.hisp.dhis.user.PasswordValidationError.PASSWORD_MUST_HAVE_UPPER;
import static org.hisp.dhis.user.PasswordValidationError.PASSWORD_TOO_LONG_TOO_SHORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Tests the {@link PasswordValidationService}
 *
 * @author Jan Bernitt
 */
class PasswordValidationServiceTest
{

    private PasswordValidationService validation;

    private PasswordEncoder encoder;

    @BeforeEach
    void setUp()
    {
        encoder = mock( PasswordEncoder.class );
        UserService userService = mock( UserService.class );
        User user = new User();
        user.setUsername( "Luke" );
        user.setPreviousPasswords( asList( "$kyWalker1", "$kyWalker2", "$kyWalker3" ) );

        when( userService.getUserByUsername( anyString() ) ).thenReturn( user );

        CurrentUserService currentUserService = mock( CurrentUserService.class );
        SystemSettingManager systemSettings = mock( SystemSettingManager.class );
        when( systemSettings.getIntSetting( SettingKey.MIN_PASSWORD_LENGTH ) ).thenReturn( 8 );
        when( systemSettings.getIntSetting( SettingKey.MAX_PASSWORD_LENGTH ) ).thenReturn( 16 );
        validation = new DefaultPasswordValidationService( encoder, userService, currentUserService, systemSettings );
    }

    @Test
    void tooShortPasswords()
    {
        assertInvalid( "Luke", "Sev€n77", PASSWORD_TOO_LONG_TOO_SHORT, 8, 16 );
        assertInvalid( "Lucky", "Sky", PASSWORD_TOO_LONG_TOO_SHORT, 8, 16 );
    }

    @Test
    void tooLongPasswords()
    {
        assertInvalid( "Luke", "17teen17teen17teen", PASSWORD_TOO_LONG_TOO_SHORT, 8, 16 );
        assertInvalid( "Lucky", "JediKnightSkywalker", PASSWORD_TOO_LONG_TOO_SHORT, 8, 16 );
    }

    @Test
    void passwordIsMandatory()
    {
        assertInvalid( "Luke", "", PASSWORD_IS_MANDATORY );
        assertInvalid( "Lucky", null, PASSWORD_IS_MANDATORY );
    }

    @Test
    void usernameIsMandatory()
    {
        assertInvalid( "", "$kyWalker7", PASSWORD_IS_MANDATORY );
        assertInvalid( null, "$kyWalker7", PASSWORD_IS_MANDATORY );
    }

    @Test
    void passwordMustHaveDigits()
    {
        assertInvalid( "Luke", "$kyWalker", PASSWORD_MUST_HAVE_DIGIT );
        assertInvalid( "Lucky", "$kyWalker", PASSWORD_MUST_HAVE_DIGIT );
    }

    @Test
    void passwordMustHaveUpperCaseLetters()
    {
        assertInvalid( "Luke", "$kywalker7", PASSWORD_MUST_HAVE_UPPER );
        assertInvalid( "Lucky", "$kywalker7", PASSWORD_MUST_HAVE_UPPER );
    }

    @Test
    void passwordMustHaveSpecialCharacters()
    {
        assertInvalid( "Luke", "SkyWalker7", PASSWORD_MUST_HAVE_SPECIAL );
        assertInvalid( "Lucky", "SkyWalker7", PASSWORD_MUST_HAVE_SPECIAL );
    }

    @Test
    void passwordMustNotContainReservedWords()
    {
        for ( String reserved : new String[] { "user", "admin", "system", "username", "password", "login", "manager" } )
        {
            assertInvalid( "Luke", "$kY7" + reserved, PASSWORD_CONTAINS_RESERVED_WORD );
            assertInvalid( "Lucky", reserved + "$kY7", PASSWORD_CONTAINS_RESERVED_WORD );
        }
    }

    @Test
    void passwordMustNotContainUsername()
    {
        assertInvalid( "Luke", "$kyLuke7", PASSWORD_CONTAINS_NAME_OR_EMAIL );
        assertInvalid( "Lucky", "LuckyW@lker7", PASSWORD_CONTAINS_NAME_OR_EMAIL );
    }

    @Test
    void passwordMustNotContainEmail()
    {
        assertInvalid( new CredentialsInfo( "Luke", "Sky@walker.nu7", "Sky@walker.nu", true ),
            PASSWORD_CONTAINS_NAME_OR_EMAIL );
        assertInvalid( new CredentialsInfo( "Luke", "Sky@walker.nu7", "Sky@walker.nu", false ),
            PASSWORD_CONTAINS_NAME_OR_EMAIL );
    }

    @Test
    void passwordMustNotHaveBeenUsedBefore()
    {
        when( encoder.matches( any(), any() ) ).thenReturn( true );
        assertInvalid( "Luke", "$kyWalker1", PASSWORD_ALREADY_USED_BEFORE, 24 );
        assertInvalid( "Lucky", "$kyWalker2", PASSWORD_ALREADY_USED_BEFORE, 24 );
    }

    @Test
    void validPasswords()
    {
        assertValid( "Luke", "$kyWalker42" );
        assertValid( "Lucky", "m@yThe4thBeWithU" );
    }

    private void assertInvalid( String username, String password, PasswordValidationError expected, Object... args )
    {
        assertInvalid( username, password, username + "@force.net", expected, args );
    }

    private void assertInvalid( String username, String password, String email, PasswordValidationError expected,
        Object... args )
    {
        if ( username != null && !username.isEmpty() )
        {
            assertInvalid( new CredentialsInfo( username, password, email, true ), expected, args );
            assertInvalid( new CredentialsInfo( username, password, null, true ), expected, args );
        }
        assertInvalid( new CredentialsInfo( username, password, email, false ), expected, args );
        assertInvalid( new CredentialsInfo( username, password, null, false ), expected, args );
    }

    private void assertInvalid( CredentialsInfo credentials, PasswordValidationError expected, Object... args )
    {
        PasswordValidationResult actual = validation.validate( credentials );
        assertFalse( actual.isValid() );
        assertEquals( expected.getI18nKey(), actual.getI18ErrorMessage(), actual.getErrorMessage() );
        assertEquals( String.format( expected.getMessage(), args ), actual.getErrorMessage() );
    }

    private void assertValid( String username, String password )
    {
        assertValid( new CredentialsInfo( username, password, username + "@force.net", true ) );
        assertValid( new CredentialsInfo( username, password, username + "@force.net", false ) );
        assertValid( new CredentialsInfo( username, password, null, true ) );
        assertValid( new CredentialsInfo( username, password, null, false ) );
    }

    private void assertValid( CredentialsInfo credentials )
    {
        PasswordValidationResult actual = validation.validate( credentials );
        assertTrue( actual.isValid() );
        assertNull( actual.getErrorMessage() );
        assertNull( actual.getI18ErrorMessage() );
    }
}
