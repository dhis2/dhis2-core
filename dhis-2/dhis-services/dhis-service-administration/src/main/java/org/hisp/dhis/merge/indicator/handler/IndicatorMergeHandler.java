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
package org.hisp.dhis.merge.indicator.handler;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.SectionStore;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.springframework.stereotype.Component;

/**
 * Merge handler for metadata entities.
 *
 * @author david mackessy
 */
@Component
@RequiredArgsConstructor
public class IndicatorMergeHandler {

  private final SectionStore sectionStore;
  private final DimensionService dimensionService;
  private final VisualizationService visualizationService;

  /**
   * Remove sources from {@link DataSet} and add target to {@link DataSet}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleDataSets(List<Indicator> sources, Indicator target) {
    Set<DataSet> dataSets =
        sources.stream()
            .map(Indicator::getDataSets)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    dataSets.forEach(
        ds -> {
          ds.addIndicator(target);
          ds.removeIndicators(sources);
        });
  }

  /**
   * Remove sources from {@link IndicatorGroup} and add target to {@link IndicatorGroup}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleIndicatorGroups(List<Indicator> sources, Indicator target) {
    Set<IndicatorGroup> indicatorGroups =
        sources.stream()
            .map(Indicator::getGroups)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    indicatorGroups.forEach(
        ig -> {
          ig.addIndicator(target);
          ig.removeIndicators(sources);
        });
  }

  /**
   * Replace any links to source {@link Indicator}s with the target {@link Indicator}
   *
   * @param sources links to be removed
   * @param target to be linked
   */
  public void handleDataDimensionItems(List<Indicator> sources, Indicator target) {
    List<DataDimensionItem> dataDimensionItems =
        dimensionService.getIndicatorDataDimensionItems(sources);
    dataDimensionItems.forEach(item -> item.setIndicator(target));
  }

  /**
   * Replace all {@link Indicator} refs in the {@link org.hisp.dhis.analytics.Sorting} 'dimension'
   * field
   *
   * @param sources to be replaced
   * @param target to replace each source
   */
  public void handleVisualizations(List<Indicator> sources, Indicator target) {
    List<String> sourceUids = IdentifiableObjectUtils.getUids(sources);
    List<Visualization> visualizations =
        visualizationService.getVisualizationsWithIndicatorSorting(sourceUids);

    visualizations.stream()
        .map(Visualization::getSorting)
        .flatMap(Collection::stream)
        .forEach(
            sorting ->
                sources.forEach(
                    source ->
                        sorting.setDimension(
                            sorting.getDimension().replace(source.getUid(), target.getUid()))));
  }

  /**
   * Remove sources from {@link Section} and add target to {@link Section}
   *
   * @param sources to be removed
   * @param target to add
   */
  public void handleSections(List<Indicator> sources, Indicator target) {
    List<Section> sections = sectionStore.getSectionsByIndicators(sources);
    sections.forEach(
        s -> {
          s.removeIndicators(sources);
          s.addIndicator(target);
        });
  }
}
