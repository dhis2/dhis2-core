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

import lombok.AllArgsConstructor;
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
     * Annotation to use as part of the OpenAPI generation to work around lack
     * of generic types when using annotations.
     * <p>
     * The annotation has different semantics depending on the annotated
     * element. See {@link #value()} for details.
     * <p>
     * Generally speaking the annotation builds a type substitution mechanism.
     * On one set of target locations it defines the actual type that
     * substitution should use. On another set of target location it marks the
     * place where the substitution should be used.
     * <p>
     * Substitution is scoped per controller and method. By default, methods
     * inherit the type defined for substitution from the controller level, but
     * it can be overridden per method by again annotating the method with a
     * different type given by {@link EntityType#value()}.
     * <p>
     * A {@link java.lang.reflect.Field} or getter
     * {@link java.lang.reflect.Method} on a complex request/response object
     * annotated with {@link EntityType} marks the annotated member for
     * substitution. The {@link #value()} then either should use
     * {@code EntityType.class} for a simple value, or
     * {@code EntityType[].class} for any array or collection.
     *
     * @author Jan Bernitt
     */
    @Inherited
    @Retention( RetentionPolicy.RUNTIME )
    @Target( { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD } )
    @interface EntityType
    {
        /**
         * <ul>
         * <li>When annotated on a controller {@link Class} type the value type
         * refers the actual type to use within the scope of this controller. If
         * the value given is {@code EntityType.class} itself this means the
         * actual type is extracted from the of the controller type's direct
         * superclass first type parameter.</li>
         * <li>When annotated on a controller {@link java.lang.reflect.Method}
         * the value type refers to the actual type within the scope of this
         * method only.</li>
         * <li>When used in an OpenAPI annotation field the target of the
         * annotation uses the actual type from the scope instead of the type
         * present in the signature.</li>
         * <li>When used on a {@link java.lang.reflect.Field} or
         * {@link java.lang.reflect.Method} in a complex request or response
         * object the annotated member's target type uses the actual type from
         * the current scope instead of the type present in the signature.</li>
         * </ul>
         *
         * @return dependent on the target the type to use for substitution, or
         *         where to substitute the type.
         */
        Class<?> value();

        /**
         * When the entire annotated type is substituted the path is empty.
         * <p>
         * If e.g. the {@code T} in a type like {@code Map<String,List<T>>}
         * should be substituted to {@code Map<String,List<MyObject>>} (assuming
         * that {@code MyObject} is the current actual type) the path has to be:
         *
         * <pre>
         * { Map.class, List.class }
         * </pre>
         *
         * Since here the target is an array like type the annotation for this
         * substitution would be:
         *
         * <pre>
         *  &#064;EntityType( value = EntityType[].class, path = 1 )
         * </pre>
         *
         * {@code Map} as root is exclusive, the target is at the {@code 1}
         * index of the root type.
         * <p>
         * Arguably this could also be given by pointing at the {@code T} in
         * {@code List}:
         *
         * <pre>
         *  &#064;EntityType( value = EntityType.class, path = {1, 0} )
         * </pre>
         *
         * @return type parameter index path to target type for substitution
         */
        int[] path() default {};
    }

    /**
     * When annotated on type level the tags are added to all endpoints of the
     * controller.
     * <p>
     * When annotated on method level the tags are added to the annotated
     * endpoint (operation).
     * <p>
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
     * <p>
     * Annotate a controller endpoint method to ignore that endpoint.
     * <p>
     * Annotate a controller endpoint method parameter to ignore that parameter.
     */
    @Inherited
    @Target( { ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD } )
    @Retention( RetentionPolicy.RUNTIME )
    @interface Ignore
    {
        // marker annotation
    }

    /**
     * Annotate on type it makes all properties being picked up unless they are
     * annotated with {@link Ignore}. This is so some members can be annotated
     * with this annotation to detail their type without at the same time
     * exclude members.
     */
    @Inherited
    @Target( { ElementType.METHOD, ElementType.FIELD, ElementType.TYPE } )
    @Retention( RetentionPolicy.RUNTIME )
    @interface Property
    {
        String name() default "";

        /**
         * When empty the type is inferred from the annotated element.
         *
         * @return the type to use for the property
         */
        Class<?>[] value() default {};

        boolean required() default false;
    }

    /**
     * Used to add a single named parameter or request body parameter that is
     * not present (or ignored) in the method signature.
     * <p>
     * Can also be used on a parameter to explicitly mark a parameter that
     * should be considered and to override or extend information about the
     * parameter. If this annotation is present on a method parameter no other
     * annotation will be considered.
     */
    @Inherited
    @Target( { ElementType.METHOD, ElementType.PARAMETER } )
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
         * <p>
         * None (length zero) uses the actual type of the parameter. More than
         * one use a {@code oneOf} union type of all the type schemas.
         *
         * @return type of the parameter, should be a simple type for a path
         *         parameter.
         */
        Class<?>[] value();

        boolean required() default false;

        /**
         * When not empty the parameter is wrapped in an object having a single
         * member with the provided property name.
         *
         * @return name of the property to use
         */
        String asProperty() default "";
    }

    /**
     * Used to add a parameter object that is not present (or ignored) in the
     * method signature. Each property of the object becomes a parameter.
     * <p>
     * Can also be used on a type to explicitly mark it as a parameter object
     * type that should be considered.
     */
    @Inherited
    @Target( { ElementType.METHOD, ElementType.TYPE } )
    @Retention( RetentionPolicy.RUNTIME )
    @Repeatable( ParamsRepeat.class )
    @interface Params
    {
        /**
         * As web classes cannot be used outside the web API module a
         * {@code WebMessage} response value can also be indicated by
         * {@link org.hisp.dhis.webmessage.WebMessageResponse}.
         *
         * @return a complex parameter object type. All properties of that type
         *         become individual parameters.
         */
        Class<?> value();
    }

    /**
     * Used to add or override the response for a specific {@link Status}.
     * <p>
     * If the {@link #status()} is the same as the success status of the method
     * this effectively overrides the return type of the method as present in
     * the signature.
     * <p>
     * Can be annotated on exception types to link all occurrences of declared
     * exception to a particular HTTP response.
     * <p>
     * Can be annotated on thrown exceptions to link a specific occurrence of a
     * declared exception to a particular HTTP response.
     */
    @Inherited
    @Target( { ElementType.METHOD, ElementType.TYPE, ElementType.TYPE_USE } )
    @Retention( RetentionPolicy.RUNTIME )
    @Repeatable( ResponseRepeat.class )
    @interface Response
    {
        /**
         * The HTTP status (actually used in DHIS2 APIs).
         * <p>
         * Needed to be independent of existing enums for module reasons.
         */
        @Getter
        @RequiredArgsConstructor
        enum Status
        {
            OK( 200 ),
            CREATED( 201 ),
            NO_CONTENT( 204 ),
            BAD_REQUEST( 400 ),
            FORBIDDEN( 403 ),
            NOT_FOUND( 404 ),
            CONFLICT( 409 );

            private final int code;
        }

        /**
         * None (length zero) uses the actual type of the method. More than one
         * use a {@code oneOf} union type of all the type schemas.
         *
         * @return body type of the response.
         */
        Class<?>[] value();

        /**
         * If status is left empty the {@link #value()} applies to the status
         * inferred from the method signature.
         *
         * @return the statuses resulting in a response described by the
         *         {@link #value()} type
         */
        Status[] status() default {};

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

    /**
     * Used to make explicit statement about a type being used as shared (named)
     * global component in the resulting OpenAPI document.
     * <p>
     * By default, schema types are shared (opt-out), parameters object types a
     * not shared (opt-in).
     */
    @Target( ElementType.TYPE )
    @Retention( RetentionPolicy.RUNTIME )
    @interface Shared
    {
        boolean value() default true;

        /**
         * @return can be used to override the class name part of a shared
         *         parameter
         */
        String name() default "";

        @Getter
        @AllArgsConstructor
        enum Pattern
        {
            DEFAULT( "" ),
            INFO( "%sInfo" );

            private final String template;
        }

        /**
         * If both name and pattern are used the pattern is ignored.
         *
         * @return naming pattern used to create a name based on the simple
         *         class name.
         */
        Pattern pattern() default Pattern.DEFAULT;
    }

    /*
     * Repeater annotations (not for direct use)
     */

    @Inherited
    @Target( { ElementType.METHOD, ElementType.PARAMETER } )
    @Retention( RetentionPolicy.RUNTIME )
    @interface ParamRepeat
    {
        Param[] value();
    }

    @Inherited
    @Target( { ElementType.METHOD, ElementType.TYPE } )
    @Retention( RetentionPolicy.RUNTIME )
    @interface ParamsRepeat
    {
        Params[] value();
    }

    @Inherited
    @Target( { ElementType.METHOD, ElementType.TYPE, ElementType.TYPE_USE } )
    @Retention( RetentionPolicy.RUNTIME )
    @interface ResponseRepeat
    {
        Response[] value();
    }
}
