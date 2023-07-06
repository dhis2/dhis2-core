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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Buffers Audit messages prior to sending them to the Audit queue. This scheduler is disabled by
 * default (config key: audit.inmemory-queue.enabled) and should be used only in very high-traffic
 * environments. Note that upon a JVM crash, the Audit messages in this queue will be lost.
 *
 * <p>The buffer is based on a {@link DelayQueue} where messages are buffered for 5 seconds, before
 * being de-queued to the Artemis broker.
 *
 * <p>To avoid excessive memory pressure, max 200 messages can stay in the queue: in-excess messages
 * are processed immediately.
 *
 * @author Luciano Fiandesio
 */
@Slf4j
@Component
public class AuditScheduler {
  private static final long DELAY = 5_000; // 5 seconds

  private static final int MAX_SIZE = 200;

  private final AuditProducerSupplier auditProducerSupplier;

  private final BlockingQueue<QueuedAudit> delayed = new DelayQueue<>();

  public AuditScheduler(AuditProducerSupplier auditProducerSupplier) {
    this.auditProducerSupplier = auditProducerSupplier;
  }

  public void addAuditItem(final Audit auditItem) {
    if (log.isDebugEnabled()) {
      log.debug(
          String.format("add Audit object with content %s to delayed queue", auditItem.toLog()));
    }

    final QueuedAudit postponed = new QueuedAudit(auditItem, DELAY);

    if (delayed.size() >= MAX_SIZE) {
      auditProducerSupplier.publish(auditItem);
    } else {
      if (!delayed.contains(postponed)) {
        delayed.offer(postponed);
      }
    }
  }

  @Scheduled(fixedDelay = 5_000)
  public void process() {
    final Collection<QueuedAudit> expired = new ArrayList<>();

    delayed.drainTo(expired);

    expired.stream().map(QueuedAudit::getAuditItem).forEach(auditProducerSupplier::publish);
  }
}
