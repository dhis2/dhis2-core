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
package org.hisp.dhis.dxf2.deprecated.tracker.aggregates;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.hisp.dhis.dxf2.deprecated.tracker.aggregates.ThreadPoolManager.getPool;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dxf2.deprecated.tracker.event.DataValue;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Note;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Relationship;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.EventStore;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 * @deprecated this is a class related to "old" (deprecated) tracker which will be removed with
 *     "old" tracker. Make sure to plan migrating to new tracker.
 */
@Component
@RequiredArgsConstructor
@Deprecated(since = "2.41")
public class EventAggregate extends AbstractAggregate {
  @Nonnull private final EventStore eventStore;

  /**
   * Key: enrollment uid -> Value: Event
   *
   * @param ids a List of {@see Enrollment} Primary Keys
   * @param ctx the {@see AggregateContext}
   * @return a Map where the key is a Enrollment Primary Key, and the value is a List of {@see
   *     Event}
   */
  Multimap<String, Event> findByEnrollmentIds(List<Long> ids, AggregateContext ctx) {
    // Fetch all the Events that are linked to the given Enrollment IDs

    Multimap<String, Event> events = this.eventStore.getEventsByEnrollmentIds(ids, ctx);

    if (events.isEmpty()) {
      return events;
    }

    List<Long> eventIds = events.values().stream().map(Event::getId).collect(Collectors.toList());

    /*
     * Async fetch Relationships for the given Event ids (only if
     * isIncludeRelationships = true)
     */
    final CompletableFuture<Multimap<String, Relationship>> relationshipAsync =
        conditionalAsyncFetch(
            ctx.getParams().getEventParams().isIncludeRelationships(),
            () -> eventStore.getRelationships(eventIds, ctx),
            getPool());

    /*
     * Async fetch Notes for the given Event ids
     */
    final CompletableFuture<Multimap<String, Note>> notesAsync =
        asyncFetch(() -> eventStore.getNotes(eventIds), getPool());

    /*
     * Async fetch DataValues for the given Event ids
     */
    final CompletableFuture<Map<String, List<DataValue>>> dataValuesAsync =
        supplyAsync(() -> eventStore.getDataValues(eventIds), getPool());

    return allOf(dataValuesAsync, notesAsync, relationshipAsync)
        .thenApplyAsync(
            fn -> {
              Map<String, List<DataValue>> dataValues = dataValuesAsync.join();
              Multimap<String, Note> notes = notesAsync.join();
              Multimap<String, Relationship> relationships = relationshipAsync.join();

              for (Event event : events.values()) {
                if (ctx.getParams().isIncludeRelationships()) {
                  event.setRelationships(new HashSet<>(relationships.get(event.getEvent())));
                }

                List<DataValue> dataValuesForEvent = dataValues.get(event.getEvent());
                if (dataValuesForEvent != null && !dataValuesForEvent.isEmpty()) {
                  event.setDataValues(new HashSet<>(dataValues.get(event.getEvent())));
                }
                event.setNotes(new ArrayList<>(notes.get(event.getEvent())));
              }

              return events;
            },
            getPool())
        .join();
  }
}
