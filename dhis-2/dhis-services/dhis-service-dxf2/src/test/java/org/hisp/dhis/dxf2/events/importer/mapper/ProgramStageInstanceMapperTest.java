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
package org.hisp.dhis.dxf2.events.importer.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStageInstance;
import org.junit.jupiter.api.Test;

class ProgramStageInstanceMapperTest {

  private final ProgramStageInstanceMapper programStageInstanceMapper;

  private final Event event;

  private final String dataElementUid = "ABC12345678";

  ProgramStageInstanceMapperTest() {
    // Identifiers
    String dataElementCode = "DE_CODE";
    String eventUid = "ABC23456789";

    // Set up DataElement
    DataElement de = new DataElement();
    de.setUid(dataElementUid);
    de.setCode(dataElementCode);

    // Set up DataValue; identifier is CODE.
    DataValue dv = new DataValue();
    dv.setDataElement(de.getCode());
    dv.setValue("VALUE");

    // Set up Event
    Event event = new Event();
    event.setUid(eventUid);
    event.setEvent(eventUid);
    event.setDataValues(Set.of(dv));

    // Prepare WorkContext collections
    Map<String, DataElement> dataElementMap = new HashMap<>();
    Map<String, Set<EventDataValue>> dataValuesMap = new HashMap<>();

    // populate dataElementMap. Identifier is CODE, value is the DataElement
    dataElementMap.put(de.getCode(), de);

    // convert DataValues to EventDataValues
    dataValuesMap.put(
        event.getUid(),
        event.getDataValues().stream()
            .map(
                r -> {
                  EventDataValue edv = new EventDataValue();
                  edv.setDataElement(r.getDataElement());
                  edv.setValue(r.getValue());
                  return edv;
                })
            .collect(Collectors.toSet()));

    // Initialize workContext, mapper and event.
    this.programStageInstanceMapper =
        new ProgramStageInstanceMapper(
            WorkContext.builder()
                .dataElementMap(dataElementMap)
                .programStageInstanceMap(new HashMap<>())
                .programInstanceMap(new HashMap<>())
                .programsMap(new HashMap<>())
                .organisationUnitMap(new HashMap<>())
                .categoryOptionComboMap(new HashMap<>())
                .eventDataValueMap(dataValuesMap)
                .importOptions(ImportOptions.getDefaultImportOptions().setIdScheme("CODE"))
                .build());

    this.event = event;
  }

  @Test
  void mapShouldChangeIdentifierFromCodeToUid() {
    ProgramStageInstance psi = programStageInstanceMapper.map(event);

    assertTrue(
        psi.getEventDataValues().stream()
            .anyMatch(dv -> dv.getDataElement().equals(dataElementUid)));
  }
}
