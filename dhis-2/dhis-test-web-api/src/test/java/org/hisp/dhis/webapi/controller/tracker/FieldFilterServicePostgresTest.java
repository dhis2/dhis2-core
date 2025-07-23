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
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.better.FieldsParser;
import org.hisp.dhis.fieldfiltering.better.FieldsPredicate;
import org.hisp.dhis.fieldfiltering.better.FieldsPropertyFilter;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.webapi.controller.tracker.view.DataValue;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.Note;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.hisp.dhis.webapi.controller.tracker.view.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// TODO(ivo) the tracker view model can be built/tested without a DB. We only need Spring to wire
// the service(s) and the ObjectMapper. As soon as we test metadata as well we should switch to
// PostgresControllerIntegrationTestBase
class FieldFilterServicePostgresTest extends H2ControllerIntegrationTestBase {
  private static final GeometryFactory geometryFactory = new GeometryFactory();

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
    Point point = geometryFactory.createPoint(new Coordinate(4, 12));

    // TODO(ivo) add relationships so we get deeper nesting to play with
    events =
        List.of(
            Event.builder()
                .event(UID.generate())
                .program(UID.generate().getValue())
                .programStage(UID.generate().getValue())
                .enrollment(UID.generate())
                .trackedEntity(UID.generate())
                .orgUnit(UID.generate().getValue())
                .occurredAt(Instant.now())
                .scheduledAt(Instant.now())
                .storedBy("fred")
                .followUp(true)
                .createdAt(Instant.now())
                .attributeOptionCombo(UID.generate().getValue())
                .attributeCategoryOptions(UID.generate().getValue())
                .geometry(point)
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
                            .value("78")
                            .storedBy("alice")
                            .build()))
                .notes(List.of(Note.builder().note(UID.generate()).value("lovely note").build()))
                .build());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "*",
        "event,dataValues",
        "event,dataValues[dataElement,value]",
        "event,dataValues[*,!storedBy]",
        "*,!enrollment",
      })
  void betterFilterShouldMatchCurrentFilter(String fields) throws JsonProcessingException {
    String actualCurrent = serializeUsingCurrentFilter(events, fields);
    String actualBetter = serializeUsingBetterFilter(events, fields);

    assertEquals(actualCurrent, actualBetter);
  }

  @Test
  void debugPager() throws BadRequestException, JsonProcessingException {
    System.out.println(
        serializeUsingBetterFilter(
            Page.withPager(
                "events",
                new org.hisp.dhis.tracker.Page(events, PageParams.of(1, 2, false)),
                "http://localhost:8080/api/events"),
            "event,dataValues[dataElement,value]"));
  }

  private String serializeUsingCurrentFilter(List<Event> events, String fields)
      throws JsonProcessingException {
    List<FieldPath> filter = FieldFilterParser.parse(fields);
    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(events, filter);
    return objectMapper.writeValueAsString(objectNodes);
  }

  private String serializeUsingBetterFilter(List<Event> events, String fields)
      throws JsonProcessingException {
    FieldsPredicate fieldsPredicate = FieldsParser.parse(fields);
    return filterMapper
        .writer()
        .withAttribute(FieldsPropertyFilter.PREDICATE_ATTRIBUTE, fieldsPredicate)
        .writeValueAsString(events);
  }

  private String serializeUsingBetterFilter(Page<Event> page, String fields)
      throws JsonProcessingException {
    FieldsPredicate fieldsPredicate = FieldsParser.parse(fields);
    FieldsPredicate pagePredicate = new FieldsPredicate();
    pagePredicate.include("pager");
    FieldsPredicate pagerPredicate = new FieldsPredicate();
    pagerPredicate.includeAll();
    pagePredicate.getChildren().put("pager", pagerPredicate);
    pagePredicate.include(page.getKey());
    pagePredicate.getChildren().put(page.getKey(), fieldsPredicate);
    return filterMapper
        .writer()
        .withAttribute(FieldsPropertyFilter.PREDICATE_ATTRIBUTE, pagePredicate)
        .writeValueAsString(page);
  }
}
