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
package org.hisp.dhis.dataanalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.dataanalysis.StdDevOutlierAnalysisService")
public class StdDevOutlierAnalysisService implements DataAnalysisService {
  private final DataAnalysisStore dataAnalysisStore;

  // -------------------------------------------------------------------------
  // DataAnalysisService implementation
  // -------------------------------------------------------------------------

  @Override
  public final List<DeflatedDataValue> analyse(
      Collection<OrganisationUnit> parents,
      Collection<DataElement> dataElements,
      Collection<Period> periods,
      Double stdDevFactor,
      Date from) {
    log.info(
        "Starting std dev analysis, no of org units: "
            + parents.size()
            + ", factor: "
            + stdDevFactor
            + ", from: "
            + from);

    List<DeflatedDataValue> outlierCollection = new ArrayList<>();

    List<String> parentsPaths =
        parents.stream().map(OrganisationUnit::getPath).collect(Collectors.toList());

    loop:
    for (DataElement dataElement : dataElements) {
      // TODO filter periods with data element period type

      if (dataElement.getValueType().isNumeric() && stdDevFactor != null) {
        Set<CategoryOptionCombo> categoryOptionCombos = dataElement.getCategoryOptionCombos();

        List<DataAnalysisMeasures> measuresList =
            dataAnalysisStore.getDataAnalysisMeasures(
                dataElement, categoryOptionCombos, parentsPaths, from);

        MapMap<Long, Long, Integer> lowBoundMapMap = new MapMap<>(); // catOptionComboId,
        // orgUnitId,
        // lowBound
        MapMap<Long, Long, Integer> highBoundMapMap = new MapMap<>(); // catOptionComboId,
        // orgUnitId,
        // highBound

        for (DataAnalysisMeasures measures : measuresList) {
          int lowBound =
              (int)
                  Math.round(
                      MathUtils.getLowBound(
                          measures.getStandardDeviation(), stdDevFactor, measures.getAverage()));
          int highBound =
              (int)
                  Math.round(
                      MathUtils.getHighBound(
                          measures.getStandardDeviation(), stdDevFactor, measures.getAverage()));

          lowBoundMapMap.putEntry(
              measures.getCategoryOptionComboId(), measures.getOrgUnitId(), lowBound);
          highBoundMapMap.putEntry(
              measures.getCategoryOptionComboId(), measures.getOrgUnitId(), highBound);
        }

        for (CategoryOptionCombo categoryOptionCombo : categoryOptionCombos) {
          Map<Long, Integer> lowBoundMap = lowBoundMapMap.get(categoryOptionCombo.getId());
          Map<Long, Integer> highBoundMap = highBoundMapMap.get(categoryOptionCombo.getId());

          outlierCollection.addAll(
              dataAnalysisStore.getDeflatedDataValues(
                  dataElement, categoryOptionCombo, periods, lowBoundMap, highBoundMap));

          if (outlierCollection.size() > MAX_OUTLIERS) {
            break loop;
          }
        }
      }
    }

    return outlierCollection;
  }
}
