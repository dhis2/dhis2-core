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
package org.hisp.dhis.cacheinvalidation.sqlobserver;

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.audit.DmlEvent;
import org.hisp.dhis.audit.DmlObservedEvent;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges DML observer events to the Redis cache invalidation channel. Publishes cache invalidation
 * messages for each DML event that has a mapped entity class.
 *
 * <p>When entityId is available, publishes fine-grained invalidation. When entityId is null,
 * publishes with "unknown" ID for coarse-grained fallback invalidation.
 */
@Slf4j
@Component
@Conditional(DmlCacheInvalidationCondition.class)
public class DmlCacheInvalidationBridge {

  public DmlCacheInvalidationBridge() {}

  @EventListener
  public void onDmlObserved(DmlObservedEvent event) {
    for (DmlEvent dmlEvent : event.getEvents()) {
      if (dmlEvent.getEntityClassName() == null) {
        continue;
      }

      String op = dmlEvent.getOperation().name().toLowerCase();
      String entityId =
          dmlEvent.getEntityId() != null ? dmlEvent.getEntityId().toString() : "unknown";

      String message =
          "serverInstanceId" + ":" + op + ":" + dmlEvent.getEntityClassName() + ":" + entityId;

      log.debug("Publishing DML cache invalidation: {}", message);
      //      messagePublisher.publish(CHANNEL_NAME, message);
    }
  }
}
