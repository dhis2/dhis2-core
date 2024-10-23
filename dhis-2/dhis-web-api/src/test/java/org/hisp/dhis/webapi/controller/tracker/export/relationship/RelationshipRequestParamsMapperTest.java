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

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.relationship.RelationshipOperationParams;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.junit.jupiter.api.Test;

class RelationshipRequestParamsMapperTest {

  private final RelationshipRequestParamsMapper mapper = new RelationshipRequestParamsMapper();

  @Test
  void shouldFailToMapIfTeiAndTrackedEntityAreSet() {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));
    relationshipRequestParams.setTei(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(relationshipRequestParams));
    assertStartsWith("Only one parameter of 'tei'", exception.getMessage());
  }

  @Test
  void getIdentifierParamThrowsIfNoParamsIsSet() {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(relationshipRequestParams));

    assertEquals(
        "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.",
        exception.getMessage());
  }

  @Test
  void getIdentifierParamThrowsIfAllParamsAreSet() {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));
    relationshipRequestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));
    relationshipRequestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(relationshipRequestParams));

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }

  @Test
  void getIdentifierClassThrowsIfTrackedEntityAndEnrollmentAreSet() {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));
    relationshipRequestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(relationshipRequestParams));

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }

  @Test
  void getIdentifierClassThrowsIfTeiAndEnrollmentAreSet() {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setTei(UID.of("Hq3Kc6HK4OZ"));
    relationshipRequestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(relationshipRequestParams));

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }

  @Test
  void getIdentifierParamThrowsIfEnrollmentAndEventAreSet() {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));
    relationshipRequestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> mapper.map(relationshipRequestParams));

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }

  @Test
  void shouldMapCorrectIdentifierWhenTrackedEntityIsSet() throws BadRequestException {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(relationshipRequestParams);

    assertEquals(UID.of("Hq3Kc6HK4OZ"), operationParams.getIdentifier());
  }

  @Test
  void shouldMapCorrectTypeWhenTrackedEntityIsSet() throws BadRequestException {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(relationshipRequestParams);

    assertEquals(TRACKED_ENTITY, operationParams.getType());
  }

  @Test
  void shouldMapCorrectIdentifierWhenEnrollmentIsSet() throws BadRequestException {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(relationshipRequestParams);

    assertEquals(UID.of("Hq3Kc6HK4OZ"), operationParams.getIdentifier());
  }

  @Test
  void shouldMapCorrectTypeWhenEnrollmentIsSet() throws BadRequestException {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(relationshipRequestParams);

    assertEquals(ENROLLMENT, operationParams.getType());
  }

  @Test
  void shouldMapCorrectIdentifierWhenEventIsSet() throws BadRequestException {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(relationshipRequestParams);

    assertEquals(UID.of("Hq3Kc6HK4OZ"), operationParams.getIdentifier());
  }

  @Test
  void shouldMapCorrectTypeWhenEventIsSet() throws BadRequestException {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(relationshipRequestParams);

    assertEquals(EVENT, operationParams.getType());
  }

  @Test
  void shouldMapOrderParameterInGivenOrderWhenFieldsAreOrderable() throws BadRequestException {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));
    relationshipRequestParams.setOrder(OrderCriteria.fromOrderString("createdAt:asc"));

    RelationshipOperationParams operationParams = mapper.map(relationshipRequestParams);

    assertEquals(List.of(new Order("created", SortDirection.ASC)), operationParams.getOrder());
  }

  @Test
  void shouldFailGivenInvalidOrderFieldName() {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));
    relationshipRequestParams.setOrder(
        OrderCriteria.fromOrderString("unsupportedProperty1:asc,createdAt:asc"));

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(relationshipRequestParams));
    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        () -> assertContains("unsupportedProperty1", exception.getMessage()));
  }

  @Test
  void testMappingOrderParamsNoOrder() throws BadRequestException {
    RelationshipRequestParams relationshipRequestParams = new RelationshipRequestParams();
    relationshipRequestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));

    RelationshipOperationParams operationParams = mapper.map(relationshipRequestParams);

    assertIsEmpty(operationParams.getOrder());
  }
}
