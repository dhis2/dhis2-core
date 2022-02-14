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
package org.hisp.dhis.common;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Set;

import javax.imageio.ImageIO;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;

/**
 * @author abyot
 *
 */
@Service( "org.hisp.dhis.common.ValueTypeValidationService" )
public class DefaultValueTypeValidationService implements ValueTypeValidationService
{
    private FileResourceService fileResourceService;

    private OrganisationUnitService organisationUnitService;

    private UserService userService;

    public DefaultValueTypeValidationService( FileResourceService fileResourceService,
        OrganisationUnitService organisationUnitService,
        UserService userService )
    {
        checkNotNull( fileResourceService );
        checkNotNull( organisationUnitService );
        checkNotNull( userService );

        this.fileResourceService = fileResourceService;
        this.organisationUnitService = organisationUnitService;
        this.userService = userService;

    }

    @Override
    public ErrorMessage dataValueIsValid( ValueTypedDimensionalItemObject dataValueObject, String value )
    {
        if ( dataValueObject == null || dataValueObject.getValueType() == null )
        {
            return new ErrorMessage( ErrorCode.E2043 );
        }

        if ( dataValueObject.getValueType() == null )
        {
            return new ErrorMessage( ErrorCode.E2044, dataValueObject.getValueType() );
        }

        if ( dataValueObject.hasOptionSet() )
        {
            return validateOptionDataValue( dataValueObject, value );
        }
        else if ( ValueType.FILE_RESOURCE == dataValueObject.getValueType() )
        {
            return validateFileResourceDataValue( value );
        }
        else if ( ValueType.IMAGE == dataValueObject.getValueType() )
        {
            return validateImageDataValue( value );
        }
        else if ( ValueType.ORGANISATION_UNIT == dataValueObject.getValueType() )
        {
            return validateOrgUnitDataValue( value );
        }
        else if ( ValueType.USERNAME == dataValueObject.getValueType() )
        {
            return validateUserDataValue( value );
        }
        return ValidationUtils.dataValueIsValid( value, dataValueObject.getValueType() ) != null
            ? new ErrorMessage( ErrorCode.E2030, dataValueObject.getValueType() )
            : null;
    }

    private ErrorMessage validateOptionDataValue( ValueTypedDimensionalItemObject dataValueObject,
        String value )
    {
        OptionSet optionSet = dataValueObject.getOptionSet();

        if ( isNullOrEmpty( value ) || optionSet == null )
        {
            return null;
        }

        return !optionSet.getOptionCodesAsSet().stream().anyMatch( value::equalsIgnoreCase )
            ? new ErrorMessage( ErrorCode.E2029, value )
            : null;
    }

    private ErrorMessage validateFileResourceDataValue( String value )
    {
        if ( isNullOrEmpty( value ) )
        {
            return null;
        }

        FileResource fileResource = fileResourceService.getFileResource( value );

        if ( fileResource == null )
        {
            return new ErrorMessage( ErrorCode.E2027, value );
        }

        if ( fileResource.isAssigned() )
        {
            return new ErrorMessage( ErrorCode.E2026, value );
        }

        return null;
    }

    private ErrorMessage validateImageDataValue( String value )
    {
        if ( isNullOrEmpty( value ) )
        {
            return null;
        }

        Set<String> validImageFormats = ImmutableSet.<String> builder().add(
            ImageIO.getReaderFormatNames() ).build();

        FileResource fileResource = fileResourceService.getFileResource( value );

        if ( fileResource == null )
        {
            return new ErrorMessage( ErrorCode.E2027, value );
        }

        if ( fileResource.isAssigned() )
        {
            return new ErrorMessage( ErrorCode.E2026, value );
        }
        else if ( !validImageFormats.contains( fileResource.getFormat() ) )
        {
            return new ErrorMessage( ErrorCode.E2040, fileResource.getFormat() );
        }

        return null;
    }

    private ErrorMessage validateOrgUnitDataValue( String value )
    {
        if ( isNullOrEmpty( value ) )
        {
            return null;
        }

        return organisationUnitService.getOrganisationUnit( value ) == null
            ? new ErrorMessage( ErrorCode.E2041, value )
            : null;
    }

    private ErrorMessage validateUserDataValue( String value )
    {
        if ( isNullOrEmpty( value ) )
        {
            return null;
        }

        return userService.getUserCredentialsByUsername( value ) == null ? new ErrorMessage( ErrorCode.E2042, value )
            : null;
    }
}
