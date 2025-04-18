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
package org.hisp.dhis.audit.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.TextMessage;
import java.util.Objects;
import org.hisp.dhis.artemis.Topics;
import org.hisp.dhis.audit.AbstractAuditConsumer;
import org.hisp.dhis.audit.AuditService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Tracker audit consumer.
 *
 * @author Morten Olav Hansen <morten@dhis2.org>
 */
@Component
public class TrackerAuditConsumer extends AbstractAuditConsumer {
  public TrackerAuditConsumer(
      AuditService auditService, ObjectMapper objectMapper, DhisConfigurationProvider dhisConfig) {
    this.auditService = auditService;
    this.objectMapper = objectMapper;

    // for legacy reasons we are overriding the default here and using "off"
    // for tracking logger (we don't have a specific key for tracker logger)
    this.isAuditLogEnabled =
        Objects.equals(dhisConfig.getPropertyOrDefault(ConfigurationKey.AUDIT_LOGGER, "off"), "on");
    this.isAuditDatabaseEnabled = dhisConfig.isEnabled(ConfigurationKey.AUDIT_DATABASE);
  }

  @JmsListener(destination = Topics.TRACKER_TOPIC_NAME)
  public void consume(TextMessage message) {
    _consume(message);
  }
}
