/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.collection;

import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.event.CollectionMembershipChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

/**
 * Applies small deltas to unloaded owning many-to-many sets. Hibernate's collection persister
 * supplies both the membership queries and row mutation SQL, including mapping-specific types
 * and column names. A temporary set contains only the delta, never the existing membership.
 *
 * <p>Extra-lazy sets still initialize on owning-side writes: Hibernate only queues inverse-side
 * operations. Do not mark the real wrapper dirty or replace its snapshot with the partial set.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Repository
@RequiredArgsConstructor
class HibernateCollectionItemStore {
  private final EntityManager entityManager;
  private final ApplicationEventPublisher eventPublisher;

  CollectionPersister prepare(Collection<?> collection) {
    if (!(collection instanceof PersistentSet set)
        || set.wasInitialized()
        || set.isDirty()
        || set.hasQueuedOperations()) {
      return null;
    }
    SessionImplementor session = session();
    var entry = session.getPersistenceContextInternal().getCollectionEntry(set);
    if (entry == null
        || !(entry.getLoadedPersister() instanceof AbstractCollectionPersister persister)
        || !persister.isManyToMany()
        || persister.isInverse()
        || !persister.isMutable()
        || persister.hasWhere()
        || persister.hasOrphanDelete()
        || persister.isAffectedByEnabledFilters(session)) {
      return null;
    }
    // Membership queries must see pending changes in a caller's transaction.
    session.flush();
    return set.wasInitialized() ? null : persister;
  }

  boolean update(
      CollectionPersister persister,
      IdentifiableObject owner,
      IdentifiableObject item,
      boolean addition) {
    SessionImplementor session = session();
    Serializable key = session.getIdentifier(owner);
    boolean present = persister.elementExists(key, item, session);
    if (present == addition) {
      return false;
    }

    invalidate(persister, key, session);
    for (CollectionPersister candidate :
        session.getFactory().getMetamodel().collectionPersisters().values()) {
      if (isInverseOf(candidate, persister)) {
        Serializable inverseKey = session.getIdentifier(item);
        invalidate(candidate, inverseKey, session);
        updateLoadedInverse(candidate, inverseKey, owner, addition, session);
      }
    }

    PersistentSet delta = new PersistentSet(session, addition ? Set.of(item) : Set.of());
    delta.setSnapshot(
        key, persister.getRole(), new HashMap<>(addition ? Map.of() : Map.of(item, item)));
    // PersistentSet resolves its snapshot through the persistence context. Register only this
    // temporary entry, not a CollectionKey: the real unloaded wrapper must remain associated.
    var entries = session.getPersistenceContextInternal().getCollectionEntries();
    entries.put(delta, new CollectionEntry(delta, session.getFactory()));
    try {
      if (addition) {
        persister.insertRows(delta, key, session);
      } else {
        persister.deleteRows(delta, key, session);
      }
      // Execute queued JDBC batches before another membership query (or a size validation).
      session.getJdbcCoordinator().executeBatch();
    } finally {
      entries.remove(delta);
    }
    invalidateQuerySpaces(persister, session);
    return true;
  }

  int size(CollectionPersister persister, IdentifiableObject owner) {
    SessionImplementor session = session();
    return persister.getSize(session.getIdentifier(owner), session);
  }

  private static boolean isInverseOf(CollectionPersister candidate, CollectionPersister owner) {
    return candidate instanceof AbstractCollectionPersister inverse
        && owner instanceof AbstractCollectionPersister owning
        && candidate.isInverse()
        && candidate.isManyToMany()
        && inverse.getTableName().equals(owning.getTableName())
        && Arrays.equals(inverse.getKeyColumnNames(), owning.getElementColumnNames())
        && Arrays.equals(inverse.getElementColumnNames(), owning.getKeyColumnNames());
  }

  @SuppressWarnings("unchecked")
  private static void updateLoadedInverse(
      CollectionPersister persister,
      Serializable key,
      IdentifiableObject owner,
      boolean addition,
      SessionImplementor session) {
    PersistentCollection inverse =
        session.getPersistenceContextInternal().getCollection(new CollectionKey(persister, key));
    if (inverse != null && inverse.wasInitialized()) {
      Collection<Object> values = (Collection<Object>) inverse.getValue();
      if (addition) {
        values.add(owner);
      } else {
        values.remove(owner);
      }
    }
  }

  private void invalidate(
      CollectionPersister persister, Serializable key, SessionImplementor session) {
    eventPublisher.publishEvent(
        new CollectionMembershipChangedEvent(
            persister.getOwnerEntityPersister().getEntityName(), persister.getRole(), key));
    if (!persister.hasCache()) {
      return;
    }
    CollectionDataAccess cache = persister.getCacheAccessStrategy();
    Object cacheKey =
        cache.generateCacheKey(key, persister, session.getFactory(), session.getTenantIdentifier());
    SoftLock lock = cache.lockItem(session, cacheKey, null);
    cache.remove(session, cacheKey);
    // Match CollectionAction: nonstrict-read-write evicts again at transaction completion,
    // so a concurrent reader cannot leave a pre-commit snapshot cached after this write.
    session
        .getActionQueue()
        .registerProcess(
            (success, completedSession) -> cache.unlockItem(completedSession, cacheKey, lock));
  }

  private static void invalidateQuerySpaces(
      CollectionPersister persister, SessionImplementor session) {
    if (!session.getFactory().getSessionFactoryOptions().isQueryCacheEnabled()) {
      return;
    }
    String[] spaces =
        Arrays.stream(persister.getCollectionSpaces()).map(Object::toString).toArray(String[]::new);
    var timestamps = session.getFactory().getCache().getTimestampsCache();
    timestamps.preInvalidate(spaces, session);
    session
        .getActionQueue()
        .registerProcess(
            (success, completedSession) -> timestamps.invalidate(spaces, completedSession));
  }

  private SessionImplementor session() {
    return entityManager.unwrap(SessionImplementor.class);
  }
}
