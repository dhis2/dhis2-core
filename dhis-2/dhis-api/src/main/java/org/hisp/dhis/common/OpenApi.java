/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;

/**
 * All annotations used to adjust the generation of OpenAPI document(s).
 *
 * @author Jan Bernitt
 */
public @interface OpenApi {

  /**
   * Marker annotation to declare inputs that are semantically required but cannot be
   * declared @{@link javax.annotation.Nonnull} because it is not guaranteed that the input layer
   * will always provide a non-null value.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.RECORD_COMPONENT, ElementType.METHOD, ElementType.FIELD})
  @interface Required {}

  /** Is a property output, input, input+output (explicitly) or input+output (assumed) */
  enum Access {
    READ,
    WRITE,
    READ_WRITE,
    DEFAULT
  }

  /**
   * Annotation to use as part of the OpenAPI generation to work around lack of generic types when
   * using annotations.
   *
   * <p>The annotation has different semantics depending on the annotated element. See {@link
   * #value()} for details.
   *
   * <p>Generally speaking the annotation builds a type substitution mechanism. On one set of target
   * locations it defines the actual type that substitution should use. On another set of target
   * location it marks the place where the substitution should be used.
   *
   * <p>Substitution is scoped per controller and method. By default, methods inherit the type
   * defined for substitution from the controller level, but it can be overridden per method by
   * again annotating the method with a different type given by {@link EntityType#value()}.
   *
   * <p>A {@link java.lang.reflect.Field} or getter {@link java.lang.reflect.Method} on a complex
   * request/response object annotated with {@link EntityType} marks the annotated member for
   * substitution. The {@link #value()} then either should use {@code EntityType.class} for a simple
   * value, or {@code EntityType[].class} for any array or collection.
   *
   * @author Jan Bernitt
   */
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
  @interface EntityType {
    /**
     *
     *
     * <ul>
     *   <li>When annotated on a controller {@link Class} type the value type refers the actual type
     *       to use within the scope of this controller. If the value given is {@code
     *       EntityType.class} itself this means the actual type is extracted from the of the
     *       controller type's direct superclass first type parameter.
     *   <li>When annotated on a controller {@link java.lang.reflect.Method} the value type refers
     *       to the actual type within the scope of this method only.
     *   <li>When used in an OpenAPI annotation field the target of the annotation uses the actual
     *       type from the scope instead of the type present in the signature.
     *   <li>When used on a {@link java.lang.reflect.Field} or {@link java.lang.reflect.Method} in a
     *       complex request or response object the annotated member's target type uses the actual
     *       type from the current scope instead of the type present in the signature.
     * </ul>
     *
     * @return dependent on the target the type to use for substitution, or where to substitute the
     *     type.
     */
    Class<?> value();

    /**
     * When the entire annotated type is substituted the path is empty.
     *
     * <p>If e.g. the {@code T} in a type like {@code Map<String,List<T>>} should be substituted to
     * {@code Map<String,List<MyObject>>} (assuming that {@code MyObject} is the current actual
     * type) the path has to be:
     *
     * <pre>
     * { Map.class, List.class }
     * </pre>
     *
     * Since here the target is an array like type the annotation for this substitution would be:
     *
     * <pre>
     *  &#064;EntityType( value = EntityType[].class, path = 1 )
     * </pre>
     *
     * {@code Map} as root is exclusive, the target is at the {@code 1} index of the root type.
     *
     * <p>Arguably this could also be given by pointing at the {@code T} in {@code List}:
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
   * Used to annotate data types that are not {@link IdentifiableObject}s but that represent a one
   * (e.g. a projection of an {@link IdentifiableObject} or DTO used in the API).
   */
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface Identifiable {
    /**
     * @return the original {@link IdentifiableObject} type the annotated type represents, e.g. a
     *     UserDTO would refer to User
     */
    Class<? extends IdentifiableObject> as();
  }

  /**
   * Kind as a higher order type bucket to group objects of similar role. This is purely for display
   * purposes showing types (schemas) of same kind in the same group.
   *
   * <p>When annotated on a top level type the enclosed types are also associated with the same kind
   * unless they have a varying annotation on their own.
   */
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface Kind {
    /**
     * @return name of the kind
     */
    String value();
  }

  /**
   * Can be used to annotate endpoint methods to constraint which concrete {@link EntityType}s will
   * support the annotated endpoint method.
   */
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Filter {

    /**
     * @return when present (non-empty) only endpoints with an {@link EntityType} contained in the
     *     given set will be considered
     */
    Class<?>[] includes() default {};

    /**
     * @return when present (non-empty) only endpoints with an {@link EntityType} not contained in
     *     the given set will be considered
     */
    Class<?>[] excludes() default {};
  }

  /**
   * Used to classify the contents of a controller so that the entire API can be split by the
   * different classifiers.
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @interface Document {

    String GROUP_QUERY = "query";
    String GROUP_MANAGE = "management";
    String GROUP_CONFIG = "configuration";

    /**
     * Alternative to {@link #entity()} for a "manual" override. Takes precedence when non-empty.
     *
     * @return name of the target document (no file extension)
     */
    String name() default "";

    /**
     * The entity is the type of object the annotated controller is about.
     *
     * <p>When generating individual files each entity becomes a separate OpenAPI document. The name
     * used for the document is the shared named of the domain class. That is the {@link
     * Class#getSimpleName()} unless the class is annotated and named via {@link Shared}.
     *
     * <p>Unless overridden by {@link #classifiers()} this is also the classifier value for {@code
     * entity}.
     *
     * @return the class that represents the domain for the annotated controller {@link Class} or
     *     endpoint {@link java.lang.reflect.Method}.
     */
    // TODO make this metadataEntity() ? => always point out the most related metadata object to
    // categorise a controller by?
    Class<?> entity() default EntityType.class;

    /**
     * Groups become "sections" within a OpenAPI document. This is done by adding a tag to the
     * annotated endpoint(s).
     *
     * @return type of group used
     */
    String group() default "";

    /**
     *
     *
     * <pre>
     *   {"team:tracker", "purpose:metadata", "path:/openapi"}
     * </pre>
     *
     * @return the scope classifiers that describe the annotated controller
     */
    String[] classifiers() default {};
  }

  @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
  @Retention(RetentionPolicy.RUNTIME)
  @interface Since {
    /**
     * @return the 2.xx version when the annotated element was introduced
     */
    int value();
  }

  /**
   * Annotate a controller type to ignore the entire controller.
   *
   * <p>Annotate a controller endpoint method to ignore that endpoint.
   *
   * <p>Annotate a controller endpoint method parameter to ignore that parameter.
   */
  @Inherited
  @Target({
    ElementType.METHOD,
    ElementType.TYPE,
    ElementType.PARAMETER,
    ElementType.FIELD,
    ElementType.TYPE_USE
  })
  @Retention(RetentionPolicy.RUNTIME)
  @interface Ignore {
    // marker annotation
  }

  /**
   * Annotate on type it makes all properties being picked up unless they are annotated with {@link
   * Ignore}. This is so some members can be annotated with this annotation to detail their type
   * without at the same time exclude members.
   */
  @Inherited
  @Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @interface Property {
    String name() default "";

    /**
     * When empty the type is inferred from the annotated element.
     *
     * @return the type to use for the property
     */
    Class<?>[] value() default {};

    boolean required() default false;

    /**
     * When set to value other than the default this takes precedence over other annotations.
     *
     * @return the access direction for this property
     */
    Access access() default Access.DEFAULT;

    /**
     * If given, this values takes precedence over the actual initial value of a field that might be
     * present.
     *
     * @return the string representation of the default value for the property (must be non-empty)
     */
    @Language("JSON")
    String defaultValue() default "";
  }

  /**
   * Used to add a single named parameter or request body parameter that is not present (or ignored)
   * in the method signature.
   *
   * <p>Can also be used on a parameter to explicitly mark a parameter that should be considered and
   * to override or extend information about the parameter. If this annotation is present on a
   * method parameter no other annotation will be considered.
   */
  @Inherited
  @Target({ElementType.METHOD, ElementType.PARAMETER})
  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(ParamRepeat.class)
  @interface Param {
    /**
     * @return name of the parameter, empty for request body parameter
     */
    String name() default "";

    /**
     * For complex parameter objects use {@link Params} instead.
     *
     * <p>None (length zero) uses the actual type of the parameter. More than one use a {@code
     * oneOf} union type of all the type schemas.
     *
     * @return type of the parameter, should be a simple type for a path parameter.
     */
    Class<?>[] value() default {};

    /**
     * When used together with {@link #value()} it is assumed that the {@link #value()} type is
     * object and that the given properties are in addition to that type's properties.
     *
     * @return the properties of the declared object
     */
    Property[] object() default {};

    boolean required() default false;

    boolean deprecated() default false;

    /**
     * If given, this values takes precedence over the actual initial value of a field that might be
     * present.
     *
     * @return the string representation of the default value for the property (must be non-empty)
     */
    @Language("JSON")
    String defaultValue() default "";
  }

  /**
   * Used to add a parameter object that is not present (or ignored) in the method signature. Each
   * property of the object becomes a parameter.
   *
   * <p>Can also be used on a type to explicitly mark it as a parameter object type that should be
   * considered.
   */
  @Inherited
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(ParamsRepeat.class)
  @interface Params {
    /**
     * @return a complex parameter object type. All properties of that type become individual
     *     parameters.
     */
    Class<?> value();
  }

  /**
   * Used to add or override the response for a specific {@link Status}.
   *
   * <p>If the {@link #status()} is the same as the success status of the method this effectively
   * overrides the return type of the method as present in the signature.
   *
   * <p>Can be annotated on exception types to link all occurrences of declared exception to a
   * particular HTTP response.
   *
   * <p>Can be annotated on thrown exceptions to link a specific occurrence of a declared exception
   * to a particular HTTP response.
   */
  @Inherited
  @Target({ElementType.METHOD, ElementType.TYPE, ElementType.TYPE_USE})
  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(ResponseRepeat.class)
  @interface Response {
    /**
     * The HTTP status (actually used in DHIS2 APIs).
     *
     * <p>Needed to be independent of existing enums for module reasons.
     */
    @Getter
    @RequiredArgsConstructor
    enum Status {
      OK(200),
      CREATED(201),
      NO_CONTENT(204),
      FOUND(302),
      NOT_MODIFIED(304),
      BAD_REQUEST(400),
      FORBIDDEN(403),
      NOT_FOUND(404),
      CONFLICT(409),
      BAD_GATEWAY(502);

      private final int code;
    }

    /**
     * No value (length zero) uses the actual type of the method unless {@link #object()} is
     * non-empty.
     *
     * <p>More than one use a {@code oneOf} union type of all the type schemas.
     *
     * @return body type of the response.
     */
    Class<?>[] value() default {};

    /**
     * When used together with {@link #value()} it is assumed that the {@link #value()} type is
     * object and that the given properties are in addition to that type's properties.
     *
     * @return the properties of the declared object
     */
    Property[] object() default {};

    /**
     * If status is left empty the {@link #value()} applies to the status inferred from the method
     * signature.
     *
     * @return the statuses resulting in a response described by the {@link #value()} type
     */
    Status[] status() default {};

    Header[] headers() default {};

    /**
     * @return supported content types of the response, if empty these are inherited from the spring
     *     annotation(s)
     */
    String[] mediaTypes() default {};
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Header {
    String name();

    Class<?> type() default String.class;

    @Language("markdown")
    String description() default "";
  }

  /**
   * Used to make explicit statement about a type being used as shared (named) global component in
   * the resulting OpenAPI document.
   *
   * <p>By default, schema types are shared (opt-out), parameters object types a not shared
   * (opt-in).
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @interface Shared {

    /**
     * Marker annotation to use on properties of a shared parameter object that cannot be shared as
     * their type is dynamic (varying).
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Inline {}

    boolean value() default true;

    /**
     * @return can be used to override the class name part of a shared parameter
     */
    String name() default "";

    /**
     * @return just for documentation purposes to indicate why the manual adjustment was made
     */
    String reason() default "";
  }

  /**
   * Provides a description text. Generally takes precedence over providing texts in markdown file,
   * but can be combined with such text by placing a placeholder {@code {md}} in the annotated text
   * value.
   *
   * <p>Can be used in various places.
   *
   * <p>It becomes...
   *
   * <ul>
   *   <li>endpoint description when placed on an endpoint method
   *   <li>response body description when placed on the class of the endpoint method's return type
   *   <li>parameter description when placed on an endpoint method's parameter
   *   <li>property description when placed on a field or accessor method of a type that becomes a
   *       schema
   *   <li>endpoint exception
   * </ul>
   *
   * When placed on a {@link Class} which is used as parameter type and that parameter has no
   * annotation directly on the parameter (includes fields in a parameter object) the type level is
   * used as a fallback for such a parameter.
   */
  @Target({ElementType.TYPE_USE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface Description {

    /**
     * If multiple values are given these are turned into a bullet list. Each item in the list is
     * left "as is" so it may use inline mark-down syntax.
     *
     * <p>A value may contain a placeholder {@code {md}} in which case the description text that
     * potentially exists in a markdown file will be inserted at that location.
     *
     * @return The description, might use mark-down syntax
     */
    @Language("markdown")
    String[] value();

    /**
     * @return when true any {@link Description} annotation present on the type (of a parameter) is
     *     ignored and only the text from this annotation is included
     */
    boolean ignoreTypeDescription() default false;

    /**
     * @return when true any matching text present in a markdown file for the annotated element is
     *     ignored and only the text from this annotation is included
     */
    boolean ignoreFileDescription() default false;
  }

  /*
   * Repeater annotations (not for direct use)
   */

  @Inherited
  @Target({ElementType.METHOD, ElementType.PARAMETER})
  @Retention(RetentionPolicy.RUNTIME)
  @interface ParamRepeat {
    Param[] value();
  }

  @Inherited
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @interface ParamsRepeat {
    Params[] value();
  }

  @Inherited
  @Target({ElementType.METHOD, ElementType.TYPE, ElementType.TYPE_USE})
  @Retention(RetentionPolicy.RUNTIME)
  @interface ResponseRepeat {
    Response[] value();
  }

  /**
   * A "virtual" property name enumeration type. It creates an OpenAPI {@code enum} string schema
   * containing all valid property names for the target type. The target type is either the actual
   * type substitute for the {@link EntityType} or the first argument type.
   */
  @NoArgsConstructor
  final class PropertyNames {}
}
