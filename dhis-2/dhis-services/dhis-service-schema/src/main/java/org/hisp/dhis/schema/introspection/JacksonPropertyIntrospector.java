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
package org.hisp.dhis.schema.introspection;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.hisp.dhis.system.util.AnnotationUtils.getAnnotation;
import static org.hisp.dhis.system.util.AnnotationUtils.isAnnotationPresent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Primitives;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

/**
 * A {@link PropertyIntrospector} that adds or retains those {@link Property} values in the map for
 * which there is a {@link JsonProperty} annotation available of the getter method (no argument
 * method).
 *
 * <p>It adds information to these properties from the following annotations:
 *
 * <ul>
 *   <li>{@link JsonProperty}
 *   <li>{@link JacksonXmlProperty}
 *   <li>{@link JacksonXmlRootElement}
 *   <li>{@link JacksonXmlElementWrapper}
 *   <li>{@link Description}
 * </ul>
 *
 * {@link Property}s already contained in the provided map will be assumed to be persisted {@link
 * Property}s.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com> (original author)
 * @author Jan Bernitt (extraction to this class)
 */
public class JacksonPropertyIntrospector implements PropertyIntrospector {
  @Override
  public void introspect(Class<?> klass, Map<String, Property> properties) {
    Map<String, Property> persistedProperties = new HashMap<>(properties);
    properties.clear();
    Set<String> classFieldNames = ReflectionUtils.getAllFieldNames(klass);

    // TODO this is quite nasty, should find a better way of exposing
    // properties at class-level
    if (isAnnotationPresent(klass, JacksonXmlRootElement.class)
        || isAnnotationPresent(klass, JsonRootName.class)) {
      properties.put(SchemaService.PROPERTY_SCHEMA, createSchemaProperty(klass));
    }

    for (Property property : collectProperties(klass)) {
      String fieldName = initFromJsonProperty(property);

      if (classFieldNames.contains(fieldName)) {
        property.setFieldName(fieldName);
      }

      if (persistedProperties.containsKey(fieldName)) {
        initFromPersistedProperty(property, persistedProperties.get(fieldName));
      }

      initFromDescription(property);
      initFromJacksonXmlProperty(property);
      initCollectionProperty(property);

      Method getterMethod = property.getGetterMethod();

      if (getterMethod != null
          && !property.isCollection()
          && isSimple(getterMethod.getReturnType())) {
        property.setSimple(true);
      }

      initFromJacksonXmlElementWrapper(property);
      initFromEnumConstants(property);

      properties.put(property.key(), property);
    }
  }

  private static boolean isSimple(Class<?> type) {
    return Primitives.allPrimitiveTypes().contains(type)
        || Primitives.allWrapperTypes().contains(type)
        || String.class.isAssignableFrom(type)
        || Enum.class.isAssignableFrom(type)
        || Date.class.isAssignableFrom(type);
  }

  private static void initFromDescription(Property property) {
    Description description = property.getAnnotation(Description.class);

    if (description != null) {
      property.setDescription(description.value());
    }
  }

  private static String initFromJsonProperty(Property property) {
    Method getter = property.getGetterMethod();

    if (getter == null) {
      return property.getFieldName();
    }

    property.setKlass(Primitives.wrap(getter.getReturnType()));
    property.setReadable(true);

    if (property.getSetterMethod() != null) {
      property.setWritable(true);
    }

    return property.getFieldName();
  }

  private static void initFromJacksonXmlElementWrapper(Property property) {
    JacksonXmlElementWrapper jacksonXmlElementWrapper =
        property.getAnnotation(JacksonXmlElementWrapper.class);

    if (!property.isCollection() || jacksonXmlElementWrapper == null) {
      return;
    }

    property.setCollectionWrapping(jacksonXmlElementWrapper.useWrapping());

    if (!isEmpty(jacksonXmlElementWrapper.localName())) {
      property.setCollectionName(jacksonXmlElementWrapper.localName());
    }
  }

  private static void initFromJacksonXmlProperty(Property property) {
    JacksonXmlProperty jacksonXmlProperty = property.getAnnotation(JacksonXmlProperty.class);

    if (jacksonXmlProperty == null) {
      return;
    }

    if (isEmpty(jacksonXmlProperty.localName())) {
      property.setName(property.getName());
    } else {
      property.setName(jacksonXmlProperty.localName());
    }

    if (!isEmpty(jacksonXmlProperty.namespace())) {
      property.setNamespace(jacksonXmlProperty.namespace());
    }

    property.setAttribute(jacksonXmlProperty.isAttribute());
  }

  private static void initFromEnumConstants(Property property) {
    if (!Enum.class.isAssignableFrom(property.getKlass())) {
      return;
    }

    Object[] enumConstants = property.getKlass().getEnumConstants();
    List<String> enumValues = new ArrayList<>();

    for (Object value : enumConstants) {
      enumValues.add(value.toString());
    }

    property.setConstants(enumValues);
  }

  private static void initFromPersistedProperty(Property property, Property persisted) {
    property.setPersisted(true);
    property.setWritable(true);
    property.setFieldName(persisted.getFieldName());
    property.setUnique(persisted.isUnique());
    property.setRequired(persisted.isRequired());
    property.setLength(persisted.getLength());
    property.setMax(persisted.getMax());
    property.setMin(persisted.getMin());
    property.setCollection(persisted.isCollection());
    property.setCascade(persisted.getCascade());
    property.setOwner(persisted.isOwner());
    property.setManyToMany(persisted.isManyToMany());
    property.setOneToMany(persisted.isOneToMany());
    property.setOneToOne(persisted.isOneToOne());
    property.setManyToOne(persisted.isManyToOne());
    property.setOwningRole(persisted.getOwningRole());
    property.setInverseRole(persisted.getInverseRole());
  }

  private static void initCollectionProperty(Property property) {
    Method getterMethod = property.getGetterMethod();

    if (getterMethod == null) {
      return;
    }

    Class<?> returnType = getterMethod.getReturnType();

    if (!Collection.class.isAssignableFrom(returnType)) {
      property.setCollection(false);
      return;
    }

    property.setCollection(true);
    property.setCollectionName(property.getName());
    property.setOrdered(List.class.isAssignableFrom(returnType));

    Type type = getterMethod.getGenericReturnType();

    if (type instanceof ParameterizedType) {
      Class<?> klass = (Class<?>) ReflectionUtils.getInnerType((ParameterizedType) type);
      property.setItemKlass(Primitives.wrap(klass));

      if (isSimple(klass)) {
        property.setSimple(true);
      }

      property.setIdentifiableObject(IdentifiableObject.class.isAssignableFrom(klass));
      property.setNameableObject(NameableObject.class.isAssignableFrom(klass));
      property.setEmbeddedObject(EmbeddedObject.class.isAssignableFrom(klass));
      property.setAnalyticalObject(AnalyticalObject.class.isAssignableFrom(klass));
    }
  }

  private static Property createSchemaProperty(Class<?> klass) {
    Property schemaProperty = new Property();
    schemaProperty.setAnnotations(getAnnotations(klass.getAnnotations()));

    if (isAnnotationPresent(klass, JsonRootName.class)) {
      JsonRootName jsonRootName = getAnnotation(klass, JsonRootName.class);

      if (!isEmpty(jsonRootName.value())) {
        schemaProperty.setName(jsonRootName.value());
      }

      if (!isEmpty(jsonRootName.namespace())) {
        schemaProperty.setNamespace(jsonRootName.namespace());
      }
    } else if (isAnnotationPresent(klass, JacksonXmlRootElement.class)) {
      JacksonXmlRootElement jacksonXmlRootElement =
          getAnnotation(klass, JacksonXmlRootElement.class);

      if (!isEmpty(jacksonXmlRootElement.localName())) {
        schemaProperty.setName(jacksonXmlRootElement.localName());
      }

      if (!isEmpty(jacksonXmlRootElement.namespace())) {
        schemaProperty.setNamespace(jacksonXmlRootElement.namespace());
      }
    }

    return schemaProperty;
  }

  private static List<Property> collectProperties(Class<?> klass) {
    boolean isPrimitiveOrWrapped = ClassUtils.isPrimitiveOrWrapper(klass);

    if (isPrimitiveOrWrapped) {
      return Collections.emptyList();
    }

    List<Field> fields =
        ReflectionUtils.findFields(klass, f -> f.isAnnotationPresent(JsonProperty.class));

    List<Method> methods =
        ReflectionUtils.findMethods(
            klass,
            m ->
                AnnotationUtils.findAnnotation(m, JsonProperty.class) != null
                    && m.getParameterTypes().length == 0);

    Multimap<String, Method> multimap = ReflectionUtils.getMethodsMultimap(klass);

    Map<String, Property> propertyMap = new HashMap<>();

    for (var field : fields) {
      Property property = new Property(klass, null, null);
      property.setAnnotations(getAnnotations(field.getAnnotations()));

      JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);

      String fieldName = field.getName();
      String name =
          StringUtils.isEmpty(requireNonNull(jsonProperty).value())
              ? fieldName
              : jsonProperty.value();

      property.setName(name);
      property.setFieldName(fieldName);
      property.setSetterMethod(ReflectionUtils.findSetterMethod(fieldName, klass));
      property.setGetterMethod(ReflectionUtils.findGetterMethod(fieldName, klass));
      property.setNamespace(trimToNull(jsonProperty.namespace()));

      propertyMap.put(name, property);
    }

    for (var method : methods) {
      JsonProperty jsonProperty = AnnotationUtils.findAnnotation(method, JsonProperty.class);

      String fieldName = ReflectionUtils.getFieldName(method);
      String name =
          StringUtils.isEmpty(requireNonNull(jsonProperty).value())
              ? fieldName
              : jsonProperty.value();

      if (propertyMap.containsKey(name)) {
        continue;
      }

      Property property = new Property(klass, method, null);
      property.setAnnotations(getAnnotations(method.getAnnotations()));

      property.setName(name);
      property.setFieldName(fieldName);
      property.setNamespace(trimToNull(jsonProperty.namespace()));

      propertyMap.put(name, property);

      String setterName = "set" + capitalize(fieldName);

      if (multimap.containsKey(setterName)) {
        property.setSetterMethod(multimap.get(setterName).iterator().next());
      }

      propertyMap.put(name, property);
    }

    return new ArrayList<>(propertyMap.values());
  }

  private static Map<Class<? extends Annotation>, Annotation> getAnnotations(
      Annotation[] annotations) {
    return Arrays.stream(annotations)
        .collect(Collectors.toMap(Annotation::annotationType, Function.identity()));
  }
}
