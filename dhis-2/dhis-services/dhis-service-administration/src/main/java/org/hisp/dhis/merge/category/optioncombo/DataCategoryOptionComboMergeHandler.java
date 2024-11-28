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

import static org.hisp.dhis.datavalue.DataValue.dataValueWithNewCatOptionCombo;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.merge.DataMergeStrategy;
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
  private final DataValueAuditStore dataValueAuditStore;
  private final EntityManager entityManager;

  /** */
  public void handleDataValues(
      List<CategoryOptionCombo> sources, CategoryOptionCombo target, MergeRequest mergeRequest) {
    // category option combos
    List<DataValue> sourceCocDataValues =
        dataValueStore.getAllDataValuesByCatOptCombo(
            UID.of(sources.stream().map(BaseIdentifiableObject::getUid).toList()));

    // get map of target data values, using the duplicate key constraints as the key
    Map<String, DataValue> targetDataValues =
        dataValueStore.getAllDataValuesByCatOptCombo(UID.of(List.of(target.getUid()))).stream()
            .collect(
                Collectors.toMap(DataCategoryOptionComboMergeHandler::getDataValueKey, dv -> dv));
    log.info(targetDataValues.size() + " target data values retrieved");

    genericcode(
        mergeRequest,
        sources,
        target,
        sourceCocDataValues,
        targetDataValues,
        dataValueStore::deleteDataValues,
        dataValueDuplicates);

    //    // merge based on chosen strategy
    //    DataMergeStrategy dataMergeStrategy = mergeRequest.getDataMergeStrategy();
    //    if (dataMergeStrategy == DataMergeStrategy.DISCARD) {
    //      log.info(dataMergeStrategy + " dataMergeStrategy being used, deleting source data
    // values");
    //      sources.forEach(dataValueStore::deleteDataValues);
    //    } else if (dataMergeStrategy == DataMergeStrategy.LAST_UPDATED) {
    //      log.info(dataMergeStrategy + " dataMergeStrategy being used");
    //      Map<Boolean, List<DataValue>> sourceDuplicateList =
    //          sourceCocDataValues.stream()
    //              .collect(
    //                  Collectors.partitioningBy(dv -> dataValueDuplicates.test(dv,
    // targetDataValues)));
    //
    //      if (!sourceDuplicateList.get(false).isEmpty())
    //        handleNonDuplicates(
    //            sourceDuplicateList.get(false), sources, target, dataValueWithNewCatOptionCombo);
    //      if (!sourceDuplicateList.get(true).isEmpty())
    //        handleDuplicates(sourceDuplicateList.get(true), targetDataValues, sources, target);
    //    }

    // attribute option combo
    //    List<DataValue> aocDataValues =
    //        dataValueStore.getAllDataValuesByAttrOptCombo(
    //            UID.of(sources.stream().map(BaseIdentifiableObject::getUid).toList()));
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

  public <T extends BaseIdentifiableObject> void genericcode(
      MergeRequest mergeRequest,
      List<T> sources,
      T target,
      List<DataValue> sourceDataValues,
      Map<String, DataValue> targetDataValues,
      Consumer<T> dvStoreDelete,
      BiPredicate<DataValue, Map<String, DataValue>> dvDuplicates) {
    log.info(targetDataValues.size() + " target data values retrieved");

    // merge based on chosen strategy
    DataMergeStrategy dataMergeStrategy = mergeRequest.getDataMergeStrategy();
    if (dataMergeStrategy == DataMergeStrategy.DISCARD) {
      log.info(dataMergeStrategy + " dataMergeStrategy being used, deleting source data values");
      sources.forEach(dvStoreDelete);
    } else if (dataMergeStrategy == DataMergeStrategy.LAST_UPDATED) {
      log.info(dataMergeStrategy + " dataMergeStrategy being used");
      Map<Boolean, List<DataValue>> sourceDuplicateList =
          sourceDataValues.stream()
              .collect(Collectors.partitioningBy(dv -> dvDuplicates.test(dv, targetDataValues)));

      if (!sourceDuplicateList.get(false).isEmpty())
        handleNonDuplicates(
            sourceDuplicateList.get(false),
            sources,
            target,
            dataValueWithNewCatOptionCombo,
            dvStoreDelete);
      if (!sourceDuplicateList.get(true).isEmpty())
        handleDuplicates(
            sourceDuplicateList.get(true), targetDataValues, sources, target, dvStoreDelete);
    }
  }

  private <T extends BaseIdentifiableObject> void handleDuplicates(
      @Nonnull Collection<DataValue> sourceDataValuesDuplicates,
      @Nonnull Map<String, DataValue> targetDataValueMap,
      @Nonnull List<T> sources,
      @Nonnull T target,
      Consumer<T> dvStoreDelete) {
    log.info(
        "Handling "
            + sourceDataValuesDuplicates.size()
            + " duplicate data values, keeping later lastUpdated value");

    // group Data values by key, so we can deal with each duplicate correctly
    Map<String, List<DataValue>> sourceDataValuesGroupedByKey =
        sourceDataValuesDuplicates.stream()
            .collect(Collectors.groupingBy(DataCategoryOptionComboMergeHandler::getDataValueKey));

    // filter groups down to single DV with latest date
    List<DataValue> filtered =
        sourceDataValuesGroupedByKey.values().stream()
            .map(ls -> Collections.max(ls, Comparator.comparing(DataValue::getLastUpdated)))
            .toList();

    for (DataValue source : filtered) {
      DataValue matchingTargetDataValue = targetDataValueMap.get(getDataValueKey(source));

      if (matchingTargetDataValue.getLastUpdated().before(source.getLastUpdated())) {
        dataValueStore.deleteDataValue(matchingTargetDataValue);

        // detaching is required here as it's not possible to add a new DataValue with essentially
        // the same composite primary key - Throws `NonUniqueObjectException: A different object
        // with the same identifier value was already associated with the session`
        entityManager.detach(matchingTargetDataValue);
        DataValue copyWithNewDataElementRef = dataValueWithNewCatOptionCombo.apply(source, target);
        dataValueStore.addDataValue(copyWithNewDataElementRef);
      }
    }

    // delete the rest of the source data values after handling the last update duplicate
    sources.forEach(dvStoreDelete);
  }

  /**
   * Method to handle merging non-duplicate {@link DataValue}s. A new {@link DataValue} will be
   * created from the old {@link DataValue} values, and it will use the target {@link
   * CategoryOptionCombo} ref. This new {@link DataValue} will be saved to the database. This
   * sequence is required as a {@link DataValue} has a composite primary key which includes {@link
   * CategoryOptionCombo}. This prohibits updating the {@link CategoryOptionCombo} ref in a source
   * {@link DataValue}.
   *
   * <p>All source {@link DataValue}s will then be deleted.
   *
   * @param dataValues {@link DataValue}s to merge
   * @param sources source {@link CategoryOptionCombo}s
   * @param target target {@link CategoryOptionCombo}
   */
  private <T extends BaseIdentifiableObject> void handleNonDuplicates(
      @Nonnull List<DataValue> dataValues,
      @Nonnull List<T> sources,
      @Nonnull T target,
      BiFunction<DataValue, BaseIdentifiableObject, DataValue> newDataValue,
      Consumer<T> dvStoreDelete) {
    log.info(
        "Handling "
            + dataValues.size()
            + " non duplicate data values. Add new DataValue entry (using target CategoryOptionCombo ref) and delete old source entry");

    dataValues.forEach(
        sourceDataValue -> {
          DataValue copyWithNewCocRef = newDataValue.apply(sourceDataValue, target);
          dataValueStore.addDataValue(copyWithNewCocRef);
        });

    log.info("Deleting all data values referencing source CategoryOptionCombos");
    sources.forEach(dvStoreDelete);
  }

  public static final BiPredicate<DataValue, Map<String, DataValue>> dataValueDuplicates =
      (sourceDv, targetDvs) -> targetDvs.containsKey(getDataValueKey(sourceDv));

  private static String getDataValueKey(DataValue dv) {
    return String.valueOf(dv.getPeriod().getId())
        + dv.getSource().getId()
        + dv.getDataElement().getId()
        + dv.getAttributeOptionCombo().getId();
  }
}
