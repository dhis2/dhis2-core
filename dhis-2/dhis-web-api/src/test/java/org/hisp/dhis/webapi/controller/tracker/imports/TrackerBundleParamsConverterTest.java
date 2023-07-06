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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.webapi.controller.tracker.view.Enrollment;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.Relationship;
import org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class TrackerBundleParamsConverterTest {

  private final BeanRandomizer rnd =
      BeanRandomizer.create(
          Map.of(
              TrackedEntity.class,
              Set.of("enrollments"),
              Enrollment.class,
              Set.of("events"),
              Event.class,
              Set.of("relationships")));

  private ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    objectMapper = JacksonObjectMapperConfig.jsonMapper;
  }

  @Test
  void verifyNestedTeiStructureIsFlattenedDuringDeserialization() throws IOException {
    List<Relationship> relationships1 = createRelationships(2, "rel1");
    List<Relationship> relationships2 = createRelationships(2, "rel2");
    List<Event> events1 = createEvent(3, "ev1", "enr1");
    List<Event> events2 = createEvent(7, "ev2", "enr2");
    List<Enrollment> enrollments = new ArrayList<>();
    Enrollment enrollment1 = createEnrollment("enr1", "teiABC", events1);
    Enrollment enrollment2 = createEnrollment("enr2", "teiABC", events2);
    enrollment1.setRelationships(relationships2);
    enrollment2.setRelationships(relationships1);
    enrollments.add(enrollment1);
    enrollments.add(enrollment2);
    TrackedEntity trackedEntity = createTrackedEntity("teiABC", enrollments);
    trackedEntity.setRelationships(relationships1);
    TrackerBundleParams build =
        TrackerBundleParams.builder()
            .trackedEntities(Collections.singletonList(trackedEntity))
            .build();
    String jsonPayload = toJson(build);
    TrackerBundleParams b2 = this.objectMapper.readValue(jsonPayload, TrackerBundleParams.class);
    assertThat(b2.getTrackedEntities(), hasSize(1));
    assertThat(b2.getEnrollments(), hasSize(2));
    assertThat(b2.getEvents(), hasSize(10));
    assertThat(b2.getRelationships(), hasSize(4));
  }

  @Test
  void verifyNestedTeiStructureHasNestedDataClearedAfterFlattening() throws IOException {
    List<Relationship> relationships1 = createRelationships(2, "rel1");
    List<Relationship> relationships2 = createRelationships(2, "rel2");
    List<Event> events1 = createEvent(3, "ev1", "enr1");
    List<Event> events2 = createEvent(7, "ev2", "enr2");
    List<Enrollment> enrollments = new ArrayList<>();
    Enrollment enrollment1 = createEnrollment("enr1", "teiABC", events1);
    Enrollment enrollment2 = createEnrollment("enr2", "teiABC", events2);
    enrollment1.setRelationships(relationships2);
    enrollment2.setRelationships(relationships1);
    enrollments.add(enrollment1);
    enrollments.add(enrollment2);
    TrackedEntity trackedEntity = createTrackedEntity("teiABC", enrollments);
    trackedEntity.setRelationships(relationships1);
    TrackerBundleParams build =
        TrackerBundleParams.builder()
            .trackedEntities(Collections.singletonList(trackedEntity))
            .build();
    String jsonPayload = toJson(build);
    TrackerBundleParams b2 = this.objectMapper.readValue(jsonPayload, TrackerBundleParams.class);
    assertThat(b2.getTrackedEntities().get(0).getEnrollments(), hasSize(0));
    assertThat(b2.getTrackedEntities().get(0).getRelationships(), hasSize(0));
    assertThat(b2.getEnrollments().get(0).getEvents(), hasSize(0));
    assertThat(b2.getEnrollments().get(0).getRelationships(), hasSize(0));
    assertThat(b2.getEvents().get(0).getRelationships(), hasSize(0));
  }

  @Test
  void verifyUidIsAssignedWhenMissing() throws IOException {
    List<Relationship> relationships1 = createRelationships(2, null);
    List<Relationship> relationships2 = createRelationships(2, "rel2");
    List<Event> events1 = createEvent(3, null, null);
    List<Event> events2 = createEvent(7, null, null);
    List<Enrollment> enrollments = new ArrayList<>();
    Enrollment enrollment1 = createEnrollment(null, null, events1);
    Enrollment enrollment2 = createEnrollment(null, null, events2);
    enrollment1.setRelationships(relationships1);
    enrollment2.setRelationships(relationships2);
    enrollments.add(enrollment1);
    enrollments.add(enrollment2);
    TrackedEntity trackedEntity = createTrackedEntity(null, enrollments);
    trackedEntity.setRelationships(relationships1);
    TrackerBundleParams build =
        TrackerBundleParams.builder()
            .trackedEntities(Collections.singletonList(trackedEntity))
            .build();
    String jsonPayload = toJson(build);
    TrackerBundleParams b2 = this.objectMapper.readValue(jsonPayload, TrackerBundleParams.class);
    // TEI has uid
    assertThat(b2.getTrackedEntities().get(0).getTrackedEntity(), is(notNullValue()));
    // Also check parent uid is set
    assertThat(
        b2.getEnrollments().get(0).getTrackedEntity(),
        is(b2.getTrackedEntities().get(0).getTrackedEntity()));
    assertThat(b2.getEnrollments().get(0).getEnrollment(), is(notNullValue()));
    // Also check parent uid is set
    assertThat(
        b2.getEnrollments().get(1).getTrackedEntity(),
        is(b2.getTrackedEntities().get(0).getTrackedEntity()));
    assertThat(b2.getEnrollments().get(1).getEnrollment(), is(notNullValue()));
    assertThat(b2.getEvents().get(0).getEvent(), is(notNullValue()));
    assertThat(b2.getEvents().get(1).getEvent(), is(notNullValue()));
    assertThat(b2.getRelationships(), hasSize(4));
    b2.getRelationships().stream()
        .forEach(r -> assertThat(r.getRelationship(), is(notNullValue())));
  }

  private TrackedEntity createTrackedEntity(String uid, List<Enrollment> enrollments) {
    TrackedEntity trackedEntity = rnd.nextObject(TrackedEntity.class);
    trackedEntity.setGeometry(null);
    trackedEntity.setTrackedEntity(uid);
    trackedEntity.setEnrollments(enrollments);
    return trackedEntity;
  }

  private String toJson(TrackerBundleParams bundle) throws JsonProcessingException {
    return this.objectMapper.writeValueAsString(bundle);
  }

  private Enrollment createEnrollment(String uid, String parent, List<Event> events) {
    Enrollment enrollment = rnd.nextObject(Enrollment.class);
    enrollment.setGeometry(null);
    enrollment.setEnrollment(uid);
    enrollment.setTrackedEntity(parent);
    enrollment.setEvents(events);
    return enrollment;
  }

  private List<Event> createEvent(int size, String uid, String parent) {
    List<Event> events = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      Event event = rnd.nextObject(Event.class);
      event.setGeometry(null);
      event.setEvent(uid + i);
      event.setEnrollment(parent);
      events.add(event);
    }
    return events;
  }

  private List<Relationship> createRelationships(int size, String uid) {
    List<Relationship> relationships = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      Relationship relationship = rnd.nextObject(Relationship.class);
      relationship.setRelationship(uid + i);
      relationships.add(relationship);
    }
    return relationships;
  }
}
