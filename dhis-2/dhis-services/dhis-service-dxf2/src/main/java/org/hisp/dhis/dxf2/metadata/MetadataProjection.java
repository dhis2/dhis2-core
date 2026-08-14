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
package org.hisp.dhis.dxf2.metadata;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.translation.Translation;

/**
 * A small, reusable engine that streams a metadata type as JSON from flat SQL projections rather
 * than a hydrated Hibernate entity graph. A {@link Def} declares, for one type, how to load its
 * scalar rows and how to serialise each JSON field (plain column, translated display value,
 * constant, computed value, an id-plucked association array, or a nested array of child objects).
 *
 * <p>The engine bulk-loads a type's scalar rows and every association it needs (each keyed on the
 * parent ids, one flat query per association), then streams the objects in the given id order —
 * never hydrating an entity. It is the "field filtering, executed as SQL projections" primitive:
 * unlike the Gist API it supports nested multi-field object collections, which this and similar
 * bypass-Hibernate reads require.
 *
 * <p>Fields are declared by their <em>ORM property name</em> (e.g. {@code valueType}, {@code
 * categoryCombo}), never a physical column: the engine unions the properties a def's fields read
 * and hands them to a {@link RowLoader}, which resolves the table and columns from the Hibernate
 * mapping. Adding, renaming or recolumn-ing a mapped property therefore needs no change here, and a
 * property that does not exist fails fast rather than silently.
 *
 * @author david mackessy
 */
final class MetadataProjection {

  private MetadataProjection() {}

  static final com.fasterxml.jackson.databind.ObjectMapper JSON_MAPPER =
      JacksonObjectMapperConfig.jsonMapper;

  /** The mapped property (and jsonb column) holding an object's translations. */
  static final String TRANSLATIONS = "translations";

  // --- row --------------------------------------------------------------------------------------

  /** A flat projected row: the primary-key id plus its selected column values by column name. */
  static final class Row {
    private final long id;
    private final Map<String, Object> columns;
    private Set<Translation> translations;

    Row(long id, Map<String, Object> columns) {
      this.id = id;
      this.columns = columns;
    }

    long id() {
      return id;
    }

    Object get(String column) {
      return columns.get(column);
    }

    String str(String column) {
      Object value = columns.get(column);
      return value == null ? null : value.toString();
    }

    String uid() {
      return str("uid");
    }

    Set<Translation> translations() {
      if (translations == null) {
        translations = parseTranslations(str("translations"));
      }
      return translations;
    }
  }

  // --- field definitions ------------------------------------------------------------------------

  /** One serialised JSON field of a projected object. */
  interface Field {
    void write(JsonGenerator gen, Row row, long id, Preparation prep, Locale locale)
        throws IOException;

    /**
     * The scalar entity properties (by ORM property name) this field reads from the row. The engine
     * unions these across a def's fields to build the flat projection's SELECT, so no physical
     * column names appear anywhere — they are resolved from the mapping by the {@link RowLoader}.
     */
    default List<String> properties() {
      return List.of();
    }
  }

  /** A plain property value (String/Boolean/Number/Date). Omitted when null. */
  record Col(String name, String property) implements Field {
    @Override
    public void write(JsonGenerator gen, Row row, long id, Preparation prep, Locale locale)
        throws IOException {
      Object value = row.get(property);
      if (value != null) {
        gen.writeObjectField(name, value);
      }
    }

    @Override
    public List<String> properties() {
      return List.of(property);
    }
  }

  /**
   * A translated display value: the per-locale translation of {@code key}, falling back to the
   * given base property. Omitted when the resolved value is null.
   */
  record Translated(String name, String baseProperty, String key) implements Field {
    @Override
    public void write(JsonGenerator gen, Row row, long id, Preparation prep, Locale locale)
        throws IOException {
      String value = translate(row.translations(), key, row.str(baseProperty), locale);
      if (value != null) {
        gen.writeStringField(name, value);
      }
    }

    @Override
    public List<String> properties() {
      return List.of(baseProperty, TRANSLATIONS);
    }
  }

  /** A constant value, always emitted. */
  record Constant(String name, Object value) implements Field {
    @Override
    public void write(JsonGenerator gen, Row row, long id, Preparation prep, Locale locale)
        throws IOException {
      gen.writeObjectField(name, value);
    }
  }

  /**
   * A value computed from the row (and locale, for derived translated values). Omitted if null. Its
   * {@code deps} declare the ORM properties the function reads, so the engine selects them.
   */
  record Computed(String name, List<String> deps, BiFunction<Row, Locale, Object> fn)
      implements Field {
    @Override
    public void write(JsonGenerator gen, Row row, long id, Preparation prep, Locale locale)
        throws IOException {
      Object value = fn.apply(row, locale);
      if (value != null) {
        gen.writeObjectField(name, value);
      }
    }

    @Override
    public List<String> properties() {
      return deps;
    }
  }

  /** An association projected to an array of child uids (like {@code x~pluck[id]}). */
  record Pluck(String name, Function<long[], Map<Long, List<String>>> loader) implements Field {
    @Override
    public void write(JsonGenerator gen, Row row, long id, Preparation prep, Locale locale)
        throws IOException {
      gen.writeArrayFieldStart(name);
      List<String> uids = prep.plucks.getOrDefault(name, Map.of()).get(id);
      if (uids != null) {
        for (String uid : uids) {
          gen.writeString(uid);
        }
      }
      gen.writeEndArray();
    }
  }

  /** An association projected to an array of nested child objects (like {@code x[a,b,c]}). */
  record Nested(String name, Def childDef, Function<long[], Map<Long, List<Long>>> childIdLoader)
      implements Field {
    @Override
    public void write(JsonGenerator gen, Row row, long id, Preparation prep, Locale locale)
        throws IOException {
      gen.writeArrayFieldStart(name);
      NestedData data = prep.nested.get(name);
      List<Long> childIds = data.childIdsByParent().get(id);
      if (childIds != null) {
        for (Long childId : childIds) {
          writeObject(gen, data.childPrep(), childId, locale);
        }
      }
      gen.writeEndArray();
    }
  }

  /**
   * A many-to-one reference projected to a single nested child object (like {@code x[a,b]}),
   * resolved from a foreign-key property on the parent row (its column is the mapped FK column).
   * Omitted when the foreign key is null.
   */
  record Ref(String name, String fkProperty, Def childDef) implements Field {
    @Override
    public void write(JsonGenerator gen, Row row, long id, Preparation prep, Locale locale)
        throws IOException {
      Long childId = asLong(row.get(fkProperty));
      if (childId != null) {
        gen.writeFieldName(name);
        writeObject(gen, prep.refs.get(name), childId, locale);
      }
    }

    @Override
    public List<String> properties() {
      return List.of(fkProperty);
    }
  }

  /**
   * A scalar value resolved in bulk for the whole id set (rather than from a single column), e.g. a
   * value produced by a domain service. Omitted when the resolved value is null.
   */
  record Bulk(String name, Function<long[], Map<Long, Object>> loader) implements Field {
    @Override
    public void write(JsonGenerator gen, Row row, long id, Preparation prep, Locale locale)
        throws IOException {
      Object value = prep.bulks.get(name).get(id);
      if (value != null) {
        gen.writeObjectField(name, value);
      }
    }
  }

  /**
   * The projection of one metadata type: the entity whose ORM mapping supplies the table and the
   * physical columns for its properties, any derived (joined/computed) columns, and the ordered
   * JSON fields. The scalar SELECT is derived from the fields, so the same property is never
   * declared twice.
   */
  record Def(Class<?> entityType, List<DerivedColumn> derived, List<Field> fields) {
    Def(Class<?> entityType, List<Field> fields) {
      this(entityType, List.of(), fields);
    }
  }

  /**
   * Loads flat rows for an entity type: given the ORM property names (and any derived columns) to
   * project and the primary-key ids, returns rows keyed by id with values under the property names.
   * Implemented over the Hibernate mapping so no physical column names live in the projections.
   */
  interface RowLoader {
    Map<Long, Row> load(
        Class<?> entityType, List<String> properties, List<DerivedColumn> derived, long[] ids);
  }

  /**
   * A scalar column derived from a SQL expression (a correlated sub-select, an existence flag)
   * rather than a mapped property, exposed under {@code alias} in the row. For the rare value that
   * lives in another table or is computed in SQL.
   */
  record DerivedColumn(String alias, String sql) {}

  // --- execution --------------------------------------------------------------------------------

  private record NestedData(Map<Long, List<Long>> childIdsByParent, Preparation childPrep) {}

  /** All data bulk-loaded for a def against a set of ids, ready to stream. */
  private static final class Preparation {
    final Def def;
    final Map<Long, Row> rows;
    final Map<String, Map<Long, List<String>>> plucks = new HashMap<>();
    final Map<String, NestedData> nested = new HashMap<>();
    final Map<String, Preparation> refs = new HashMap<>();
    final Map<String, Map<Long, Object>> bulks = new HashMap<>();

    Preparation(Def def, Map<Long, Row> rows) {
      this.def = def;
      this.rows = rows;
    }
  }

  /** Bulk-loads the rows and every association a def needs, recursing into nested child defs. */
  private static Preparation prepare(Def def, long[] ids, RowLoader loader) {
    Preparation prep =
        new Preparation(
            def, loader.load(def.entityType(), scalarProperties(def), def.derived(), ids));
    for (Field field : def.fields()) {
      if (field instanceof Pluck pluck) {
        prep.plucks.put(pluck.name(), pluck.loader().apply(ids));
      } else if (field instanceof Nested nested) {
        Map<Long, List<Long>> childIdsByParent = nested.childIdLoader().apply(ids);
        long[] childIds =
            childIdsByParent.values().stream()
                .flatMap(List::stream)
                .mapToLong(Long::longValue)
                .distinct()
                .toArray();
        prep.nested.put(
            nested.name(),
            new NestedData(childIdsByParent, prepare(nested.childDef(), childIds, loader)));
      } else if (field instanceof Ref ref) {
        prep.refs.put(
            ref.name(), prepare(ref.childDef(), foreignKeys(prep.rows, ref.fkProperty()), loader));
      } else if (field instanceof Bulk bulk) {
        prep.bulks.put(bulk.name(), bulk.loader().apply(ids));
      }
    }
    return prep;
  }

  /** The distinct scalar properties every field in the def reads, in declaration order. */
  private static List<String> scalarProperties(Def def) {
    LinkedHashSet<String> properties = new LinkedHashSet<>();
    for (Field field : def.fields()) {
      properties.addAll(field.properties());
    }
    return new ArrayList<>(properties);
  }

  /** The distinct non-null foreign-key ids held in {@code property} across the given rows. */
  private static long[] foreignKeys(Map<Long, Row> rows, String property) {
    LinkedHashSet<Long> ids = new LinkedHashSet<>();
    for (Row row : rows.values()) {
      Long fk = asLong(row.get(property));
      if (fk != null) {
        ids.add(fk);
      }
    }
    long[] array = new long[ids.size()];
    int i = 0;
    for (Long id : ids) {
      array[i++] = id;
    }
    return array;
  }

  private static Long asLong(Object value) {
    return value == null ? null : ((Number) value).longValue();
  }

  /** Writes the given ids as JSON objects (no surrounding array) using the def. */
  static void writeObjects(
      JsonGenerator gen, Def def, long[] orderedIds, Locale locale, RowLoader loader)
      throws IOException {
    Preparation prep = prepare(def, orderedIds, loader);
    for (long id : orderedIds) {
      writeObject(gen, prep, id, locale);
    }
  }

  private static void writeObject(JsonGenerator gen, Preparation prep, long id, Locale locale)
      throws IOException {
    Row row = prep.rows.get(id);
    gen.writeStartObject();
    for (Field field : prep.def.fields()) {
      field.write(gen, row, id, prep, locale);
    }
    gen.writeEndObject();
  }

  // --- translation (shared with the entity path's BaseIdentifiableObject#getTranslation) --------

  static Set<Translation> parseTranslations(String json) {
    if (json == null || json.isEmpty() || "[]".equals(json) || "{}".equals(json)) {
      return Set.of();
    }
    try {
      return JSON_MAPPER.readValue(json, new TypeReference<Set<Translation>>() {});
    } catch (IOException e) {
      return Set.of();
    }
  }

  static String translate(
      Set<Translation> translations, String key, String defaultValue, Locale locale) {
    if (locale == null || translations.isEmpty()) {
      return defaultValue;
    }
    for (Translation t : translations) {
      if (locale.equals(t.getLocale())
          && key.equalsIgnoreCase(t.getProperty())
          && t.getValue() != null
          && !t.getValue().isEmpty()) {
        return t.getValue();
      }
    }
    return defaultValue;
  }
}
