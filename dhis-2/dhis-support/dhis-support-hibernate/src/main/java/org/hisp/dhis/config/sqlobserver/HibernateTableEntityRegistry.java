/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.config.sqlobserver;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Maps SQL table names to the Hibernate entity classes whose ETag versions a write to that table
 * must bump: the entity mapped to the table, or for a collection (join) table such as {@code
 * usergroupmembers} the owners of every collection stored in it. Populated lazily to break the
 * circular dependency between DataSource and EntityManagerFactory.
 *
 * @author Morten Svanæs
 */
@Slf4j
public class HibernateTableEntityRegistry {

  public record TableInfo(String tableName, List<Class<?>> entityClasses) {
    public TableInfo {
      Objects.requireNonNull(tableName);
      entityClasses = List.copyOf(entityClasses);
    }

    public TableInfo(String tableName, Class<?> entityClass) {
      this(tableName, List.of(entityClass));
    }
  }

  private final ObjectProvider<EntityManagerFactory> emfProvider;
  private final AtomicReference<Map<String, TableInfo>> tableMap = new AtomicReference<>();

  public HibernateTableEntityRegistry(ObjectProvider<EntityManagerFactory> emfProvider) {
    this.emfProvider = emfProvider;
  }

  /** Builds the registry once when Hibernate is fully initialized. */
  public void initialize() {
    if (tableMap.get() == null) {
      synchronized (this) {
        if (tableMap.get() == null) {
          tableMap.set(buildTableMap());
        }
      }
    }
  }

  /** Returns table info for the given SQL table name (lowercase), or null if not mapped. */
  public TableInfo getTableInfo(String tableName) {
    initialize();
    return tableMap.get().get(tableName.toLowerCase());
  }

  private Map<String, TableInfo> buildTableMap() {
    Map<String, TableInfo> map = new HashMap<>();

    try {
      EntityManagerFactory emf = emfProvider.getObject();
      SessionFactoryImplementor sf = emf.unwrap(SessionFactoryImplementor.class);
      MetamodelImplementor metamodel = sf.getMetamodel();

      for (EntityPersister persister : metamodel.entityPersisters().values()) {
        if (persister instanceof AbstractEntityPersister aep) {
          String tableName = normalizeTableName(aep.getTableName());
          map.put(tableName, new TableInfo(tableName, persister.getMappedClass()));
        }
      }
      map.putAll(collectionTables(metamodel.collectionPersisters().values(), map));

      log.info("DML observer: built table→entity registry with {} mappings", map.size());
    } catch (Exception e) {
      throw new IllegalStateException(
          "DML observer: failed to build table→entity registry. "
              + "Cannot start server with broken cache invalidation layer.",
          e);
    }

    return Map.copyOf(map);
  }

  /**
   * Collection tables that no entity is mapped to, each with the owners of all collections stored
   * in it. Both sides of a many-to-many share the table, so a membership change bumps both types.
   */
  private static Map<String, TableInfo> collectionTables(
      Iterable<CollectionPersister> persisters, Map<String, TableInfo> entityTables) {
    Map<String, Set<Class<?>>> owners = new HashMap<>();
    for (CollectionPersister persister : persisters) {
      if (persister instanceof AbstractCollectionPersister acp) {
        String tableName = normalizeTableName(acp.getTableName());
        if (!entityTables.containsKey(tableName)) {
          owners
              .computeIfAbsent(tableName, k -> new LinkedHashSet<>())
              .add(acp.getOwnerEntityPersister().getMappedClass());
        }
      }
    }
    Map<String, TableInfo> tables = new HashMap<>();
    owners.forEach(
        (tableName, classes) ->
            tables.put(tableName, new TableInfo(tableName, List.copyOf(classes))));
    return tables;
  }

  /** Lowercase, without a schema prefix ("public.tablename" becomes "tablename"). */
  private static String normalizeTableName(String tableName) {
    String name = tableName.toLowerCase();
    int dotIdx = name.lastIndexOf('.');
    return dotIdx >= 0 ? name.substring(dotIdx + 1) : name;
  }
}
