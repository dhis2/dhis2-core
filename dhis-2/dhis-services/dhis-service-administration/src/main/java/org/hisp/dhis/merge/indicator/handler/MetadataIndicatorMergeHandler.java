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
package org.hisp.dhis.merge.indicator.handler;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.SectionService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorService;
import org.springframework.stereotype.Service;

/**
 * Merge handler for metadata entities.
 *
 * @author david mackessy
 */
@Service
@RequiredArgsConstructor
public class MetadataIndicatorMergeHandler {

  private final SectionService sectionService;
  private final IndicatorService indicatorService;
  private final DataEntryFormService dataEntryFormService;
  private final DimensionService dimensionService;

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

  public void handleDataDimensionalItems(List<Indicator> sources, Indicator target) {
    List<DataDimensionItem> dataDimensionItems =
        dimensionService.getIndicatorDataDimensionItems(sources);
    dataDimensionItems.forEach(item -> item.setIndicator(target));
  }

  public void handleSections(List<Indicator> sources, Indicator target) {
    List<Section> sections = sectionService.getSectionsByIndicators(sources);
    sources.stream()
        .distinct()
        .forEach(
            i ->
                sections.forEach(
                    s -> {
                      s.getIndicators().remove(i);
                      s.getIndicators().add(target);
                    }));
  }

  public void handleIndicatorRefsInIndicator(List<Indicator> sources, Indicator target) {
    for (Indicator source : sources) {
      // numerators
      List<Indicator> numeratorIndicators =
          indicatorService.getIndicatorsWithNumeratorContaining(source.getUid());
      if (CollectionUtils.isNotEmpty(numeratorIndicators)) {
        for (Indicator foundIndicator : numeratorIndicators) {
          String existingNumerator = foundIndicator.getNumerator();
          foundIndicator.setNumerator(existingNumerator.replace(source.getUid(), target.getUid()));
        }
      }

      // denominators
      List<Indicator> denominatorIndicators =
          indicatorService.getIndicatorsWithDenominatorContaining(source.getUid());
      if (CollectionUtils.isNotEmpty(denominatorIndicators)) {
        for (Indicator foundIndicator : denominatorIndicators) {
          String existingDenominator = foundIndicator.getDenominator();
          foundIndicator.setDenominator(
              existingDenominator.replace(source.getUid(), target.getUid()));
        }
      }
    }
  }

  public void handleIndicatorRefsInCustomForms(List<Indicator> sources, Indicator target) {
    for (Indicator source : sources) {
      List<DataEntryForm> forms =
          dataEntryFormService.getDataEntryFormsWithHtmlContaining(source.getUid());

      if (CollectionUtils.isNotEmpty(forms)) {
        for (DataEntryForm dataEntryForm : forms) {
          String existingHtml = dataEntryForm.getHtmlCode();
          dataEntryForm.setHtmlCode(existingHtml.replace(source.getUid(), target.getUid()));
        }
      }
    }
  }
}
