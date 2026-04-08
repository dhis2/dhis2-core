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
package org.hisp.dhis.tracker.export.event;

import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.springframework.transaction.annotation.Transactional;

public abstract class EventChangeLogService<T, S extends SoftDeletableObject> {

  private final EventService eventService;
  private final HibernateEventChangeLogStore<T, S> hibernateEventChangeLogStore;

  protected EventChangeLogService(
      EventService eventService, HibernateEventChangeLogStore<T, S> hibernateEventChangeLogStore) {
    this.eventService = eventService;
    this.hibernateEventChangeLogStore = hibernateEventChangeLogStore;
  }

  @Nonnull
  @Transactional(readOnly = true)
  public Page<EventChangeLog> getEventChangeLog(
      UID event, EventChangeLogOperationParams operationParams, PageParams pageParams)
      throws NotFoundException {
    if (!eventService.exists(event)) {
      throw new NotFoundException(Event.class, event);
    }

    return hibernateEventChangeLogStore.getEventChangeLogs(event, operationParams, pageParams);
  }

  @Transactional
  public void deleteEventChangeLog(S event) {
    hibernateEventChangeLogStore.deleteEventChangeLog(event);
  }

  @Transactional
  public void deleteEventChangeLog(DataElement dataElement) {
    hibernateEventChangeLogStore.deleteEventChangeLog(dataElement);
  }

  @Transactional(readOnly = true)
  public Set<String> getOrderableFields() {
    return hibernateEventChangeLogStore.getOrderableFields();
  }

  public Set<Pair<String, Class<?>>> getFilterableFields() {
    return hibernateEventChangeLogStore.getFilterableFields();
  }
}
