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
import java.util.Set;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
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
 * <p>Runs at flush time, when Hibernate still knows what changed, and sends the audit after the
 * transaction committed. The decision whether an update is worth an audit entry can only be made at
 * flush time: a collection changed in place is not part of {@link
 * PostUpdateEvent#getDirtyProperties()}, and it is no longer dirty once the transaction committed.
 *
 * @author Luciano Fiandesio
 */
@Component
public class PostUpdateAuditListener extends AbstractHibernateListener
    implements PostUpdateEventListener {

  /**
   * Bookkeeping properties: they record that something happened (a login, a write), they are not
   * data or metadata. An update that changes only these properties, and no collection, is not
   * audited. Matched by Hibernate property name on every auditable entity. Keep this set small: a
   * property listed here can never be the reason for an audit entry.
   *
   * <p>The properties stay in the payload of updates that are audited, as that payload is a
   * snapshot of the full state.
   */
  static final Set<String> AUDIT_IGNORED_PROPERTIES =
      Set.of("lastUpdated", "lastUpdatedBy", "lastLogin", "lastCheckedInterpretations");

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
  public void onPostUpdate(PostUpdateEvent postUpdateEvent) {
    getAuditable(postUpdateEvent.getEntity(), "update")
        .filter(auditable -> !isBookkeepingOnlyUpdate(postUpdateEvent))
        .ifPresent(
            auditable ->
                postUpdateEvent
                    .getSession()
                    .getActionQueue()
                    .registerProcess(
                        (success, session) -> {
                          if (success) {
                            sendAudit(postUpdateEvent, auditable);
                          }
                        }));
  }

  /** The audit is sent by the process registered in {@link #onPostUpdate(PostUpdateEvent)}. */
  @Override
  public boolean requiresPostCommitHanding(EntityPersister entityPersister) {
    return false;
  }

  /**
   * Returns true if the update changed only {@link #AUDIT_IGNORED_PROPERTIES} and no collection.
   * When Hibernate does not know which properties changed (an entity reattached without a dirty
   * check), the update counts as a real change.
   */
  static boolean isBookkeepingOnlyUpdate(PostUpdateEvent postUpdateEvent) {
    int[] dirtyProperties = postUpdateEvent.getDirtyProperties();
    Object[] oldState = postUpdateEvent.getOldState();

    if (dirtyProperties == null || oldState == null) {
      return false;
    }

    String[] propertyNames = postUpdateEvent.getPersister().getPropertyNames();

    for (int index : dirtyProperties) {
      if (!AUDIT_IGNORED_PROPERTIES.contains(propertyNames[index])) {
        return false;
      }
    }

    Type[] propertyTypes = postUpdateEvent.getPersister().getPropertyTypes();
    Object[] state = postUpdateEvent.getState();

    for (int i = 0; i < propertyTypes.length; i++) {
      if (propertyTypes[i].isCollectionType() && isChangedCollection(oldState[i], state[i])) {
        return false;
      }
    }

    return true;
  }

  /** A collection that was replaced, removed or changed in place. */
  private static boolean isChangedCollection(Object oldValue, Object newValue) {
    return oldValue != newValue
        || (newValue instanceof PersistentCollection collection && collection.isDirty());
  }

  private void sendAudit(PostUpdateEvent postUpdateEvent, Auditable auditable) {
    auditManager.send(
        Audit.builder()
            .auditType(getAuditType())
            .auditScope(auditable.scope())
            .createdAt(LocalDateTime.now())
            .createdBy(getCreatedBy())
            .object(postUpdateEvent.getEntity())
            .attributes(
                auditManager.collectAuditAttributes(
                    postUpdateEvent.getEntity(), postUpdateEvent.getEntity().getClass()))
            .auditableEntity(
                new AuditableEntity(
                    postUpdateEvent.getEntity().getClass(), createAuditEntry(postUpdateEvent)))
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
}
