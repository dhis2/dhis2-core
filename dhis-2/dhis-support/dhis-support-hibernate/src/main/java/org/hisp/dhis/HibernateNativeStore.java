/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis;

import jakarta.persistence.EntityManager;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.query.NativeQuery;
import org.intellij.lang.annotations.Language;

/**
 * Base class for stores that use hibernate mapping but all access is done via native SQL.
 *
 * @param <T> hibernate mapped entity type
 * @author Jan Bernitt
 */
@Slf4j
public abstract class HibernateNativeStore<T> {

  protected final EntityManager entityManager;
  protected final Class<T> clazz;
  protected final String tableName;

  protected HibernateNativeStore(EntityManager entityManager, Class<T> clazz) {
    this.entityManager = entityManager;
    this.clazz = clazz;
    this.tableName = getTableName(entityManager, clazz);
  }

  private static String getTableName(EntityManager em, Class<?> entityClass) {
    // Note: this is the same way we extract the table name for Schema
    try {
      MetamodelImplementor metamodelImplementor = (MetamodelImplementor) em.getMetamodel();
      EntityPersister entityPersister = metamodelImplementor.entityPersister(entityClass);
      if (entityPersister instanceof SingleTableEntityPersister persister) {
        return persister.getTableName();
      }
    } catch (Exception ex) {
      log.warn("Failed to set table name for: " + entityClass, ex);
    }
    return null;
  }

  /** Could be overridden programmatically. */
  @Nonnull
  public Class<T> getClazz() {
    return clazz;
  }

  /**
   * Returns the current session.
   *
   * @return the current session.
   */
  protected final Session getSession() {
    return entityManager.unwrap(Session.class);
  }

  /**
   * Create a Hibernate {@link NativeQuery} instance with {@code SynchronizedEntityClass} set to the
   * current class of the store. Use this to avoid all Hibernate second level caches from being
   * invalidated.
   *
   * <p>Be aware that it is only correct to use this if and only if the only table touched by the
   * native query is the one table belonging to the store.
   *
   * @param sql the SQL query to execute
   * @return the {@link NativeQuery} instance
   */
  @SuppressWarnings("rawtypes")
  protected NativeQuery nativeSynchronizedQuery(@Language("SQL") String sql) {
    return getSession().createNativeQuery(sql).addSynchronizedEntityClass(getClazz());
  }

  /**
   * Same as {@link #nativeSynchronizedQuery(String)} just with the return type being specified as
   * the store entity type. Use only when the result is a of the store entity type or a list of it.
   */
  protected NativeQuery<T> nativeSynchronizedTypedQuery(@Language("SQL") String sql) {
    return getSession().createNativeQuery(sql, clazz).addSynchronizedEntityClass(getClazz());
  }
}
