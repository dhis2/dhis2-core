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
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.OpenApi;

/**
 * Extracts the properties of "record" like objects.
 *
 * <p>This is based on annotations and some heuristics.
 *
 * @author Jan Bernitt
 */
@Slf4j
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Property {

  private static final Map<Class<?>, Object> INSTANCES_BY_TYPE = new ConcurrentHashMap<>();
  private static final Map<Class<?>, Collection<Property>> PROPERTIES = new ConcurrentHashMap<>();

  @Nonnull String name;
  @Nonnull Type type;
  @Nonnull Member source;
  @CheckForNull Boolean required;
  @CheckForNull Object defaultValue;

  private Property(Field f) {
    this(
        getPropertyName(f),
        getType(f, f.getGenericType()),
        f,
        isRequired(f, f.getType()),
        defaultValue(f));
  }

  private Property(Method m, Field f) {
    this(
        getPropertyName(m),
        getType(m, isSetter(m) ? m.getGenericParameterTypes()[0] : m.getGenericReturnType()),
        m,
        isRequired(m, m.getReturnType()),
        defaultValue(f));
  }

  static Collection<Property> getProperties(Class<?> in) {
    return PROPERTIES.computeIfAbsent(in, Property::propertiesIn);
  }

  private static Collection<Property> propertiesIn(Class<?> object) {
    // map for order by name and avoiding duplicates
    Map<String, Property> properties = new TreeMap<>();
    Consumer<Property> add = property -> properties.putIfAbsent(property.name, property);
    Consumer<Field> addField = field -> add.accept(new Property(field));
    BiConsumer<Method, Field> addMethod =
        (method, field) -> add.accept(new Property(method, field));

    Map<String, Field> propertyFields = new HashMap<>();
    fieldsIn(object).forEach(field -> propertyFields.putIfAbsent(getPropertyName(field), field));
    Set<String> ignoredFields =
        propertyFields.values().stream()
            .filter(f -> f.isAnnotationPresent(OpenApi.Ignore.class))
            .map(Property::getPropertyName)
            .collect(Collectors.toSet());
    propertyFields.values().stream()
        .filter(Property::isProperty)
        .filter(Property::isExplicitlyIncluded)
        .forEach(addField);
    methodsIn(object)
        .filter(method -> Property.isGetter(method) || Property.isSetter(method))
        .filter(Property::isExplicitlyIncluded)
        .filter(method -> !ignoredFields.contains(getPropertyName(method)))
        .forEach(method -> addMethod.accept(method, propertyFields.get(getPropertyName(method))));
    if (properties.isEmpty() || object.isAnnotationPresent(OpenApi.Property.class)) {
      methodsIn(object)
          .filter(Property::isGetter)
          .filter(method -> !ignoredFields.contains(getPropertyName(method)))
          .forEach(method -> addMethod.accept(method, propertyFields.get(getPropertyName(method))));
    }
    return List.copyOf(properties.values());
  }

  private static boolean isProperty(Field source) {
    return !isExplicitlyExcluded(source);
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
        || source.isAnnotationPresent(JsonIgnore.class);
  }

  private static boolean isExplicitlyIncluded(AnnotatedElement source) {
    return source.isAnnotationPresent(JsonProperty.class)
        || source.isAnnotationPresent(OpenApi.Property.class);
  }

  private static Type getType(AnnotatedElement source, Type type) {
    if (source.isAnnotationPresent(OpenApi.Property.class)) {
      OpenApi.Property a = source.getAnnotation(OpenApi.Property.class);
      return a.value().length > 0 ? a.value()[0] : type;
    } else if (type instanceof Class<?>
        && ((Class<?>) type).isAnnotationPresent(OpenApi.Property.class)
        && ((Class<?>) type).getAnnotation(OpenApi.Property.class).value().length > 0) {
      return ((Class<?>) type).getAnnotation(OpenApi.Property.class).value()[0];
    }
    return type;
  }

  private static <T extends Member & AnnotatedElement> String getPropertyName(T member) {
    String name = member.getName();
    if (member instanceof Method) {
      String prop = name.substring(name.startsWith("is") ? 2 : 3);
      name = Character.toLowerCase(prop.charAt(0)) + prop.substring(1);
    }
    OpenApi.Property oap = member.getAnnotation(OpenApi.Property.class);
    String nameOverride = oap == null ? "" : oap.name();
    if (!nameOverride.isEmpty()) {
      return nameOverride;
    }
    JsonProperty property = member.getAnnotation(JsonProperty.class);
    nameOverride = property == null ? "" : property.value();
    return nameOverride.isEmpty() ? name : nameOverride;
  }

  private static <T extends Member & AnnotatedElement> Boolean isRequired(T source, Class<?> type) {
    JsonProperty a = source.getAnnotation(JsonProperty.class);
    if (a != null && a.required()) return true;
    if (a != null && !a.defaultValue().isEmpty()) return false;
    return type.isPrimitive() && type != boolean.class || type.isEnum() ? true : null;
  }

  @SuppressWarnings("java:S3011")
  private static Object defaultValue(Field source) {
    if (source == null) return null;
    OpenApi.Property oapiProperty = source.getAnnotation(OpenApi.Property.class);
    if (oapiProperty != null && !oapiProperty.defaultValue().isEmpty())
      return oapiProperty.defaultValue();
    JsonProperty property = source.getAnnotation(JsonProperty.class);
    if (property != null && !property.defaultValue().isEmpty()) return property.defaultValue();
    try {
      Object obj = source.getDeclaringClass().getConstructor().newInstance();
      source.setAccessible(true);
      return source.get(obj);
    } catch (IllegalAccessException ex) {
      log.warn("Cannot access field value for default value: " + source, ex);
      return null;
    } catch (Exception ex) {
      return null;
    }
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
