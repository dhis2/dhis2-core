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
package org.hisp.dhis.webapi.controller.tracker.message;

import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.program.message.ProgramMessageOperationParams;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.webapi.controller.message.ProgramMessageRequestParamMapper;
import org.hisp.dhis.webapi.controller.message.ProgramMessageRequestParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Zubair Asghar
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ProgramMessageRequestParamsMapperTest {
  private static final UID ENROLLMENT = UID.generate();
  private static final UID EVENT = UID.generate();

  @InjectMocks private ProgramMessageRequestParamMapper subject;

  @Test
  void shouldMapProgramMessageStatus() throws ConflictException, BadRequestException {
    ProgramMessageRequestParams requestParams = new ProgramMessageRequestParams();
    requestParams.setEnrollment(ENROLLMENT);
    requestParams.setMessageStatus(ProgramMessageStatus.SENT);

    ProgramMessageOperationParams operationParams = subject.map(requestParams);

    assertEquals(ProgramMessageStatus.SENT, operationParams.getMessageStatus());
  }

  @Test
  void shouldMapProgramMessageEnrollment() throws ConflictException, BadRequestException {
    ProgramMessageRequestParams requestParams = new ProgramMessageRequestParams();
    requestParams.setEnrollment(ENROLLMENT);

    ProgramMessageOperationParams operationParams = subject.map(requestParams);

    assertEquals(ENROLLMENT, operationParams.getEnrollment());
  }

  @Test
  void shouldFailIfBothEnrollmentAndEventPresent() {
    ProgramMessageRequestParams requestParams = new ProgramMessageRequestParams();
    requestParams.setEnrollment(ENROLLMENT);
    requestParams.setEvent(EVENT);

    ConflictException exception =
        assertThrows(ConflictException.class, () -> subject.map(requestParams));

    assertStartsWith("Enrollment and Event cannot be processed together.", exception.getMessage());
  }

  @Test
  void shouldFailIfBothEnrollmentAndEventMissing() {
    ProgramMessageRequestParams requestParams = new ProgramMessageRequestParams();

    ConflictException exception =
        assertThrows(ConflictException.class, () -> subject.map(requestParams));

    assertStartsWith("Enrollment or Event must be specified.", exception.getMessage());
  }
}
