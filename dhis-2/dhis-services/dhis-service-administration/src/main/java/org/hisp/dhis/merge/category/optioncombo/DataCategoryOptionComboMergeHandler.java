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

import static org.hisp.dhis.datavalue.DataValue.dataValueWithNewAttrOptionCombo;
import static org.hisp.dhis.datavalue.DataValue.dataValueWithNewCatOptionCombo;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.merge.CommonDataMergeHandler;
import org.hisp.dhis.merge.CommonDataMergeHandler.DataValueMergeParams;
import org.hisp.dhis.merge.MergeRequest;
import org.springframework.stereotype.Component;

/**
 * Merge handler for data types.
 *
 * @author david mackessy
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCategoryOptionComboMergeHandler {

  private final DataValueStore dataValueStore;
  //  private final DataValueAuditStore dataValueAuditStore;
  private final CommonDataMergeHandler commonDataMergeHandler;

  public void handleDataValues(
      @Nonnull List<CategoryOptionCombo> sources,
      @Nonnull CategoryOptionCombo target,
      @Nonnull MergeRequest mergeRequest) {
    // ----------------------
    // category option combos
    // ----------------------
    List<DataValue> sourceCocDataValues =
        dataValueStore.getAllDataValuesByCatOptCombo(
            UID.of(sources.stream().map(BaseIdentifiableObject::getUid).toList()));
    log.info(
        "{} data values retrieved for source categoryOptionCombos", sourceCocDataValues.size());

    // get map of target data values, using the duplicate key constraints as the key
    Map<String, DataValue> targetCocDataValues =
        dataValueStore.getAllDataValuesByCatOptCombo(UID.of(List.of(target.getUid()))).stream()
            .collect(Collectors.toMap(getCocDataValueKey, dv -> dv));
    log.info("{} data values retrieved for target categoryOptionCombo", targetCocDataValues.size());

    commonDataMergeHandler.handleDataValues(
        new DataValueMergeParams<>(
            mergeRequest,
            sources,
            target,
            sourceCocDataValues,
            targetCocDataValues,
            dataValueStore::deleteDataValuesByCategoryOptionCombo,
            cocDataValueDuplicates,
            dataValueWithNewCatOptionCombo,
            getCocDataValueKey));

    // ----------------------
    // attribute option combos
    // ----------------------
    List<DataValue> sourceAocDataValues =
        dataValueStore.getAllDataValuesByAttrOptCombo(
            UID.of(sources.stream().map(BaseIdentifiableObject::getUid).toList()));
    log.info(
        "{} data values retrieved for source attributeOptionCombos", sourceAocDataValues.size());

    // get map of target data values, using the duplicate key constraints as the key
    Map<String, DataValue> targetAocDataValues =
        dataValueStore.getAllDataValuesByAttrOptCombo(UID.of(List.of(target.getUid()))).stream()
            .collect(Collectors.toMap(getAocDataValueKey, dv -> dv));
    log.info(
        "{} data values retrieved for target attributeOptionCombo", targetAocDataValues.size());

    commonDataMergeHandler.handleDataValues(
        new DataValueMergeParams<>(
            mergeRequest,
            sources,
            target,
            sourceAocDataValues,
            targetAocDataValues,
            dataValueStore::deleteDataValuesByAttributeOptionCombo,
            aocDataValueDuplicates,
            dataValueWithNewAttrOptionCombo,
            getAocDataValueKey));
  }

  /** */
  public void handleDataValueAudits(List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    // TODO x 2
  }

  /** */
  public void handleDataApprovals(List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    // TODO
  }

  /** */
  public void handleDataApprovalAudits(
      List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    // TODO
  }

  /** */
  public void handleEvents(List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    // TODO
  }

  /** */
  public void handleCompleteDataSetRegistrations(
      List<CategoryOptionCombo> sources, CategoryOptionCombo target) {
    // TODO
  }

  private static final Function<DataValue, String> getCocDataValueKey =
      dv ->
          String.valueOf(dv.getPeriod().getId())
              + dv.getSource().getId()
              + dv.getDataElement().getId()
              + dv.getAttributeOptionCombo().getId();

  private static final BiPredicate<DataValue, Map<String, DataValue>> cocDataValueDuplicates =
      (sourceDv, targetDvs) -> targetDvs.containsKey(getCocDataValueKey.apply(sourceDv));

  private static final Function<DataValue, String> getAocDataValueKey =
      dv ->
          String.valueOf(dv.getPeriod().getId())
              + dv.getSource().getId()
              + dv.getDataElement().getId()
              + dv.getCategoryOptionCombo().getId();

  private static final BiPredicate<DataValue, Map<String, DataValue>> aocDataValueDuplicates =
      (sourceDv, targetDvs) -> targetDvs.containsKey(getAocDataValueKey.apply(sourceDv));
}
