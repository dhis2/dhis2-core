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

import java.util.Collections;
import java.util.List;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSectionService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgramStageObjectBundleHookTest {
  private ProgramStageObjectBundleHook subject;

  @Mock private AclService aclService;

  @Mock private ProgramStageSectionService programStageSectionService;

  private ProgramStage programStage;

  private Program program;

  private DataElement dataElement;

  private User user;

  private Preheat preheat = new Preheat();

  @BeforeEach
  public void init() {
    this.subject = new ProgramStageObjectBundleHook(aclService, programStageSectionService);

    program = DhisConvenienceTest.createProgram('A');
    program.setUid("jGRqKgwvvb6");

    programStage = DhisConvenienceTest.createProgramStage('A', program);
    programStage.setUid("giQh4EFWKOk");

    dataElement = DhisConvenienceTest.createDataElement('A');
    dataElement.setUid("qtplcYVR1oO");
    programStage.addDataElement(dataElement, 0);
    user = DhisConvenienceTest.createUser('A');
    user.setUid("WAoGUm593U7");

    preheat.put(PreheatIdentifier.UID, program);
    preheat.put(PreheatIdentifier.UID, programStage);
    preheat.put(PreheatIdentifier.UID, dataElement);
  }

  @Test
  void testValidateDataElementAcl() {
    ObjectBundleParams objectBundleParams = new ObjectBundleParams();
    objectBundleParams.setPreheatIdentifier(PreheatIdentifier.UID);
    objectBundleParams.setUser(user);
    ObjectBundle bundle =
        new ObjectBundle(
            objectBundleParams,
            preheat,
            Collections.singletonMap(OptionSet.class, Collections.singletonList(programStage)));

    List<ErrorReport> errors = subject.validate(programStage, bundle);
    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(ErrorCode.E3012, errors.get(0).getErrorCode());
  }
}
