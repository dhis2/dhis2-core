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
package org.hisp.dhis.webapi.json;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Represents a string JSON node.
 *
 * @author Jan Bernitt
 */
public interface JsonString extends JsonPrimitive
{

    /**
     * @return string value of the property or {@code null} when this property
     *         is undefined or defined as JSON {@code null}.
     */
    String string();

    default String string( String orDefault )
    {
        return exists() ? string() : orDefault;
    }

    /**
     * In contrast to {@link #mapNonNull(Object, Function)} this function simply
     * returns {@code null} when {@link #string()} is {@code null}. This
     * includes the case that this value is not defined in the JSON content.
     *
     * @param parser function that parses a given {@link String} to the returned
     *        type.
     * @param <T> return type
     * @return {@code null} when {@link #string()} returns {@code null}
     *         otherwise the result of calling provided parser with result of
     *         {@link #string()}.
     */
    default <T> T parsed( Function<String, T> parser )
    {
        String value = string();
        return value == null ? null : parser.apply( value );
    }

    default <T> T converted( Callable<T> converter )
    {
        try
        {
            return converter.call();
        }
        catch ( Exception ex )
        {
            throw new IllegalArgumentException( ex );
        }
    }

    default Class<?> parsedClass()
    {
        return converted( () -> Class.forName( string() ) );
    }
}
