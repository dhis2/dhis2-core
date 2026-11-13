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

import com.google.common.collect.Lists;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.event.CollectionMembershipChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

/**
 * Applies deltas to unloaded owning many-to-many sets. One query per partition of items reads which
 * of them are members. Hibernate's collection persister supplies the row mutation SQL, including
 * mapping-specific types and column names. A temporary set contains only the delta, never the
 * existing membership.
 *
 * <p>The owner's row is locked before the membership is read. A concurrent change of the same set
 * waits for this transaction to end and then reads its committed rows, as a later request would.
 * Without the lock both would read the same membership: a second addition of an item would violate
 * the join table's key and a second removal would count a row that no longer exists.
 *
 * <p>Extra-lazy sets still initialize on owning-side writes: Hibernate only queues inverse-side
 * operations. Do not mark the real wrapper dirty or replace its snapshot with the partial set.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Repository
@RequiredArgsConstructor
class HibernateCollectionItemStore {
  /** Items per membership query, as when the items themselves are loaded by UID. */
  private static final int MEMBERSHIP_QUERY_SIZE = 20_000;

  /** Rows per JDBC batch. Without a batch size Hibernate spends a round trip on every row. */
  private static final int ROW_BATCH_SIZE = 100;

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

  /**
   * Adds the items to, or removes them from, the unloaded set of the owner. Neither the statements
   * nor the invalidations of the owner's set grow with each item.
   *
   * @return the number of items whose membership changed
   */
  int update(
      CollectionPersister persister,
      IdentifiableObject owner,
      Collection<? extends IdentifiableObject> items,
      boolean addition) {
    SessionImplementor session = session();
    entityManager.lock(owner, LockModeType.PESSIMISTIC_WRITE);
    Serializable key = session.getIdentifier(owner);
    List<IdentifiableObject> changes = changes(persister, key, items, addition, session);
    if (changes.isEmpty()) {
      return 0;
    }

    invalidate(persister, key, session);
    for (CollectionPersister candidate :
        session.getFactory().getMetamodel().collectionPersisters().values()) {
      if (isInverseOf(candidate, persister)) {
        for (IdentifiableObject item : changes) {
          Serializable inverseKey = session.getIdentifier(item);
          invalidate(candidate, inverseKey, session);
          updateLoadedInverse(candidate, inverseKey, owner, addition, session);
        }
      }
    }
    writeRows(persister, key, changes, addition, session);
    invalidateQuerySpaces(persister, session);
    return changes.size();
  }

  /** The items to add that are not members yet, or the items to remove that are members. */
  private static List<IdentifiableObject> changes(
      CollectionPersister persister,
      Serializable key,
      Collection<? extends IdentifiableObject> items,
      boolean addition,
      SessionImplementor session) {
    Map<Serializable, IdentifiableObject> itemsById = new LinkedHashMap<>();
    items.forEach(item -> itemsById.put(session.getIdentifier(item), item));
    Set<Serializable> members = members(persister, key, List.copyOf(itemsById.keySet()), session);
    return itemsById.entrySet().stream()
        .filter(entry -> members.contains(entry.getKey()) != addition)
        .map(Map.Entry::getValue)
        .toList();
  }

  /** Reads which of the items are members with one query per partition, not one per item. */
  private static Set<Serializable> members(
      CollectionPersister persister,
      Serializable key,
      List<Serializable> itemIds,
      SessionImplementor session) {
    EntityPersister ownerPersister = persister.getOwnerEntityPersister();
    EntityPersister itemPersister = ((QueryableCollection) persister).getElementPersister();
    String property = persister.getRole().substring(ownerPersister.getEntityName().length() + 1);

    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<Serializable> query = builder.createQuery(Serializable.class);
    Root<?> owner = query.from(ownerPersister.getMappedClass());
    Path<Serializable> itemId = owner.join(property).get(itemPersister.getIdentifierPropertyName());
    ParameterExpression<Serializable> ownerId = builder.parameter(Serializable.class);
    ParameterExpression<Collection<?>> ids = builder.parameter(collectionType());
    query
        .select(itemId)
        .where(
            builder.equal(owner.get(ownerPersister.getIdentifierPropertyName()), ownerId),
            itemId.in(ids));

    Set<Serializable> members = new HashSet<>();
    for (List<Serializable> partition : Lists.partition(itemIds, MEMBERSHIP_QUERY_SIZE)) {
      members.addAll(
          session
              .createQuery(query)
              .setParameter(ownerId, key)
              .setParameter(ids, partition)
              .getResultList());
    }
    return members;
  }

  @SuppressWarnings("unchecked")
  private static Class<Collection<?>> collectionType() {
    return (Class<Collection<?>>) (Class<?>) Collection.class;
  }

  private static void writeRows(
      CollectionPersister persister,
      Serializable key,
      List<IdentifiableObject> changes,
      boolean addition,
      SessionImplementor session) {
    PersistentSet delta =
        new PersistentSet(session, addition ? new LinkedHashSet<>(changes) : Set.of());
    HashMap<IdentifiableObject, IdentifiableObject> snapshot = new HashMap<>();
    if (!addition) {
      changes.forEach(item -> snapshot.put(item, item));
    }
    delta.setSnapshot(key, persister.getRole(), snapshot);
    // PersistentSet resolves its snapshot through the persistence context. Register only this
    // temporary entry, not a CollectionKey: the real unloaded wrapper must remain associated.
    var entries = session.getPersistenceContextInternal().getCollectionEntries();
    entries.put(delta, new CollectionEntry(delta, session.getFactory()));
    Integer batchSize = session.getJdbcBatchSize();
    session.setJdbcBatchSize(ROW_BATCH_SIZE);
    try {
      if (addition) {
        persister.insertRows(delta, key, session);
      } else {
        persister.deleteRows(delta, key, session);
      }
      // Execute the rows still queued before another membership query (or a size validation).
      session.getJdbcCoordinator().executeBatch();
    } finally {
      session.setJdbcBatchSize(batchSize);
      entries.remove(delta);
    }
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
