/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.better.Fields;
import org.hisp.dhis.fieldfiltering.better.FieldsParser;
import org.hisp.dhis.fieldfiltering.better.FieldsPropertyFilter;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.webapi.controller.tracker.view.DataValue;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.Note;
import org.hisp.dhis.webapi.controller.tracker.view.Relationship;
import org.hisp.dhis.webapi.controller.tracker.view.RelationshipItem;
import org.hisp.dhis.webapi.controller.tracker.view.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

/**
 * This test ensures that simple POJOs like Tracker view classes can be serialized and field
 * filtered to JSON by Jackson. The test also ensures the better field filtering is backwards
 * compatible with the current {@link FieldFilterParser} and {@link FieldFilterService}. Due to how
 * the current field filtering is built cannot ensure most of this using unit tests.
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// TODO(ivo) the tracker view model can be built/tested without a DB. We only need Spring to wire
// the service(s) and the ObjectMapper. As soon as we test metadata as well we should switch to
// PostgresControllerIntegrationTestBase
class FieldFilterSerializationTest extends H2ControllerIntegrationTestBase {
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
  private static final Instant DATE = Instant.parse("2023-03-15T14:30:45Z");

  @Autowired private FieldFilterService fieldFilterService;

  // use primary ObjectMapper from JacksonObjectMapperConfig to serialize the current ObjectNode to
  // a JSON string
  @Autowired private ObjectMapper objectMapper;

  // use the filter ObjectMapper from JacksonObjectMapperConfig to serialize, filter and transform
  // an Object to a JSON string
  @Qualifier("jsonFilterMapper")
  @Autowired
  private ObjectMapper filterMapper;

  private List<Event> events;

  @BeforeAll
  void setUp() {
    events = createEvents(2);
  }

  // TODO(ivo) make sure that all cases that can be unit tested in better FieldsParser are. If we
  // replace the current field filtering parser we should still keep these to test the combination
  // of FieldsParser and Jackson FieldsPropertyFilter
  @ParameterizedTest
  @ValueSource(
      strings = {
        "*",
        "!event",
        "event,dataValues",
        "event,!dataValues",
        "event,!dataValues,*",
        "dataValues[!value]",
        "dataValues,dataValues[!value]",
        "dataValues,dataValues[value]",
        "dataValues[dataElement,!value]",
        "event,*,dataValues[!value]",
        "event,dataValues[dataElement,value]",
        "event,dataValues[*,!storedBy]",
        "*,!enrollment",
        "relationships,relationships[from]",
        "relationships[]",
        "relationships[unknownfield]", // TODO(ivo) anyway we can replicate this behavior? I do not
        // want to know what fields actually exist as this makes everything complicated
        "relationships[f rom[trackedEntity[ org Unit ]",
      })
  void betterFilterShouldMatchCurrentFilterOnSimplePojo(String fields)
      throws JsonProcessingException {
    String actualCurrent = serializeUsingCurrentFilter(events, fields);
    String actualBetter = serializeUsingBetterFilter(events, fields);

    assertEquals(actualCurrent, actualBetter);
  }

  private String serializeUsingCurrentFilter(List<Event> events, String fields)
      throws JsonProcessingException {
    List<FieldPath> filter = FieldFilterParser.parse(fields);
    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(events, filter);
    return objectMapper.writeValueAsString(objectNodes);
  }

  private String serializeUsingBetterFilter(List<Event> events, String fieldsInput)
      throws JsonProcessingException {
    Fields fields = FieldsParser.parse(fieldsInput);
    return filterMapper
        .writer()
        .withAttribute(FieldsPropertyFilter.FIELDS_ATTRIBUTE, fields)
        .writeValueAsString(events);
  }

  public static List<Event> createEvents(int n) {
    List<Event> events = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      events.add(createEvent());
    }
    return events;
  }

  public static Event createEvent() {
    return Event.builder()
        .event(UID.generate())
        .program(UID.generate().getValue())
        .programStage(UID.generate().getValue())
        .enrollment(UID.generate())
        .trackedEntity(UID.generate())
        .orgUnit(UID.generate().getValue())
        .relationships(
            List.of(
                Relationship.builder()
                    .relationship(UID.generate())
                    .relationshipName("Mother-Child")
                    .relationshipType(UID.generate().getValue())
                    .createdAt(DATE)
                    .bidirectional(false)
                    .from(
                        RelationshipItem.builder()
                            .trackedEntity(
                                RelationshipItem.TrackedEntity.builder()
                                    .trackedEntity(UID.generate())
                                    .trackedEntityType(UID.generate().getValue())
                                    .createdAt(DATE)
                                    .orgUnit(UID.generate().getValue())
                                    .build())
                            .build())
                    .to(
                        RelationshipItem.builder()
                            .trackedEntity(
                                RelationshipItem.TrackedEntity.builder()
                                    .trackedEntity(UID.generate())
                                    .trackedEntityType(UID.generate().getValue())
                                    .createdAt(DATE)
                                    .orgUnit(UID.generate().getValue())
                                    .build())
                            .build())
                    .build(),
                Relationship.builder()
                    .relationship(UID.generate())
                    .relationshipName("Sibling")
                    .relationshipType(UID.generate().getValue())
                    .createdAt(DATE)
                    .bidirectional(true)
                    .from(
                        RelationshipItem.builder()
                            .event(
                                RelationshipItem.Event.builder()
                                    .event(UID.generate())
                                    .program(UID.generate().getValue())
                                    .programStage(UID.generate().getValue())
                                    .orgUnit(UID.generate().getValue())
                                    .occurredAt(DATE)
                                    .createdAt(DATE)
                                    .build())
                            .build())
                    .to(
                        RelationshipItem.builder()
                            .enrollment(
                                RelationshipItem.Enrollment.builder()
                                    .enrollment(UID.generate())
                                    .program(UID.generate().getValue())
                                    .orgUnit(UID.generate().getValue())
                                    .enrolledAt(DATE)
                                    .createdAt(DATE)
                                    .build())
                            .build())
                    .build()))
        .scheduledAt(DATE)
        .storedBy("fred")
        .followUp(true)
        .createdAt(DATE)
        .attributeOptionCombo(UID.generate().getValue())
        .attributeCategoryOptions(UID.generate().getValue())
        .geometry(GEOMETRY_FACTORY.createPoint(new Coordinate(4, 12)))
        .createdBy(
            User.builder()
                .uid(UID.generate().getValue())
                .username("fred")
                .displayName("Freddy")
                .build())
        .dataValues(
            Set.of(
                DataValue.builder()
                    .dataElement(UID.generate().getValue())
                    .value("14")
                    .storedBy("alice")
                    .build(),
                DataValue.builder()
                    .dataElement(UID.generate().getValue())
                    .value("78")
                    .storedBy("bob")
                    .build()))
        .notes(List.of(Note.builder().note(UID.generate()).value("lovely note").build()))
        .build();
  }
}
