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
package org.hisp.dhis.merge.category.optioncombo;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryComboDeletionHandler;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.datadimensionitem.DataDimensionItemStore;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperandStore;
import org.hisp.dhis.expression.ExpressionStore;
import org.hisp.dhis.indicator.IndicatorStore;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementStore;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.predictor.PredictorStore;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.command.hibernate.SMSCommandStore;
import org.springframework.stereotype.Component;

/**
 * Merge handler for metadata entities.
 *
 * @author david mackessy
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataCategoryOptionComboMergeHandler {

  private final DataElementOperandStore dataElementOperandStore;
  private final DataDimensionItemStore dataDimensionItemStore;
  private final MinMaxDataElementStore minMaxDataElementStore;
  private final PredictorStore predictorStore;
  private final SMSCommandStore smsCommandStore;
  private final IndicatorStore indicatorStore;
  private final ExpressionStore expressionStore;

  /**
   * Remove all {@link CategoryOption}s from source {@link CategoryOptionCombo}s
   *
   * @param sources to be removed
   */
  public void handleCategoryOptions(@Nonnull List<CategoryOptionCombo> sources) {
    for (CategoryOptionCombo coc : sources) coc.removeAllCategoryOptions();
    log.info("Removed all category option references for source category option combos");
  }

  /**
   * Although nothing is done but log in this method, it is worth having to expose how the {@link
   * CategoryCombo}s refs are being handled and also show that this has been thought about. This
   * helps keep it up front and not hidden. The easiest way to remove this relationship is to let
   * the {@link CategoryComboDeletionHandler} look after it.
   */
  public void handleCategoryCombos() {
    log.info(
        "Category combo references will be removed when the category option combo is deleted at the end of the merge process");
  }

  /**
   * Set target to {@link DataElementOperand}
   *
   * @param sources to be actioned
   * @param target to add
   */
  public void handleDataElementOperands(
      List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    log.info("Merging source data element operands");
    List<DataElementOperand> dataElementOperands =
        dataElementOperandStore.getByCategoryOptionCombo(
            UID.of(sources.stream().map(IdentifiableObject::getUid).toList()));

    dataElementOperands.forEach(deo -> deo.setCategoryOptionCombo(target));

    // A data element operand is also a data dimension item.
    // The above update does not cascade the reference change though.
    // The Data dimension item table also needs updating
    int dataDimensionItemsUpdated =
        dataDimensionItemStore.updateDeoCategoryOptionCombo(
            sources.stream().map(IdentifiableObject::getId).collect(Collectors.toSet()),
            target.getId());
    log.info(
        "{} data dimension items updated as part of category option combo merge",
        dataDimensionItemsUpdated);
  }

  /**
   * Set target to {@link MinMaxDataElement}
   *
   * @param sources to be actioned
   * @param target to add
   */
  public void handleMinMaxDataElements(
      List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    log.info("Merging source min max data elements");
    List<MinMaxDataElement> minMaxDataElements =
        minMaxDataElementStore.getByCategoryOptionCombo(
            UID.of(sources.stream().map(IdentifiableObject::getUid).toList()));

    minMaxDataElements.forEach(mmde -> mmde.setOptionCombo(target));
  }

  /**
   * Set target to {@link Predictor}
   *
   * @param sources to be actioned
   * @param target to add
   */
  public void handlePredictors(List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    log.info("Merging source predictors");
    List<Predictor> predictors =
        predictorStore.getByCategoryOptionCombo(
            UID.of(sources.stream().map(IdentifiableObject::getUid).toList()));

    predictors.forEach(p -> p.setOutputCombo(target));
  }

  /**
   * Set target to {@link SMSCode}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleSmsCodes(List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    log.info("Merging source SMS codes");
    List<SMSCode> smsCodes =
        smsCommandStore.getCodesByCategoryOptionCombo(
            UID.of(sources.stream().map(IdentifiableObject::getUid).toList()));

    smsCodes.forEach(smsCode -> smsCode.setOptionId(target));
  }

  /**
   * Update each Indicator numerator and denominator values, replacing any source ref with the
   * target ref.
   *
   * @param sources to be replaced
   * @param target to replace source refs
   */
  public void handleIndicators(List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    log.info("Merging source indicators");
    int totalUpdates = 0;
    for (CategoryOptionCombo source : sources) {
      totalUpdates +=
          indicatorStore.updateNumeratorDenominatorContaining(source.getUid(), target.getUid());
    }

    log.info("{} indicators updated", totalUpdates);
  }

  /**
   * Update each Expression expression value, replacing any source ref with the target ref.
   *
   * @param sources to be replaced
   * @param target to replace source refs
   */
  public void handleExpressions(List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    log.info("Merging source expressions");
    int totalUpdates = 0;
    for (CategoryOptionCombo source : sources) {
      totalUpdates += expressionStore.updateExpressionContaining(source.getUid(), target.getUid());
    }

    log.info("{} expressions updated", totalUpdates);
  }
}
