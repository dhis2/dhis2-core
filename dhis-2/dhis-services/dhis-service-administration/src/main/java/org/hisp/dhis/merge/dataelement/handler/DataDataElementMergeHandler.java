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
package org.hisp.dhis.merge.dataelement.handler;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.merge.DataMergeStrategy;
import org.hisp.dhis.merge.MergeRequest;
import org.springframework.stereotype.Component;

/**
 * Merge handler for data
 *
 * @author david mackessy
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataDataElementMergeHandler {

  private final DataValueStore dataValueStore;
  private final DataValueAuditStore dataValueAuditStore;
  private final EntityManager entityManager;

  /**
   * Method retrieving {@link DataValue}s by source {@link DataElement} references. All retrieved
   * {@link DataValue}s will either be discarded or try to be merged based on the last updated
   * value, based on the chosen {@link DataMergeStrategy}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link DataValue}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for a {@link
   *     DataValue}
   */
  public void handleDataValueDataElement(
      @Nonnull List<DataElement> sources,
      @Nonnull DataElement target,
      @Nonnull MergeRequest mergeRequest) {
    // get DVs from sources
    List<DataValue> sourceDataValues = dataValueStore.getAllDataValuesByDataElement(sources);
    log.info(sourceDataValues.size() + " source data values retrieved");

    // get map of target data values, using the duplicate key constraints as the key
    Map<String, DataValue> targetDataValues =
        dataValueStore.getAllDataValuesByDataElement(List.of(target)).stream()
            .collect(Collectors.toMap(DataDataElementMergeHandler::getDataValueKey, dv -> dv));
    log.info(targetDataValues.size() + " target data values retrieved");

    // merge based on chosen strategy
    DataMergeStrategy dataMergeStrategy = mergeRequest.getDataMergeStrategy();
    if (dataMergeStrategy == DataMergeStrategy.DISCARD) {
      log.info(dataMergeStrategy + " dataMergeStrategy being used, deleting source data values");
      sources.forEach(dataValueStore::deleteDataValues);
    } else if (dataMergeStrategy == DataMergeStrategy.LAST_UPDATED) {
      log.info(dataMergeStrategy + " dataMergeStrategy being used");
      Map<Boolean, List<DataValue>> sourceDuplicateList =
          sourceDataValues.stream()
              .collect(
                  Collectors.partitioningBy(dv -> dataValueDuplicates.test(dv, targetDataValues)));

      if (!sourceDuplicateList.get(false).isEmpty())
        handleNonDuplicates(sourceDuplicateList.get(false), sources, target);
      if (!sourceDuplicateList.get(true).isEmpty())
        handleDuplicates(sourceDuplicateList.get(true), targetDataValues, sources, target);
    }
  }

  /**
   * Method retrieving {@link DataValueAudit}s by source {@link DataElement} references. All
   * retrieved {@link DataValueAudit}s will either be deleted or left as is, based on whether the
   * source {@link DataElement}s are being deleted or not.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link DataValueAudit}s
   */
  public void handleDataValueAuditDataElement(
      @Nonnull List<DataElement> sources, @Nonnull MergeRequest mergeRequest) {
    if (mergeRequest.isDeleteSources()) {
      log.info("Deleting source data value audit records as source DataElements are being deleted");
      sources.forEach(dataValueAuditStore::deleteDataValueAudits);
    } else {
      log.info(
          "Leaving source data value audit records as is, source DataElements are not being deleted");
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
   * a composite primary key which includes {@link DataElement}. This prohibits updating the {@link
   * DataElement} ref in a source {@link DataValue}. The matching target {@link DataValue}s will
   * then be deleted.
   *
   * @param sourceDataValuesDuplicates {@link DataValue}s to merge
   * @param targetDataValueMap target {@link DataValue}s
   * @param sources source {@link DataElement}s
   * @param target target {@link DataElement}
   */
  private void handleDuplicates(
      @Nonnull Collection<DataValue> sourceDataValuesDuplicates,
      @Nonnull Map<String, DataValue> targetDataValueMap,
      @Nonnull List<DataElement> sources,
      @Nonnull DataElement target) {
    log.info(
        "Handling "
            + sourceDataValuesDuplicates.size()
            + " duplicate data values, keeping later lastUpdated value");

    // group Data values by key so we can deal with each duplicate correctly
    Map<String, List<DataValue>> sourceDataValuesGroupedByKey =
        sourceDataValuesDuplicates.stream()
            .collect(Collectors.groupingBy(DataDataElementMergeHandler::getDataValueKey));

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
        DataValue copyWithNewDataElementRef = DataValue.dataValueWithNewDataElement(source, target);
        dataValueStore.addDataValue(copyWithNewDataElementRef);
      }
    }

    // delete the rest of the source data values after handling the last update duplicate
    sources.forEach(dataValueStore::deleteDataValues);
  }

  /**
   * Method to handle merging non-duplicate {@link DataValue}s. A new {@link DataValue} will be
   * created from the old {@link DataValue} values, and it will use the target {@link DataElement}
   * ref. This new {@link DataValue} will be saved to the database. This sequence is required as a
   * {@link DataValue} has a composite primary key which includes {@link DataElement}. This
   * prohibits updating the {@link DataElement} ref in a source {@link DataValue}.
   *
   * <p>All source {@link DataValue}s will then be deleted.
   *
   * @param dataValues {@link DataValue}s to merge
   * @param sources source {@link DataElement}s
   * @param target target {@link DataElement}
   */
  private void handleNonDuplicates(
      @Nonnull List<DataValue> dataValues,
      @Nonnull List<DataElement> sources,
      @Nonnull DataElement target) {
    log.info(
        "Handling "
            + dataValues.size()
            + " non duplicate data values. Add new DataValue entry (using target DataElement ref) and delete old source entry");

    dataValues.forEach(
        sourceDataValue -> {
          DataValue copyWithNewDataElementRef =
              DataValue.dataValueWithNewDataElement(sourceDataValue, target);

          dataValueStore.addDataValue(copyWithNewDataElementRef);
        });

    log.info("Deleting all data values referencing source data elements");
    sources.forEach(dataValueStore::deleteDataValues);
  }

  public static final BiPredicate<DataValue, Map<String, DataValue>> dataValueDuplicates =
      (sourceDv, targetDvs) -> targetDvs.containsKey(getDataValueKey(sourceDv));

  private static String getDataValueKey(DataValue dv) {
    return String.valueOf(dv.getPeriod().getId())
        + dv.getSource().getId()
        + dv.getCategoryOptionCombo().getId()
        + dv.getAttributeOptionCombo().getId();
  }
}
