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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleActionValidationResult;
import org.hisp.dhis.programrule.action.validation.ProgramRuleActionValidationContext;
import org.hisp.dhis.programrule.action.validation.ProgramRuleActionValidationContextLoader;
import org.hisp.dhis.programrule.action.validation.ProgramRuleActionValidator;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */
@Component("programRuleActionObjectBundle")
@Slf4j
public class ProgramRuleActionObjectBundleHook extends AbstractObjectBundleHook<ProgramRuleAction> {
  @Nonnull
  private final Map<ProgramRuleActionType, ProgramRuleActionValidator>
      programRuleActionValidatorMap;

  @Nonnull private final ProgramRuleActionValidationContextLoader contextLoader;

  public ProgramRuleActionObjectBundleHook(
      @Nonnull Map<ProgramRuleActionType, ProgramRuleActionValidator> programRuleActionValidatorMap,
      @Nonnull ProgramRuleActionValidationContextLoader contextLoader) {
    this.programRuleActionValidatorMap = programRuleActionValidatorMap;
    this.contextLoader = contextLoader;
  }

  @Override
  public void validate(
      ProgramRuleAction programRuleAction, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    ProgramRuleActionValidationResult validationResult =
        validateProgramRuleAction(programRuleAction, bundle);

    if (!validationResult.isValid()) {
      addReports.accept(validationResult.getErrorReport());
    }
  }

  @Override
  public void preCreate(ProgramRuleAction object, ObjectBundle bundle) {
    getNotificationTemplate(object, bundle);
  }

  @Override
  public void preUpdate(
      ProgramRuleAction object, ProgramRuleAction persistedObject, ObjectBundle bundle) {
    getNotificationTemplate(object, bundle);
  }

  private ProgramRuleActionValidationResult validateProgramRuleAction(
      ProgramRuleAction ruleAction, ObjectBundle bundle) {
    ProgramRuleActionValidationResult validationResult;

    ProgramRuleActionValidationContext validationContext =
        contextLoader.load(bundle.getPreheat(), bundle.getPreheatIdentifier(), ruleAction);

    ProgramRuleActionValidator validator =
        programRuleActionValidatorMap.get(ruleAction.getProgramRuleActionType());

    validationResult = validator.validate(ruleAction, validationContext);

    return validationResult;
  }

  private void getNotificationTemplate(ProgramRuleAction object, ObjectBundle bundle) {
    if (object.getTemplateUid() == null) {
      // Nothing to do when templateUid is null
      return;
    }

    // try to get the template from the preheated bundle.
    ProgramNotificationTemplate template =
        bundle
            .getPreheat()
            .get(
                bundle.getPreheatIdentifier(),
                ProgramNotificationTemplate.class,
                object.getTemplateUid());

    // If not found in preheated objects, fetch from the database.
    if (template == null) {
      template = manager.get(ProgramNotificationTemplate.class, object.getTemplateUid());
    }

    if (template == null) {
      log.info("No ProgramNotificationTemplate found for {}: ", object.getTemplateUid());
      return;
    }

    object.setNotificationTemplate(template);
  }
}
