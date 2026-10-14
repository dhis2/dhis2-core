/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.programstageworkinglist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;

class ProgramStageWorkingListTest {

  private final Program program = program("ProgramUidA");

  private final ProgramStage programStage = programStage("StageUid0A");

  private final ProgramStageQueryCriteria criteria = new ProgramStageQueryCriteria();

  @Test
  void equalsWhenIdentityAndDomainPropertiesMatch() {
    ProgramStageWorkingList a = workingList("Description");
    ProgramStageWorkingList b = workingList("Description");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void notEqualsWhenDescriptionDiffers() {
    assertNotEquals(workingList("Description"), workingList("Other description"));
  }

  @Test
  void notEqualsWhenProgramDiffers() {
    ProgramStageWorkingList other = workingList("Description");
    other.setProgram(program("ProgramUidB"));

    assertNotEquals(workingList("Description"), other);
  }

  @Test
  void notEqualsWhenProgramStageDiffers() {
    ProgramStageWorkingList other = workingList("Description");
    other.setProgramStage(programStage("StageUid0B"));

    assertNotEquals(workingList("Description"), other);
  }

  @Test
  void notEqualsWhenQueryCriteriaDiffers() {
    ProgramStageWorkingList other = workingList("Description");
    other.setProgramStageQueryCriteria(new ProgramStageQueryCriteria());

    assertNotEquals(workingList("Description"), other);
  }

  @Test
  void notEqualsWhenUidDiffers() {
    ProgramStageWorkingList other = workingList("Description");
    other.setUid("WorkList00B");

    assertNotEquals(workingList("Description"), other);
  }

  private ProgramStageWorkingList workingList(String description) {
    ProgramStageWorkingList workingList = new ProgramStageWorkingList();
    workingList.setUid("WorkList00A");
    workingList.setCode("CODE_A");
    workingList.setName("Working list");
    workingList.setDescription(description);
    workingList.setProgram(program);
    workingList.setProgramStage(programStage);
    workingList.setProgramStageQueryCriteria(criteria);
    return workingList;
  }

  private static Program program(String uid) {
    Program program = new Program();
    program.setUid(uid);
    return program;
  }

  private static ProgramStage programStage(String uid) {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid(uid);
    return programStage;
  }
}
