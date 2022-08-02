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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation.attribute;

import static java.util.AbstractMap.SimpleImmutableEntry;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

/**
 * Validate {@link AttributeValue} using series of predefined validator function
 * such as {@link NumberCheck}, {@link DateCheck}, {@link TextCheck},
 * {@link EntityCheck}, {@link UserCheck}.
 *
 * @author viet
 */
@Component
@RequiredArgsConstructor
public class DefaultAttributeValidator implements AttributeValidator
{
    private final IdentifiableObjectManager manager;

    private final UserService userService;

    private final Map<ValueType, Function<String, List<ErrorReport>>> mapValidators = Map.ofEntries(
        new SimpleImmutableEntry<>( ValueType.INTEGER, NumberCheck.isInteger ),
        new SimpleImmutableEntry<>( ValueType.INTEGER_POSITIVE, NumberCheck.isPositiveInteger ),
        new SimpleImmutableEntry<>( ValueType.INTEGER_NEGATIVE, NumberCheck.isNegativeInteger ),
        new SimpleImmutableEntry<>( ValueType.NUMBER, NumberCheck.isNumber ),
        new SimpleImmutableEntry<>( ValueType.INTEGER_ZERO_OR_POSITIVE, NumberCheck.isZeroOrPositiveInteger ),
        new SimpleImmutableEntry<>( ValueType.PERCENTAGE, NumberCheck.isPercentage ),
        new SimpleImmutableEntry<>( ValueType.UNIT_INTERVAL, NumberCheck.isUnitInterval ),
        new SimpleImmutableEntry<>( ValueType.DATE, DateCheck.isDate ),
        new SimpleImmutableEntry<>( ValueType.DATETIME, DateCheck.isDateTime ),
        new SimpleImmutableEntry<>( ValueType.BOOLEAN, TextCheck.isBoolean ),
        new SimpleImmutableEntry<>( ValueType.TRUE_ONLY, TextCheck.isTrueOnly ),
        new SimpleImmutableEntry<>( ValueType.EMAIL, TextCheck.isEmail ) );

    private final Map<ValueType, EntityCheck> mapEntityCheck = Map.ofEntries(
        new SimpleImmutableEntry<>( ValueType.ORGANISATION_UNIT, EntityCheck.isOrganisationUnitExist ),
        new SimpleImmutableEntry<>( ValueType.FILE_RESOURCE, EntityCheck.isFileResourceExist ) );

    private final Map<ValueType, UserCheck.Function> mapUserCheck = Map.ofEntries(
        new SimpleImmutableEntry<>( ValueType.USERNAME, UserCheck.isUserNameExist ) );

    /**
     * Call all predefined map of validators for checking given value and
     * {@link ValueType}.
     * <p>
     * If there is no validator defined for given {@link ValueType} then return
     * emptyList.
     *
     * @param valueType Metadata Attribute {@link ValueType}.
     * @param value the value for validating.
     * @param addError {@link Consumer} which will accept generated
     *        {@link ErrorReport}
     */
    @Override
    public void validate( ValueType valueType, String value, Consumer<ErrorReport> addError )
    {
        mapValidators.getOrDefault( valueType, str -> List.of() ).apply( value ).forEach( addError::accept );

        mapEntityCheck.getOrDefault( valueType, EntityCheck.empty )
            .apply( value, klass -> manager.get( klass, value ) != null ).forEach( addError::accept );

        mapUserCheck.getOrDefault( valueType, UserCheck.empty )
            .apply( value, userService )
            .forEach( addError::accept );
    }
}
