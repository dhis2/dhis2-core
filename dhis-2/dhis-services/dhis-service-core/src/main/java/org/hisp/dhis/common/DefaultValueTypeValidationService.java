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
    public String dataValueIsValid( ValueTypedDimensionalItemObject dataValueObject, String value )
    {
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
        return ValidationUtils.dataValueIsValid( value, dataValueObject.getValueType() );
    }

    private String validateOptionDataValue( ValueTypedDimensionalItemObject dataValueObject,
        String value )
    {
        OptionSet optionSet = dataValueObject.getOptionSet();

        if ( isNullOrEmpty( value ) || optionSet == null )
        {
            return null;
        }

        return !optionSet.getOptionCodesAsSet().contains( value )
            ? "Value is not a valid option of the data element option set: " + value
            : null;
    }

    private String validateFileResourceDataValue( String value )
    {
        if ( isNullOrEmpty( value ) )
        {
            return null;
        }

        FileResource fileResource = fileResourceService.getFileResource( value );

        if ( fileResource == null || !fileResource.isAssigned() )
        {
            return "Value is not a valid file resource: " + value;
        }

        return null;
    }

    private String validateImageDataValue( String value )
    {
        if ( isNullOrEmpty( value ) )
        {
            return null;
        }

        Set<String> validImageFormats = ImmutableSet.<String> builder().add(
            ImageIO.getReaderFormatNames() ).build();

        FileResource fileResource = fileResourceService.getFileResource( value );

        if ( fileResource == null || !fileResource.isAssigned() )
        {
            return "Value is not a valid file resource: " + value;
        }
        else if ( !validImageFormats.contains( fileResource.getFormat() ) )
        {
            return "File resource is not a valid image: " + value;
        }

        return null;
    }

    private String validateOrgUnitDataValue( String value )
    {
        if ( isNullOrEmpty( value ) )
        {
            return null;
        }

        return organisationUnitService.getOrganisationUnit( value ) == null
            ? "Value is not a valid organisation unit: " + value
            : null;
    }

    private String validateUserDataValue( String value )
    {
        if ( isNullOrEmpty( value ) )
        {
            return null;
        }

        return userService.getUserCredentialsByUsername( value ) == null ? "Value is not a valid username:  " + value
            : null;
    }
}
