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
package org.hisp.dhis.reservedvalue;

import static java.util.Calendar.DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import org.hisp.dhis.common.Objects;
import org.hisp.dhis.textpattern.DefaultTextPatternService;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternGenerationException;
import org.hisp.dhis.textpattern.TextPatternParser;
import org.hisp.dhis.textpattern.TextPatternService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ReservedValueServiceTest {

  private ReservedValueService reservedValueService;

  private final TextPatternService textPatternService = new DefaultTextPatternService();

  @Mock private ReservedValueStore reservedValueStore;

  @Mock private ValueGeneratorService valueGeneratorService;

  @Captor private ArgumentCaptor<ReservedValue> reservedValue;

  private static final String simpleText = "\"FOO\"";

  private static final String sequentialText = "\"TEST-\"+SEQUENTIAL(##)";

  private static final String randomText = "\"TEST-\"+RANDOM(XXX)";

  private static Date futureDate;

  private static final String ownerUid = "uid";

  @BeforeEach
  void setUpClass() {
    reservedValueService =
        new DefaultReservedValueService(
            textPatternService, reservedValueStore, valueGeneratorService);
    Calendar calendar = Calendar.getInstance();
    calendar.add(DATE, 1);
    futureDate = calendar.getTime();
  }

  @Test
  void shouldReserveSimpleTextPattern()
      throws TextPatternParser.TextPatternParsingException,
          TextPatternGenerationException,
          ReserveValueException {
    assertEquals(
        1,
        reservedValueService
            .reserve(
                createTrackedEntityAttribute(Objects.TRACKEDENTITYATTRIBUTE, ownerUid, simpleText),
                1,
                new HashMap<>(),
                futureDate)
            .size());
    verify(reservedValueStore, times(1)).reserveValues(any());
  }

  @Test
  void shouldNotReserveSimpleTextPatternAlreadyReserved() {
    when(reservedValueStore.getNumberOfUsedValues(reservedValue.capture())).thenReturn(1);
    assertThrows(
        ReserveValueException.class,
        () ->
            reservedValueService.reserve(
                createTrackedEntityAttribute(Objects.TRACKEDENTITYATTRIBUTE, ownerUid, simpleText),
                1,
                new HashMap<>(),
                futureDate));
    assertEquals(Objects.TRACKEDENTITYATTRIBUTE.name(), reservedValue.getValue().getOwnerObject());
    assertEquals(ownerUid, reservedValue.getValue().getOwnerUid());
    verify(reservedValueStore, times(0)).reserveValues(any());
  }

  @Test
  void shouldNotReserveValuesSequentialPattern()
      throws TextPatternParser.TextPatternParsingException,
          TextPatternGenerationException,
          ReserveValueException,
          ExecutionException,
          InterruptedException {
    when(valueGeneratorService.generateValues(any(), any(), any(), anyInt()))
        .thenReturn(Arrays.asList("12", "34"));
    int requestedValues = 10;
    assertEquals(
        requestedValues,
        reservedValueService
            .reserve(
                createTrackedEntityAttribute(
                    Objects.TRACKEDENTITYATTRIBUTE, ownerUid, sequentialText),
                requestedValues,
                new HashMap<>(),
                futureDate)
            .size());
    verify(reservedValueStore, times(0)).reserveValues(any());
    verify(reservedValueStore, times(0)).bulkInsertReservedValues(anyList());
  }

  @Test
  void shouldReserveValuesRandomPattern()
      throws TextPatternParser.TextPatternParsingException,
          TextPatternGenerationException,
          ReserveValueException {
    when(reservedValueStore.getAvailableValues(any(), any(), any()))
        .thenReturn(
            Arrays.asList(
                ReservedValue.builder().build(),
                ReservedValue.builder().build(),
                ReservedValue.builder().build()));
    assertEquals(
        2,
        reservedValueService
            .reserve(
                createTrackedEntityAttribute(Objects.TRACKEDENTITYATTRIBUTE, ownerUid, randomText),
                2,
                new HashMap<>(),
                futureDate)
            .size());
    verify(reservedValueStore, times(1)).getAvailableValues(any(), any(), any());
    verify(reservedValueStore, times(1)).bulkInsertReservedValues(anyList());
  }

  @Test
  void shouldRemoveDuplicatesReserveValuesRandomPattern()
      throws TextPatternParser.TextPatternParsingException,
          TextPatternGenerationException,
          ReserveValueException {
    when(reservedValueStore.getAvailableValues(any(), any(), any()))
        .thenReturn(
            Arrays.asList(
                ReservedValue.builder()
                    .ownerUid(ownerUid)
                    .ownerObject(Objects.TRACKEDENTITYATTRIBUTE.name())
                    .key("key")
                    .value("value")
                    .build(),
                ReservedValue.builder()
                    .ownerUid(ownerUid)
                    .ownerObject(Objects.TRACKEDENTITYATTRIBUTE.name())
                    .key("key")
                    .value("value")
                    .build(),
                ReservedValue.builder()
                    .ownerUid("owner1")
                    .ownerObject("ownerObject1")
                    .key("key1")
                    .value("value")
                    .build()));
    assertEquals(
        2,
        reservedValueService
            .reserve(
                createTrackedEntityAttribute(Objects.TRACKEDENTITYATTRIBUTE, ownerUid, randomText),
                2,
                new HashMap<>(),
                futureDate)
            .size());
    verify(reservedValueStore, times(1)).getAvailableValues(any(), any(), any());
    verify(reservedValueStore, times(1))
        .bulkInsertReservedValues(argThat(list -> list.size() == 2));
  }

  @Test
  void shouldDeleteUsedOrExpiredReservedValues() {
    reservedValueService.removeUsedOrExpiredReservations();
    verify(reservedValueStore, times(1)).removeUsedOrExpiredReservations();
  }

  private static TrackedEntityAttribute createTrackedEntityAttribute(
      Objects objects, String uid, String pattern)
      throws TextPatternParser.TextPatternParsingException {
    TextPattern textPattern = TextPatternParser.parse(pattern);
    textPattern.setOwnerObject(objects);
    textPattern.setOwnerUid(uid);
    TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
    trackedEntityAttribute.setTextPattern(textPattern);
    trackedEntityAttribute.setGenerated(true);
    return trackedEntityAttribute;
  }
}
