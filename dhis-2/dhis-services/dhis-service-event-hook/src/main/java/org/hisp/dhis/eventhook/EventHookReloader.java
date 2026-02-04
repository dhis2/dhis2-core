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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.Getter;
import org.hisp.dhis.eventhook.handlers.WebhookReactiveHandler;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

public abstract class EventHookReloader {

  @Autowired private EventHookService eventHookService;

  @Autowired protected ApplicationContext applicationContext;

  @Getter
  protected transient EventHookContext eventHookContext = EventHookContext.builder().build();

  @PostConstruct
  @EventListener(OnEventHookChange.class)
  public void reload() {
    eventHookContext.closeTargets();

    List<EventHook> eventHooks = eventHookService.getAll();
    Map<String, List<ReactiveHandler>> targets = new HashMap<>();

    for (EventHook eh : eventHooks) {
      if (eh.isDisabled()) {
        continue;
      }

      targets.put(eh.getUid(), new ArrayList<>());

      for (Target target : eh.getTargets()) {
        if (WebhookTarget.TYPE.equals(target.getType())) {
          targets
              .get(eh.getUid())
              .add(new WebhookReactiveHandler(applicationContext, (WebhookTarget) target));
        }
      }
    }

    eventHookContext = EventHookContext.builder().eventHooks(eventHooks).targets(targets).build();
  }
}
