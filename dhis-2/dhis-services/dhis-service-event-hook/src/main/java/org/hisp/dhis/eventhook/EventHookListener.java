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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.eventhook.handlers.WebhookHandler;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Morten Olav Hansen
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventHookListener
{
    private final ObjectMapper objectMapper;

    private final FieldFilterService fieldFilterService;

    private final Map<String, List<Handler>> targets = new HashMap<>();

    private final List<EventHook> eventHooks = new ArrayList<>();

    private final EventHookService eventHookService;

    @Async
    @TransactionalEventListener( classes = Event.class, phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true )
    public void eventListener( Event event )
        throws EventHookException,
        JsonProcessingException
    {
        for ( EventHook eventHook : eventHooks )
        {
            if ( eventHook.getSource().getPath().startsWith( eventHook.getSource().getPath() ) )
            {
                if ( !targets.containsKey( eventHook.getUid() ) || targets.get( eventHook.getUid() ).isEmpty() )
                {
                    continue;
                }

                ObjectNode objectNode = fieldFilterService.toObjectNode( event.getObject(),
                    List.of( eventHook.getSource().getFields() ) );
                String payload = objectMapper.writeValueAsString( Map.of(
                    "path", event.getPath(),
                    "meta", event.getMeta(),
                    "object", objectNode ) );

                List<Handler> handlers = targets.get( eventHook.getUid() );

                for ( Handler handler : handlers )
                {
                    handler.run( payload );
                }
            }
        }
    }

    @PostConstruct
    @EventListener( ReloadEventHookListener.class )
    public void reload()
    {
        eventHooks.clear();
        targets.clear();

        eventHooks.addAll( eventHookService.getAll() );

        eventHooks.forEach( eh -> {
            targets.put( eh.getUid(), new ArrayList<>() );

            for ( Target target : eh.getTargets() )
            {
                if ( target.getType().equals( "webhook" ) )
                {
                    targets.get( eh.getUid() ).add( new WebhookHandler( (WebhookTarget) target ) );
                }
            }
        } );
    }
}
