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
package org.hisp.dhis.merge;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueStore;
import org.springframework.stereotype.Component;

/**
 * Common Merge handler for data entities. The merge operations here are shared by multiple merge
 * use cases (e.g. CategoryOptionCombo & DataElement merge), hence the need for common handlers, to
 * reuse code and avoid duplication. This keeps merges consistent, for better or for worse.
 *
 * @author david mackessy
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonDataMergeHandler {

  private final DataValueStore dataValueStore;
  private final EntityManager entityManager;

  public <T extends BaseIdentifiableObject> void handleDataValues(
      @Nonnull DataValueMergeParams<T> merge) {
    log.info(merge.targetDataValues.size() + " target data values retrieved");

    // merge based on chosen strategy
    DataMergeStrategy dataMergeStrategy = merge.mergeRequest.getDataMergeStrategy();
    if (dataMergeStrategy == DataMergeStrategy.DISCARD) {
      log.info(dataMergeStrategy + " dataMergeStrategy being used, deleting source data values");
      merge.sources.forEach(merge.dvStoreDelete);
    } else if (dataMergeStrategy == DataMergeStrategy.LAST_UPDATED) {
      log.info(dataMergeStrategy + " dataMergeStrategy being used");
      Map<Boolean, List<DataValue>> sourceDuplicateList =
          merge.sourceDataValues.stream()
              .collect(
                  Collectors.partitioningBy(
                      dv -> merge.dvDuplicates.test(dv, merge.targetDataValues)));

      if (!sourceDuplicateList.get(false).isEmpty())
        handleNonDuplicates(sourceDuplicateList.get(false), merge);
      if (!sourceDuplicateList.get(true).isEmpty())
        handleDuplicates(sourceDuplicateList.get(true), merge);
    }
  }

  /**
   * Method to handle merging duplicate {@link DataValue}s. There may be multiple potential {@link
   * DataValue} duplicates. The {@link DataValue} with the latest `lastUpdated` value is filtered
   * out, the rest are then deleted at the end of the process (We can only have one of these entries
   * due to the composite key constraint). The filtered-out {@link DataValue} will be compared with
   * the target {@link DataValue} lastUpdated date.
   *
   * <p>If the target date is later, no action is required.
   *
   * <p>If the source date is later, then A new {@link DataValue} will be created from the old
   * {@link DataValue} values and it will use the target {@link DataElement} ref. This new {@link
   * DataValue} will be saved to the database. This sequence is required as a {@link DataValue} has
   * a composite primary key. This prohibits updating the ref in a source {@link DataValue}. The
   * matching target {@link DataValue}s will then be deleted.
   *
   * @param sourceDataValueDuplicates {@link DataValue}s to merge
   * @param dvMergeParams {@link DataValueMergeParams}
   */
  private <T extends BaseIdentifiableObject> void handleDuplicates(
      @Nonnull Collection<DataValue> sourceDataValueDuplicates,
      @Nonnull DataValueMergeParams<T> dvMergeParams) {
    log.info(
        "Handling "
            + sourceDataValueDuplicates.size()
            + " duplicate data values, keeping later lastUpdated value");

    // group Data values by key, so we can deal with each duplicate correctly
    Map<String, List<DataValue>> sourceDataValuesGroupedByKey =
        sourceDataValueDuplicates.stream()
            .collect(Collectors.groupingBy(dvMergeParams.dataValueKey));

    // filter groups down to single DV with latest date
    List<DataValue> filtered =
        sourceDataValuesGroupedByKey.values().stream()
            .map(ls -> Collections.max(ls, Comparator.comparing(DataValue::getLastUpdated)))
            .toList();

    for (DataValue source : filtered) {
      DataValue matchingTargetDataValue =
          dvMergeParams.targetDataValues.get(dvMergeParams.dataValueKey.apply(source));

      if (matchingTargetDataValue.getLastUpdated().before(source.getLastUpdated())) {
        dataValueStore.deleteDataValue(matchingTargetDataValue);

        // detaching is required here as it's not possible to add a new DataValue with essentially
        // the same composite primary key - Throws `NonUniqueObjectException: A different object
        // with the same identifier value was already associated with the session`
        entityManager.detach(matchingTargetDataValue);
        DataValue copyWithNewRef = dvMergeParams.newDataValue.apply(source, dvMergeParams.target);
        dataValueStore.addDataValue(copyWithNewRef);
      }
    }

    // delete the rest of the source data values after handling the last update duplicate
    dvMergeParams.sources.forEach(dvMergeParams.dvStoreDelete);
  }

  /**
   * Method to handle merging non-duplicate {@link DataValue}s. A new {@link DataValue} will be
   * created from the old {@link DataValue} values, and it will use the target {@link
   * CategoryOptionCombo} ref. This new {@link DataValue} will be saved to the database. This
   * sequence is required as a {@link DataValue} has a composite primary key. This prohibits
   * updating the ref in a source {@link DataValue}.
   *
   * <p>All source {@link DataValue}s will then be deleted.
   *
   * @param dataValues {@link DataValue}s to merge
   * @param dvMergeParams {@link DataValueMergeParams}
   */
  private <T extends BaseIdentifiableObject> void handleNonDuplicates(
      @Nonnull List<DataValue> dataValues, @Nonnull DataValueMergeParams<T> dvMergeParams) {
    log.info(
        "Handling "
            + dataValues.size()
            + " non duplicate data values. Add new DataValue entry (using target ref) and delete old source entry");

    dataValues.forEach(
        sourceDataValue -> {
          DataValue copyWithNewCocRef =
              dvMergeParams.newDataValue.apply(sourceDataValue, dvMergeParams.target);
          dataValueStore.addDataValue(copyWithNewCocRef);
        });

    log.info("Deleting all data values referencing sources");
    dvMergeParams.sources.forEach(dvMergeParams.dvStoreDelete);
  }

  public record DataValueMergeParams<T extends BaseIdentifiableObject>(
      @Nonnull MergeRequest mergeRequest,
      @Nonnull List<T> sources,
      @Nonnull T target,
      @Nonnull List<DataValue> sourceDataValues,
      @Nonnull Map<String, DataValue> targetDataValues,
      @Nonnull Consumer<T> dvStoreDelete,
      @Nonnull BiPredicate<DataValue, Map<String, DataValue>> dvDuplicates,
      @Nonnull BiFunction<DataValue, BaseIdentifiableObject, DataValue> newDataValue,
      @Nonnull Function<DataValue, String> dataValueKey) {}
}
