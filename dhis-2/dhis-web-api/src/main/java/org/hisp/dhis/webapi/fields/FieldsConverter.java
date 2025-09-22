/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.fields;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.tracker.export.fieldfiltering.Fields;
import org.hisp.dhis.tracker.export.fieldfiltering.FieldsParser;
import org.hisp.dhis.tracker.export.fieldfiltering.SchemaFieldsPresets;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Spring converter that converts field filter strings to {@link Fields}. Works for both
 * {@code @RequestParam} and properties within {@code @ModelAttribute} objects. Uses request context
 * to determine the controller's {@link @OpenApi.EntityType} annotation to fetch the {@link Schema}.
 * The {@link Schema} is needed to expand presets into fields.
 */
@Component
@RequiredArgsConstructor
public class FieldsConverter implements ConditionalGenericConverter {

  private final SchemaService schemaService;
  private final SchemaFieldsPresets schemaFieldsPresets;

  public static final Map<String, Function<Schema, Set<String>>> PRESETS =
      Map.of(
          ":all", FieldsParser.PRESET_ALL,
          ":simple", SchemaFieldsPresets::mapSimple,
          ":identifiable", SchemaFieldsPresets::mapIdentifiable,
          ":nameable", SchemaFieldsPresets::mapNameable,
          ":owner", SchemaFieldsPresets::mapOwner,
          ":persisted", SchemaFieldsPresets::mapPersisted);

  @Override
  public boolean matches(@Nonnull TypeDescriptor sourceType, TypeDescriptor targetType) {
    return Fields.class.equals(targetType.getResolvableType().resolve());
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Set.of(
        new ConvertiblePair(String.class, Fields.class),
        new ConvertiblePair(String[].class, Fields.class));
  }

  @Override
  public Object convert(
      Object source, TypeDescriptor sourceType, @Nonnull TypeDescriptor targetType) {

    String fieldsString;
    if (sourceType.isArray()) {
      /*
       * Undo Spring's splitting of
       * {@code fields=attributes[attribute,value],deleted} into
       * <ul>
       * <li>0 = "attributes[attribute"</li>
       * <li>1 = "value]"</li>
       * <li>2 = "deleted"</li>
       * </ul>
       * separating nested fields attribute and value.
       */
      fieldsString = String.join(",", (String[]) source);
    } else {
      fieldsString = (String) source;
    }

    Class<?> entityClass = getOpenApiEntityType();
    if (entityClass == null) {
      throw new IllegalArgumentException(
          "Cannot convert fields without @OpenApi.EntityType annotation on the controller or method. "
              + "Ensure controller or method has @OpenApi.EntityType annotation and conversion happens within web request context.");
    }
    Schema schema = schemaService.getDynamicSchema(entityClass);
    if (schema == null) {
      throw new IllegalArgumentException(
          "No schema found for entity class "
              + entityClass.getSimpleName()
              + ". Ensure the entity class is properly configured in the schema service.");
    }

    return FieldsParser.parse(fieldsString, schema, schemaFieldsPresets::getSchema, PRESETS);
  }

  @Nullable
  private Class<?> getOpenApiEntityType() {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null) {
      return null;
    }

    Object handler =
        requestAttributes.getAttribute(
            HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    if (handler == null) {
      return null;
    }

    if (handler instanceof org.springframework.web.method.HandlerMethod handlerMethod) {
      OpenApi.EntityType methodEntityType =
          handlerMethod.getMethodAnnotation(OpenApi.EntityType.class);
      if (methodEntityType != null) {
        return methodEntityType.value();
      }

      OpenApi.EntityType classEntityType =
          handlerMethod.getBeanType().getAnnotation(OpenApi.EntityType.class);
      return classEntityType != null ? classEntityType.value() : null;
    } else {
      OpenApi.EntityType entityType = handler.getClass().getAnnotation(OpenApi.EntityType.class);
      return entityType != null ? entityType.value() : null;
    }
  }
}
