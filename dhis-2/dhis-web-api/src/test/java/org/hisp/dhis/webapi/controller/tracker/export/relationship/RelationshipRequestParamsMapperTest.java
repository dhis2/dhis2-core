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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static org.hisp.dhis.tracker.imports.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.imports.TrackerType.EVENT;
import static org.hisp.dhis.tracker.imports.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.tracker.export.relationship.RelationshipOperationParams;
import org.hisp.dhis.webapi.common.UID;
import org.junit.jupiter.api.Test;

class RelationshipRequestParamsMapperTest {

  private final RelationshipRequestParamsMapper mapper = new RelationshipRequestParamsMapper();

  @Test
  void shouldFailToMapIfTeiAndTrackedEntityAreSet() {
    RequestParams requestParams = new RequestParams();
    requestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setTei(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(requestParams));
    assertStartsWith("Only one parameter of 'tei'", exception.getMessage());
  }

  @Test
  void getIdentifierParamThrowsIfNoParamsIsSet() {
    RequestParams requestParams = new RequestParams();

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertEquals(
        "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.",
        exception.getMessage());
  }

  @Test
  void getIdentifierParamThrowsIfAllParamsAreSet() {
    RequestParams requestParams = new RequestParams();
    requestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }

  @Test
  void getIdentifierClassThrowsIfTrackedEntityAndEnrollmentAreSet() {
    RequestParams requestParams = new RequestParams();
    requestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }

  @Test
  void getIdentifierClassThrowsIfTeiAndEnrollmentAreSet() {
    RequestParams requestParams = new RequestParams();
    requestParams.setTei(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }

  @Test
  void getIdentifierParamThrowsIfEnrollmentAndEventAreSet() {
    RequestParams requestParams = new RequestParams();
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(requestParams));

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }

  @Test
  void shouldMapCorrectIdentifierWhenTrackedEntityIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(requestParams);

    assertEquals("Hq3Kc6HK4OZ", operationParams.getIdentifier());
  }

  @Test
  void shouldMapCorrectTypeWhenTrackedEntityIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(requestParams);

    assertEquals(TRACKED_ENTITY, operationParams.getType());
  }

  @Test
  void shouldMapCorrectIdentifierWhenEnrollmentIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(requestParams);

    assertEquals("Hq3Kc6HK4OZ", operationParams.getIdentifier());
  }

  @Test
  void shouldMapCorrectTypeWhenEnrollmentIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(requestParams);

    assertEquals(ENROLLMENT, operationParams.getType());
  }

  @Test
  void shouldMapCorrectIdentifierWhenEventIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(requestParams);

    assertEquals("Hq3Kc6HK4OZ", operationParams.getIdentifier());
  }

  @Test
  void shouldMapCorrectTypeWhenEventIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(requestParams);

    assertEquals(EVENT, operationParams.getType());
  }
}
