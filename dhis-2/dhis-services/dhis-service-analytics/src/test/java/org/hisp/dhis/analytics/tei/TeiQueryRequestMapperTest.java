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
package org.hisp.dhis.analytics.tei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.analytics.common.QueryRequest;
import org.hisp.dhis.analytics.common.processing.CommonQueryRequestMapper;
import org.hisp.dhis.analytics.common.processing.Processor;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeiQueryRequestMapperTest {
  private final TrackedEntityTypeService trackedEntityTypeService =
      mock(TrackedEntityTypeService.class);

  private final ProgramService programService = mock(ProgramService.class);

  private TeiQueryRequestMapper teiQueryRequestMapper;

  @BeforeEach
  public void setUp() {
    teiQueryRequestMapper =
        new TeiQueryRequestMapper(
            mock(CommonQueryRequestMapper.class),
            trackedEntityTypeService,
            mock(Processor.class),
            programService);
  }

  @Test
  void testOneProgramFailing() {
    testValidateTrackedEntityType(
        "T1", "Program(s) `[nameB (B)]` are not defined on Tracked Entity Type `nameT1 (T1)`");
  }

  @Test
  void testTwoProgramsFailing() {
    testValidateTrackedEntityType(
        "T3",
        "Program(s) `[nameA (A), nameB (B)]` are not defined on Tracked Entity Type `nameT3 (T3)`");
  }

  void testValidateTrackedEntityType(String trackedEntityTypeUid, String expectedMessage) {
    Program programA = stubProgram("A", "T1");
    Program programB = stubProgram("B", "T2");

    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);

    QueryRequest<TeiQueryRequest> queryRequest =
        QueryRequest.<TeiQueryRequest>builder()
            .request(new TeiQueryRequest(trackedEntityTypeUid))
            .commonQueryRequest(new CommonQueryRequest().withProgram(Set.of("A", "B")))
            .build();

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    when(programService.getPrograms(Set.of("A", "B"))).thenReturn(Set.of(programA, programB));

    final IllegalQueryException thrown =
        assertThrows(IllegalQueryException.class, () -> teiQueryRequestMapper.map(queryRequest));

    assertEquals(expectedMessage, thrown.getMessage());
  }

  @Test
  void testOK() {
    String trackedEntityTypeUid = "T1";

    Program programA = stubProgram("A", "T1");
    Program programB = stubProgram("B", "T1");

    TrackedEntityType trackedEntityType = stubTrackedEntityType(trackedEntityTypeUid);

    QueryRequest<TeiQueryRequest> queryRequest =
        QueryRequest.<TeiQueryRequest>builder()
            .request(new TeiQueryRequest(trackedEntityTypeUid))
            .commonQueryRequest(new CommonQueryRequest().withProgram(Set.of("A", "B")))
            .build();

    when(trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeUid))
        .thenReturn(trackedEntityType);

    when(programService.getPrograms(Set.of("A", "B"))).thenReturn(Set.of(programA, programB));

    TeiQueryParams mapped = teiQueryRequestMapper.map(queryRequest);
  }

  private Program stubProgram(String uid, String tetUid) {
    Program program = new Program("name" + uid, "description" + uid);
    program.setUid(uid);
    program.setTrackedEntityType(stubTrackedEntityType(tetUid));
    return program;
  }

  private TrackedEntityType stubTrackedEntityType(String tetUid) {
    TrackedEntityType trackedEntityType =
        new TrackedEntityType("name" + tetUid, "description" + tetUid);
    trackedEntityType.setUid(tetUid);
    return trackedEntityType;
  }
}
