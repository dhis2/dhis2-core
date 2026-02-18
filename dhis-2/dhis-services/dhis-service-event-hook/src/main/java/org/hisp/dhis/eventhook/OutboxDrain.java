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
package org.hisp.dhis.eventhook;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import javax.annotation.concurrent.NotThreadSafe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.eventhook.handlers.ReactiveHandlerCallback;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.leader.election.LeaderManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * If cluster leader and event hooks are enabled then:
 *
 * <ul>
 *   <li>Reads a sequence of message in batch from each outbox table
 *   <li>Emits the event pulled out from the outbox message to the event hook target
 * </ul>
 *
 * A batch consists of messages with IDs equal to or greater than the <code>nextOutboxMessageId
 * </code> column found in the corresponding event hook row of the <code>
 * EventHookOutboxLog</code> table.
 *
 * <p>When each event in the batch is successfully delivered, <code>OutboxDrain</code> updates the
 * event hook's offset in <code>EventHookOutboxLog</code> to point to the starting message ID for
 * the next batch. In contrast, a failed delivery in the batch will cause <code>OutboxDrain
 * </code> to update the offset such that it points to the failed message ID. <code>OutboxDrain
 * </code/> will keep retrying to deliver the failed event until it is successful before moving on
 * to the next outbox message. Since draining does not happen within a transaction, it is possible
 * that successful messages are re-drained following an unexpected failure. This can lead to
 * duplicate messages on a non-idempotent event hook consumer.
 *
 * <p>A single thread is routinely dispatched to drain the outbox messages so draining should be
 * non-blocking as far as possible. Blocking I/O for emitting events will destroy the performance of
 * this class. Furthermore, events must be emitted in sequence per event hook to guarantee ordering.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@NotThreadSafe
public class OutboxDrain {
  private final LeaderManager leaderManager;
  private final Map<UID, Semaphore> semaphores = new HashMap<>();
  private final JdbcTemplate jdbcTemplate;
  private final EventHookService eventHookService;
  private final DhisConfigurationProvider dhisConfig;

  @PersistenceUnit private final EntityManagerFactory entityManagerFactory;

  private record DefaultHandlerCallback(
      EventHookOutboxLog eventHookOutboxLog,
      EntityManagerFactory entityManagerFactory,
      EventHook eventHook,
      Semaphore semaphore)
      implements ReactiveHandlerCallback {

    @Override
    public void onError(Throwable throwable, Map<String, Object> outboxMessageCause) {
      updateOutboxLog((Long) outboxMessageCause.get("id"));
      log.warn(
          "Failed to deliver outbox message {} to event hook [{}]. Retaining message and retrying until successfully delivered before emitting next message to this event hook. Error => {}",
          outboxMessageCause.get("id"),
          eventHook.getName(),
          throwable.getMessage());
    }

    @Override
    public void onSuccess(Map<String, Object> lastSuccessfulOutboxMessage) {
      updateOutboxLog((Long) lastSuccessfulOutboxMessage.get("id") + 1);
    }

    @Override
    public void onComplete() {
      semaphore.release();
    }

    private void updateOutboxLog(long newNextOutboxMessageId) {
      if (eventHookOutboxLog.getNextOutboxMessageId() != newNextOutboxMessageId) {
        eventHookOutboxLog.setNextOutboxMessageId(newNextOutboxMessageId);
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
          em.merge(eventHookOutboxLog);
          em.flush();
        }
      }
    }
  }

  @Scheduled(fixedRate = 100, scheduler = "singleThreadedTaskScheduler")
  public void drainOutboxes() {
    if (dhisConfig.isEnabled(ConfigurationKey.EVENT_HOOKS_ENABLED) && leaderManager.isLeader()) {
      try (EntityManager em = entityManagerFactory.createEntityManager()) {
        List<EventHookTargets> eventHooksTargets = eventHookService.getEventHookTargets();
        Map<UID, Semaphore> activeSemaphores = new HashMap<>();
        for (EventHookTargets eventHookTargets : eventHooksTargets) {
          Semaphore semaphore = null;
          try {
            semaphore =
                semaphores.getOrDefault(eventHookTargets.getEventHook().getUID(), new Semaphore(1));
            activeSemaphores.put(eventHookTargets.getEventHook().getUID(), semaphore);
            if (semaphore.tryAcquire()) {
              drainOutbox(eventHookTargets, semaphore, em);
            }
          } catch (Throwable t) {
            if (semaphore != null) {
              semaphore.release();
            }
            log.error(t.getMessage(), t);
          }
        }

        semaphores.clear();
        semaphores.putAll(activeSemaphores);
      }
    }
  }

  protected void drainOutbox(
      EventHookTargets eventHookTargets, Semaphore semaphore, EntityManager entityManager) {
    String outboxTableName =
        EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHookTargets.getEventHook().getUID();
    EventHookOutboxLog eventHookOutboxLog =
        entityManager.find(EventHookOutboxLog.class, outboxTableName);
    List<Map<String, Object>> outboxMessages = Collections.emptyList();
    if (eventHookOutboxLog != null) {
      outboxMessages =
          jdbcTemplate.queryForList(
              String.format(
                  "SELECT * FROM \"%s\" WHERE id >= ? ORDER BY id LIMIT 100", outboxTableName),
              eventHookOutboxLog.getNextOutboxMessageId());

      if (!outboxMessages.isEmpty()) {
        entityManager.detach(eventHookOutboxLog);
        emit(
            outboxMessages,
            eventHookTargets,
            new DefaultHandlerCallback(
                eventHookOutboxLog,
                entityManagerFactory,
                eventHookTargets.getEventHook(),
                semaphore));
      }
    }

    if (outboxMessages.isEmpty()) {
      semaphore.release();
    }
  }

  protected void emit(
      List<Map<String, Object>> outboxMessages,
      EventHookTargets eventHookTargets,
      ReactiveHandlerCallback handlerCallback) {
    List<ReactiveHandler> handlers = eventHookTargets.getTargets();

    Flux<Map<String, Object>> outboxMessagesFlux = Flux.fromIterable(outboxMessages);
    for (ReactiveHandler handler : handlers) {
      handler.accept(outboxMessagesFlux, handlerCallback);
    }
  }
}
