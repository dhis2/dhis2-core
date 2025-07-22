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

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.time.Instant;
import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldFilterService.IgnoreJsonSerializerRefinementAnnotationInspector;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.better.FieldsParser;
import org.hisp.dhis.fieldfiltering.better.FieldsPredicate;
import org.hisp.dhis.fieldfiltering.better.FieldsPropertyFilter;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// TODO(ivo) the tracker view model can be built/tested without a DB. We only need Spring to wire
// the service(s) and the ObjectMapper. As soon as we test metadata as well we should switch to
// PostgresControllerIntegrationTestBase
class FieldFilterServicePostgresTest extends H2ControllerIntegrationTestBase {
  @Autowired private FieldFilterService fieldFilterService;

  @Autowired private ObjectMapper objectMapper;

  private List<Event> events;

  // ObjectMapper used for the better filter, which is a copy of the default ObjectMapper with a
  // custom mixin and annotation introspector
  private ObjectMapper betterObjectMapper;

  @BeforeAll
  void setUp() {
    // TODO(ivo) add all properties
    events =
        List.of(
            Event.builder()
                .event(UID.generate())
                .status(EventStatus.COMPLETED)
                .program(UID.generate().getValue())
                .programStage(UID.generate().getValue())
                .enrollment(UID.generate())
                .trackedEntity(UID.generate())
                .orgUnit(UID.generate().getValue())
                .occurredAt(Instant.now())
                .scheduledAt(Instant.now())
                .build());

    // TODO(ivo) this replicates what we do in FieldFilterService#configureFieldFilterObjectMapper
    // in the end we should create a fieldsObjectMapper bean in JacksonObjectMapperConfig
    betterObjectMapper = objectMapper.copy();
    SimpleModule module = new SimpleModule();
    module.setMixInAnnotation(Object.class, FieldFilterMixin.class);
    betterObjectMapper.registerModule(module);
    betterObjectMapper.setAnnotationIntrospector(
        new IgnoreJsonSerializerRefinementAnnotationInspector());
  }

  @JsonFilter(FieldsPropertyFilter.FILTER_ID)
  public interface FieldFilterMixin {}

  @ParameterizedTest
  @ValueSource(
      strings = {
        "*",
        "event",
        "*,!enrollment",
      })
  void betterFilterShouldMatchCurrentFilter(String fields) throws JsonProcessingException {
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

  private String serializeUsingBetterFilter(List<Event> events, String fields)
      throws JsonProcessingException {
    FieldsPredicate filter = FieldsParser.parse(fields);
    FilterProvider filters =
        new SimpleFilterProvider()
            .addFilter(FieldsPropertyFilter.FILTER_ID, new FieldsPropertyFilter());
    return betterObjectMapper
        .writer(filters)
        .withAttribute(FieldsPropertyFilter.PREDICATE_ATTRIBUTE, filter)
        .writeValueAsString(events);
  }
}
