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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * All annotations used to adjust the generation of OpenAPI document(s).
 *
 * @author Jan Bernitt
 */
public @interface OpenApi
{
    /**
     * When annotated on type level the tags are added to all endpoints of the
     * controller.
     *
     * When annotated on method level the tags are added to the annotated
     * endpoint (operation).
     *
     * Tags can be used to split generation into multiple OpenAPI document.
     */
    @Inherited
    @Target( { ElementType.METHOD, ElementType.TYPE } )
    @Retention( RetentionPolicy.RUNTIME )
    @interface Tags
    {
        String[] value();
    }

    /**
     * Annotate a controller type to ignore the entire controller.
     *
     * Annotate a controller endpoint method to ignore that endpoint.
     *
     * Annotate a controller endpoint method parameter to ignore that parameter.
     */
    @Inherited
    @Target( { ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER } )
    @Retention( RetentionPolicy.RUNTIME )
    @interface Ignore
    {
        // marker annotation
    }

    /**
     * Used to add a single named parameter or request body parameter that is
     * not present (or ignored) in the method signature.
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

        boolean required() default false;
    }

    /**
     * Used to add a parameter object that is not present (or ignored) in the
     * method signature. Each property of the object becomes a parameter.
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

    /**
     * Used to add or override the response for a specific {@link Status}.
     *
     * If the {@link #status()} is the same as the success status of the method
     * this effectively overrides the return type of the method as present in
     * the signature.
     */
    @Inherited
    @Target( ElementType.METHOD )
    @Retention( RetentionPolicy.RUNTIME )
    @Repeatable( ResponseRepeat.class )
    @interface Response
    {
        /**
         * The HTTP status (actually used in DHIS2 APIs).
         *
         * Needed to be independent of existing enums for module reasons.
         */
        @Getter
        @RequiredArgsConstructor
        enum Status
        {
            BAD_REQUEST( 400 ),
            FORBIDDEN( 403 ),
            NOT_FOUND( 404 ),
            CONFLICT( 409 );

            private final int code;
        }

        /**
         * @return body type of the response
         */
        Class<?> value();

        /**
         * If status is left empty the {@link #value()} applies to the status
         * inferred from the method signature.
         *
         * @return the statuses resulting in a response described by the
         *         {@link #value()} type
         */
        Status[] status() default {};

        String description() default "";

        Header[] headers() default {};

        /**
         * @return supported content types of the response, if empty these are
         *         inherited from the spring annotation(s)
         */
        String[] mediaTypes() default {};
    }

    @Target( ElementType.METHOD )
    @Retention( RetentionPolicy.RUNTIME )
    @interface Header
    {
        String name();

        Class<?> type() default String.class;

        String description() default "";
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

    @Inherited
    @Target( ElementType.METHOD )
    @Retention( RetentionPolicy.RUNTIME )
    @interface ResponseRepeat
    {
        Response[] value();
    }
}
