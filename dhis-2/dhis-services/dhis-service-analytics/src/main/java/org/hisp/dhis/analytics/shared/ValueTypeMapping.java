/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.shared;

import static java.util.Arrays.stream;
import static org.hisp.dhis.common.ValueType.INTEGER;
import static org.hisp.dhis.common.ValueType.INTEGER_NEGATIVE;
import static org.hisp.dhis.common.ValueType.INTEGER_POSITIVE;
import static org.hisp.dhis.common.ValueType.INTEGER_ZERO_OR_POSITIVE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hisp.dhis.common.ValueType;

public enum ValueTypeMapping
{
    // TODO: adds mappings here
    NUMERIC( BigInteger::new, INTEGER, INTEGER_NEGATIVE, INTEGER_POSITIVE, INTEGER_ZERO_OR_POSITIVE ),
    DECIMAL( BigDecimal::new, ValueType.NUMBER ),
    STRING( s -> s ),
    TEXT( s -> s );

    private final Function<String, Object> converter;

    private final ValueType[] valueTypes;

    ValueTypeMapping(
        Function<String, Object> converter,
        ValueType... valueTypes )
    {
        this.converter = converter;
        this.valueTypes = valueTypes;
    }

    public static ValueTypeMapping fromValueType( ValueType valueType )
    {
        return stream( values() )
            .filter( valueTypeMapping -> valueTypeMapping.supports( valueType ) )
            .findFirst()
            .orElse( TEXT );
    }

    private boolean supports( ValueType valueType )
    {
        return stream( valueTypes )
            .anyMatch( vt -> vt == valueType );
    }

    public Object convertSingle( String s )
    {
        return converter.apply( s );
    }

    public Object convertMany( List<String> values )
    {
        return values.stream()
            .map( converter )
            .collect( Collectors.toList() );
    }
}
