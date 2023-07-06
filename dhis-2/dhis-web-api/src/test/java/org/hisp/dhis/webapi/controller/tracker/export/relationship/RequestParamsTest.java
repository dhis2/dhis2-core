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

import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.webapi.common.UID;
import org.junit.jupiter.api.Test;

class RequestParamsTest {
  @Test
  void getIdentifierParamIfTrackedEntityIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));

    assertEquals("Hq3Kc6HK4OZ", requestParams.getIdentifierParam());
    assertEquals(
        "Hq3Kc6HK4OZ", requestParams.getIdentifierParam(), "should return cached identifier");
  }

  @Test
  void getIdentifierParamIfTeiIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setTei(UID.of("Hq3Kc6HK4OZ"));

    assertEquals("Hq3Kc6HK4OZ", requestParams.getIdentifierParam());
  }

  @Test
  void getIdentifierParamFailsIfTrackedEntityAndTeiAreSet() {
    RequestParams requestParams = new RequestParams();
    requestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setTei(UID.of("Hq3Kc6HK4OZ"));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> requestParams.getIdentifierParam());
    assertStartsWith("Only one parameter of 'tei'", exception.getMessage());
  }

  @Test
  void getIdentifierNameIfTrackedEntityIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));

    assertEquals("trackedEntity", requestParams.getIdentifierName());
  }

  @Test
  void getIdentifierNameIfTeiIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setTei(UID.of("Hq3Kc6HK4OZ"));

    assertEquals("trackedEntity", requestParams.getIdentifierName());
  }

  @Test
  void getIdentifierClassIfTrackedEntityIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));

    assertEquals(TrackedEntity.class, requestParams.getIdentifierClass());
  }

  @Test
  void getIdentifierClassIfTeiIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setTei(UID.of("Hq3Kc6HK4OZ"));

    assertEquals(TrackedEntity.class, requestParams.getIdentifierClass());
  }

  @Test
  void getIdentifierParamIfEnrollmentIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    assertEquals("Hq3Kc6HK4OZ", requestParams.getIdentifierParam());
  }

  @Test
  void getIdentifierNameIfEnrollmentIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    assertEquals("enrollment", requestParams.getIdentifierName());
  }

  @Test
  void getIdentifierClassIfEnrollmentIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    assertEquals(Enrollment.class, requestParams.getIdentifierClass());
  }

  @Test
  void getIdentifierParamIfEventIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    assertEquals("Hq3Kc6HK4OZ", requestParams.getIdentifierParam());
  }

  @Test
  void getIdentifierNameIfEventIsSet() throws BadRequestException {

    RequestParams requestParams = new RequestParams();
    requestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    assertEquals("event", requestParams.getIdentifierName());
  }

  @Test
  void getIdentifierClassIfEventIsSet() throws BadRequestException {
    RequestParams requestParams = new RequestParams();
    requestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    assertEquals(Event.class, requestParams.getIdentifierClass());
  }

  @Test
  void getIdentifierParamThrowsIfNoParamsIsSet() {
    RequestParams requestParams = new RequestParams();

    BadRequestException exception =
        assertThrows(BadRequestException.class, requestParams::getIdentifierParam);

    assertEquals(
        "Missing required parameter 'trackedEntity', 'enrollment' or 'event'.",
        exception.getMessage());
  }

  @Test
  void getIdentifierNameThrowsIfNoParamsIsSet() {
    RequestParams requestParams = new RequestParams();

    BadRequestException exception =
        assertThrows(BadRequestException.class, requestParams::getIdentifierName);

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
        assertThrows(BadRequestException.class, requestParams::getIdentifierParam);

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }

  @Test
  void getIdentifierNameThrowsIfAllParamsAreSet() {
    RequestParams requestParams = new RequestParams();
    requestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, requestParams::getIdentifierName);

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }

  @Test
  void getIdentifierClassThrowsIfAllParamsAreSet() {
    RequestParams requestParams = new RequestParams();
    requestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setEvent(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, requestParams::getIdentifierClass);

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }

  @Test
  void getIdentifierParamThrowsIfTrackedEntityAndEnrollmentAreSet() {
    RequestParams requestParams = new RequestParams();
    requestParams.setTrackedEntity(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, requestParams::getIdentifierParam);

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }

  @Test
  void getIdentifierParamThrowsIfTeiAndEnrollmentAreSet() {
    RequestParams requestParams = new RequestParams();
    requestParams.setTei(UID.of("Hq3Kc6HK4OZ"));
    requestParams.setEnrollment(UID.of("Hq3Kc6HK4OZ"));

    BadRequestException exception =
        assertThrows(BadRequestException.class, requestParams::getIdentifierParam);

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
        assertThrows(BadRequestException.class, requestParams::getIdentifierClass);

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
        assertThrows(BadRequestException.class, requestParams::getIdentifierClass);

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
        assertThrows(BadRequestException.class, requestParams::getIdentifierParam);

    assertEquals(
        "Only one of parameters 'trackedEntity', 'enrollment' or 'event' is allowed.",
        exception.getMessage());
  }
}
