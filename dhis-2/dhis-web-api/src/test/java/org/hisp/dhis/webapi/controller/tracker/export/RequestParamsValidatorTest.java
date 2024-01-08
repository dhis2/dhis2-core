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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria.fromOrderString;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrderParams;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.feedback.BadRequestException;
import org.junit.jupiter.api.Test;

/** Tests {@link RequestParamsValidator}. */
class RequestParamsValidatorTest {

  @Test
  void shouldPassOrderParamsValidationWhenGivenOrderIsOrderable() throws BadRequestException {
    Set<String> supportedFieldNames = Set.of("createdAt", "scheduledAt");

    validateOrderParams(fromOrderString("createdAt:asc,scheduledAt:asc"), supportedFieldNames, "");
  }

  @Test
  void shouldFailOrderParamsValidationWhenGivenInvalidOrderComponents() {
    Set<String> supportedFieldNames = Set.of("enrolledAt");
    String invalidUID = "Cogn34Del";
    assertFalse(CodeGenerator.isValidUid(invalidUID));

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                validateOrderParams(
                    fromOrderString(
                        "unsupportedProperty1:asc,enrolledAt:asc,"
                            + invalidUID
                            + ",unsupportedProperty2:desc"),
                    supportedFieldNames,
                    "data element and attribute"));
    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        () ->
            assertContains(
                "Supported are data element and attribute UIDs and fields", exception.getMessage()),
        // order of fields might not always be the same; therefore using contains
        () -> assertContains(invalidUID, exception.getMessage()),
        () -> assertContains("unsupportedProperty1", exception.getMessage()),
        () -> assertContains("unsupportedProperty2", exception.getMessage()));
  }

  @Test
  void shouldPassOrderParamsValidationWhenGivenInvalidOrderNameWhichIsAValidUID()
      throws BadRequestException {
    Set<String> supportedFieldNames = Set.of("enrolledAt");
    // This test case shows that some field names are valid UIDs. We can thus not rule out all
    // invalid field names and UIDs at this stage as we do not have access to data element/attribute
    // services. Such invalid order values will be caught in the service (mapper).
    assertTrue(CodeGenerator.isValidUid("lastUpdated"));

    validateOrderParams(fromOrderString("lastUpdated:desc"), supportedFieldNames, "");
  }

  @Test
  void shouldFailOrderParamsValidationWhenGivenRepeatedOrderComponents() {
    Set<String> supportedFieldNames = Set.of("createdAt", "enrolledAt");

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                validateOrderParams(
                    fromOrderString(
                        "zGlzbfreTOH,createdAt:asc,enrolledAt:asc,enrolledAt,zGlzbfreTOH"),
                    supportedFieldNames,
                    ""));

    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        // order of fields might not always be the same; therefore using contains
        () -> assertContains("repeated", exception.getMessage()),
        () -> assertContains("enrolledAt", exception.getMessage()),
        () -> assertContains("zGlzbfreTOH", exception.getMessage()));
  }
}
