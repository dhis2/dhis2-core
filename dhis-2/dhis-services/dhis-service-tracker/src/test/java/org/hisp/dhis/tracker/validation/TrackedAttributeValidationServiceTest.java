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
package org.hisp.dhis.tracker.validation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.trackedentity.*;
import org.hisp.dhis.tracker.util.Constant;
import org.hisp.dhis.tracker.validation.service.attribute.TrackedAttributeValidationService;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.joda.time.IllegalFieldValueException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TrackedAttributeValidationServiceTest
{

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private TrackedAttributeValidationService trackedEntityAttributeService;

    @Mock
    private UserService userService;

    @Mock
    private FileResourceService fileResourceService;

    @Mock
    private FileResource fileResource;

    private TrackedEntityAttribute tea;

    @Before
    public void setUp()
    {

        trackedEntityAttributeService = new TrackedAttributeValidationService( userService, fileResourceService );

        tea = new TrackedEntityAttribute();
        tea.setUid( "TeaUid12345" );
        tea.setUnique( true );
        tea.setValueType( ValueType.TEXT );
        tea.setOrgunitScope( false );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldThrowWhenTeaIsNull()
    {
        trackedEntityAttributeService.validateValueType( null, "" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldThrowWhenNotAValidDate()
    {
        tea.setValueType( ValueType.DATE );
        String teaValue = "Firstname";
        trackedEntityAttributeService.validateValueType( tea, teaValue );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldThrowWhenNullValue()
    {
        trackedEntityAttributeService.validateValueType( tea, null );
    }

    @Test
    public void shouldFailValueOverMaxLength()
    {
        StringBuilder stringBuilder = new StringBuilder();

        for ( int i = 0; i < Constant.ATTRIBUTE_VALUE_MAX_LENGTH + 1; i++ )
        {
            stringBuilder.append( "a" );
        }

        assertNotNull( trackedEntityAttributeService.validateValueType( tea, stringBuilder.toString() ) );
    }

    @Test
    public void shouldFailValidationWhenTextValueAndDifferentValueType()
    {
        tea.setValueType( ValueType.NUMBER );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "value" ) );

        tea.setValueType( ValueType.BOOLEAN );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "value" ) );
    }

    @Test( expected = IllegalFieldValueException.class )
    public void shouldFailValidationWhenInvalidDate()
    {
        tea.setValueType( ValueType.DATE );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "1970-01-32" ) );
    }

    @Test
    public void shouldFailValidationWhenInvalidDateFormat()
    {
        tea.setValueType( ValueType.DATE );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "19700131" ) );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "1970" ) );
    }

    @Test
    public void successValidationWhenValidDate()
    {
        tea.setValueType( ValueType.DATE );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "1970-01-01" ) );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "1970-01-01T00:00:00.000+02:00" ) );
    }

    @Test
    public void successWhenTextValueIsCorrect()
    {
        tea.setValueType( ValueType.TEXT );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "Firstname" ) );

        tea.setValueType( ValueType.NUMBER );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "123" ) );

        tea.setValueType( ValueType.BOOLEAN );
        assertNull( trackedEntityAttributeService.validateValueType( tea, String.valueOf( true ) ) );

        tea.setValueType( ValueType.DATE );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "2019-01-01" ) );
    }

    @Test
    public void shouldFailWhenUserCredentialNotFound()
    {
        when( userService.getUserCredentialsByUsername( "user" ) ).thenReturn( null );

        tea.setValueType( ValueType.USERNAME );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "user" ) );
    }

    @Test
    public void successWhenUserCredentialExists()
    {
        when( userService.getUserCredentialsByUsername( "user" ) ).thenReturn( new UserCredentials() );

        tea.setValueType( ValueType.USERNAME );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "user" ) );
    }

    @Test
    public void shouldFailWhenInvalidDateTime()
    {
        tea.setValueType( ValueType.DATETIME );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "1970-01-01" ) );
    }

    @Test
    public void successWhenValidDateTime()
    {
        tea.setValueType( ValueType.DATETIME );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "1970-01-01T00:00:00.000+02:00" ) );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "1970-01-01T00:00:00" ) );
    }

    @Test
    public void shouldFailWhenImageValidationFail()
    {
        when( fileResourceService.getFileResource( "uid" ) ).thenReturn( null );

        tea.setValueType( ValueType.IMAGE );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "uid" ) );
    }

    @Test
    public void shouldFailWhenInvalidImageFormat()
    {
        when( fileResourceService.getFileResource( "uid" ) ).thenReturn( fileResource );

        tea.setValueType( ValueType.IMAGE );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "uid" ) );
    }
}
