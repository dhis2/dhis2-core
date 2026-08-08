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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class OptionValueSupplierTest {

  private JdbcTemplate jdbcTemplate;

  private OptionValueSupplier supplier;

  private OptionSet optionSet;

  private DataElement dataElement;

  private TrackedEntityAttribute trackedEntityAttribute;

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    supplier = new OptionValueSupplier(jdbcTemplate);

    optionSet = new OptionSet();
    optionSet.setId(1L);

    dataElement = new DataElement();
    dataElement.setUid("dataElement");
    dataElement.setValueType(ValueType.TEXT);
    dataElement.setOptionSet(optionSet);

    trackedEntityAttribute = new TrackedEntityAttribute();
    trackedEntityAttribute.setUid("attribute");
    trackedEntityAttribute.setValueType(ValueType.MULTI_TEXT);
    trackedEntityAttribute.setOptionSet(optionSet);
  }

  @Test
  void shouldCollectCandidateFromEventDataValue() {
    TrackerPreheat preheat = new TrackerPreheat();
    preheat.put(dataElement);

    DataValue dataValue =
        DataValue.builder()
            .dataElement(MetadataIdentifier.ofUid("dataElement"))
            .value("A00")
            .build();
    TrackerEvent event =
        TrackerEvent.builder().event(UID.generate()).dataValues(Set.of(dataValue)).build();
    TrackerObjects trackerObjects = TrackerObjects.builder().events(List.of(event)).build();

    Set<Pair<Long, String>> candidates = supplier.collectCandidates(trackerObjects, preheat);

    assertEquals(Set.of(Pair.of(1L, "A00")), candidates);
  }

  @Test
  void shouldSplitMultiTextValuesFromTrackedEntityAndEnrollmentAttributes() {
    TrackerPreheat preheat = new TrackerPreheat();
    preheat.put(TrackerIdSchemeParam.UID, trackedEntityAttribute);

    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid("attribute"))
            .value("A00,B99")
            .build();
    TrackedEntity trackedEntity =
        TrackedEntity.builder()
            .trackedEntity(UID.generate())
            .attributes(List.of(attribute))
            .build();
    Enrollment enrollment =
        Enrollment.builder().enrollment(UID.generate()).attributes(List.of(attribute)).build();
    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .trackedEntities(List.of(trackedEntity))
            .enrollments(List.of(enrollment))
            .build();

    Set<Pair<Long, String>> candidates = supplier.collectCandidates(trackerObjects, preheat);

    assertEquals(Set.of(Pair.of(1L, "A00"), Pair.of(1L, "B99")), candidates);
  }

  @Test
  void shouldSkipValuesWithoutAnOptionSet() {
    DataElement plainDataElement = new DataElement();
    plainDataElement.setUid("plain");
    plainDataElement.setValueType(ValueType.TEXT);

    TrackerPreheat preheat = new TrackerPreheat();
    preheat.put(plainDataElement);

    DataValue dataValue =
        DataValue.builder()
            .dataElement(MetadataIdentifier.ofUid("plain"))
            .value("anything")
            .build();
    TrackerEvent event =
        TrackerEvent.builder().event(UID.generate()).dataValues(Set.of(dataValue)).build();
    TrackerObjects trackerObjects = TrackerObjects.builder().events(List.of(event)).build();

    Set<Pair<Long, String>> candidates = supplier.collectCandidates(trackerObjects, preheat);

    assertEquals(Set.of(), candidates);
  }

  @Test
  void shouldSkipValuesWhoseDataElementIsNotPreheated() {
    TrackerPreheat preheat = new TrackerPreheat();

    DataValue dataValue =
        DataValue.builder().dataElement(MetadataIdentifier.ofUid("unknown")).value("A00").build();
    TrackerEvent event =
        TrackerEvent.builder().event(UID.generate()).dataValues(Set.of(dataValue)).build();
    TrackerObjects trackerObjects = TrackerObjects.builder().events(List.of(event)).build();

    Set<Pair<Long, String>> candidates = supplier.collectCandidates(trackerObjects, preheat);

    assertEquals(Set.of(), candidates);
  }

  @Test
  void shouldNotQueryTheDatabaseWhenNoValueReferencesAnOptionSet() {
    DataElement plainDataElement = new DataElement();
    plainDataElement.setUid("plain");
    plainDataElement.setValueType(ValueType.TEXT);

    TrackerPreheat preheat = new TrackerPreheat();
    preheat.put(plainDataElement);

    DataValue dataValue =
        DataValue.builder()
            .dataElement(MetadataIdentifier.ofUid("plain"))
            .value("anything")
            .build();
    TrackerEvent event =
        TrackerEvent.builder().event(UID.generate()).dataValues(Set.of(dataValue)).build();
    TrackerObjects trackerObjects = TrackerObjects.builder().events(List.of(event)).build();

    supplier.preheatAdd(trackerObjects, preheat);

    verifyNoInteractions(jdbcTemplate);
  }

  @Test
  void shouldMarkOptionSetAsResolvedEvenWhenNoCodeTurnsOutToBeValid() {
    TrackerPreheat preheat = new TrackerPreheat();
    preheat.put(dataElement);

    DataValue dataValue =
        DataValue.builder()
            .dataElement(MetadataIdentifier.ofUid("dataElement"))
            .value("A00")
            .build();
    TrackerEvent event =
        TrackerEvent.builder().event(UID.generate()).dataValues(Set.of(dataValue)).build();
    TrackerObjects trackerObjects = TrackerObjects.builder().events(List.of(event)).build();

    supplier.collectCandidates(trackerObjects, preheat);

    assertTrue(preheat.isOptionSetResolved(1L));
    // no code was confirmed against the database, the option set is still "resolved"
    assertFalse(preheat.isValidOptionCode(1L, "A00"));
  }

  @Test
  void shouldNotMarkAnOptionSetAsResolvedWhenNoValueReferencesOne() {
    DataElement plainDataElement = new DataElement();
    plainDataElement.setUid("plain");
    plainDataElement.setValueType(ValueType.TEXT);

    TrackerPreheat preheat = new TrackerPreheat();
    preheat.put(plainDataElement);

    DataValue dataValue =
        DataValue.builder()
            .dataElement(MetadataIdentifier.ofUid("plain"))
            .value("anything")
            .build();
    TrackerEvent event =
        TrackerEvent.builder().event(UID.generate()).dataValues(Set.of(dataValue)).build();
    TrackerObjects trackerObjects = TrackerObjects.builder().events(List.of(event)).build();

    supplier.collectCandidates(trackerObjects, preheat);

    assertFalse(preheat.isOptionSetResolved(1L));
  }
}
