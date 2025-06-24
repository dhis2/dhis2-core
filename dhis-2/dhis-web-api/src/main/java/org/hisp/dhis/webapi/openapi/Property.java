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

import static java.lang.Character.isUpperCase;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Access;
import org.hisp.dhis.jsontree.Json;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.setting.Settings;

/**
 * Extracts the properties of "record" like objects.
 *
 * <p>This is based on annotations and some heuristics.
 *
 * @author Jan Bernitt
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Property {

  private static final Map<Class<?>, List<Property>> PROPERTIES = new ConcurrentHashMap<>();

  @Nonnull Class<?> declaringClass;
  @Nonnull String name;
  @Nonnull Type type;
  @Nonnull AnnotatedElement source;
  @Nonnull Access access;
  @CheckForNull Boolean required;
  @CheckForNull JsonValue defaultValue;

  private Property(Field f) {
    this(
        f.getDeclaringClass(),
        getPropertyName(f),
        getType(f, f.getGenericType()),
        f,
        getAccess(f),
        isRequired(f, f.getType()),
        defaultValue(f));
  }

  private Property(Method m, Field f) {
    this(
        m.getDeclaringClass(),
        getPropertyName(m),
        getType(m, isSetter(m) ? m.getGenericParameterTypes()[0] : m.getGenericReturnType()),
        m,
        getAccess(m),
        isRequired(m, m.getReturnType()),
        defaultValue(f));
  }

  private Property(JsonObject.Property p, Type type) {
    this(
        p.in(),
        p.jsonName(),
        getType(p.source(), type),
        p.source(),
        Access.DEFAULT,
        isRequired(p.source(), p.javaType().getType()),
        defaultValueFromAnnotation(p.source()));
  }

  static List<Property> getProperties(Class<?> in) {
    return PROPERTIES.computeIfAbsent(in, Property::propertiesIn);
  }

  private static List<Property> propertiesIn(Class<?> object) {
    if (JsonObject.class.isAssignableFrom(object)) return propertiesInJson(object);
    if (JsonValue.class.isAssignableFrom(object)) return List.of();
    // map for order by name and avoiding duplicates
    Map<String, Property> properties = new TreeMap<>();
    Consumer<Property> add = property -> properties.putIfAbsent(property.name, property);
    Consumer<Field> addField = field -> add.accept(new Property(field));
    BiConsumer<Method, Field> addMethod =
        (method, field) -> add.accept(new Property(method, field));

    boolean includeByDefault = isIncludeAllByDefault(object);
    Map<String, Field> fieldsByJavaPropertyName = new HashMap<>();
    fieldsIn(object)
        .forEach(field -> fieldsByJavaPropertyName.putIfAbsent(getJavaPropertyName(field), field));
    Set<String> ignoredFields =
        fieldsByJavaPropertyName.values().stream()
            .filter(f -> f.isAnnotationPresent(OpenApi.Ignore.class))
            .map(Property::getJavaPropertyName)
            .collect(Collectors.toSet());
    fieldsByJavaPropertyName.values().stream()
        .filter(Property::isProperty)
        .filter(f -> includeByDefault || isExplicitlyIncluded(f))
        .forEach(addField);
    methodsIn(object)
        .filter(Property::isExplicitlyIncluded)
        .filter(Property::isAccessor)
        .filter(method -> !ignoredFields.contains(getJavaPropertyName(method)))
        .forEach(
            method ->
                addMethod.accept(
                    method, fieldsByJavaPropertyName.get(getJavaPropertyName(method))));
    if (properties.isEmpty() || includeByDefault) {
      methodsIn(object)
          .filter(Property::isAccessor)
          .filter(method -> !ignoredFields.contains(getJavaPropertyName(method)))
          .forEach(
              method ->
                  addMethod.accept(
                      method, fieldsByJavaPropertyName.get(getJavaPropertyName(method))));
    }
    return List.copyOf(properties.values());
  }

  /**
   * When {@link JsonProperty} is present all are just included by default if the type is annotated
   * with {@link OpenApi.Property}. Otherwise, if any field or method is annotated {@link
   * OpenApi.Property} (but none has {@link JsonProperty}) this also includes all by default.
   */
  private static boolean isIncludeAllByDefault(Class<?> object) {
    if (object.isAnnotationPresent(OpenApi.Property.class)) return true;
    if (fieldsIn(object).anyMatch(f -> f.isAnnotationPresent(JsonProperty.class))) return false;
    if (methodsIn(object).anyMatch(m -> m.isAnnotationPresent(JsonProperty.class))) return false;
    return fieldsIn(object).anyMatch(f -> f.isAnnotationPresent(OpenApi.Property.class))
        || methodsIn(object).anyMatch(m -> m.isAnnotationPresent(OpenApi.Property.class));
  }

  @SuppressWarnings("unchecked")
  private static List<Property> propertiesInJson(Class<?> object) {
    if (!JsonObject.class.isAssignableFrom(object)) return List.of();
    List<Property> res = new ArrayList<>();
    Map<String, List<JsonObject.Property>> propertiesByName =
        JsonObject.properties((Class<? extends JsonObject>) object).stream()
            .collect(Collectors.groupingBy(JsonObject.Property::jsonName));
    propertiesByName.forEach(
        (name, properties) -> {
          JsonObject.Property p0 = properties.get(0);
          // for 1:1 Java:JSON we can use the java type,
          // otherwise use the JSON type (which should be the same for all usages)
          Type type = properties.size() == 1 ? p0.javaType().getType() : p0.jsonType();
          res.add(new Property(p0, type));
        });
    return List.copyOf(res);
  }

  private static boolean isProperty(Field source) {
    return !isExplicitlyExcluded(source)
        && !source.getType().isAnnotationPresent(OpenApi.Ignore.class);
  }

  private static boolean isAccessor(Method source) {
    return isGetter(source) || isSetter(source);
  }

  private static boolean isSetter(Method source) {
    String name = source.getName();
    return !isExplicitlyExcluded(source)
        && source.getParameterCount() == 1
        && name.startsWith("set")
        && name.length() > 3
        && isUpperCase(name.charAt(3));
  }

  private static boolean isGetter(Method source) {
    String name = source.getName();
    return !isExplicitlyExcluded(source)
        && !source.getReturnType().isAnnotationPresent(OpenApi.Ignore.class)
        && source.getParameterCount() == 0
        && source.getReturnType() != void.class
        && Stream.of("is", "has", "get").anyMatch(prefix -> isGetterPrefix(name, prefix));
  }

  private static boolean isGetterPrefix(String name, String prefix) {
    return name.startsWith(prefix)
        && name.length() > prefix.length()
        && isUpperCase(name.charAt(prefix.length()));
  }

  private static <T extends Member & AnnotatedElement> boolean isExplicitlyExcluded(T source) {
    return source.isSynthetic()
        || isStatic(source.getModifiers())
        || source.isAnnotationPresent(OpenApi.Ignore.class)
        || (source.isAnnotationPresent(JsonIgnore.class)
            && !source.isAnnotationPresent(OpenApi.Property.class));
  }

  private static boolean isExplicitlyIncluded(AnnotatedElement source) {
    return source.isAnnotationPresent(JsonProperty.class)
        || source.isAnnotationPresent(OpenApi.Property.class)
        || source.isAnnotationPresent(OpenApi.Description.class);
  }

  private static Type getType(AnnotatedElement source, Type type) {
    if (source.isAnnotationPresent(OpenApi.Property.class)) {
      OpenApi.Property a = source.getAnnotation(OpenApi.Property.class);
      return a.value().length > 0 ? a.value()[0] : type;
    }
    if (type instanceof Class<?>
        && ((Class<?>) type).isAnnotationPresent(OpenApi.Property.class)
        && ((Class<?>) type).getAnnotation(OpenApi.Property.class).value().length > 0) {
      // Note: this does not support oneOf directly but the annotation is checked again
      // then properties are converted to an Api.Schema
      return ((Class<?>) type).getAnnotation(OpenApi.Property.class).value()[0];
    }
    return type;
  }

  private static <T extends Member & AnnotatedElement> String getPropertyName(T member) {
    OpenApi.Property oap = member.getAnnotation(OpenApi.Property.class);
    String nameOverride = oap == null ? "" : oap.name();
    if (!nameOverride.isEmpty()) return nameOverride;
    JsonProperty property = member.getAnnotation(JsonProperty.class);
    nameOverride = property == null ? "" : property.value();
    if (!nameOverride.isEmpty()) return nameOverride;
    String name = getJavaPropertyName(member);
    if (Settings.class.isAssignableFrom(member.getDeclaringClass())) {
      // for now, we do this extra resolve step hard coded as it is complicated to add as a general
      // feature
      name = Settings.getKey(name);
    }
    return name;
  }

  private static <T extends Member & AnnotatedElement> String getJavaPropertyName(T member) {
    String name = member.getName();
    if (member instanceof Method) {
      String prop = name.substring(name.startsWith("is") ? 2 : 3);
      name = Character.toLowerCase(prop.charAt(0)) + prop.substring(1);
    }
    return name;
  }

  @CheckForNull
  private static Boolean isRequired(AnnotatedElement source, Type type) {
    JsonProperty a = source.getAnnotation(JsonProperty.class);
    if (a != null && a.required()) return true;
    if (a != null && !a.defaultValue().isEmpty()) return false;
    OpenApi.Property a2 = source.getAnnotation(OpenApi.Property.class);
    if (a2 != null && a2.required()) return true;
    if (a2 != null && !a2.defaultValue().isEmpty()) return false;
    if (source.isAnnotationPresent(Nonnull.class)) return true;
    if (type instanceof Class<?> cls)
      return cls.isPrimitive() && cls != boolean.class || cls.isEnum() ? true : null;
    return null;
  }

  @Nonnull
  private static Access getAccess(AnnotatedElement source) {
    OpenApi.Property primary = source.getAnnotation(OpenApi.Property.class);
    if (primary != null && primary.access() != Access.DEFAULT) return primary.access();
    JsonProperty secondary = source.getAnnotation(JsonProperty.class);
    if (secondary == null) return Access.DEFAULT;
    return switch (secondary.access()) {
      case READ_ONLY -> Access.READ;
      case WRITE_ONLY -> Access.WRITE;
      case READ_WRITE -> Access.READ_WRITE;
      case AUTO -> Access.DEFAULT;
    };
  }

  @SuppressWarnings("java:S3011")
  private static JsonValue defaultValue(Field source) {
    JsonValue defaultValue = defaultValueFromAnnotation(source);
    if (defaultValue != null) return defaultValue;
    try {
      Object obj = source.getDeclaringClass().getConstructor().newInstance();
      source.setAccessible(true);
      return toJson(source.get(obj));
    } catch (Exception ex) {
      return null;
    }
  }

  private static JsonValue defaultValueFromAnnotation(AnnotatedElement source) {
    if (source == null) return Json.ofNull();
    OpenApi.Property oapiProperty = source.getAnnotation(OpenApi.Property.class);
    if (oapiProperty != null && !oapiProperty.defaultValue().isEmpty())
      return JsonValue.of(oapiProperty.defaultValue());
    JsonProperty property = source.getAnnotation(JsonProperty.class);
    if (property != null && !property.defaultValue().isEmpty())
      return JsonValue.of(property.defaultValue());
    return null;
  }

  @SuppressWarnings("unchecked")
  private static JsonValue toJson(Object value) {
    if (value == null) return Json.ofNull();
    if (value instanceof JsonValue v) return v;
    if (value instanceof Number n) return Json.of(n);
    if (value instanceof Boolean b) return Json.of(b);
    if (value instanceof String s) return Json.of(s);
    if (value instanceof Object[] arr) return Json.array(Property::toJson, arr);
    if (value instanceof Collection<?> c) return Json.array(Property::toJson, c);
    if (value instanceof Map<?, ?> map) return Json.object(Property::toJson, (Map<String, ?>) map);
    return Json.of(value.toString()); // assume it is some string wrapper type
  }

  private static Stream<Field> fieldsIn(Class<?> type) {
    if (type.isInterface() || type.isArray() || type.isEnum() || type.isPrimitive()) {
      return Stream.empty();
    }
    Stream<Field> fields = stream(type.getDeclaredFields());
    Class<?> parent = type.getSuperclass();
    return parent == null || parent == Object.class
        ? fields
        : Stream.concat(fields, fieldsIn(parent));
  }

  private static Stream<Method> methodsIn(Class<?> type) {
    return stream(type.getMethods()).filter(m -> m.getDeclaringClass() != Object.class);
  }
}
