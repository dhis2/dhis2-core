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

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.merge.DataMergeStrategy;
import org.springframework.stereotype.Service;

/**
 * Merge handler for data
 *
 * @author david mackessy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataDataElementMergeHandler {

  private final DataValueStore dataValueStore;

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
      List<DataElement> sources, DataElement target, DataMergeStrategy dataMergeStrategy) {
    // get DVs from sources
    List<DataValue> sourceDataValues = dataValueStore.getAllDataValuesByDataElement(sources);
    log.info(sourceDataValues.size() + " source data values retrieved");

    // get DVs from target
    List<DataValue> targetDataValues =
        dataValueStore.getAllDataValuesByDataElement(List.of(target));
    log.info(targetDataValues.size() + " target data values retrieved");

    // check merge strategy
    switch (dataMergeStrategy) {
      case DISCARD -> {
        log.info(dataMergeStrategy + " dataMergeStrategy being used, deleting source data values");
        sources.forEach(dataValueStore::deleteDataValues);
      }
      case LAST_UPDATED -> {
        log.info(dataMergeStrategy + " dataMergeStrategy being used");
        Map<Boolean, List<DataValue>> sourceDuplicateList =
            sourceDataValues.stream()
                .collect(
                    Collectors.partitioningBy(
                        dv -> dataValueDuplicates.test(dv, targetDataValues)));

        handleNonDuplicates(sourceDuplicateList.get(false), sources, target);
        handleDuplicates(sourceDuplicateList.get(true), targetDataValues, target);
      }
    }
  }

  /**
   * Method to handle merging duplicate {@link DataValue}s. It will first compare the source {@link
   * DataValue} lastUpdated date with the target {@link DataValue} lastUpdated date.
   *
   * <p>If the target date is later, the source {@link DataValue} is deleted.
   *
   * <p>If the source date is later, then A new {@link DataValue} will be created from the old
   * {@link DataValue} values and it will use the target {@link DataElement} ref. This new {@link
   * DataValue} will be saved to the database. This sequence is required as a {@link DataValue} has
   * a composite primary key which includes {@link DataElement}. This prohibits updating the {@link
   * DataElement} ref in a source {@link DataValue}. The matching target {@link DataValue}s will
   * then be deleted.
   *
   * @param dataValues {@link DataValue}s to merge
   * @param targetDataValues target {@link DataValue}s
   * @param target target {@link DataElement}
   */
  private void handleDuplicates(
      List<DataValue> dataValues, List<DataValue> targetDataValues, DataElement target) {
    log.info(
        "Updating "
            + dataValues.size()
            + " duplicate data values, keeping later lastUpdated value");
    dataValues.forEach(
        sourceDataValue -> {
          DataValue matchingTargetDataValue =
              matchingDataValueUniqueKey.apply(targetDataValues, sourceDataValue);

          if (matchingTargetDataValue.getLastUpdated().after(sourceDataValue.getLastUpdated())) {
            log.info("target duplicate has later lastUpdated date, delete source data value");
            dataValueStore.deleteDataValue(sourceDataValue);
          } else {
            log.info(
                "target duplicate has earlier lastUpdated date, assign source data value with target and delete target data value");
            DataValue copyWithNewDataElementRef =
                DataValue.dataValueWithNewDataElement(sourceDataValue, target);

            dataValueStore.addDataValue(copyWithNewDataElementRef);
            dataValueStore.deleteDataValue(matchingTargetDataValue);
          }
        });
  }

  /**
   * Method to handle merging non-duplicate {@link DataValue}s. A new {@link DataValue} will be
   * created from the old {@link DataValue} values and it will use the target {@link DataElement}
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
      List<DataValue> dataValues, List<DataElement> sources, DataElement target) {
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

  public static final BiPredicate<DataValue, DataValue> matchingUniqueKey =
      (dv1, dv2) ->
          ((dv1.getPeriod().getId() == dv2.getPeriod().getId())
              && (dv1.getSource().getId() == dv2.getSource().getId())
              && (dv1.getCategoryOptionCombo().getId() == dv2.getCategoryOptionCombo().getId())
              && (dv1.getAttributeOptionCombo().getId() == dv2.getAttributeOptionCombo().getId()));

  public static final BiFunction<List<DataValue>, DataValue, DataValue> matchingDataValueUniqueKey =
      (targetDataValues, sourceDataValue) ->
          targetDataValues.stream()
              .filter(dv -> matchingUniqueKey.test(dv, sourceDataValue))
              .findFirst()
              .orElseThrow();

  public static final BiPredicate<DataValue, List<DataValue>> dataValueDuplicates =
      (sourceDv, targetDvs) ->
          targetDvs.stream().anyMatch(tdv -> matchingUniqueKey.test(sourceDv, tdv));
}
