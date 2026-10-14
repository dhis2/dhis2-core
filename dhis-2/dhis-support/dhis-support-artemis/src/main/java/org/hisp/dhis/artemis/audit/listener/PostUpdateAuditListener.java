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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
 * collections and flushes before it updates their owner. So the listener records the owners of
 * changed collections during the transaction, and each update registers its own after-commit
 * process that decides whether to audit it once all flushes are done.
 *
 * <p>The audits keep the order of the actions, as the process of an update is queued at the
 * position of that update, between the processes that send the CREATE and DELETE audits.
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

  /** The collection changes of the transaction running in a session, until it completes. */
  private final Map<EventSource, TransactionCollectionChanges> collectionChangesBySession =
      new ConcurrentHashMap<>();

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
        .ifPresent(auditable -> registerAudit(event, auditable));
  }

  /**
   * Registers the process that audits the update once the transaction committed: a real change, or
   * a bookkeeping-only update of an entity with a collection changed anywhere in the transaction.
   */
  private void registerAudit(PostUpdateEvent event, Auditable auditable) {
    EventSource session = event.getSession();
    TransactionCollectionChanges collectionChanges = collectionChangesOf(session);
    session
        .getActionQueue()
        .registerProcess(
            (success, completedSession) -> {
              if (success
                  && (!isBookkeepingOnlyUpdate(event)
                      || collectionChanges.isOwner(event.getEntity()))) {
                sendAudit(event, auditable);
              }
            });
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
      collectionChangesOf(event.getSession()).addOwner(owner);
    }
  }

  /** The audits are sent by the processes registered in {@link #registerAudit}. */
  @Override
  public boolean requiresPostCommitHanding(EntityPersister entityPersister) {
    return false;
  }

  /** Returns the collection changes of the transaction running in the given session. */
  private TransactionCollectionChanges collectionChangesOf(EventSource session) {
    return collectionChangesBySession.computeIfAbsent(session, this::startCollectionChanges);
  }

  /**
   * Starts recording the collection changes of the transaction running in the given session. The
   * processes of the updates keep their own reference to them, so the entry is only needed until
   * the transaction completes.
   */
  private TransactionCollectionChanges startCollectionChanges(EventSource session) {
    TransactionCollectionChanges collectionChanges = new TransactionCollectionChanges();
    session
        .getActionQueue()
        .registerProcess((success, completedSession) -> collectionChangesBySession.remove(session));
    return collectionChanges;
  }

  private void sendAudit(PostUpdateEvent event, Auditable auditable) {
    auditManager.send(
        Audit.builder()
            .auditType(getAuditType())
            .auditScope(auditable.scope())
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

  /**
   * Returns true if the update changed only {@link #AUDIT_IGNORED_PROPERTIES}. When Hibernate does
   * not know which properties changed (an entity reattached without a dirty check), the update
   * counts as a real change. Collections are not part of the dirty properties when changed in
   * place, see {@link TransactionCollectionChanges}.
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

  /**
   * The owners of the collections changed in a transaction. A collection changed in place is
   * reported as a collection event, not as a dirty property of its owner's update.
   */
  private static final class TransactionCollectionChanges {
    /** Compared by identity, as a session holds a single instance per entity. */
    private final Set<Object> owners = Collections.newSetFromMap(new IdentityHashMap<>());

    void addOwner(Object owner) {
      owners.add(owner);
    }

    boolean isOwner(Object entity) {
      return owners.contains(entity);
    }
  }
}
