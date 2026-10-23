/*
 * Copyright (c) 2004-2022, University of Oslo
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

import com.google.common.collect.Sets;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.collection.CollectionPersister;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetailsChangeCollector;
import org.hisp.dhis.user.UserGroup;
import org.springframework.stereotype.Component;

/**
 * Reports the users that joined or left a {@link UserGroup} through Hibernate to the {@link
 * UserDetailsChangeCollector}. This covers every write of the {@code UserGroup.members} collection:
 * metadata import and sync, the {@code /userGroups/{uid}/users} and {@code /users/{uid}/userGroups}
 * collection endpoints and {@code UserGroupService#updateUserGroup}.
 *
 * <p>Only the difference between the members as loaded and as flushed is reported, so saving a
 * group with unchanged members reports nobody. Deleting a user group is not reported.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class UserGroupMembersCollectionListener
    implements PreCollectionUpdateEventListener, PreCollectionRecreateEventListener {

  private static final String MEMBERS_ROLE = UserGroup.class.getName() + ".members";

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
    registry.appendListeners(EventType.PRE_COLLECTION_RECREATE, this);
  }

  @Override
  public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
    PersistentCollection members = event.getCollection();
    if (!isMembers(event) || !members.wasInitialized()) {
      return;
    }
    Set<Long> before = userIds(snapshotElements(members.getStoredSnapshot()));
    Set<Long> after = userIds((Collection<?>) members);
    userDetailsChangeCollector.usersChanged(Sets.symmetricDifference(before, after));
  }

  /** A members collection is recreated when the group is created. */
  @Override
  public void onPreRecreateCollection(PreCollectionRecreateEvent event) {
    PersistentCollection members = event.getCollection();
    if (isMembers(event)) {
      userDetailsChangeCollector.usersChanged(userIds((Collection<?>) members));
    }
  }

  /**
   * Uses the persister the collection is being flushed with, since a collection wrapped for a new
   * group has no role yet.
   */
  private static boolean isMembers(AbstractCollectionEvent event) {
    CollectionEntry entry =
        event
            .getSession()
            .getPersistenceContextInternal()
            .getCollectionEntry(event.getCollection());
    CollectionPersister persister = entry == null ? null : entry.getCurrentPersister();
    return persister != null && MEMBERS_ROLE.equals(persister.getRole());
  }

  /** The snapshot of a set is a map with the elements as keys. */
  private static Collection<?> snapshotElements(Serializable snapshot) {
    if (snapshot instanceof Map<?, ?> map) {
      return map.keySet();
    }
    return snapshot instanceof Collection<?> collection ? collection : List.of();
  }

  /** Reads the ids only, so that member proxies are not initialized while flushing. */
  private static Set<Long> userIds(Collection<?> users) {
    return users.stream().map(User.class::cast).map(User::getId).collect(toSet());
  }
}
