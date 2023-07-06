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
package org.hisp.dhis.programrule;

import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.springframework.stereotype.Component;

/**
 * @author markusbekken
 */
@Component
@RequiredArgsConstructor
public class ProgramRuleVariableDeletionHandler extends DeletionHandler {
  private final ProgramRuleVariableService programRuleVariableService;

  @Override
  protected void register() {
    whenVetoing(ProgramStage.class, this::allowDeleteProgramStage);
    whenDeleting(Program.class, this::deleteProgram);
  }

  private DeletionVeto allowDeleteProgramStage(ProgramStage programStage) {
    String programRuleVariables =
        programRuleVariableService.getProgramRuleVariable(programStage.getProgram()).stream()
            .filter(prv -> Objects.equals(prv.getProgramStage(), programStage))
            .map(BaseIdentifiableObject::getName)
            .collect(Collectors.joining(", "));

    return StringUtils.isBlank(programRuleVariables)
        ? ACCEPT
        : new DeletionVeto(ProgramRuleVariable.class, programRuleVariables);
  }

  private void deleteProgram(Program program) {
    for (ProgramRuleVariable programRuleVariable :
        programRuleVariableService.getProgramRuleVariable(program)) {
      programRuleVariableService.deleteProgramRuleVariable(programRuleVariable);
    }
  }
}
