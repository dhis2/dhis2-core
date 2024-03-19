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

import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.springframework.stereotype.Component;

/**
 * @author markusbekken
 */
@Component
@RequiredArgsConstructor
public class ProgramRuleDeletionHandler extends DeletionHandler {
  private final ProgramRuleService programRuleService;

  @Override
  protected void register() {
    whenDeleting(Program.class, this::deleteProgram);
    whenVetoing(ProgramStageSection.class, this::allowDeleteProgramStageSection);
    whenVetoing(ProgramStage.class, this::allowDeleteProgramStage);
  }

  private void deleteProgram(Program program) {
    for (ProgramRule programRule : programRuleService.getProgramRule(program)) {
      programRuleService.deleteProgramRule(programRule);
    }
  }

  private DeletionVeto allowDeleteProgramStageSection(ProgramStageSection programStageSection) {
    ProgramStage programStage = programStageSection.getProgramStage();
    if (programStage == null) {
      return DeletionVeto.ACCEPT;
    }
    String programRules =
        programRuleService.getProgramRule(programStage.getProgram()).stream()
            .filter(pr -> isLinkedToProgramStageSection(programStageSection, pr))
            .map(IdentifiableObject::getName)
            .collect(Collectors.joining(", "));

    return StringUtils.isBlank(programRules)
        ? DeletionVeto.ACCEPT
        : new DeletionVeto(ProgramRule.class, programRules);
  }

  private DeletionVeto allowDeleteProgramStage(ProgramStage programStage) {
    String programRules =
        programRuleService.getProgramRule(programStage.getProgram()).stream()
            .filter(pr -> isLinkedToProgramStage(programStage, pr))
            .map(IdentifiableObject::getName)
            .collect(Collectors.joining(", "));

    return StringUtils.isBlank(programRules)
        ? DeletionVeto.ACCEPT
        : new DeletionVeto(ProgramRule.class, programRules);
  }

  private boolean isLinkedToProgramStage(ProgramStage programStage, ProgramRule programRule) {
    return Objects.equals(programRule.getProgramStage(), programStage)
        || programRule.getProgramRuleActions().stream()
            .anyMatch(pra -> Objects.equals(pra.getProgramStage(), programStage))
        || programStage.getProgramStageSections().stream()
            .anyMatch(s -> isLinkedToProgramStageSection(s, programRule));
  }

  private boolean isLinkedToProgramStageSection(
      ProgramStageSection programStageSection, ProgramRule programRule) {
    return programRule.getProgramRuleActions().stream()
        .anyMatch(pra -> Objects.equals(pra.getProgramStageSection(), programStageSection));
  }
}
