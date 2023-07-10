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
package org.hisp.dhis.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
public abstract class AbstractAuditConsumer implements AuditConsumer {
  protected AuditService auditService;

  protected ObjectMapper objectMapper;

  protected boolean isAuditLogEnabled;

  protected boolean isAuditDatabaseEnabled;

  protected void _consume(TextMessage message) {
    try {
      org.hisp.dhis.artemis.audit.Audit auditMessage =
          objectMapper.readValue(message.getText(), org.hisp.dhis.artemis.audit.Audit.class);

      if (auditMessage.getData() != null && !(auditMessage.getData() instanceof String)) {
        auditMessage.setData(objectMapper.writeValueAsString(auditMessage.getData()));
      }

      org.hisp.dhis.audit.Audit audit = auditMessage.toAudit();

      if (isAuditLogEnabled) {
        log.info(objectMapper.writeValueAsString(audit));
      }

      if (isAuditDatabaseEnabled) {
        auditService.addAudit(audit);
      }
    } catch (IOException e) {
      log.error(
          "An error occurred de-serializing the message payload. The message can not be de-serialized to an Audit object.",
          e);
    } catch (Exception e) {
      log.error("An error occurred persisting an Audit message of type 'TRACKER'", e);
    }
  }
}
