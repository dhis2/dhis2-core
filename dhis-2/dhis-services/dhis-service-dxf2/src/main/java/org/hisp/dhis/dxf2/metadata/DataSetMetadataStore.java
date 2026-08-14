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

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Component;

/**
 * Read-only flat-projection store backing {@link MetadataProjection}. It exposes three generic,
 * type-agnostic loaders — a scalar row loader and two association loaders — each issuing a single
 * flat SQL query keyed on a bounded id set bound as one {@code bigint[]} array parameter. It never
 * hydrates managed entities, so neither the per-parent N+1 selects nor the memory cost of the
 * entity graph applies, and the query/parameter count does not grow with the size of the graph.
 *
 * <p>The scalar loader is driven by <em>ORM property names</em>, not physical columns: it resolves
 * the table and each property's column from the Hibernate mapping (via the entity persister), so a
 * projection never hard-codes a column name and a mapping change is picked up automatically.
 *
 * @author david mackessy
 */
@Component
@RequiredArgsConstructor
class DataSetMetadataStore {

  private final JdbcTemplate jdbcTemplate;

  private final EntityManagerFactory entityManagerFactory;

  private final Map<Class<?>, AbstractEntityPersister> persisters = new ConcurrentHashMap<>();

  /**
   * Loads the given ORM properties of {@code entityType} for the given primary-key ids, as {@link
   * MetadataProjection.Row}s keyed by id. The table and each property's physical column are
   * resolved from the Hibernate mapping; each value lands in the row under its property name.
   * {@code jsonb} columns (e.g. {@code translations}) are read as-is; everything via {@code
   * getObject}. Each {@link MetadataProjection.DerivedColumn} is additionally selected under its
   * alias, for the rare value that lives in another table or is computed in SQL.
   */
  Map<Long, MetadataProjection.Row> loadRows(
      Class<?> entityType,
      List<String> properties,
      List<MetadataProjection.DerivedColumn> derived,
      long[] ids) {
    if (ids.length == 0) {
      return Map.of();
    }
    AbstractEntityPersister persister = persister(entityType);
    String table = persister.getTableName();
    String idColumn = persister.getIdentifierColumnNames()[0];

    // the projected table is aliased t so derived sub-selects can correlate on t.<column>
    // independently of the physical table name resolved from the mapping
    List<String> selections = new ArrayList<>();
    for (String property : properties) {
      selections.add("t.%s as \"%s\"".formatted(column(persister, property), property));
    }
    for (MetadataProjection.DerivedColumn column : derived) {
      selections.add("(%s) as \"%s\"".formatted(column.sql(), column.alias()));
    }
    String sql =
        "select t.%s%s from %s t where t.%s = any(?) order by t.%s"
            .formatted(
                idColumn,
                selections.isEmpty() ? "" : ", " + String.join(", ", selections),
                table,
                idColumn,
                idColumn);
    return jdbcTemplate.query(
        sql,
        bind(ids),
        rs -> {
          Map<Long, MetadataProjection.Row> rows = new LinkedHashMap<>();
          while (rs.next()) {
            long id = rs.getLong(idColumn);
            Map<String, Object> values = new HashMap<>();
            for (String property : properties) {
              values.put(property, rs.getObject(property));
            }
            for (MetadataProjection.DerivedColumn column : derived) {
              values.put(column.alias(), rs.getObject(column.alias()));
            }
            rows.put(id, new MetadataProjection.Row(id, values));
          }
          return rows;
        });
  }

  /** The Hibernate persister for an entity type, cached; the mapping's single source of columns. */
  private AbstractEntityPersister persister(Class<?> entityType) {
    return persisters.computeIfAbsent(
        entityType,
        type -> {
          MetamodelImplementor metamodel =
              (MetamodelImplementor) entityManagerFactory.getMetamodel();
          return (AbstractEntityPersister) metamodel.entityPersister(type);
        });
  }

  /**
   * The single physical column mapped to {@code property}, failing fast if it is not one column.
   */
  private static String column(AbstractEntityPersister persister, String property) {
    String[] columns = persister.getPropertyColumnNames(property);
    if (columns.length != 1) {
      throw new IllegalStateException(
          "expected exactly one column for property '%s' of %s, got %s"
              .formatted(property, persister.getEntityName(), Arrays.toString(columns)));
    }
    return columns[0];
  }

  /**
   * Runs a query selecting {@code (parentId, childUid)} and returns child uids grouped by parent
   * id, preserving the query's {@code ORDER BY}. Used for id-plucked association arrays.
   */
  Map<Long, List<String>> uidLists(String sql, long[] ids) {
    if (ids.length == 0) {
      return Map.of();
    }
    return jdbcTemplate.query(
        sql,
        bind(ids),
        rs -> {
          Map<Long, List<String>> byParent = new LinkedHashMap<>();
          while (rs.next()) {
            byParent.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getString(2));
          }
          return byParent;
        });
  }

  /**
   * Runs a query selecting {@code (parentId, childId)} and returns child ids grouped by parent id,
   * preserving the query's {@code ORDER BY}. Used to link a parent to its nested child objects.
   */
  Map<Long, List<Long>> idLists(String sql, long[] ids) {
    if (ids.length == 0) {
      return Map.of();
    }
    return jdbcTemplate.query(
        sql,
        bind(ids),
        rs -> {
          Map<Long, List<Long>> byParent = new LinkedHashMap<>();
          while (rs.next()) {
            byParent.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getLong(2));
          }
          return byParent;
        });
  }

  private static PreparedStatementSetter bind(long[] ids) {
    return ps -> ps.setArray(1, ps.getConnection().createArrayOf("bigint", box(ids)));
  }

  private static Long[] box(long[] ids) {
    Long[] boxed = new Long[ids.length];
    for (int i = 0; i < ids.length; i++) {
      boxed[i] = ids[i];
    }
    return boxed;
  }

  static long[] toArray(Collection<Long> ids) {
    long[] array = new long[ids.size()];
    int i = 0;
    for (Long id : ids) {
      array[i++] = id;
    }
    return array;
  }
}
