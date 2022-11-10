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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to use as part of the OpenAPI generation to work around lack of
 * generic types when using annotations.
 *
 * The annotation has different semantics depending on the annotated element.
 * See {@link #value()} for details.
 *
 * Generally speaking the annotation builds a type substitution mechanism. On
 * one set of target locations it defines the actual type that substitution
 * should use. On another set of target location it marks the place where the
 * substitution should be used.
 *
 * Substitution is scoped per controller and method. By default, methods inherit
 * the type defined for substitution from the controller level, but it can be
 * overridden per method by again annotating the method with a different type
 * given by {@link EntityType#value()}.
 *
 * A {@link java.lang.reflect.Field} or getter {@link java.lang.reflect.Method}
 * on a complex request/response object annotated with {@link EntityType} marks
 * the annotated member for substitution. The {@link #value()} then either
 * should use {@code EntityType.class} for a simple value, or
 * {@code EntityType[].class} for any array or collection.
 *
 * @author Jan Bernitt
 */
@Inherited
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD } )
public @interface EntityType
{
    /**
     * <ul>
     * <li>When annotated on a controller {@link Class} type the value type
     * refers the actual type to use within the scope of this controller. If the
     * value given is {@code EntityType.class} itself this means the actual type
     * is extracted from the of the controller type's direct superclass first
     * type parameter.</li>
     * <li>When annotated on a controller {@link java.lang.reflect.Method} the
     * value type refers to the actual type within the scope of this method
     * only.</li>
     * <li>When used in an OpenAPI annotation field the target of the annotation
     * uses the actual type from the scope instead of the type present in the
     * signature.</li>
     * <li>When used on a {@link java.lang.reflect.Field} or
     * {@link java.lang.reflect.Method} in a complex request or response object
     * the annotated member's target type uses the actual type from the current
     * scope instead of the type present in the signature.</li>
     * </ul>
     *
     * @return dependent on the target the type to use for substitution, or
     *         where to substitute the type.
     */
    Class<?> value();

    /**
     * When the entire annotated type is substituted the path is empty.
     *
     * If e.g. the {@code T} in a type like {@code Map<String,List<T>>} should
     * be substituted to {@code Map<String,List<MyObject>>} (assuming that
     * {@code MyObject} is the current actual type) the path has to be:
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
     * {@code Map} as root is exclusive, the target is at the {@code 1} index of
     * the root type.
     *
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
