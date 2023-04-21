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
package org.hisp.dhis.tracker.imports.validation.service.attribute;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.imports.util.Constant;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luca Cambi
 */
@Component
public class TrackedAttributeValidationService
{
    /**
     * @author Luca Cambi
     */
    @Getter
    @RequiredArgsConstructor
    private static class ValueTypeValidationFunction
    {
        private final ValueType valueType;

        private final Predicate<String> function;

        private final String message;
    }

    private static final String VALUE_STRING = "Value";

    private final FileResourceService fileResourceService;

    private final List<ValueTypeValidationFunction> valueTypeValidationFunctions;

    public TrackedAttributeValidationService( UserService userService, FileResourceService fileResourceService )
    {
        this.fileResourceService = fileResourceService;
        valueTypeValidationFunctions = List.of(
            new ValueTypeValidationFunction( ValueType.NUMBER,
                v -> !MathUtils.isNumeric( v ),
                " '%s' is not a valid numeric type for attribute %s " ),
            new ValueTypeValidationFunction( ValueType.BOOLEAN,
                v -> !MathUtils.isBool( v ),
                " '%s' is not a valid boolean type for attribute %s " ),
            new ValueTypeValidationFunction( ValueType.DATE,
                v -> DateUtils.parseDate( v ) == null,
                " '%s' is not a valid date type for attribute %s " ),
            new ValueTypeValidationFunction( ValueType.DATE,
                v -> !DateUtils.dateIsValid( v ),
                " '%s' is not a valid date for attribute %s " ),
            new ValueTypeValidationFunction( ValueType.TRUE_ONLY,
                v -> !"true".equals( v ),
                " '%s' is not true (true-only type) for attribute %s " ),
            new ValueTypeValidationFunction( ValueType.USERNAME,
                v -> userService.getUserByUsername( v ) == null,
                " '%s' is not true (true-only type) for attribute %s " ),
            new ValueTypeValidationFunction( ValueType.DATETIME,
                v -> !DateUtils.dateTimeIsValid( v ),
                " '%s' is not a valid datetime for attribute %s " ) );
    }

    @Transactional( readOnly = true )
    public String validateValueType( TrackedEntityAttribute trackedEntityAttribute, String value )
    {
        ValueType valueType = Optional.ofNullable( trackedEntityAttribute )
            .orElseThrow( () -> new IllegalArgumentException( "tracked entity attribute is required" ) )
            .getValueType();

        if ( Optional.ofNullable( value )
            .orElseThrow( () -> new IllegalArgumentException(
                VALUE_STRING + " is required for tracked entity " + trackedEntityAttribute.getUid() ) )
            .length() > Constant.ATTRIBUTE_VALUE_MAX_LENGTH )
        {
            return VALUE_STRING + " length is greater than " + Constant.ATTRIBUTE_VALUE_MAX_LENGTH
                + " chars for attribute "
                + trackedEntityAttribute.getUid();
        }

        if ( valueType == ValueType.IMAGE )
        {
            return validateImage( value );
        }
        ValueTypeValidationFunction function = valueTypeValidationFunctions.stream()
            .filter( f -> f.getValueType() == valueType )
            .filter( f -> f.getFunction().test( value ) ).findFirst().orElse( null );

        return Optional.ofNullable( function )
            .map( f -> VALUE_STRING + String.format( f.getMessage(), StringUtils.substring( value, 0, 30 ),
                trackedEntityAttribute.getUid() ) )
            .orElse( null );
    }

    public String validateImage( String uid )
    {
        FileResource fileResource = fileResourceService.getFileResource( uid );

        if ( fileResource == null )
        {
            return VALUE_STRING + " '" + uid + "' is not the uid of a file";
        }
        if ( !Constant.VALID_IMAGE_FORMATS.contains( fileResource.getFormat() ) )
        {
            return "File resource with uid '" + uid + "' is not a valid image";
        }

        return null;
    }

}
