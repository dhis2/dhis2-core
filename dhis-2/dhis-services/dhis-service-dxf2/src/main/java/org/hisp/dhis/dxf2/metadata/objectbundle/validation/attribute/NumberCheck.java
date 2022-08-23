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

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.system.util.ValidationUtils;

/**
 * Contains validators for Number types of {@link AttributeValue}
 *
 * @author viet
 */
@FunctionalInterface
public interface NumberCheck extends Function<String, List<ErrorReport>>
{
    NumberCheck empty = str -> List.of();

    NumberCheck isInteger = check( MathUtils::isInteger, ErrorCode.E6006 );

    NumberCheck isPositiveInteger = check( MathUtils::isPositiveInteger, ErrorCode.E6007 );

    NumberCheck isNegativeInteger = check( MathUtils::isNegativeInteger, ErrorCode.E6013 );

    NumberCheck isNumber = check( MathUtils::isNumeric, ErrorCode.E6008 );

    NumberCheck isZeroOrPositiveInteger = check( MathUtils::isZeroOrPositiveInteger, ErrorCode.E6009 );

    NumberCheck isPercentage = check( MathUtils::isPercentage, ErrorCode.E6010 );

    NumberCheck isUnitInterval = check( MathUtils::isUnitInterval, ErrorCode.E6011 );

    NumberCheck isPhoneNumber = check( ValidationUtils::isPhoneNumber, ErrorCode.E6021 );

    static NumberCheck check( final Predicate<String> predicate, ErrorCode errorCode )
    {
        return str -> !predicate.test( str ) ? List.of( new ErrorReport( AttributeValue.class, errorCode, str ) )
            : List.of();
    }
}