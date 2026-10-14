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
package org.hisp.dhis.artemis.audit.listener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.AuditableEntity;
import org.hisp.dhis.artemis.audit.configuration.AuditMatrix;
import org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory;
import org.hisp.dhis.artemis.config.UsernameSupplier;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Component;

/**
 * Audits entity updates.
 *
 * <p>An entity whose updates in a transaction changed only {@link #AUDIT_IGNORED_PROPERTIES}, and
 * none of its collections, is not audited. Whether an entity changed is a question about the whole
 * transaction, not about a single flush: Hibernate reports the changed properties with each update,
 * but a collection changed in place only as a collection event, which can come in an earlier or a
 * later flush than the update of its owner. A metadata import, for example, clears embedded
 * collections and flushes before it updates their owner. So the listener collects the updates and
 * the owners of changed collections during the transaction, and decides and sends the audits once
 * the transaction committed.
 *
 * @author Luciano Fiandesio
 */
@Component
public class PostUpdateAuditListener extends AbstractHibernateListener
    implements PostUpdateEventListener,
        PostCollectionRecreateEventListener,
        PostCollectionUpdateEventListener,
        PostCollectionRemoveEventListener {

  /**
   * Bookkeeping properties: they record that something happened (a login, a write), they are not
   * data or metadata. An entity whose updates change only these properties, and no collection, is
   * not audited. Matched by Hibernate property name on every auditable entity. Keep this set small:
   * a property listed here can never be the reason for an audit entry.
   *
   * <p>The properties stay in the payload of updates that are audited, as that payload is a
   * snapshot of the full state.
   */
  static final Set<String> AUDIT_IGNORED_PROPERTIES =
      Set.of("lastUpdated", "lastUpdatedBy", "lastLogin", "lastCheckedInterpretations");

  /** The updates of the transaction running in a session, until that transaction completes. */
  private final Map<EventSource, TransactionUpdates> updatesBySession = new ConcurrentHashMap<>();

  public PostUpdateAuditListener(
      AuditManager auditManager,
      AuditObjectFactory auditObjectFactory,
      AuditMatrix auditMatrix,
      UsernameSupplier userNameSupplier,
      SchemaService schemaService) {
    super(auditManager, auditObjectFactory, auditMatrix, userNameSupplier, schemaService);
  }

  @Override
  AuditType getAuditType() {
    return AuditType.UPDATE;
  }

  @Override
  public void onPostUpdate(PostUpdateEvent event) {
    getAuditable(event.getEntity(), "update")
        .ifPresent(auditable -> updatesOf(event.getSession()).add(event, auditable));
  }

  @Override
  public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
    onCollectionChange(event);
  }

  @Override
  public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
    onCollectionChange(event);
  }

  @Override
  public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
    onCollectionChange(event);
  }

  private void onCollectionChange(AbstractCollectionEvent event) {
    Object owner = event.getAffectedOwnerOrNull();

    if (owner != null && getAuditable(owner, "update").isPresent()) {
      updatesOf(event.getSession()).addCollectionOwner(owner);
    }
  }

  /** The audits are sent by the process registered in {@link #startTransactionUpdates}. */
  @Override
  public boolean requiresPostCommitHanding(EntityPersister entityPersister) {
    return false;
  }

  /** Returns the updates of the transaction running in the given session. */
  private TransactionUpdates updatesOf(EventSource session) {
    return updatesBySession.computeIfAbsent(session, this::startTransactionUpdates);
  }

  /**
   * Starts collecting the updates of the transaction running in the given session, and registers
   * the process that sends their audits once the transaction committed.
   */
  private TransactionUpdates startTransactionUpdates(EventSource session) {
    TransactionUpdates updates = new TransactionUpdates();
    session
        .getActionQueue()
        .registerProcess(
            (success, completedSession) -> {
              updatesBySession.remove(session);

              if (success) {
                updates.audited().forEach(this::sendAudit);
              }
            });
    return updates;
  }

  private void sendAudit(Update update) {
    PostUpdateEvent event = update.event();

    auditManager.send(
        Audit.builder()
            .auditType(getAuditType())
            .auditScope(update.auditable().scope())
            .createdAt(LocalDateTime.now())
            .createdBy(getCreatedBy())
            .object(event.getEntity())
            .attributes(
                auditManager.collectAuditAttributes(
                    event.getEntity(), event.getEntity().getClass()))
            .auditableEntity(
                new AuditableEntity(event.getEntity().getClass(), createAuditEntry(event)))
            .build());
  }

  /** Create Audit entry for update event */
  private Object createAuditEntry(PostUpdateEvent postUpdateEvent) {
    return super.createAuditEntry(
        postUpdateEvent.getEntity(),
        postUpdateEvent.getState(),
        postUpdateEvent.getSession(),
        postUpdateEvent.getId(),
        postUpdateEvent.getPersister());
  }

  private record Update(PostUpdateEvent event, Auditable auditable) {}

  /** The updates of auditable entities in a transaction, and their changed collections. */
  private static final class TransactionUpdates {
    private final List<Update> updates = new ArrayList<>();

    /** Compared by identity, as a session holds a single instance per entity. */
    private final Set<Object> collectionOwners = Collections.newSetFromMap(new IdentityHashMap<>());

    void add(PostUpdateEvent event, Auditable auditable) {
      updates.add(new Update(event, auditable));
    }

    void addCollectionOwner(Object owner) {
      collectionOwners.add(owner);
    }

    /** Real changes, and bookkeeping-only updates of entities with a changed collection. */
    Stream<Update> audited() {
      return updates.stream()
          .filter(
              update ->
                  !isBookkeepingOnlyUpdate(update.event())
                      || collectionOwners.contains(update.event().getEntity()));
    }

    /**
     * Returns true if the update changed only {@link #AUDIT_IGNORED_PROPERTIES}. When Hibernate
     * does not know which properties changed (an entity reattached without a dirty check), the
     * update counts as a real change. Collections are not part of the dirty properties when changed
     * in place, see {@link #audited()}.
     */
    private static boolean isBookkeepingOnlyUpdate(PostUpdateEvent event) {
      int[] dirtyProperties = event.getDirtyProperties();

      if (dirtyProperties == null) {
        return false;
      }

      String[] propertyNames = event.getPersister().getPropertyNames();

      for (int index : dirtyProperties) {
        if (!AUDIT_IGNORED_PROPERTIES.contains(propertyNames[index])) {
          return false;
        }
      }

      return true;
    }
  }
}
