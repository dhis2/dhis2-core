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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.context;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.event.DataValue;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.Event;

/**
 * Consolidates Event Data Values in a single Map, where the key is the Event uid and the value is a
 * Set of {@see EventDataValue}, ready for persistence.
 *
 * @author Luciano Fiandesio
 */
public class EventDataValueAggregator {
  public Map<String, Set<EventDataValue>> aggregateDataValues(
      List<org.hisp.dhis.dxf2.deprecated.tracker.event.Event> events,
      Map<String, Event> programStageInstanceMap,
      ImportOptions importOptions) {
    checkNotNull(programStageInstanceMap);
    checkNotNull(events);

    Map<String, Set<EventDataValue>> eventDataValueMap = new HashMap<>();
    for (org.hisp.dhis.dxf2.deprecated.tracker.event.Event event : events) {
      if (isNew(event.getUid(), programStageInstanceMap)) {
        eventDataValueMap.put(event.getUid(), getDataValues(event.getDataValues()));
      } else {
        eventDataValueMap.put(
            event.getUid(), aggregateForUpdate(event, programStageInstanceMap, importOptions));
      }
    }
    return eventDataValueMap;
  }

  private Set<EventDataValue> aggregateForUpdate(
      org.hisp.dhis.dxf2.deprecated.tracker.event.Event event,
      Map<String, Event> programStageInstanceMap,
      ImportOptions importOptions) {
    final Event programStageInstance = programStageInstanceMap.get(event.getUid());

    Set<EventDataValue> eventDataValues = new HashSet<>();

    if (importOptions.isMergeDataValues()) {
      // FIXME Luciano - this will not work if Id Scheme for data value is
      // not UID
      List<String> eventDataValueDataElementUids =
          event.getDataValues().stream()
              .map(DataValue::getDataElement)
              .collect(Collectors.toList());

      programStageInstance
          .getEventDataValues()
          .forEach(
              edv -> {
                if (!eventDataValueDataElementUids.contains(edv.getDataElement())) {
                  eventDataValues.add(edv);
                }
              });
    }

    eventDataValues.addAll(getDataValues(programStageInstance, event.getDataValues()));

    return eventDataValues;
  }

  private Set<EventDataValue> getDataValues(Event psi, Set<DataValue> dataValues) {
    Set<EventDataValue> result = new HashSet<>();

    for (DataValue dataValue : dataValues) {
      EventDataValue eventDataValue = exist(dataValue, psi.getEventDataValues());
      if (isNotEmpty(dataValue.getValue()) && !dataValue.getValue().equals("null")) {
        result.add(toEventDataValue(dataValue, eventDataValue));
      }
    }

    return result;
  }

  private EventDataValue toEventDataValue(DataValue dataValue, EventDataValue existing) {
    EventDataValue eventDataValue = new EventDataValue();
    eventDataValue.setDataElement(dataValue.getDataElement());
    eventDataValue.setValue(dataValue.getValue());
    // storedBy is always set by the EventStoredByPreProcessor
    eventDataValue.setStoredBy(dataValue.getStoredBy());

    eventDataValue.setProvidedElsewhere(dataValue.getProvidedElsewhere());

    if (existing != null) {
      eventDataValue.setCreated(existing.getCreated());
    }
    eventDataValue.setLastUpdated(new Date());
    return eventDataValue;
  }

  private EventDataValue exist(DataValue dataValue, Set<EventDataValue> eventDataValues) {
    for (EventDataValue eventDataValue : eventDataValues) {
      final String dataElement = eventDataValue.getDataElement();
      if (isNotEmpty(dataValue.getDataElement())
          && dataValue.getDataElement().equals(dataElement)) {
        return eventDataValue;
      }
    }
    return null;
  }

  private Set<EventDataValue> getDataValues(Set<DataValue> dataValues) {
    return dataValues.stream()
        // do not include data values with no value
        .filter(dv -> isNotEmpty(dv.getValue()))
        .map(dv -> toEventDataValue(dv, null))
        .collect(Collectors.toSet());
  }

  private boolean isNew(String eventUid, Map<String, Event> programStageInstanceMap) {
    return !programStageInstanceMap.containsKey(eventUid);
  }
}
