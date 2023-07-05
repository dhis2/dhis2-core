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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.event.DataValue;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.EventTestUtils;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class EventDataValueAggregatorTest {

  private EventDataValueAggregator subject;

  @BeforeEach
  void setUp() {
    this.subject = new EventDataValueAggregator();
  }

  private final ImportOptions importOptions = ImportOptions.getDefaultImportOptions();

  @Test
  void verifyAggregateDataValuesOnNewEvent() {
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event1 =
        createEvent(
            EventTestUtils.createDataValue("abcd", "val1"),
            EventTestUtils.createDataValue("efgh", "val2"),
            EventTestUtils.createDataValue("ilmn", "val3"));
    Map<String, Set<EventDataValue>> dataValues =
        subject.aggregateDataValues(Lists.newArrayList(event1), new HashMap<>(), importOptions);
    assertThat(dataValues, is(notNullValue()));
    assertThat(dataValues.keySet(), hasSize(1));
    assertThat(dataValues.get(event1.getUid()), hasSize(3));
    assertDataValue(dataValues.get(event1.getUid()), "abcd", "val1");
    assertDataValue(dataValues.get(event1.getUid()), "efgh", "val2");
    assertDataValue(dataValues.get(event1.getUid()), "ilmn", "val3");
  }

  @Test
  void verifyAggregateDataValuesOnExistingPSI() {
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event1 =
        createEvent(
            EventTestUtils.createDataValue("abcd", "val1"),
            EventTestUtils.createDataValue("efgh", "val2"),
            EventTestUtils.createDataValue("ilmn", "val3"));
    Map<String, Event> programStageInstanceMap = new HashMap<>();
    programStageInstanceMap.put(
        event1.getUid(),
        createPsi(
            event1.getUid(),
            EventTestUtils.createEventDataValue("abcd", "val1"),
            EventTestUtils.createEventDataValue("efgh", "val2"),
            EventTestUtils.createEventDataValue("ilmn", "val3")));
    Map<String, Set<EventDataValue>> dataValues =
        subject.aggregateDataValues(
            Lists.newArrayList(event1), programStageInstanceMap, importOptions);
    assertThat(dataValues, is(notNullValue()));
    assertThat(dataValues.keySet(), hasSize(1));
    assertThat(dataValues.get(event1.getUid()), hasSize(3));
    assertDataValue(dataValues.get(event1.getUid()), "abcd", "val1");
    assertDataValue(dataValues.get(event1.getUid()), "efgh", "val2");
    assertDataValue(dataValues.get(event1.getUid()), "ilmn", "val3");
  }

  @Test
  void verifyAggregateDataValuesOnExistingPSI_PayloadHasLessDataValues() {
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event1 =
        createEvent(EventTestUtils.createDataValue("abcd", "val1"));
    Map<String, Event> programStageInstanceMap = new HashMap<>();
    programStageInstanceMap.put(
        event1.getUid(),
        createPsi(
            event1.getUid(),
            EventTestUtils.createEventDataValue("abcd", "val1"),
            EventTestUtils.createEventDataValue("efgh", "val2"),
            EventTestUtils.createEventDataValue("ilmn", "val3")));
    Map<String, Set<EventDataValue>> dataValues =
        subject.aggregateDataValues(
            Lists.newArrayList(event1), programStageInstanceMap, importOptions);
    assertThat(dataValues, is(notNullValue()));
    assertThat(dataValues.keySet(), hasSize(1));
    assertThat(dataValues.get(event1.getUid()), hasSize(1));
    assertDataValue(dataValues.get(event1.getUid()), "abcd", "val1");
  }

  @Test
  void verifyAggregateDataValuesOnExistingPSI_PayloadHasMoreDataValues() {
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event1 =
        createEvent(
            EventTestUtils.createDataValue("abcd", "val1"),
            EventTestUtils.createDataValue("efgh", "val2"),
            EventTestUtils.createDataValue("ilmn", "val3"),
            EventTestUtils.createDataValue("gggg", "val4"));
    Map<String, Event> programStageInstanceMap = new HashMap<>();
    programStageInstanceMap.put(
        event1.getUid(),
        createPsi(
            event1.getUid(),
            EventTestUtils.createEventDataValue("abcd", "val1"),
            EventTestUtils.createEventDataValue("efgh", "val2"),
            EventTestUtils.createEventDataValue("ilmn", "val3")));
    Map<String, Set<EventDataValue>> dataValues =
        subject.aggregateDataValues(
            Lists.newArrayList(event1), programStageInstanceMap, importOptions);
    assertThat(dataValues, is(notNullValue()));
    assertThat(dataValues.keySet(), hasSize(1));
    assertThat(dataValues.get(event1.getUid()), hasSize(4));
    assertDataValue(dataValues.get(event1.getUid()), "abcd", "val1");
    assertDataValue(dataValues.get(event1.getUid()), "efgh", "val2");
    assertDataValue(dataValues.get(event1.getUid()), "ilmn", "val3");
    assertDataValue(dataValues.get(event1.getUid()), "gggg", "val4");
  }

  @Test
  void verifyAggregateDataValuesOnExistingPSI_PayloadHasNoDataValues() {
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event1 =
        new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();
    event1.setUid(CodeGenerator.generateUid());
    event1.setDataValues(Sets.newHashSet());
    Map<String, Event> programStageInstanceMap = new HashMap<>();
    programStageInstanceMap.put(
        event1.getUid(),
        createPsi(
            event1.getUid(),
            EventTestUtils.createEventDataValue("abcd", "val1"),
            EventTestUtils.createEventDataValue("efgh", "val2"),
            EventTestUtils.createEventDataValue("ilmn", "val3")));
    Map<String, Set<EventDataValue>> dataValues =
        subject.aggregateDataValues(
            Lists.newArrayList(event1), programStageInstanceMap, importOptions);
    assertThat(dataValues, is(notNullValue()));
    assertThat(dataValues.keySet(), hasSize(1));
    assertThat(dataValues.get(event1.getUid()), hasSize(0));
  }

  @Test
  void verifyAggregateDataValuesOnExistingPSI_PayloadHasEmptyDataValues() {
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event1 =
        createEvent(
            EventTestUtils.createDataValue("abcd", ""),
            EventTestUtils.createDataValue("efgh", ""),
            EventTestUtils.createDataValue("ilmn", ""));
    Map<String, Event> programStageInstanceMap = new HashMap<>();
    programStageInstanceMap.put(
        event1.getUid(),
        createPsi(
            event1.getUid(),
            EventTestUtils.createEventDataValue("abcd", "val1"),
            EventTestUtils.createEventDataValue("efgh", "val2"),
            EventTestUtils.createEventDataValue("ilmn", "val3")));
    Map<String, Set<EventDataValue>> dataValues =
        subject.aggregateDataValues(
            Lists.newArrayList(event1), programStageInstanceMap, importOptions);
    assertThat(dataValues, is(notNullValue()));
    assertThat(dataValues.keySet(), hasSize(1));
    assertThat(dataValues.get(event1.getUid()), hasSize(0));
  }

  @Test
  void verifyAggregateDataValuesOnExistingPSIwithMerge() {
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event1 =
        createEvent(EventTestUtils.createDataValue("abcd", "val5"));
    importOptions.setMergeDataValues(true);
    Map<String, Event> programStageInstanceMap = new HashMap<>();
    programStageInstanceMap.put(
        event1.getUid(),
        createPsi(
            event1.getUid(),
            EventTestUtils.createEventDataValue("abcd", "val1"),
            EventTestUtils.createEventDataValue("efgh", "val2"),
            EventTestUtils.createEventDataValue("ilmn", "val3")));
    Map<String, Set<EventDataValue>> dataValues =
        subject.aggregateDataValues(
            Lists.newArrayList(event1), programStageInstanceMap, importOptions);
    assertThat(dataValues, is(notNullValue()));
    assertThat(dataValues.keySet(), hasSize(1));
    assertThat(dataValues.get(event1.getUid()), hasSize(3));
    assertDataValue(dataValues.get(event1.getUid()), "abcd", "val5");
    assertDataValue(dataValues.get(event1.getUid()), "efgh", "val2");
    assertDataValue(dataValues.get(event1.getUid()), "ilmn", "val3");
  }

  @Test
  void verifyAggregateDataValuesOnExistingPSIwithMergeAndEmptyDataValue() {
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event1 =
        createEvent(EventTestUtils.createDataValue("abcd", ""));
    importOptions.setMergeDataValues(true);
    Map<String, Event> programStageInstanceMap = new HashMap<>();
    programStageInstanceMap.put(
        event1.getUid(),
        createPsi(
            event1.getUid(),
            EventTestUtils.createEventDataValue("abcd", "val1"),
            EventTestUtils.createEventDataValue("efgh", "val2"),
            EventTestUtils.createEventDataValue("ilmn", "val3")));
    Map<String, Set<EventDataValue>> dataValues =
        subject.aggregateDataValues(
            Lists.newArrayList(event1), programStageInstanceMap, importOptions);
    assertThat(dataValues, is(notNullValue()));
    assertThat(dataValues.keySet(), hasSize(1));
    assertThat(dataValues.get(event1.getUid()), hasSize(2));
    assertDataValue(dataValues.get(event1.getUid()), "efgh", "val2");
    assertDataValue(dataValues.get(event1.getUid()), "ilmn", "val3");
  }

  private Event createPsi(String uid, EventDataValue... eventDataValues) {
    Event event = new Event();
    event.setUid(uid);
    event.setEventDataValues(Sets.newHashSet(eventDataValues));
    return event;
  }

  private void assertDataValue(Set<EventDataValue> dataValues, String dataElement, String value) {
    dataValues.stream()
        .filter(dv -> dv.getDataElement().equals(dataElement))
        .findFirst()
        .ifPresent(
            dv -> {
              assertThat(dv.getValue(), is(value));
              assertThat(dv.getStoredBy(), is(nullValue()));
              assertThat(dv.getLastUpdated(), is(notNullValue()));
            });
  }

  private org.hisp.dhis.dxf2.deprecated.tracker.event.Event createEvent(DataValue... dataValues) {
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();
    event.setUid(CodeGenerator.generateUid());
    event.setDataValues(Sets.newHashSet(dataValues));
    return event;
  }
}
