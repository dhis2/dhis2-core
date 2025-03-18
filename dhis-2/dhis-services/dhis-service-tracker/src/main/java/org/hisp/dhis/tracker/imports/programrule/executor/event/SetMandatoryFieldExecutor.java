/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.imports.programrule.executor.event;

import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.error;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.validateDeletionMandatoryDataValue;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.validateMandatoryDataValue;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;

/**
 * This executor checks if a field is not empty in the {@link TrackerBundle} @Author Enrico
 * Colasante
 */
@RequiredArgsConstructor
public class SetMandatoryFieldExecutor implements RuleActionExecutor<Event> {
  private final UID ruleUid;

  private final UID fieldUid;

  @Override
  public UID getDataElementUid() {
    return fieldUid;
  }

  @Override
  public Optional<ProgramRuleIssue> executeRuleAction(TrackerBundle bundle, Event event) {
    TrackerPreheat preheat = bundle.getPreheat();
    TrackerIdSchemeParams idSchemes = preheat.getIdSchemes();
    ProgramStage programStage = preheat.getProgramStage(event.getProgramStage());

    Optional<ProgramRuleIssue> programRuleIssue =
        validateDeletionMandatoryDataValue(
                event,
                programStage,
                List.of(
                    idSchemes.toMetadataIdentifier(preheat.getDataElement(fieldUid.getValue()))))
            .stream()
            .map(e -> error(ruleUid, ValidationCode.E1314, e.getIdentifierOrAttributeValue()))
            .findAny();

    if (programRuleIssue.isEmpty()) {
      return validateMandatoryDataValue(
              bundle,
              event,
              programStage,
              List.of(idSchemes.toMetadataIdentifier(preheat.getDataElement(fieldUid.getValue()))))
          .stream()
          .map(e -> error(ruleUid, ValidationCode.E1301, e.getIdentifierOrAttributeValue()))
          .findAny();
    }

    return programRuleIssue;
  }
}
