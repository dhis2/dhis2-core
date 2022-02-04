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
package org.hisp.dhis.tracker.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.util.Constant;
import org.hisp.dhis.tracker.validation.service.attribute.TrackedAttributeValidationService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.joda.time.IllegalFieldValueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class TrackedAttributeValidationServiceTest
{

    private TrackedAttributeValidationService trackedEntityAttributeService;

    @Mock
    private UserService userService;

    @Mock
    private FileResourceService fileResourceService;

    @Mock
    private FileResource fileResource;

    private TrackedEntityAttribute tea;

    @BeforeEach
    public void setUp()
    {

        trackedEntityAttributeService = new TrackedAttributeValidationService( userService, fileResourceService );

        tea = new TrackedEntityAttribute();
        tea.setUid( "TeaUid12345" );
        tea.setUnique( true );
        tea.setValueType( ValueType.TEXT );
        tea.setOrgunitScope( false );
    }

    @Test
    void shouldThrowWhenTeaIsNull()
    {
        assertThrows( IllegalArgumentException.class,
            () -> trackedEntityAttributeService.validateValueType( null, "" ) );
    }

    @Test
    void shouldThrowWhenNotAValidDate()
    {
        tea.setValueType( ValueType.DATE );
        String teaValue = "Firstname";
        assertThrows( IllegalArgumentException.class,
            () -> trackedEntityAttributeService.validateValueType( tea, teaValue ) );
    }

    @Test
    void shouldThrowWhenNullValue()
    {
        assertThrows( IllegalArgumentException.class,
            () -> trackedEntityAttributeService.validateValueType( tea, null ) );
    }

    @Test
    void shouldFailValueOverMaxLength()
    {
        StringBuilder stringBuilder = new StringBuilder();

        for ( int i = 0; i < Constant.ATTRIBUTE_VALUE_MAX_LENGTH + 1; i++ )
        {
            stringBuilder.append( "a" );
        }

        assertNotNull( trackedEntityAttributeService.validateValueType( tea, stringBuilder.toString() ) );
    }

    @Test
    void shouldFailValidationWhenTextValueAndDifferentValueType()
    {
        tea.setValueType( ValueType.NUMBER );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "value" ) );

        tea.setValueType( ValueType.BOOLEAN );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "value" ) );
    }

    @Test
    void shouldFailValidationWhenInvalidDate()
    {
        tea.setValueType( ValueType.DATE );
        assertThrows( IllegalFieldValueException.class,
            () -> assertNotNull( trackedEntityAttributeService.validateValueType( tea, "1970-01-32" ) ) );
    }

    @Test
    void shouldFailValidationWhenInvalidDateFormat()
    {
        tea.setValueType( ValueType.DATE );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "19700131" ) );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "1970" ) );
    }

    @Test
    void successValidationWhenValidDate()
    {
        tea.setValueType( ValueType.DATE );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "1970-01-01" ) );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "1970-01-01T00:00:00.000+02:00" ) );
    }

    @Test
    void successWhenTextValueIsCorrect()
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
    void shouldFailWhenUserNotFound()
    {
        when( userService.getUserByUsername( "user" ) ).thenReturn( null );

        tea.setValueType( ValueType.USERNAME );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "user" ) );
    }

    @Test
    void successWhenUserExists()
    {
        when( userService.getUserByUsername( "user" ) ).thenReturn( new User() );

        tea.setValueType( ValueType.USERNAME );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "user" ) );
    }

    @Test
    void shouldFailWhenInvalidDateTime()
    {
        tea.setValueType( ValueType.DATETIME );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "1970-01-01" ) );
    }

    @Test
    void successWhenValidDateTime()
    {
        tea.setValueType( ValueType.DATETIME );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "1970-01-01T00:00:00.000+02:00" ) );
        assertNull( trackedEntityAttributeService.validateValueType( tea, "1970-01-01T00:00:00" ) );
    }

    @Test
    void shouldFailWhenImageValidationFail()
    {
        when( fileResourceService.getFileResource( "uid" ) ).thenReturn( null );

        tea.setValueType( ValueType.IMAGE );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "uid" ) );
    }

    @Test
    void shouldFailWhenInvalidImageFormat()
    {
        when( fileResourceService.getFileResource( "uid" ) ).thenReturn( fileResource );

        tea.setValueType( ValueType.IMAGE );
        assertNotNull( trackedEntityAttributeService.validateValueType( tea, "uid" ) );
    }
}
