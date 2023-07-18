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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AclService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class ProgramObjectBundleHookTest {
  private ProgramObjectBundleHook subject;

  @Mock private ProgramInstanceService programInstanceService;

  @Mock private ProgramService programService;

  @Mock private ProgramStageService programStageService;

  @Mock private AclService aclService;

  @Mock private SessionFactory sessionFactory;

  private Program programA;

  @BeforeEach
  public void setUp() {
    this.subject =
        new ProgramObjectBundleHook(programInstanceService, programStageService, aclService);

    programA = createProgram('A');
    programA.setId(100);
  }

  @Test
  void verifyNullObjectIsIgnored() {
    subject.preCreate(null, null);

    verifyNoInteractions(programInstanceService);
  }

  @Test
  void verifyMissingBundleIsIgnored() {
    subject.preCreate(programA, null);

    verifyNoInteractions(programInstanceService);
  }

  @Test
  void verifyProgramInstanceIsSavedForEventProgram() {
    ArgumentCaptor<ProgramInstance> argument = ArgumentCaptor.forClass(ProgramInstance.class);

    programA.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    subject.postCreate(programA, null);

    verify(programInstanceService).addProgramInstance(argument.capture());

    assertThat(argument.getValue().getEnrollmentDate(), is(notNullValue()));
    assertThat(argument.getValue().getIncidentDate(), is(notNullValue()));
    assertThat(argument.getValue().getProgram(), is(programA));
    assertThat(argument.getValue().getStatus(), is(ProgramStatus.ACTIVE));
    assertThat(argument.getValue().getStoredBy(), is("system-process"));
  }

  @Test
  void verifyProgramInstanceIsNotSavedForTrackerProgram() {
    ArgumentCaptor<ProgramInstance> argument = ArgumentCaptor.forClass(ProgramInstance.class);

    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    subject.postCreate(programA, null);

    verify(programInstanceService, times(0)).addProgramInstance(argument.capture());
  }

  @Test
  void verifyProgramValidates() {
    assertEquals(0, subject.validate(programA, null).size());
  }

  @Test
  void verifyProgramFailsValidation() {
    ProgramInstanceQueryParams programInstanceQueryParams = new ProgramInstanceQueryParams();
    programInstanceQueryParams.setProgram(programA);
    programInstanceQueryParams.setProgramStatus(ProgramStatus.ACTIVE);

    when(programInstanceService.getProgramInstances(programA, ProgramStatus.ACTIVE))
        .thenReturn(Lists.newArrayList(new ProgramInstance(), new ProgramInstance()));

    List<ErrorReport> errors = subject.validate(programA, null);

    assertEquals(1, errors.size());
    assertEquals(errors.get(0).getErrorCode(), ErrorCode.E6000);
  }

  @Test
  void verifyValidationIsSkippedWhenObjectIsTransient() {
    Program transientObj = createProgram('A');
    subject.validate(transientObj, null);

    verifyNoInteractions(programInstanceService);
  }

  @Test
  void verifyUpdateProgramStage() {
    ProgramStage programStage = createProgramStage('A', 1);
    programA.getProgramStages().add(programStage);

    assertNull(programA.getProgramStages().iterator().next().getProgram());

    subject.postCreate(programA, null);

    assertNotNull(programA.getProgramStages().iterator().next().getProgram());
  }
}
