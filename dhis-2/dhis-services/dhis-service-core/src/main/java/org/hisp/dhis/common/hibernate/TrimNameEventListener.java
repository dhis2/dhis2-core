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
package org.hisp.dhis.common.hibernate;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.hisp.dhis.commons.util.TextUtils;
import org.springframework.stereotype.Component;

/**
 * Trims the name of metadata before it is inserted or updated, the same way as the {@code
 * trg_trim_name} database trigger. The trigger trims the name in the database row only, so without
 * this listener the object in the Hibernate session keeps the untrimmed name after a flush and
 * differs from the stored row.
 *
 * <p>Like the trigger, only entities having a {@code uid}, a {@code code} and a {@code name}
 * property are trimmed.
 */
@Component
public class TrimNameEventListener implements PreInsertEventListener, PreUpdateEventListener {
  private static final String NAME = "name";

  private static final List<String> REQUIRED_PROPERTIES = List.of("uid", "code", NAME);

  @PersistenceUnit private transient EntityManagerFactory emf;

  @PostConstruct
  protected void init() {
    EventListenerRegistry registry =
        emf.unwrap(SessionFactoryImpl.class)
            .getServiceRegistry()
            .getService(EventListenerRegistry.class);

    registry.getEventListenerGroup(EventType.PRE_INSERT).appendListener(this);
    registry.getEventListenerGroup(EventType.PRE_UPDATE).appendListener(this);
  }

  @Override
  public boolean onPreInsert(PreInsertEvent event) {
    trimName(event.getPersister(), event.getEntity(), event.getState());
    return false;
  }

  @Override
  public boolean onPreUpdate(PreUpdateEvent event) {
    trimName(event.getPersister(), event.getEntity(), event.getState());
    return false;
  }

  /**
   * Trims the name in both the entity state, which Hibernate writes to the database, and the entity
   * itself, which stays in the session.
   */
  private static void trimName(EntityPersister persister, Object entity, Object[] state) {
    List<String> propertyNames = List.of(persister.getPropertyNames());

    if (!propertyNames.containsAll(REQUIRED_PROPERTIES)) {
      return;
    }

    int index = propertyNames.indexOf(NAME);

    if (!(state[index] instanceof String name)) {
      return;
    }

    String trimmed = TextUtils.trimName(name);

    if (!trimmed.equals(name)) {
      state[index] = trimmed;
      persister.setPropertyValue(entity, index, trimmed);
    }
  }
}
