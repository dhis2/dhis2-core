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
package org.hisp.dhis.tracker.export.fieldfiltering;

import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Component;

/** {@link Schema} aware implementations of {@link org.hisp.dhis.fieldfiltering.FieldPreset}s. */
@RequiredArgsConstructor
@Component
public class SchemaFieldsPresets {
  private final SchemaService schemaService;

  @Nonnull
  public static Set<String> mapSimple(@Nonnull Schema schema) {
    return schema.getProperties().stream()
        .filter(p -> p.getPropertyType().isSimple())
        .map(SchemaFieldsPresets::toFieldName)
        .collect(Collectors.toSet());
  }

  @Nonnull
  public static Set<String> mapIdentifiable(@Nonnull Schema schema) {
    Set<String> identifiableFields =
        Set.of("id", "name", "code", "created", "lastUpdated", "lastUpdatedBy");
    return schema.getProperties().stream()
        .filter(p -> identifiableFields.contains(p.getName()))
        .map(SchemaFieldsPresets::toFieldName)
        .collect(Collectors.toSet());
  }

  @Nonnull
  public static Set<String> mapNameable(@Nonnull Schema schema) {
    Set<String> nameableFields =
        Set.of("id", "name", "shortName", "description", "code", "created", "lastUpdated");
    return schema.getProperties().stream()
        .filter(p -> nameableFields.contains(p.getName()))
        .map(SchemaFieldsPresets::toFieldName)
        .collect(Collectors.toSet());
  }

  @Nonnull
  public static Set<String> mapOwner(@Nonnull Schema schema) {
    return schema.getProperties().stream()
        .filter(Property::isOwner)
        .map(SchemaFieldsPresets::toFieldName)
        .collect(Collectors.toSet());
  }

  @Nonnull
  public static Set<String> mapPersisted(@Nonnull Schema schema) {
    return schema.getProperties().stream()
        .filter(Property::isPersisted)
        .map(SchemaFieldsPresets::toFieldName)
        .collect(Collectors.toSet());
  }

  @Nullable
  public Schema getSchema(@Nonnull Schema schema, @Nonnull String field) {
    Property property = schema.getProperty(field);

    if (property == null) {
      return null; // invalid field
    }

    if (property.isCollection()) {
      return schemaService.getDynamicSchema(property.getItemKlass());
    }
    return schemaService.getDynamicSchema(property.getKlass());
  }

  private static String toFieldName(Property property) {
    return property.isCollection() ? property.getCollectionName() : property.getName();
  }
}
