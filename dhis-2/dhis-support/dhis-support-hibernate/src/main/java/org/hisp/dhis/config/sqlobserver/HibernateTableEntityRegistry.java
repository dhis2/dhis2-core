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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Maps SQL table names to Hibernate entity classes and their PK column names. Populated lazily to
 * break the circular dependency between DataSource and EntityManagerFactory.
 *
 * @author Morten Svanæs
 */
@Slf4j
public class HibernateTableEntityRegistry {

  public record TableInfo(String tableName, Class<?> entityClass, List<String> pkColumnNames) {
    public TableInfo {
      Objects.requireNonNull(tableName);
      Objects.requireNonNull(entityClass);
      pkColumnNames = List.copyOf(pkColumnNames);
    }
  }

  private final ObjectProvider<EntityManagerFactory> emfProvider;
  private volatile Map<String, TableInfo> tableMap;

  public HibernateTableEntityRegistry(ObjectProvider<EntityManagerFactory> emfProvider) {
    this.emfProvider = emfProvider;
  }

  /** Builds the registry once when Hibernate is fully initialized. */
  public void initialize() {
    if (tableMap == null) {
      synchronized (this) {
        if (tableMap == null) {
          tableMap = buildTableMap();
        }
      }
    }
  }

  /** Returns table info for the given SQL table name (lowercase), or null if not mapped. */
  public TableInfo getTableInfo(String tableName) {
    initialize();
    return tableMap.get(tableName.toLowerCase());
  }

  private Map<String, TableInfo> buildTableMap() {
    Map<String, TableInfo> map = new ConcurrentHashMap<>();

    try {
      EntityManagerFactory emf = emfProvider.getObject();
      SessionFactoryImplementor sf = emf.unwrap(SessionFactoryImplementor.class);
      MetamodelImplementor metamodel = sf.getMetamodel();

      for (EntityPersister persister : metamodel.entityPersisters().values()) {
        if (persister instanceof AbstractEntityPersister aep) {
          String tableName = aep.getTableName().toLowerCase();

          // Strip schema prefix if present (e.g. "public.tablename" -> "tablename")
          int dotIdx = tableName.lastIndexOf('.');
          if (dotIdx >= 0) {
            tableName = tableName.substring(dotIdx + 1);
          }

          String[] pkColumns = aep.getIdentifierColumnNames();
          List<String> lowerPkColumns = new java.util.ArrayList<>(pkColumns.length);
          for (String pkColumn : pkColumns) {
            lowerPkColumns.add(pkColumn.toLowerCase());
          }

          Class<?> entityClass = persister.getMappedClass();
          map.put(tableName, new TableInfo(tableName, entityClass, lowerPkColumns));
        }
      }

      log.info("DML observer: built table→entity registry with {} mappings", map.size());
    } catch (Exception e) {
      throw new IllegalStateException(
          "DML observer: failed to build table→entity registry. "
              + "Cannot start server with broken cache invalidation layer.",
          e);
    }

    return map;
  }
}
