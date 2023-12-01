/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.artemis.audit;

import com.google.common.base.Strings;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.artemis.MessageManager;
import org.hisp.dhis.audit.AuditScope;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
@Component
public class AuditProducerSupplier {
  private final MessageManager messageManager;

  private final Map<AuditScope, String> auditScopeDestinationMap;

  public AuditProducerSupplier(
      MessageManager messageManager, Map<AuditScope, String> auditScopeDestinationMap) {
    this.messageManager = messageManager;
    this.auditScopeDestinationMap = auditScopeDestinationMap;
  }

  public void publish(Audit audit) {
    String topic = getTopicName(audit);

    if (!Strings.isNullOrEmpty(topic)) {
      if (log.isDebugEnabled()) {
        log.debug(
            "sending auditing message to topic: [" + topic + "] with content: " + audit.toLog());
      }
      this.messageManager.send(topic, audit);
    } else {
      log.error(
          String.format(
              "Unable to map AuditScope [%s] to a topic name. Sending aborted",
              audit.getAuditScope()));
    }
  }

  private String getTopicName(Audit audit) {
    return auditScopeDestinationMap.get(audit.getAuditScope());
  }
}
