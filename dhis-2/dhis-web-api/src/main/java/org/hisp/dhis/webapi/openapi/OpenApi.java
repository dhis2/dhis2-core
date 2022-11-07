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
package org.hisp.dhis.webapi.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Target( ElementType.METHOD )
@Retention( RetentionPolicy.RUNTIME )
public @interface OpenApi
{
    /**
     * A single named parameter or request body parameter.
     */
    @Inherited
    @Target( ElementType.METHOD )
    @Retention( RetentionPolicy.RUNTIME )
    @Repeatable( ParamRepeat.class )
    @interface Param
    {
        /**
         * @return name of the parameter, empty for request body parameter
         */
        String name() default "";

        /**
         * For complex parameter objects use {@link Params} instead.
         *
         * @return type of the parameter, should be a simple type for a path
         *         parameter.
         */
        Class<?> value();

        /**
         * @return set of property names to include from the {@link #value()}
         *         type, all other properties are implicitly excluded.
         */
        String[] includes() default {};

        /**
         * @return set of property names to exclude from the {@link #value()}
         *         type
         */
        String[] excludes() default {};
    }

    /**
     * A parameter object. Each property of the object becomes a parameter.
     */
    @Inherited
    @Target( ElementType.METHOD )
    @Retention( RetentionPolicy.RUNTIME )
    @Repeatable( ParamsRepeat.class )
    @interface Params
    {
        Class<?> value();

        /**
         * @return set of property names to include from the {@link #value()}
         *         type, all other properties are implicitly excluded.
         */
        String[] includes() default {};

        /**
         * @return set of property names to exclude from the {@link #value()}
         *         type
         */
        String[] excludes() default {};
    }

    @Inherited
    @Target( ElementType.METHOD )
    @Retention( RetentionPolicy.RUNTIME )
    @interface ParamRepeat
    {
        Param[] value();
    }

    @Inherited
    @Target( ElementType.METHOD )
    @Retention( RetentionPolicy.RUNTIME )
    @interface ParamsRepeat
    {
        Params[] value();
    }
}
