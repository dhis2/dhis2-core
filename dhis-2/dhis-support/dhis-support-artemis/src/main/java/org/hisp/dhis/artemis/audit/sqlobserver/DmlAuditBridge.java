/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.artemis.audit.sqlobserver;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.AuditableEntity;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.audit.DmlEvent;
import org.hisp.dhis.audit.DmlEvent.DmlOperation;
import org.hisp.dhis.audit.DmlObservedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Bridges DML observer events to the existing audit system. Listens for {@link DmlObservedEvent}
 * and converts each {@link DmlEvent} into an {@link Audit} message sent via {@link AuditManager}.
 *
 * <p>Only processes events where the entity class has the {@link Auditable} annotation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DmlAuditBridge {

  private final AuditManager auditManager;

  @EventListener
  public void onDmlObserved(DmlObservedEvent event) {
    String currentUser = getCurrentUser();

    for (DmlEvent dmlEvent : event.getEvents()) {
      if (dmlEvent.getEntityClassName() == null) {
        continue;
      }

      try {
        Class<?> entityClass = Class.forName(dmlEvent.getEntityClassName());
        Auditable auditable = entityClass.getAnnotation(Auditable.class);
        if (auditable == null) {
          continue;
        }

        AuditType auditType = toAuditType(dmlEvent.getOperation());
        AuditScope auditScope = auditable.scope();
        String uid = dmlEvent.getEntityId() != null ? dmlEvent.getEntityId().toString() : null;

        Audit audit =
            Audit.builder()
                .auditType(auditType)
                .auditScope(auditScope)
                .createdAt(LocalDateTime.now())
                .createdBy(currentUser)
                .klass(dmlEvent.getEntityClassName())
                .uid(uid)
                .auditableEntity(new AuditableEntity(entityClass, null))
                .build();

        auditManager.send(audit);
      } catch (ClassNotFoundException e) {
        log.trace("Entity class not found for DML audit: {}", dmlEvent.getEntityClassName());
      } catch (Exception e) {
        log.warn("Failed to send DML audit for {}", dmlEvent.getEntityClassName(), e);
      }
    }
  }

  private static AuditType toAuditType(DmlOperation operation) {
    return switch (operation) {
      case INSERT -> AuditType.CREATE;
      case UPDATE -> AuditType.UPDATE;
      case DELETE -> AuditType.DELETE;
    };
  }

  private static String getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null ? auth.getName() : "system";
  }
}
