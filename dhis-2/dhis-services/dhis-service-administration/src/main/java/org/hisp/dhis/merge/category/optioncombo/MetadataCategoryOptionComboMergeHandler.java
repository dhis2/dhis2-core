/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge.category.optioncombo;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionStore;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.springframework.stereotype.Component;

/**
 * Merge handler for metadata entities.
 *
 * @author david mackessy
 */
@Component
@RequiredArgsConstructor
public class MetadataCategoryOptionComboMergeHandler {

  private final CategoryOptionStore categoryOptionStore;

  /**
   * Remove sources from {@link CategoryOption} and add target to {@link CategoryOption}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleCategoryOptions(List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    List<CategoryOption> categoryOptions =
        categoryOptionStore.getByCategoryOptionCombo(UID.toUidValueSet(sources));

    categoryOptions.forEach(
        co -> {
          co.addCategoryOptionCombo(target);
          co.removeCategoryOptionCombos(sources);
        });
  }

  /**
   * Remove sources from {@link CategoryOptionCombo} and add target to {@link CategoryOptionCombo}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleCategoryCombos(List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    // TODO
  }

  /**
   * Remove sources from {@link DataElementOperand} and add target to {@link DataElementOperand}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleDataElementOperands(
      List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    // TODO x 2
  }

  /**
   * Remove sources from {@link MinMaxDataElement} and add target to {@link MinMaxDataElement}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleMinMaxDataElements(
      List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    // TODO
  }

  /**
   * Remove sources from {@link Predictor} and add target to {@link Predictor}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handlePredictors(List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    // TODO
  }

  /**
   * Remove sources from {@link SMSCode} and add target to {@link SMSCode}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleSmsCodes(List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    // TODO
  }
}
