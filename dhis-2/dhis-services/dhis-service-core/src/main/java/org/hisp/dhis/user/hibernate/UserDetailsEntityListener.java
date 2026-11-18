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

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.Type;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeCollector;
import org.springframework.stereotype.Component;

/**
 * Reports updates and deletions of a {@link User} that change its {@link UserDetails} to the {@link
 * UserDetailsChangeCollector}, whichever API or service wrote them.
 *
 * <p>An update is reported when one of the {@link #ACCESS_PROPERTIES} changed. These are the
 * properties of the user that the session principal carries and that decide what the session may
 * do: whether the account is enabled and not expired, how deep analytics may look, and the login
 * rules that the refresh of a session enforces. Every login updates the user, so other properties
 * such as {@code lastLogin} are ignored. Collections of the user are reported by {@link
 * UserDetailsCollectionListener}.
 *
 * <p>A changed username is reported by the old username as well: the change tracker is keyed by
 * username, and the sessions of the user still hold the old one. The metadata import refuses to
 * change a username, but {@code UserService#updateUser} does not.
 *
 * <p>The comparison needs the state the user was loaded with. A detached user that is updated
 * without being loaded again has no such state, and comparing would need a query while flushing.
 * Such an update is not reported; the periodic refresh of the session covers it. Reporting it
 * instead would mark the user on every login with HTTP Basic outside an open session in view, as on
 * {@code /api/tracker}.
 *
 * <p>A deleted user is reported by username, since the user cannot be resolved by its primary key
 * after commit. The next request of a session of the user then finds no user and ends the session.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class UserDetailsEntityListener implements PostUpdateEventListener, PostDeleteEventListener {

  /** The persistent properties of {@link User} whose change is reported. */
  private static final Set<String> ACCESS_PROPERTIES =
      Set.of(
          "disabled",
          "accountExpiry",
          "dataViewMaxOrganisationUnitLevel",
          "externalAuth",
          "twoFactorType");

  private static final String USERNAME_PROPERTY = "username";

  private final EntityManagerFactory entityManagerFactory;

  private final UserDetailsChangeCollector userDetailsChangeCollector;

  /** The indexes of the {@link #ACCESS_PROPERTIES} in the entity state of a user. */
  private int[] accessPropertyIndexes;

  /** The index of the username in the entity state of a user. */
  private int usernameIndex;

  @PostConstruct
  void registerListener() {
    SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
    EntityMetamodel userMetamodel =
        sessionFactory.getMetamodel().entityPersister(User.class).getEntityMetamodel();
    accessPropertyIndexes =
        ACCESS_PROPERTIES.stream().mapToInt(userMetamodel::getPropertyIndex).toArray();
    usernameIndex = userMetamodel.getPropertyIndex(USERNAME_PROPERTY);

    EventListenerRegistry registry =
        sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
    registry.appendListeners(EventType.POST_UPDATE, this);
    registry.appendListeners(EventType.POST_DELETE, this);
  }

  @Override
  public void onPostUpdate(PostUpdateEvent event) {
    if (!(event.getEntity() instanceof User user) || event.getOldState() == null) {
      return;
    }
    if (isDirty(event, usernameIndex)
        && event.getOldState()[usernameIndex] instanceof String oldUsername) {
      userDetailsChangeCollector.usernamesChanged(Set.of(oldUsername));
    }
    for (int index : accessPropertyIndexes) {
      if (isDirty(event, index)) {
        userDetailsChangeCollector.usersChanged(Set.of(user.getId()));
        return;
      }
    }
  }

  /** Compares with the type of the property, so that a date and a timestamp compare by value. */
  private static boolean isDirty(PostUpdateEvent event, int index) {
    Type type = event.getPersister().getPropertyTypes()[index];
    return type.isDirty(event.getOldState()[index], event.getState()[index], event.getSession());
  }

  @Override
  public void onPostDelete(PostDeleteEvent event) {
    if (event.getEntity() instanceof User user) {
      userDetailsChangeCollector.usernamesChanged(Set.of(user.getUsername()));
    }
  }

  @Override
  public boolean requiresPostCommitHanding(EntityPersister persister) {
    return false;
  }
}
