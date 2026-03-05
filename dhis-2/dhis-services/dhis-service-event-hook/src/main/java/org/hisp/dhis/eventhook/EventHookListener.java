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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Intercepts the event tied to the database commit, filters it, and persists it to the matching
 * event hook outbox tables as part of the web request transaction. An event that is not part of a
 * database transaction can also be intercepted (e.g., scheduler events) with the caveat that the
 * listener creates a new transaction instead of joining an existing one.
 *
 * <p>Any exception results in the listener logging the error, partially rolling back the
 * transaction, and swallowing the exception so that web request is not impacted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventHookListener {
  private final ObjectMapper objectMapper;

  private final FieldFilterService fieldFilterService;

  private final EventHookService eventHookService;

  private final DataSource dataSource;

  @TransactionalEventListener(
      classes = Event.class,
      phase = TransactionPhase.BEFORE_COMMIT,
      fallbackExecution = true)
  public void onPreCommit(Event event) {
    try {
      doOnPreCommit(event);
    } catch (Throwable t) {
      log.error(t.getMessage(), t);
    }
  }

  protected void doOnPreCommit(Event event) {
    Connection connection = null;
    try {
      connection = DataSourceUtils.getConnection(dataSource);
      JdbcTemplate jdbcTemplate =
          new JdbcTemplate(new SingleConnectionDataSource(connection, true));
      for (EventHookTargets eventHookTargets : eventHookService.getEventHookTargets()) {
        try {
          if (doPersistOutboxMessage(event, eventHookTargets)) {
            persistOutboxMessage(
                event,
                eventHookTargets,
                jdbcTemplate,
                DataSourceUtils.isConnectionTransactional(connection, dataSource));
          }
        } catch (Throwable t) {
          log.error(t.getMessage(), t);
        }
      }
    } finally {
      if (connection != null) {
        DataSourceUtils.releaseConnection(connection, dataSource);
      }
    }
  }

  protected void persistOutboxMessage(
      Event event,
      EventHookTargets eventHookTargets,
      JdbcTemplate jdbcTemplate,
      boolean inTransaction)
      throws JsonProcessingException {
    EventHook eventHook = eventHookTargets.getEventHook();
    final Event filteredEvent;
    if (event.getObject() instanceof Collection) {
      List<ObjectNode> objects = new ArrayList<>();

      for (Object object : ((Collection<?>) event.getObject())) {
        objects.add(fieldFilterService.toObjectNode(object, eventHook.getSource().getFields()));
      }

      filteredEvent = event.withObject(objects);
    } else {
      ObjectNode objectNode =
          fieldFilterService.toObjectNode(event.getObject(), eventHook.getSource().getFields());
      filteredEvent = event.withObject(objectNode);
    }

    if (filteredEvent != null) {
      String eventAsString = objectMapper.writeValueAsString(event);
      if (inTransaction) {
        jdbcTemplate.execute("SAVEPOINT event_hook_" + eventHook.getUID());
      }
      try {
        String outboxMessageInsert =
            String.format(
                "INSERT INTO \"%s\" (payload) VALUES (?::JSONB); ",
                EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID());
        if (inTransaction) {
          outboxMessageInsert += String.format("RELEASE event_hook_%s;", eventHook.getUID());
        }
        jdbcTemplate.update(outboxMessageInsert, eventAsString);
      } catch (DataAccessException e) {
        if (inTransaction) {
          jdbcTemplate.execute("ROLLBACK to event_hook_" + eventHook.getUID());
        }
        throw e;
      }
    }
  }

  protected boolean doPersistOutboxMessage(Event event, EventHookTargets eventHookTargets) {
    return event.getPath().startsWith(eventHookTargets.getEventHook().getSource().getPath())
        && !(event.getObject() instanceof EventHook)
        && !eventHookTargets.getTargets().isEmpty();
  }
}
