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
package org.hisp.dhis.programrule.config;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.action.validation.AlwaysValidProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.AssignProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.BaseProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.HideOptionProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.HideProgramStageProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.HideSectionProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.NotificationProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.ProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.ScheduleEventProgramRuleActionValidator;
import org.hisp.dhis.programrule.action.validation.ShowHideOptionGroupProgramRuleActionValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("programRuleServiceConfig")
public class ServiceConfig {

  private final Map<Class<? extends ProgramRuleActionValidator>, ProgramRuleActionValidator>
      programRuleActionValidatorsByClass;

  public ServiceConfig(Collection<ProgramRuleActionValidator> programRuleActionValidators) {
    programRuleActionValidatorsByClass = byClass(programRuleActionValidators);
  }

  @SuppressWarnings("unchecked")
  private <T> Map<Class<? extends T>, T> byClass(Collection<T> items) {
    return items.stream()
        .collect(Collectors.toMap(e -> (Class<? extends T>) e.getClass(), Functions.identity()));
  }

  private ProgramRuleActionValidator getProgramRuleActionValidatorByClass(
      Class<? extends ProgramRuleActionValidator> programRuleActionValidatorClass) {
    return getByClass(programRuleActionValidatorsByClass, programRuleActionValidatorClass);
  }

  private <T> T getByClass(
      Map<Class<? extends T>, ? extends T> tByClass, Class<? extends T> clazz) {
    return Optional.ofNullable(tByClass.get(clazz))
        .orElseThrow(
            () -> new IllegalArgumentException("Unable to find validator by class: " + clazz));
  }

  @Bean
  public Map<ProgramRuleActionType, ProgramRuleActionValidator> programRuleActionValidatorMap() {
    return ImmutableMap.<ProgramRuleActionType, ProgramRuleActionValidator>builder()
        .put(
            ProgramRuleActionType.SENDMESSAGE,
            getProgramRuleActionValidatorByClass(NotificationProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.SCHEDULEMESSAGE,
            getProgramRuleActionValidatorByClass(NotificationProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.SHOWOPTIONGROUP,
            getProgramRuleActionValidatorByClass(
                ShowHideOptionGroupProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.HIDEOPTIONGROUP,
            getProgramRuleActionValidatorByClass(
                ShowHideOptionGroupProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.DISPLAYTEXT,
            getProgramRuleActionValidatorByClass(AlwaysValidProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.DISPLAYKEYVALUEPAIR,
            getProgramRuleActionValidatorByClass(AlwaysValidProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.ASSIGN,
            getProgramRuleActionValidatorByClass(AssignProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.CREATEEVENT,
            getProgramRuleActionValidatorByClass(BaseProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.HIDEFIELD,
            getProgramRuleActionValidatorByClass(BaseProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.SCHEDULEEVENT,
            getProgramRuleActionValidatorByClass(ScheduleEventProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.WARNINGONCOMPLETE,
            getProgramRuleActionValidatorByClass(AlwaysValidProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.ERRORONCOMPLETE,
            getProgramRuleActionValidatorByClass(AlwaysValidProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.SHOWWARNING,
            getProgramRuleActionValidatorByClass(AlwaysValidProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.SHOWERROR,
            getProgramRuleActionValidatorByClass(AlwaysValidProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.SETMANDATORYFIELD,
            getProgramRuleActionValidatorByClass(BaseProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.HIDEOPTION,
            getProgramRuleActionValidatorByClass(HideOptionProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.HIDESECTION,
            getProgramRuleActionValidatorByClass(HideSectionProgramRuleActionValidator.class))
        .put(
            ProgramRuleActionType.HIDEPROGRAMSTAGE,
            getProgramRuleActionValidatorByClass(HideProgramStageProgramRuleActionValidator.class))
        .build();
  }
}
