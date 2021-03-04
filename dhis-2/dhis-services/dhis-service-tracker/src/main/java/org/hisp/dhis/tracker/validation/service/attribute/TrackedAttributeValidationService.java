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
package org.hisp.dhis.tracker.validation.service.attribute;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.util.Constant;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luca Cambi
 */
@Component
@RequiredArgsConstructor
public class TrackedAttributeValidationService
{
    private final UserService userService;

    private final FileResourceService fileResourceService;

    @Transactional( readOnly = true )
    public String validateValueType( TrackedEntityAttribute trackedEntityAttribute, String value )
    {

        ValueType valueType = Optional.ofNullable( trackedEntityAttribute )
            .orElseThrow( () -> new IllegalArgumentException( "tracked entity attribute is required" ) )
            .getValueType();

        if ( value == null )
        {
            throw new IllegalArgumentException(
                "value is required for tracked entity " + trackedEntityAttribute.getUid() );
        }

        if ( value.length() > Constant.ATTRIBUTE_VALUE_MAX_LENGTH )
        {
            return "Value length is greater than " + Constant.ATTRIBUTE_VALUE_MAX_LENGTH + " chars for attribute "
                + trackedEntityAttribute.getUid();
        }

        String errorValue = StringUtils.substring( value, 0, 30 );

        switch ( valueType )
        {
        case NUMBER:
            if ( !MathUtils.isNumeric( value ) )
            {
                return "Value '" + errorValue + "' is not a valid numeric type for attribute "
                    + trackedEntityAttribute.getUid();
            }
            break;
        case BOOLEAN:
            if ( !MathUtils.isBool( value ) )
            {
                return "Value '" + errorValue + "' is not a valid boolean type for attribute "
                    + trackedEntityAttribute.getUid();
            }
            break;
        case DATE:
            if ( DateUtils.parseDate( value ) == null )
            {
                return "Value '" + errorValue + "' is not a valid date type for attribute "
                    + trackedEntityAttribute.getUid();
            }

            if ( !DateUtils.dateIsValid( value ) )
            {
                return "Value '" + errorValue + "' is not a valid date for attribute "
                    + trackedEntityAttribute.getUid();
            }
            break;
        case TRUE_ONLY:
            if ( !"true".equals( value ) )
            {
                return "Value '" + errorValue + "' is not true (true-only type) for attribute "
                    + trackedEntityAttribute.getUid();
            }
            break;
        case USERNAME:
            if ( userService.getUserCredentialsByUsername( value ) == null )
            {
                return "Value '" + errorValue + "' is not a valid username for attribute "
                    + trackedEntityAttribute.getUid();
            }
            break;
        case DATETIME:
            if ( !DateUtils.dateTimeIsValid( value ) )
            {
                return "Value '" + errorValue + "' is not a valid datetime for attribute "
                    + trackedEntityAttribute.getUid();
            }
            break;
        case IMAGE:
            return validateImage( value );
        }

        return null;
    }

    private String validateImage( String uid )
    {
        FileResource fileResource = fileResourceService.getFileResource( uid );

        if ( fileResource == null )
        {
            return "Value '" + uid + "' is not the uid of a file";
        }
        else if ( !Constant.VALID_IMAGE_FORMATS.contains( fileResource.getFormat() ) )
        {
            return "File resource with uid '" + uid + "' is not a valid image";
        }

        return null;
    }
}
