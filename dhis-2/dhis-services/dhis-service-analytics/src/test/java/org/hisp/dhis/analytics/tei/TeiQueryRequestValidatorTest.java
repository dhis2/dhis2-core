/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.tei;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.analytics.common.QueryRequest;
import org.hisp.dhis.analytics.common.processing.CommonQueryRequestValidator;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TeiQueryRequestValidator}.
 *
 * @author maikel arabori
 */
class TeiQueryRequestValidatorTest {
  @Test
  void testValidateWithSuccess() {
    // Given
    TeiQueryRequest teiQueryRequest = new TeiQueryRequest("uidabcdef11");
    CommonQueryRequest commonQueryRequest = new CommonQueryRequest();
    commonQueryRequest.setProgram(Set.of("prgabcdef11"));
    commonQueryRequest.setDimension(Set.of("ou:jmIPBj66vD6"));

    QueryRequest<TeiQueryRequest> queryRequest =
        QueryRequest.<TeiQueryRequest>builder()
            .commonQueryRequest(commonQueryRequest)
            .request(teiQueryRequest)
            .build();

    CommonQueryRequestValidator commonQueryRequestValidator = new CommonQueryRequestValidator();
    TeiQueryRequestValidator teiQueryRequestValidator =
        new TeiQueryRequestValidator(commonQueryRequestValidator);

    // Then
    assertDoesNotThrow(() -> teiQueryRequestValidator.validate(queryRequest));
  }

  @Test
  void testValidateWhenProgramIsNotDefined() {
    // Given
    TeiQueryRequest teiQueryRequest = new TeiQueryRequest("uidabcdef11");
    CommonQueryRequest commonQueryRequest = new CommonQueryRequest();
    commonQueryRequest.setDimension(Set.of("ou:jmIPBj66vD6"));

    QueryRequest<TeiQueryRequest> queryRequest =
        QueryRequest.<TeiQueryRequest>builder()
            .commonQueryRequest(commonQueryRequest)
            .request(teiQueryRequest)
            .build();

    CommonQueryRequestValidator commonQueryRequestValidator = new CommonQueryRequestValidator();
    TeiQueryRequestValidator teiQueryRequestValidator =
        new TeiQueryRequestValidator(commonQueryRequestValidator);

    // When
    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class, () -> teiQueryRequestValidator.validate(queryRequest));

    // Then
    assertEquals("Program is not specified", exception.getMessage());
  }

  @Test
  void testValidateWhenPeIsDefined() {
    // Given
    TeiQueryRequest teiQueryRequest = new TeiQueryRequest("uidabcdef11");
    CommonQueryRequest commonQueryRequest = new CommonQueryRequest();
    commonQueryRequest.setProgram(Set.of("prgabcdef11"));
    commonQueryRequest.setDimension(Set.of("pe:LAST_YEAR"));

    QueryRequest<TeiQueryRequest> queryRequest =
        QueryRequest.<TeiQueryRequest>builder()
            .commonQueryRequest(commonQueryRequest)
            .request(teiQueryRequest)
            .build();

    CommonQueryRequestValidator commonQueryRequestValidator = new CommonQueryRequestValidator();
    TeiQueryRequestValidator teiQueryRequestValidator =
        new TeiQueryRequestValidator(commonQueryRequestValidator);

    // When
    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class, () -> teiQueryRequestValidator.validate(queryRequest));

    // Then
    assertEquals("Query item or filter is invalid: `pe:LAST_YEAR`", exception.getMessage());
  }

  @Test
  void testValidateWhenNoTrackedEntityType() {
    // Given
    TeiQueryRequest teiQueryRequest = new TeiQueryRequest(null);

    QueryRequest<TeiQueryRequest> queryRequest =
        QueryRequest.<TeiQueryRequest>builder().request(teiQueryRequest).build();

    CommonQueryRequestValidator commonQueryRequestValidator = new CommonQueryRequestValidator();
    TeiQueryRequestValidator teiQueryRequestValidator =
        new TeiQueryRequestValidator(commonQueryRequestValidator);

    // When
    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class, () -> teiQueryRequestValidator.validate(queryRequest));

    // Then
    assertEquals("Invalid UID `null` for property `trackedEntityType`", exception.getMessage());
  }

  @Test
  void testValidateWhenTrackedEntityTypeIsInvalid() {
    String teiUid = CodeGenerator.generateUid() + "invalid";
    // Given
    TeiQueryRequest teiQueryRequest = new TeiQueryRequest(teiUid);

    QueryRequest<TeiQueryRequest> queryRequest =
        QueryRequest.<TeiQueryRequest>builder().request(teiQueryRequest).build();

    CommonQueryRequestValidator commonQueryRequestValidator = new CommonQueryRequestValidator();
    TeiQueryRequestValidator teiQueryRequestValidator =
        new TeiQueryRequestValidator(commonQueryRequestValidator);

    // When
    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class, () -> teiQueryRequestValidator.validate(queryRequest));

    // Then
    assertEquals(
        "Invalid UID `" + teiUid + "` for property `trackedEntityType`", exception.getMessage());
  }

  @Test
  void testValidateWhenProgramUidIsInvalid() {
    String teiUid = CodeGenerator.generateUid();
    String invalidProgramUid = CodeGenerator.generateUid() + "invalid";
    String validProgramUid = CodeGenerator.generateUid();
    // Given
    TeiQueryRequest teiQueryRequest = new TeiQueryRequest(teiUid);

    CommonQueryRequest commonQueryRequest = new CommonQueryRequest();
    commonQueryRequest.setProgram(Set.of(validProgramUid, invalidProgramUid));

    QueryRequest<TeiQueryRequest> queryRequest =
        QueryRequest.<TeiQueryRequest>builder()
            .commonQueryRequest(commonQueryRequest)
            .request(teiQueryRequest)
            .build();

    CommonQueryRequestValidator commonQueryRequestValidator = new CommonQueryRequestValidator();
    TeiQueryRequestValidator teiQueryRequestValidator =
        new TeiQueryRequestValidator(commonQueryRequestValidator);

    // When
    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class, () -> teiQueryRequestValidator.validate(queryRequest));

    // Then
    assertEquals(
        "Invalid UID `" + invalidProgramUid + "` for property `program`", exception.getMessage());
  }

  @Test
  void testProgramStatusAndEnrollmentStatusThrowsException() {
    CommonQueryRequest request =
        new CommonQueryRequest()
            .withProgram(Set.of("IpHINAT79UW"))
            .withProgramStatus(Set.of("IpHINAT79UW.COMPLETED"))
            .withEnrollmentStatus(Set.of("IpHINAT79UW.COMPLETED"));

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class, () -> new CommonQueryRequestValidator().validate(request));

    assertEquals(
        "programStatus and enrollmentStatus cannot be used together.", exception.getMessage());
  }
}
