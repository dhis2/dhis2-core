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
package org.hisp.dhis.webapi.openapi;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.Maturity;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Access;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.period.Period;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Captures the result of the controller analysis process in a data model.
 *
 * <p>Simple fields in the model are "immutable" while collections are used "mutable" to aggregate
 * the results during the analysis process.
 *
 * <p>Descriptions that are added later use a {@link Maybe} box, so they can be used "mutable" too.
 *
 * @author Jan Bernitt
 */
@Value
@Slf4j
public class Api {

  /**
   * Can be used in {@link OpenApi.Param#value()} to point not to the type to use but the generator
   * to use.
   *
   * <p>All generators must provide an accessible no args constructor and be stateless.
   */
  @FunctionalInterface
  public interface SchemaGenerator {
    Schema generate(Endpoint endpoint, Type source, Class<?>... args);
  }

  /**
   * The included classes can be filtered based on REST API resource path or {@link
   * OpenApi.Document#entity()} present on the controller class level. Method level path and tags
   * will not be considered for this filter.
   *
   * @param controllers controllers all potential controllers
   * @param filters the scope filter used (empty includes all)
   * @param matches those classes in {@link #controllers} that are included in the filter
   */
  record Scope(
      @Nonnull Set<Class<?>> controllers,
      @Nonnull Map<String, Set<String>> filters,
      @Nonnull Set<Class<?>> matches) {}

  Scope scope;

  /** Can be set to enable debug mode */
  Maybe<Boolean> debug = new Maybe<>(false);

  /** "Global" tag descriptions */
  Map<String, Tag> tags = new TreeMap<>();

  /** The controllers as collected by the analysis phase */
  List<Controller> controllers = new ArrayList<>();

  /**
   * The merged endpoints grouped by path and request method computed by the finalisation phase.
   * This is the basis of the OpenAPI document generation.
   */
  Map<String, Map<RequestMethod, Endpoint>> endpoints = new TreeMap<>();

  Components components = new Components();

  /**
   * Note that this needs to use the {@link ConcurrentSkipListMap} as most other maps do not allow
   * to be modified from within a callback that itself is adding an entry like {@link
   * Map#computeIfAbsent(Object, Function)}. Here, while one {@link Schema} is resolved more {@link
   * Schema} might be added.
   */
  Map<Class<?>, Schema> schemas = new ConcurrentSkipListMap<>(comparing(Class::getName));

  /**
   * First level key is the {@link SchemaGenerator} type, second level is the {@link
   * Schema#getRawType()} of the generated {@link Schema}.
   */
  Map<Class<?>, Map<Class<?>, Schema>> generatorSchemas =
      new ConcurrentSkipListMap<>(comparing(Class::getName));

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  static final class Maybe<T> {
    T value;

    boolean isPresent() {
      return value != null;
    }

    T init(Supplier<T> ifNotPresent) {
      if (!isPresent()) {
        setValue(ifNotPresent.get());
      }
      return getValue();
    }

    public void setIfAbsent(T value) {
      if (this.value == null) {
        this.value = value;
      }
    }

    T orElse(T defaultValue) {
      return value != null ? value : defaultValue;
    }

    public Stream<T> stream() {
      return isPresent() ? Stream.of(value) : Stream.empty();
    }
  }

  /** Shared {@code components} in an OpenAPi document. */
  @Value
  static class Components {
    /** Only the shared schemas of the API by their unique name */
    Map<String, Schema> schemas = new LinkedHashMap<>();

    /**
     * Shared parameters originating from parameter object classes. These are reused purely for sake
     * of removing duplication from the resulting OpenAPI document.
     */
    Map<Class<?>, List<Parameter>> parameters = new ConcurrentHashMap<>();
  }

  @Value
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  static class Tag {
    @EqualsAndHashCode.Include String name;

    Maybe<String> description = new Maybe<>();
    Maybe<String> externalDocsUrl = new Maybe<>();
    Maybe<String> externalDocsDescription = new Maybe<>();
  }

  @Value
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  static class Controller {

    @ToString.Exclude Api in;
    @ToString.Exclude @EqualsAndHashCode.Include Class<?> source;
    @ToString.Exclude @EqualsAndHashCode.Include Class<?> entityType;

    String name;
    Map<String, String> classifiers = new TreeMap<>();
    List<String> paths = new ArrayList<>();
    List<Endpoint> endpoints = new ArrayList<>();
  }

  @Value
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  public static class Endpoint {

    @ToString.Exclude Controller in;
    @ToString.Exclude Method source;
    @ToString.Exclude Class<?> entityType;

    @EqualsAndHashCode.Include String name;
    @CheckForNull String group;

    Maybe<String> description = new Maybe<>();
    List<String> authorities = new ArrayList<>(0);

    Boolean deprecated;
    @CheckForNull Maturity.Classification maturity;

    @EqualsAndHashCode.Include Set<RequestMethod> methods = EnumSet.noneOf(RequestMethod.class);

    @EqualsAndHashCode.Include Set<String> paths = new LinkedHashSet<>();

    Maybe<RequestBody> requestBody = new Maybe<>();

    /** Endpoint parameter by simple name (endpoint local name) */
    Map<String, Parameter> parameters = new TreeMap<>();

    Map<HttpStatus, Response> responses = new EnumMap<>(HttpStatus.class);

    boolean isSynthetic() {
      return source == null;
    }

    boolean isDeprecated() {
      return Boolean.TRUE == deprecated;
    }

    String getEntityTypeName() {
      return entityType == null ? "?" : entityType.getSimpleName();
    }

    OpenApi.Since getSince() {
      return source == null ? null : source.getAnnotation(OpenApi.Since.class);
    }
  }

  @Value
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  static class RequestBody {

    @ToString.Exclude AnnotatedElement source;
    boolean required;
    Maybe<String> description = new Maybe<>();
    @EqualsAndHashCode.Include Map<MediaType, Schema> consumes = new TreeMap<>();
  }

  @Value
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  static class Parameter {
    public enum In {
      PATH,
      QUERY,
      BODY
    }

    /**
     * The annotated {@link Method} when originating from a {@link OpenApi.Param}, a {@link
     * java.lang.reflect.Field} or {@link Method} when originating from a property in a {@link
     * OpenApi.Params} type, a {@link java.lang.reflect.Parameter} when originating from a usual
     * endpoint method parameter.
     */
    @ToString.Exclude AnnotatedElement source;

    @EqualsAndHashCode.Include String name;
    @EqualsAndHashCode.Include In in;
    boolean required;
    Schema type;

    @CheckForNull Boolean deprecated;
    @CheckForNull Maturity.Classification maturity;

    /**
     * The default value as JSON.
     *
     * <p>Note that oddly enough the OpenAPI spec does not have a default value for parameters but
     * instead uses a default value for the parameter {@link #type}. This value is therefore passed
     * on during JSON generation.
     */
    Maybe<JsonValue> defaultValue = new Maybe<>();

    Maybe<String> description = new Maybe<>();

    /**
     * In case of a parameter this also refers to the class name containing the parameter, not the
     * name of the field the parameter originates from. If not explicitly given using @{@link
     * OpenApi.Shared} this value is {@code null} during analysis and first decided in the synthesis
     * step.
     */
    Maybe<String> sharedName = new Maybe<>();

    boolean isDeprecated() {
      return Boolean.TRUE == deprecated;
    }

    /**
     * @return true, if this parameter is one or many in a complex parameter object, false, if this
     *     parameter directly occurred individually in the endpoint method signature.
     */
    boolean isShared() {
      return sharedName.isPresent();
    }

    /**
     * @return either the simple parameter name if it is not shared (unique only in the context of
     *     the endpoint) or the globally unique name when shared.
     */
    String getFullName() {
      return isShared() ? sharedName.getValue() + "." + name : name;
    }

    OpenApi.Since getSince() {
      return source.getAnnotation(OpenApi.Since.class);
    }
  }

  @Value
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  static class Response {
    @EqualsAndHashCode.Include HttpStatus status;

    Map<String, Header> headers = new TreeMap<>();

    Maybe<String> description = new Maybe<>();

    @EqualsAndHashCode.Include Map<MediaType, Schema> content = new TreeMap<>();

    Response add(Set<MediaType> produces, Schema body) {
      produces.forEach(mediaType -> content.put(mediaType, body));
      return this;
    }

    Response add(Set<Header> headers) {
      headers.forEach(header -> this.headers.put(header.getName(), header));
      return this;
    }
  }

  @Value
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  static class Header {
    @EqualsAndHashCode.Include String name;

    String description;

    Schema type;
  }

  @Value
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  static class Property {

    @CheckForNull @ToString.Exclude AnnotatedElement source;

    @EqualsAndHashCode.Include String name;

    Boolean required;

    @Nonnull Access access;

    /**
     * OBS! This cannot be included in {@link #toString()} because it might be a circular with the
     * {@link Schema} containing the {@link Property}.
     */
    @ToString.Exclude Schema type;

    Maybe<String> description;

    /**
     * A property in a schema might limit the type visible in the API using annotations. In such
     * cases the {@link #type} will reflect the limited type but the original type is captured here.
     * This cannot be stored in the {@link #type}'s {@link Schema} as that is likely to be a
     * singleton for the specific type. As such it cannot hold information that might differ for
     * each use in a property. For example, many such properties are visible as a {@link
     * IdentifiableObject} only and this would capture the original type from the method signature,
     * like a UserRole.
     */
    @ToString.Exclude Maybe<Schema> originalType = new Maybe<>();

    OpenApi.Since getSince() {
      return source == null ? null : source.getAnnotation(OpenApi.Since.class);
    }

    public Property(
        @CheckForNull AnnotatedElement source, String name, Boolean required, Schema type) {
      this(source, name, required, Access.DEFAULT, type, new Maybe<>());
    }

    Property withType(Schema type) {
      return type == this.type
          ? this
          : new Property(source, name, required, access, type, description);
    }

    Property withAccess(Access access) {
      return access == this.access
          ? this
          : new Property(source, name, required, access, type, description);
    }

    boolean isInput() {
      return access != Access.READ;
    }

    boolean isOutput() {
      return access != Access.WRITE;
    }
  }

  @Value
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode(onlyExplicitlyIncluded = true)
  public static class Schema {

    public static Schema ofAny(java.lang.reflect.Type source) {
      if (source != Object.class) log.warn("OpenAPI failed to analyse the type: " + source);
      return new Schema(Type.ANY, source, Object.class, null).asSingleton();
    }

    public static Schema ofUID(Class<?> of) {
      return new Schema(Type.UID, of, of, null).asSingleton();
    }

    public static Schema ofOneOf(List<Class<?>> types, Function<Class<?>, Schema> toSchema) {
      if (types.size() == 1) {
        return toSchema.apply(types.get(0));
      }
      Schema oneOf = new Schema(Type.ONE_OF, Object.class, Object.class, null);
      types.forEach(
          type ->
              oneOf.addProperty(
                  new Property(null, oneOf.properties.size() + "", null, toSchema.apply(type))));
      return oneOf.sealed();
    }

    public static Schema ofEnum(Class<?> source, Class<?> of, List<String> values) {
      Schema schema = new Schema(Type.ENUM, source, of, null);
      schema.getValues().addAll(values);
      return schema.asSingleton();
    }

    public static Schema ofObject(Class<?> type) {
      return ofObject(type, type);
    }

    public static Schema ofObject(java.lang.reflect.Type source, Class<?> rawType) {
      return new Schema(Type.OBJECT, source, rawType, identifiableAs(rawType));
    }

    public static Schema ofArray(Class<?> type) {
      return ofArray(type, type);
    }

    public static Schema ofArray(java.lang.reflect.Type source, Class<?> rawType) {
      return new Schema(Type.ARRAY, source, rawType, null);
    }

    public static Schema ofSimple(Class<?> type) {
      return new Schema(Type.SIMPLE, type, type, null).asSingleton();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends IdentifiableObject> identifiableAs(Class<?> type) {
      if (type == Period.class || EmbeddedObject.class.isAssignableFrom(type)) return null;
      if (type.isAnnotationPresent(OpenApi.Identifiable.class))
        return type.getAnnotation(OpenApi.Identifiable.class).as();
      if (!IdentifiableObject.class.isAssignableFrom(type)) return null;
      return (Class<? extends IdentifiableObject>) type;
    }

    public enum Type {
      // Note that schemas are generated in the order of types given here
      // each type group being sorted alphabetically by shared name
      ANY,
      SIMPLE,
      ARRAY,
      OBJECT,
      ONE_OF,
      ENUM,
      UID
    }

    @EqualsAndHashCode.Include Type type;

    @ToString.Exclude java.lang.reflect.Type source;

    @ToString.Exclude @EqualsAndHashCode.Include Class<?> rawType;

    /** Which UID type is used to refer to objects of this schema */
    @CheckForNull @ToString.Exclude Class<? extends IdentifiableObject> identifyAs;

    /** Is empty for primitive types */
    @EqualsAndHashCode.Include List<Property> properties = new ArrayList<>();

    /** Marks this schema as a candidate for JVM wide singleton instance for the {@link #source} */
    @With(AccessLevel.PRIVATE)
    AtomicBoolean singleton = new AtomicBoolean(false);

    /** Enum values in case this is an enum schema. */
    List<String> values = new ArrayList<>();

    /** The globally unique name of this is a shared schema. */
    Maybe<String> sharedName = new Maybe<>();

    public enum Direction {
      IN,
      OUT,
      INOUT
    }

    /**
     * A {@link #isBidirectional()} object is of the same structure for input and output whereas a
     * unidirectional object is different for input and output due to the use of references to other
     * identifiable objects.
     *
     * <p>All schemata start out bidirectional and when properties get added via {@link
     * #addProperty(Property)} they might become unidirectional.
     */
    Maybe<Direction> direction = new Maybe<>();

    /**
     * This {@link Schema} but as used for input (that is using "shallow" object with just an ID
     * field for identifiable objects). Only needed for object schema that are not {@link
     * #direction}.
     */
    Maybe<Schema> input = new Maybe<>();

    /** A shared schema gets a kind from {@link OpenApi.Kind} if available */
    Maybe<String> kind = new Maybe<>();

    public boolean isShared() {
      return sharedName.isPresent();
    }

    public boolean isBidirectional() {
      return !direction.isPresent();
    }

    public boolean isSingleton() {
      return singleton.get();
    }

    /**
     * True for object schemas that have an identifier that can reference to a persisted object
     * value.
     */
    public boolean isIdentifiable() {
      return identifyAs != null;
    }

    public boolean isMap() {
      return type == Type.OBJECT
          && properties.size() == 2
          && properties.get(0).name.equals("_keys")
          && properties.get(1).name.equals("_values");
    }

    Set<String> getRequiredProperties() {
      return getProperties().stream()
          .filter(property -> Boolean.TRUE.equals(property.getRequired()))
          .map(Property::getName)
          .collect(toSet());
    }

    Schema getElementType() {
      return getProperties().get(0).getType();
    }

    Api.Schema addProperty(Property property) {
      if (isSingleton()) {
        throw new IllegalStateException("Cannot change a schema once it is marked as singleton");
      }
      properties.add(property);
      Schema schema = property.getType();
      if (!direction.isPresent()
          && (isUnidirectional(schema)
              || schema.getType() == Type.ARRAY
                  && isUnidirectional(schema.getProperties().get(0).getType()))) {
        direction.setValue(Direction.OUT);
      }
      return this;
    }

    private static boolean isUnidirectional(Schema schema) {
      return !schema.isBidirectional()
          || (schema.getType() == Type.OBJECT
              && IdentifiableObject.class.isAssignableFrom(schema.getRawType()));
    }

    Api.Schema withElements(Schema componentType) {
      return addProperty(new Property(null, "components", true, componentType));
    }

    Api.Schema withEntries(Schema keyType, Schema valueType) {
      return addProperty(new Api.Property(null, "_keys", true, keyType))
          .addProperty(new Api.Property(null, "_values", true, valueType));
    }

    private Api.Schema asSingleton() {
      if (source == rawType) singleton.set(true);
      return this;
    }

    /**
     * Called for arrays and objects once all properties have been added to them and the schema
     * declaration is complete. All other schema types can and implicitly do make this evaluation at
     * construction time.
     *
     * @return this type in a finalised state
     */
    Api.Schema sealed() {
      return properties.stream().allMatch(p -> p.type.isSingleton()) ? asSingleton() : this;
    }
  }
}
