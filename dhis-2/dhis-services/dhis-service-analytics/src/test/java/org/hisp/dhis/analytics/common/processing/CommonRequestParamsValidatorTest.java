/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CommonQueryRequestValidator}.
 *
 * @author maikel arabori
 */
class CommonRequestParamsValidatorTest {
  @Test
  void testValidateWithSuccess() {
    // Given
    CommonRequestParams commonRequestParams = new CommonRequestParams();
    commonRequestParams.setProgram(Set.of("prgabcdef11"));
    commonRequestParams.setDimension(Set.of("ou:jmIPBj66vD6"));

    // Then
    assertDoesNotThrow(() -> new CommonQueryRequestValidator().validate(commonRequestParams));
  }

  @Test
  void testValidateWhenPeIsDefined() {
    // Given
    CommonRequestParams commonRequestParams = new CommonRequestParams();
    commonRequestParams.setProgram(Set.of("prgabcdef11"));
    commonRequestParams.setDimension(Set.of("pe:LAST_YEAR"));

    CommonQueryRequestValidator validator = new CommonQueryRequestValidator();

    // When
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> validator.validate(commonRequestParams));

    // Then
    assertEquals("Query item or filter is invalid: `pe:LAST_YEAR`", exception.getMessage());
  }

  @Test
  void testValidateWhenProgramUidIsInvalid() {
    // Given
    String invalidProgramUid = CodeGenerator.generateUid() + "invalid";
    String validProgramUid = CodeGenerator.generateUid();
    CommonRequestParams commonRequestParams = new CommonRequestParams();
    commonRequestParams.setProgram(Set.of(validProgramUid, invalidProgramUid));

    CommonQueryRequestValidator validator = new CommonQueryRequestValidator();

    // When
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> validator.validate(commonRequestParams));

    // Then
    assertEquals(
        "Invalid UID `" + invalidProgramUid + "` for property `program`", exception.getMessage());
  }

  @Test
  void testProgramStatusAndEnrollmentStatusThrowsException() {
    CommonRequestParams request = new CommonRequestParams();
    request.setProgram(Set.of("IpHINAT79UW"));
    request.setProgramStatus(Set.of("IpHINAT79UW.COMPLETED"));
    request.setEnrollmentStatus(Set.of("IpHINAT79UW.COMPLETED"));

    CommonQueryRequestValidator validator = new CommonQueryRequestValidator();

    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> validator.validate(request));

    assertEquals(
        "Parameters programStatus and enrollmentStatus cannot be used together",
        exception.getMessage());
  }
}
