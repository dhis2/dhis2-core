/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.springframework.stereotype.Component;

/**
 * Common Merge handler for metadata entities. The merge operations here are shared by many merge
 * use cases (e.g. Indicator merge & DataElement merge), hence the need for common handlers, to
 * reuse code and avoid duplication.
 *
 * @author david mackessy
 */
@Component
@RequiredArgsConstructor
public class CommonMergeHandler {

  private final IndicatorService indicatorService;
  private final DataEntryFormService dataEntryFormService;

  /**
   * Replace all {@link BaseIdentifiableObject} refs in the numerator and denominator fields of
   * {@link Indicator}s
   *
   * @param sources to be replaced
   * @param target to replace each source
   */
  public <T extends BaseIdentifiableObject> void handleRefsInIndicatorExpression(
      List<T> sources, T target) {
    for (T source : sources) {
      // numerators
      List<Indicator> numeratorIndicators =
          indicatorService.getIndicatorsWithNumeratorContaining(UID.of(source.getUid()));

      numeratorIndicators.forEach(
          i -> {
            String existingNumerator = i.getNumerator();
            i.setNumerator(existingNumerator.replace(source.getUid(), target.getUid()));
          });

      // denominators
      List<Indicator> denominatorIndicators =
          indicatorService.getIndicatorsWithDenominatorContaining(UID.of(source.getUid()));

      denominatorIndicators.forEach(
          i -> {
            String existingDenominator = i.getDenominator();
            i.setDenominator(existingDenominator.replace(source.getUid(), target.getUid()));
          });
    }
  }

  /**
   * Replace all {@link BaseIdentifiableObject} refs in the htmlCode fields
   *
   * @param sources to be replaced
   * @param target to replace each source
   */
  public <T extends BaseIdentifiableObject> void handleRefsInCustomForms(
      List<T> sources, T target) {
    for (T source : sources) {
      List<DataEntryForm> forms =
          dataEntryFormService.getDataEntryFormsWithHtmlContaining(source.getUid());

      if (CollectionUtils.isNotEmpty(forms)) {
        for (DataEntryForm dataEntryForm : forms) {
          String existingHtml = dataEntryForm.getHtmlCode();
          dataEntryForm.setHtmlCode(existingHtml.replace(source.getUid(), target.getUid()));
        }
      }
    }
  }
}
