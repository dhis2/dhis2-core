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

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.springframework.stereotype.Component;

/**
 * @author Volker Schmidt
 */
@Component
@AllArgsConstructor
public class OptionObjectBundleHook extends AbstractObjectBundleHook<Option> {
  private final OptionService optionService;

  @Override
  public void validate(Option option, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    if (option.getOptionSet() != null) {
      OptionSet optionSet =
          bundle
              .getPreheat()
              .get(bundle.getPreheatIdentifier(), OptionSet.class, option.getOptionSet());

      checkDuplicateOption(optionSet, option, addReports);
      ErrorMessage error = optionService.validateOption(optionSet, option);
      if (error != null) {
        addReports.accept(new ErrorReport(OptionSet.class, error));
      }
    }
  }

  @Override
  public void preCreate(Option option, ObjectBundle bundle) {
    if (option.getOptionSet() == null) {
      return;
    }

    // If OptionSet doesn't contains Option but Option has reference to
    // OptionSet
    // then we need to update OptionSet.options collection.
    OptionSet optionSet =
        bundle
            .getPreheat()
            .get(bundle.getPreheatIdentifier(), OptionSet.class, option.getOptionSet().getUid());

    if (optionSet != null && optionSet.getOptionByUid(option.getUid()) == null) {
      optionSet.addOption(option);
    }
  }

  @Override
  public void preUpdate(Option option, Option persistedObject, ObjectBundle bundle) {
    if (option.getOptionSet() == null) {
      return;
    }

    OptionSet optionSet =
        bundle
            .getPreheat()
            .get(bundle.getPreheatIdentifier(), OptionSet.class, option.getOptionSet().getUid());

    if (optionSet != null) {
      // Remove the existed option from OptionSet, will add the updating
      // one
      // in postUpdate()
      optionSet.removeOption(persistedObject);
    }
  }

  @Override
  public void postUpdate(Option option, ObjectBundle bundle) {
    if (option.getOptionSet() == null) {
      return;
    }

    OptionSet optionSet =
        bundle
            .getPreheat()
            .get(bundle.getPreheatIdentifier(), OptionSet.class, option.getOptionSet().getUid());

    if (optionSet != null) {
      // Add the updated Option to OptionSet, this will allow Hibernate to
      // re-organize sortOrder gaps if any.
      optionSet.addOption(option);
    }
  }

  /** Check for duplication of Option's name OR code within given OptionSet */
  private void checkDuplicateOption(
      OptionSet optionSet, Option checkOption, Consumer<ErrorReport> addReports) {
    if (optionSet == null || optionSet.getOptions().isEmpty() || checkOption == null) {
      return;
    }

    for (Option option : optionSet.getOptions()) {
      if (option == null
          || option.getName() == null
          || option.getCode() == null
          || ObjectUtils.allNotNull(option.getUid(), checkOption.getUid())
              && option.getUid().equals(checkOption.getUid())) {
        continue;
      }

      if (option.getName().equals(checkOption.getName())
          || option.getCode().equals(checkOption.getCode())) {
        addReports.accept(
            new ErrorReport(OptionSet.class, ErrorCode.E4028, optionSet.getUid(), option.getUid()));
      }
    }
  }
}
