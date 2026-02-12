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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.eventhook.handlers.ReactiveHandlerCallback;
import org.hisp.dhis.leader.election.LeaderManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class OutboxDrain {
  private final EntityManagerFactory entityManagerFactory;
  private final LeaderManager leaderManager;
  private final Map<UID, Semaphore> semaphores = new ConcurrentHashMap<>();
  private final EntityManager entityManager;
  private final JdbcTemplate jdbcTemplate;
  private final EventHookService eventHookService;

  private record DefaultHandlerCallback(
      EventHookOutboxLog eventHookOutboxLog,
      EntityManagerFactory entityManagerFactory,
      Semaphore semaphore)
      implements ReactiveHandlerCallback {

    @Override
    public void onError(Map<String, Object> outboxMessageCause) {
      updateOutboxLog((Long) outboxMessageCause.get("id"));
    }

    @Override
    public void onSuccess(Map<String, Object> lastSuccessfulOutboxMessage) {
      updateOutboxLog((Long) lastSuccessfulOutboxMessage.get("id") + 1);
    }

    @Override
    public void onComplete() {
      semaphore.release();
    }

    private void updateOutboxLog(long nextOutboxMessageId) {
      eventHookOutboxLog.setNextOutboxMessageId(nextOutboxMessageId);
      try (EntityManager em = entityManagerFactory.createEntityManager()) {
        em.merge(eventHookOutboxLog);
        em.flush();
      }
    }
  }

  @Scheduled(fixedRate = 100)
  @Qualifier("outboxDrainTaskScheduler")
  public void drainOutboxes() {
    if (leaderManager.isLeader()) {
      for (EventHookTargets eventHookTargets : eventHookService.getEventHookTargets()) {
        Semaphore semaphore =
            semaphores.computeIfAbsent(
                eventHookTargets.getEventHook().getUID(), s -> new Semaphore(1));
        try {
          if (semaphore.tryAcquire()) {
            drainOutbox(eventHookTargets, semaphore);
          }
        } catch (Throwable t) {
          semaphore.release();
          throw t;
        }
      }
    }
  }

  private void drainOutbox(EventHookTargets eventHookTargets, Semaphore semaphore) {
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
        emit(
            outboxMessages,
            eventHookTargets,
            new DefaultHandlerCallback(eventHookOutboxLog, entityManagerFactory, semaphore));
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
