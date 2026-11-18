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
package org.hisp.dhis.user.hibernate;

import static java.util.stream.Collectors.toSet;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.collection.CollectionPersister;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeCollector;
import org.hisp.dhis.user.UserGroup;
import org.springframework.stereotype.Component;

/**
 * Reports changes of the collections that the {@link UserDetails} of users are built from to the
 * {@link UserDetailsChangeCollector}, whichever API or service wrote them:
 *
 * <ul>
 *   <li>{@code User.userRoles}, {@code User.organisationUnits}, {@code
 *       User.dataViewOrganisationUnits} and {@code User.teiSearchOrganisationUnits}: the user that
 *       owns the collection is reported
 *   <li>{@code UserGroup.managedGroups}: the members of the managing group are reported, resolved
 *       after commit
 * </ul>
 *
 * <p>All of them are owned by the entity they belong to, so every write goes through the owner's
 * collection, also the inverse endpoints such as {@code /organisationUnits/{uid}/users} or {@code
 * /userRoles/{uid}/users}. The members of a user group are reported by {@link
 * UserGroupMembersCollectionListener}.
 *
 * <p>Only real differences are reported. A collection is either changed in place, which Hibernate
 * flushes as an update, or replaced by a new collection, which Hibernate flushes as the removal of
 * the old collection and the recreation of the new one. Both cases compare the content the old
 * collection was loaded with to the content that is flushed. The recreation is never reported: a
 * new user has no session, the members of a new user group are reported by {@link
 * UserGroupMembersCollectionListener}, and a replacement is decided when the old collection is
 * removed.
 *
 * <p>The listener runs in every flush that touches one of these collections, so it reads only what
 * the event provides: the stored snapshot, the flushed collection and the primary keys of the
 * elements. It never runs a query and never initializes a collection. When the old content is not
 * known, because the old collection was never initialized, the owner is reported.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class UserDetailsCollectionListener
    implements PreCollectionUpdateEventListener, PreCollectionRemoveEventListener {

  /** The users affected by a change of a collection. */
  private enum Affected {
    /** The user that owns the collection. */
    OWNER,
    /** The members of the user group that owns the collection. */
    MEMBERS_OF_OWNER
  }

  /**
   * A collection that {@link UserDetails} are built from.
   *
   * @param elements reads the collection from its owner, which is how a replacement of the
   *     collection is found
   * @param affected the users affected by a change
   */
  private record TrackedCollection(Function<Object, Collection<?>> elements, Affected affected) {}

  private static final Map<String, TrackedCollection> TRACKED_COLLECTIONS =
      Map.of(
          User.class.getName() + ".userRoles",
          new TrackedCollection(owner -> ((User) owner).getUserRoles(), Affected.OWNER),
          User.class.getName() + ".organisationUnits",
          new TrackedCollection(owner -> ((User) owner).getOrganisationUnits(), Affected.OWNER),
          User.class.getName() + ".dataViewOrganisationUnits",
          new TrackedCollection(
              owner -> ((User) owner).getDataViewOrganisationUnits(), Affected.OWNER),
          User.class.getName() + ".teiSearchOrganisationUnits",
          new TrackedCollection(
              owner -> ((User) owner).getTeiSearchOrganisationUnits(), Affected.OWNER),
          UserGroup.class.getName() + ".managedGroups",
          new TrackedCollection(
              owner -> ((UserGroup) owner).getManagedGroups(), Affected.MEMBERS_OF_OWNER));

  private final EntityManagerFactory entityManagerFactory;

  private final UserDetailsChangeCollector userDetailsChangeCollector;

  @PostConstruct
  void registerListener() {
    EventListenerRegistry registry =
        entityManagerFactory
            .unwrap(SessionFactoryImpl.class)
            .getServiceRegistry()
            .getService(EventListenerRegistry.class);
    registry.appendListeners(EventType.PRE_COLLECTION_UPDATE, this);
    registry.appendListeners(EventType.PRE_COLLECTION_REMOVE, this);
  }

  /**
   * A collection changed in place. A collection owned by its entity is initialized whenever its
   * content was changed, so an uninitialized one has nothing to report.
   */
  @Override
  public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
    PersistentCollection collection = event.getCollection();
    TrackedCollection tracked = trackedCollection(event);
    if (tracked == null || !collection.wasInitialized()) {
      return;
    }
    if (!sameElements(
        snapshotElements(collection.getStoredSnapshot()), (Collection<?>) collection)) {
      Object owner = event.getAffectedOwnerOrNull();
      report(tracked.affected(), owner != null ? owner : collection.getOwner());
    }
  }

  /**
   * A collection is removed when it was replaced by another collection or by {@code null}, and when
   * its owner is deleted. The deletion of a user is reported by {@link UserDetailsEntityListener},
   * and the members of a deleted user group are reported by {@link
   * UserGroupMembersCollectionListener}, so a deleted owner is skipped.
   *
   * <p>The event carries no collection when a detached owner is updated with a collection that
   * Hibernate did not load. Neither the collection nor its old content is known then, so a user
   * owner, or the members of a user group owner, are reported.
   */
  @Override
  public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
    Object owner = event.getAffectedOwnerOrNull();
    if (owner == null || isDeleted(event, owner)) {
      return;
    }
    PersistentCollection collection = event.getCollection();
    if (collection == null) {
      if (owner instanceof User) {
        report(Affected.OWNER, owner);
      } else if (owner instanceof UserGroup) {
        report(Affected.MEMBERS_OF_OWNER, owner);
      }
      return;
    }
    TrackedCollection tracked = trackedCollection(event);
    if (tracked == null) {
      return;
    }
    if (!collection.wasInitialized()) {
      report(tracked.affected(), owner);
      return;
    }
    Collection<?> replacement = tracked.elements().apply(owner);
    if (!sameElements(
        snapshotElements(collection.getStoredSnapshot()),
        replacement == null ? List.of() : replacement)) {
      report(tracked.affected(), owner);
    }
  }

  private void report(Affected affected, Object owner) {
    if (owner == null) {
      return;
    }
    Set<Long> ownerId = Set.of(((PrimaryKeyObject) owner).getId());
    switch (affected) {
      case OWNER -> userDetailsChangeCollector.usersChanged(ownerId);
      case MEMBERS_OF_OWNER -> userDetailsChangeCollector.membersOfUserGroupsChanged(ownerId);
    }
  }

  /**
   * Uses the persister the collection is being flushed with and falls back to the persister it was
   * loaded with, since a removed collection has no current persister.
   */
  private static TrackedCollection trackedCollection(AbstractCollectionEvent event) {
    CollectionEntry entry =
        event
            .getSession()
            .getPersistenceContextInternal()
            .getCollectionEntry(event.getCollection());
    if (entry == null) {
      return null;
    }
    CollectionPersister persister =
        entry.getCurrentPersister() != null
            ? entry.getCurrentPersister()
            : entry.getLoadedPersister();
    return persister == null ? null : TRACKED_COLLECTIONS.get(persister.getRole());
  }

  private static boolean isDeleted(AbstractCollectionEvent event, Object owner) {
    EntityEntry entry = event.getSession().getPersistenceContextInternal().getEntry(owner);
    return entry != null
        && (entry.getStatus() == Status.DELETED || entry.getStatus() == Status.GONE);
  }

  /** The snapshot of a set is a map with the elements as keys. */
  private static Collection<?> snapshotElements(Serializable snapshot) {
    if (snapshot instanceof Map<?, ?> map) {
      return map.keySet();
    }
    return snapshot instanceof Collection<?> collection ? collection : List.of();
  }

  /** Compares the primary keys only, so that element proxies are not initialized. */
  private static boolean sameElements(Collection<?> before, Collection<?> after) {
    return before.size() == after.size() && primaryKeys(before).equals(primaryKeys(after));
  }

  private static Set<Long> primaryKeys(Collection<?> elements) {
    return elements.stream()
        .map(PrimaryKeyObject.class::cast)
        .map(PrimaryKeyObject::getId)
        .collect(toSet());
  }
}
