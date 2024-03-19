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
package org.hisp.dhis.eventhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.eventhook.handlers.ConsoleHandler;
import org.hisp.dhis.eventhook.handlers.JmsHandler;
import org.hisp.dhis.eventhook.handlers.KafkaHandler;
import org.hisp.dhis.eventhook.handlers.WebhookHandler;
import org.hisp.dhis.eventhook.targets.ConsoleTarget;
import org.hisp.dhis.eventhook.targets.JmsTarget;
import org.hisp.dhis.eventhook.targets.KafkaTarget;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author Morten Olav Hansen
 */
@Component
@RequiredArgsConstructor
public class EventHookListener {
  private final ObjectMapper objectMapper;

  private final FieldFilterService fieldFilterService;

  private EventHookContext eventHookContext = EventHookContext.builder().build();

  private final EventHookService eventHookService;

  @Async("eventHookTaskExecutor")
  @TransactionalEventListener(
      classes = Event.class,
      phase = TransactionPhase.AFTER_COMMIT,
      fallbackExecution = true)
  public void eventListener(Event event) throws JsonProcessingException {
    for (EventHook eventHook : eventHookContext.getEventHooks()) {
      if (event.getPath().startsWith(eventHook.getSource().getPath())) {
        if (!eventHookContext.hasTarget(eventHook.getUid())) {
          continue;
        }

        if (event.getObject() instanceof Collection) {
          List<ObjectNode> objects = new ArrayList<>();

          for (Object object : ((Collection<?>) event.getObject())) {
            objects.add(fieldFilterService.toObjectNode(object, eventHook.getSource().getFields()));
          }

          event = event.withObject(objects);
        } else {
          ObjectNode objectNode =
              fieldFilterService.toObjectNode(event.getObject(), eventHook.getSource().getFields());
          event = event.withObject(objectNode);
        }

        String payload = objectMapper.writeValueAsString(event);

        List<Handler> handlers = eventHookContext.getTarget(eventHook.getUid());

        for (Handler handler : handlers) {
          handler.run(eventHook, event, payload);
        }
      }
    }
  }

  @PostConstruct
  @EventListener(ReloadEventHookListeners.class)
  public void reload() {
    eventHookContext.closeTargets();

    List<EventHook> eventHooks = eventHookService.getAll();
    Map<String, List<Handler>> targets = new HashMap<>();

    for (EventHook eh : eventHooks) {
      if (eh.isDisabled()) {
        continue;
      }

      targets.put(eh.getUid(), new ArrayList<>());

      for (Target target : eh.getTargets()) {
        if (WebhookTarget.TYPE.equals(target.getType())) {
          targets.get(eh.getUid()).add(new WebhookHandler((WebhookTarget) target));
        } else if (ConsoleTarget.TYPE.equals(target.getType())) {
          targets.get(eh.getUid()).add(new ConsoleHandler((ConsoleTarget) target));
        } else if (JmsTarget.TYPE.equals(target.getType())) {
          targets.get(eh.getUid()).add(new JmsHandler((JmsTarget) target));
        } else if (KafkaTarget.TYPE.equals(target.getType())) {
          targets.get(eh.getUid()).add(new KafkaHandler((KafkaTarget) target));
        }
      }
    }

    eventHookContext = EventHookContext.builder().eventHooks(eventHooks).targets(targets).build();
  }
}
