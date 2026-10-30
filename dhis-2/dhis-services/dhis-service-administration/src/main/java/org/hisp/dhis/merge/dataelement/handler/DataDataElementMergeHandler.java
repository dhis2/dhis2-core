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
package org.hisp.dhis.merge.dataelement.handler;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueChangelog;
import org.hisp.dhis.datavalue.DataValueChangelogStore;
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
  private final DataValueChangelogStore dataValueChangelogStore;

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
    if (DataMergeStrategy.DISCARD == mergeRequest.getDataMergeStrategy()) {
      log.info(
          mergeRequest.getDataMergeStrategy()
              + " dataMergeStrategy being used, deleting source data values");
      dataValueStore.deleteDataValues(sources);
    } else {
      dataValueStore.mergeDataValuesWithDataElements(
          target.getId(), IdentifiableObjectUtils.getIdentifiersSet(sources));
    }
  }

  /**
   * Method retrieving {@link DataValueChangelog}s by source {@link DataElement} references. All
   * retrieved {@link DataValueChangelog}s will either be deleted or left as is, based on whether
   * the source {@link DataElement}s are being deleted or not.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link DataValueChangelog}s
   */
  public void handleDataValueAuditDataElement(
      @Nonnull List<DataElement> sources, @Nonnull MergeRequest mergeRequest) {
    if (mergeRequest.isDeleteSources()) {
      log.info("Deleting source data value audit records as source DataElements are being deleted");
      sources.forEach(de -> dataValueChangelogStore.deleteByDataElement(UID.of(de)));
    } else {
      log.info(
          "Leaving source data value audit records as is, source DataElements are not being deleted");
    }
  }

  private static final Function<DataValue, String> getDataValueKey =
      dv ->
          String.valueOf(dv.getPeriod().getId())
              + dv.getSource().getId()
              + dv.getCategoryOptionCombo().getId()
              + dv.getAttributeOptionCombo().getId();

  public static final BiPredicate<DataValue, Map<String, DataValue>> dataValueDuplicates =
      (sourceDv, targetDvs) -> targetDvs.containsKey(getDataValueKey.apply(sourceDv));
}
