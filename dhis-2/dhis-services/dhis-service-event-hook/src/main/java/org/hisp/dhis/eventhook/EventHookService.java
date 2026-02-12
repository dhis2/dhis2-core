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
package org.hisp.dhis.eventhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.eventhook.handlers.WebhookReactiveHandler;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen
 */
@Service
@RequiredArgsConstructor
public class EventHookService {
  public static final String OUTBOX_PREFIX_TABLE_NAME = "outbox_";

  private final JdbcTemplate jdbcTemplate;

  private final EntityManager entityManager;

  private final EventHookStore eventHookStore;

  private final ObjectMapper objectMapper;

  private final EventHookSecretManager secretManager;

  private final CacheProvider cacheProvider;

  private final ApplicationContext applicationContext;

  private Cache<EventHookTargets> eventHookTargetsCache;

  @Setter @Getter private int partitionRange = 100000;

  @PostConstruct
  public void postConstruct() throws Exception {
    eventHookTargetsCache = cacheProvider.createEventHookTargetsCache();
    reload();
  }

  @Nonnull
  public List<EventHook> getAll() {
    List<EventHook> eventHooks = new ArrayList<>();

    for (EventHook eventHook : eventHookStore.getAll()) {
      try {
        EventHook eh =
            objectMapper.readValue(objectMapper.writeValueAsString(eventHook), EventHook.class);
        secretManager.decrypt(eh);
        eventHooks.add(eh);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    return eventHooks;
  }

  public List<EventHookTargets> getEventHookTargets() {
    return eventHookTargetsCache.getAll().toList();
  }

  @EventListener(OnEventHookChange.class)
  protected void reload() throws Exception {
    eventHookTargetsCache.getAll().forEach(EventHookTargets::closeTargets);
    eventHookTargetsCache.invalidateAll();

    List<EventHook> eventHooks = getAll();

    for (EventHook eh : eventHooks.stream().filter(eh -> !eh.isDisabled()).toList()) {
      List<ReactiveHandler> handlers = new ArrayList<>();
      for (Target target : eh.getTargets()) {
        if (WebhookTarget.TYPE.equals(target.getType())) {
          handlers.add(new WebhookReactiveHandler(applicationContext, (WebhookTarget) target));
        }
      }
      eventHookTargetsCache.put(
          eh.getUID().getValue(),
          EventHookTargets.builder().eventHook(eh).targets(handlers).build());
    }
  }

  public void createOutbox(EventHook eventHook) {
    String outboxTableName = OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID();
    deleteOutbox(eventHook.getUID());
    jdbcTemplate.execute(
        String.format(
            "CREATE TABLE \"%s\" (id BIGINT GENERATED ALWAYS AS IDENTITY (CYCLE) PRIMARY KEY, payload JSONB) PARTITION BY RANGE (id);",
            outboxTableName));
    addOutboxPartition(eventHook, 0, 1, partitionRange);

    EventHookOutboxLog eventHookOutboxLog = new EventHookOutboxLog();
    eventHookOutboxLog.setOutboxTableName(outboxTableName);
    eventHookOutboxLog.setNextOutboxMessageId(1);
    eventHookOutboxLog.setEventHook(eventHook);

    entityManager.persist(eventHookOutboxLog);
  }

  public void deleteOutbox(UID eventHookUid) {
    jdbcTemplate.execute(
        String.format("DROP TABLE IF EXISTS \"%s\"", OUTBOX_PREFIX_TABLE_NAME + eventHookUid));
  }

  public void addOutboxPartition(
      EventHook eventHook, long index, long lowerBound, long upperBound) {
    jdbcTemplate.execute(
        String.format(
            "CREATE TABLE IF NOT EXISTS \"%s\" PARTITION OF \"%s\" FOR VALUES FROM (%s) TO (%s);",
            OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID() + "_" + index,
            OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID(),
            lowerBound,
            upperBound));
  }
}
