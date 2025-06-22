/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker;

import static java.util.Collections.singletonList;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.tracker.imports.domain.MetadataIdentifier.ofAttribute;
import static org.hisp.dhis.tracker.imports.domain.MetadataIdentifier.ofCode;
import static org.hisp.dhis.tracker.imports.domain.MetadataIdentifier.ofName;
import static org.hisp.dhis.tracker.imports.domain.MetadataIdentifier.ofUid;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.TrackerIdentifierCollector;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.Note;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrackerIdentifierCollectorTest {

  private TrackerIdentifierCollector collector;

  @BeforeEach
  void setUp() {

    ProgramRuleService programRuleService = mock(ProgramRuleService.class);
    ProgramRuleActionService programRuleActionService = mock(ProgramRuleActionService.class);
    collector = new TrackerIdentifierCollector(programRuleService, programRuleActionService);
  }

  @Test
  void collectTrackedEntities() {
    TrackedEntity trackedEntity =
        TrackedEntity.builder()
            .trackedEntity(uid())
            .trackedEntityType(ofAttribute("NTVsGflP5Ix", "sunshine"))
            .orgUnit(ofName("ward"))
            .attributes(teAttributes("VohJnvWfvyo", "qv9xOw8fBzy"))
            .build();

    TrackerObjects trackerObjects =
        TrackerObjects.builder().trackedEntities(singletonList(trackedEntity)).build();

    Map<Class<?>, Set<String>> ids = collector.collect(trackerObjects);

    assertNotNull(ids);
    assertContainsOnly(
        Set.of(trackedEntity.getTrackedEntity().getValue()), ids.get(TrackedEntity.class));
    assertContainsOnly(Set.of("sunshine"), ids.get(TrackedEntityType.class));
    assertContainsOnly(Set.of("ward"), ids.get(OrganisationUnit.class));
    assertContainsOnly(Set.of("VohJnvWfvyo", "qv9xOw8fBzy"), ids.get(TrackedEntityAttribute.class));
  }

  @Test
  void collectEnrollments() {
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(uid())
            .trackedEntity(uid())
            .program(ofAttribute("NTVsGflP5Ix", "sunshine"))
            .orgUnit(ofName("ward"))
            .attributes(teAttributes("VohJnvWfvyo", "qv9xOw8fBzy"))
            .build();

    TrackerObjects trackerObjects =
        TrackerObjects.builder().enrollments(singletonList(enrollment)).build();

    Map<Class<?>, Set<String>> ids = collector.collect(trackerObjects);

    assertNotNull(ids);
    assertContainsOnly(Set.of(enrollment.getUid().getValue()), ids.get(Enrollment.class));
    assertContainsOnly(
        Set.of(enrollment.getTrackedEntity().getValue()), ids.get(TrackedEntity.class));
    assertContainsOnly(Set.of("sunshine"), ids.get(Program.class));
    assertContainsOnly(Set.of("ward"), ids.get(OrganisationUnit.class));
    assertContainsOnly(Set.of("VohJnvWfvyo", "qv9xOw8fBzy"), ids.get(TrackedEntityAttribute.class));
  }

  @Test
  void collectEvents() {
    Event event =
        TrackerEvent.builder()
            .event(uid())
            .enrollment(uid())
            .program(ofAttribute("NTVsGflP5Ix", "sunshine"))
            .programStage(ofAttribute("NTVsGflP5Ix", "flowers"))
            .orgUnit(ofName("ward"))
            .dataValues(dataValues("VohJnvWfvyo", "qv9xOw8fBzy"))
            .attributeOptionCombo(ofCode("rgb"))
            .attributeCategoryOptions(Set.of(ofCode("red"), ofCode("green"), ofCode("blue")))
            .notes(List.of(Note.builder().note(UID.of("i1vviSlidJE")).value("nice day!").build()))
            .build();

    TrackerObjects trackerObjects = TrackerObjects.builder().events(singletonList(event)).build();

    Map<Class<?>, Set<String>> ids = collector.collect(trackerObjects);

    assertNotNull(ids);
    assertContainsOnly(Set.of(event.getUid().getValue()), ids.get(Event.class));
    assertContainsOnly(Set.of(event.getEnrollment().getValue()), ids.get(Enrollment.class));
    assertContainsOnly(Set.of("sunshine"), ids.get(Program.class));
    assertContainsOnly(Set.of("flowers"), ids.get(ProgramStage.class));
    assertContainsOnly(Set.of("ward"), ids.get(OrganisationUnit.class));
    assertContainsOnly(Set.of("VohJnvWfvyo", "qv9xOw8fBzy"), ids.get(DataElement.class));
    assertContainsOnly(Set.of("rgb"), ids.get(CategoryOptionCombo.class));
    assertContainsOnly(Set.of("red", "green", "blue"), ids.get(CategoryOption.class));
    assertContainsOnly(Set.of("i1vviSlidJE"), ids.get(org.hisp.dhis.note.Note.class));
  }

  @Test
  void collectEventsSkipsNotesWithoutAValue() {
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .notes(List.of(Note.builder().note(UID.of("i1vviSlidJE")).build()))
            .build();

    TrackerObjects trackerObjects = TrackerObjects.builder().events(singletonList(event)).build();

    Map<Class<?>, Set<String>> ids = collector.collect(trackerObjects);

    assertNotNull(ids);
    assertNull(ids.get(org.hisp.dhis.note.Note.class));
  }

  @Test
  void collectRelationships() {
    Relationship relationship =
        Relationship.builder()
            .relationship(uid())
            .relationshipType(ofAttribute("NTVsGflP5Ix", "sunshine"))
            .from(RelationshipItem.builder().enrollment(uid()).build())
            .to(RelationshipItem.builder().event(uid()).build())
            .build();

    TrackerObjects trackerObjects =
        TrackerObjects.builder().relationships(singletonList(relationship)).build();

    Map<Class<?>, Set<String>> ids = collector.collect(trackerObjects);

    assertNotNull(ids);
    assertContainsOnly(
        Set.of(relationship.getRelationship().getValue()), ids.get(Relationship.class));
    assertContainsOnly(Set.of("sunshine"), ids.get(RelationshipType.class));
    assertContainsOnly(
        Set.of(relationship.getFrom().getEnrollment().getValue()), ids.get(Enrollment.class));
    assertContainsOnly(Set.of(relationship.getTo().getEvent().getValue()), ids.get(Event.class));
  }

  private UID uid() {
    return UID.generate();
  }

  private List<Attribute> teAttributes(String... uids) {

    List<Attribute> result = new ArrayList<>();
    for (String uid : uids) {
      result.add(teAttribute(uid));
    }
    return result;
  }

  private Attribute teAttribute(String uid) {
    return Attribute.builder().attribute(ofUid(uid)).build();
  }

  private Set<DataValue> dataValues(String... dataElementUids) {

    Set<DataValue> result = new HashSet<>();
    for (String uid : dataElementUids) {
      result.add(dataValue(uid));
    }
    return result;
  }

  private DataValue dataValue(String dataElementUid) {
    return DataValue.builder().dataElement(ofUid(dataElementUid)).build();
  }
}
