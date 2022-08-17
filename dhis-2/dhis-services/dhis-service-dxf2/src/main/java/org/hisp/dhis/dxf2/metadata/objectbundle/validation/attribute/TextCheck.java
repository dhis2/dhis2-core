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
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.system.util.ValidationUtils;

/**
 * Contains validators for Text types of {@link ValueType}
 *
 * @author viet
 */
@FunctionalInterface
public interface TextCheck extends Function<String, List<ErrorReport>>
{
    DateCheck empty = $ -> List.of();

    DateCheck isBoolean = check( str -> MathUtils.isBool( str ), ErrorCode.E6016 );

    DateCheck isTrueOnly = check( str -> "true".equals( str ), ErrorCode.E6017 );

    DateCheck isEmail = check( str -> ValidationUtils.emailIsValid( str ), ErrorCode.E6018 );

    static DateCheck check( final Predicate<String> predicate, ErrorCode errorCode )
    {
        return str -> !predicate.test( str ) ? List.of( new ErrorReport( AttributeValue.class, errorCode, str ) )
            : List.of();
    }
}
