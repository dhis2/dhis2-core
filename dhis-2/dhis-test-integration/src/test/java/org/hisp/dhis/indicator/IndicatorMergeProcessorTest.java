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
package org.hisp.dhis.indicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.Sorting;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeProcessor;
import org.hisp.dhis.merge.MergeType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author david mackessy
 */
class IndicatorMergeProcessorTest extends IntegrationTestBase {
  @Autowired private MergeProcessor indicatorMergeProcessor;

  @Autowired private IdentifiableObjectManager manager;
  @Autowired private ConfigurationService configService;

  @Test
  @DisplayName("merge with no sources")
  void mergeWithNoSourcesTest() {
    IndicatorType indType1 = createIndicatorType('a');
    manager.save(indType1);
    Indicator validTarget = createIndicator('a', indType1);
    manager.save(validTarget);

    // given merge params with no source indicators
    MergeParams params = new MergeParams();
    params.setSources(Set.of());
    params.setTarget(UID.of(validTarget.getUid()));

    // when a merge request is processed
    ConflictException conflictException =
        assertThrows(
            ConflictException.class,
            () -> indicatorMergeProcessor.processMerge(params, MergeType.INDICATOR));

    // then the merge report has the correct error info
    MergeReport mergeReport = conflictException.getMergeReport();
    List<String> list =
        mergeReport.getMergeErrors().stream().map(ErrorMessage::getMessage).toList();
    assertEquals(1, list.size());
    assertTrue(list.contains("At least one source indicator must be specified"));
  }

  @Test
  @DisplayName("merge with invalid target indicator")
  void mergeWithInvalidTargetTest() {
    IndicatorType indType1 = createIndicatorType('b');
    manager.save(indType1);
    Indicator validSource1 = createIndicator('b', indType1);
    manager.save(validSource1);

    // given merge params with an invalid target indicator
    MergeParams params = new MergeParams();
    params.setSources(Set.of(UID.of(validSource1.getUid())));
    params.setTarget(UID.of("Uid00000011"));

    // when a merge request is processed
    ConflictException conflictException =
        assertThrows(
            ConflictException.class,
            () -> indicatorMergeProcessor.processMerge(params, MergeType.INDICATOR));

    // then the merge report has the correct error info
    MergeReport mergeReport = conflictException.getMergeReport();
    List<String> list =
        mergeReport.getMergeErrors().stream().map(ErrorMessage::getMessage).toList();
    assertEquals(1, list.size());
    assertTrue(list.contains("Target indicator does not exist: `Uid00000011`"));
  }

  @Test
  @DisplayName("merge with invalid source indicator")
  void mergeWithInvalidSourceTest() {
    IndicatorType indType1 = createIndicatorType('c');
    manager.save(indType1);
    Indicator validSource1 = createIndicator('c', indType1);
    manager.save(validSource1);
    Indicator validTarget = createIndicator('d', indType1);
    manager.save(validTarget);

    // given merge params with an invalid source indicator
    MergeParams params = new MergeParams();
    params.setSources(Set.of(UID.of(validSource1.getUid()), UID.of("Uid00000011")));
    params.setTarget(UID.of(validTarget.getUid()));

    // when a merge request is processed
    ConflictException conflictException =
        assertThrows(
            ConflictException.class,
            () -> indicatorMergeProcessor.processMerge(params, MergeType.INDICATOR));

    // then the merge report has the correct error info
    MergeReport mergeReport = conflictException.getMergeReport();
    List<String> list =
        mergeReport.getMergeErrors().stream().map(ErrorMessage::getMessage).toList();
    assertEquals(1, list.size());
    assertTrue(list.contains("Source indicator does not exist: `Uid00000011`"));
  }

  @Test
  @DisplayName("merge with target indicator in source indicators")
  void mergeWithTargetAsSourceTest() {
    IndicatorType indType1 = createIndicatorType('e');
    manager.save(indType1);
    Indicator validTarget = createIndicator('e', indType1);
    manager.save(validTarget);

    // given merge params with a target indicator as a source indicator
    MergeParams params = new MergeParams();
    params.setSources(Set.of(UID.of(validTarget.getUid())));
    params.setTarget(UID.of(validTarget.getUid()));

    // when a merge request is processed
    ConflictException conflictException =
        assertThrows(
            ConflictException.class,
            () -> indicatorMergeProcessor.processMerge(params, MergeType.INDICATOR));

    // then the merge report has the correct error info
    MergeReport mergeReport = conflictException.getMergeReport();
    List<String> list =
        mergeReport.getMergeErrors().stream().map(ErrorMessage::getMessage).toList();
    assertEquals(1, list.size());

    assertTrue(list.contains("Target indicator cannot be a source indicator"));
  }

  @Test
  @DisplayName("merge with no target indicator")
  void mergeWithNoTargetTest() {
    IndicatorType indType1 = createIndicatorType('f');
    manager.save(indType1);
    Indicator validTarget = createIndicator('f', indType1);
    manager.save(validTarget);

    // given merge params with a target indicator as a source indicator
    MergeParams params = new MergeParams();
    params.setSources(Set.of(UID.of(validTarget.getUid())));
    params.setTarget(null);

    // when a merge request is processed
    ConflictException conflictException =
        assertThrows(
            ConflictException.class,
            () -> indicatorMergeProcessor.processMerge(params, MergeType.INDICATOR));

    // then the merge report has the correct error info
    MergeReport mergeReport = conflictException.getMergeReport();
    List<String> list =
        mergeReport.getMergeErrors().stream().map(ErrorMessage::getMessage).toList();
    assertEquals(1, list.size());
    assertTrue(list.contains("Target indicator must be specified"));
  }

  @Test
  @DisplayName("valid indicator merge with source indicators replaced and then deleted")
  void validMergeTest() throws ConflictException {
    IndicatorType indType1 = createIndicatorType('g');
    manager.save(indType1);
    Indicator validSource1 = createIndicator('g', indType1);
    Indicator validSource2 = createIndicator('h', indType1);
    Indicator validTarget = createIndicator('i', indType1);
    Indicator indicatorWithSourceNumerator = createIndicator('j', indType1);
    indicatorWithSourceNumerator.setNumerator(validSource1.getUid());

    Indicator indicatorWithSourceDenominator = createIndicator('k', indType1);
    indicatorWithSourceDenominator.setDenominator(validSource2.getUid());

    DataEntryForm entryForm = createDataEntryForm('l');
    entryForm.setHtmlCode("<p>{#%s}</p>".formatted(validSource1.getUid()));

    PeriodType periodType = PeriodType.getPeriodType(PeriodTypeEnum.MONTHLY);
    DataSet dataSet = createDataSet('m');
    dataSet.addIndicator(validSource1);
    dataSet.addIndicator(validSource2);
    dataSet.setPeriodType(periodType);
    validSource1.addDataSet(dataSet);
    validSource2.addDataSet(dataSet);

    IndicatorGroup group = createIndicatorGroup('n');
    group.addIndicator(validSource1);
    group.addIndicator(validSource2);
    validSource1.addIndicatorGroup(group);
    validSource2.addIndicatorGroup(group);

    Section section = new Section();
    section.setName("section1");
    section.addIndicator(validSource1);
    section.addIndicator(validSource2);
    section.setDataSet(dataSet);

    Visualization viz = createVisualization('o');
    viz.addDataDimensionItem(validSource1);
    viz.addDataDimensionItem(validSource2);

    Visualization viz2 = createVisualization('p');
    Sorting sorting = new Sorting();
    sorting.setDimension(validSource1.getUid());
    viz2.setSorting(new ArrayList<>(List.of(sorting)));

    Configuration config = new Configuration();
    config.setInfrastructuralIndicators(group);

    manager.save(validSource1);
    manager.save(validSource2);
    manager.save(validTarget);
    manager.save(indicatorWithSourceNumerator);
    manager.save(indicatorWithSourceDenominator);
    manager.save(entryForm);
    manager.save(dataSet);
    manager.save(group);
    manager.save(section);
    manager.save(viz);
    manager.save(viz2);
    configService.setConfiguration(config);

    // given merge params with a target indicator and source indicators
    MergeParams params = new MergeParams();
    params.setSources(Set.of(UID.of(validSource1.getUid()), UID.of(validSource2.getUid())));
    params.setTarget(UID.of(validTarget.getUid()));
    params.setDeleteSources(true);

    // when a merge request is processed
    MergeReport report = indicatorMergeProcessor.processMerge(params, MergeType.INDICATOR);

    // then the merge report has the correct error info
    assertFalse(report.hasErrorMessages());

    // and indicator numerators and denominators have been updated
    Indicator numeratorIndicator =
        manager.get(Indicator.class, indicatorWithSourceNumerator.getUid());
    assertNotNull(numeratorIndicator);
    assertEquals(validTarget.getUid(), numeratorIndicator.getNumerator());
    Indicator denominatorIndicator =
        manager.get(Indicator.class, indicatorWithSourceDenominator.getUid());
    assertNotNull(denominatorIndicator);
    assertEquals(validTarget.getUid(), denominatorIndicator.getDenominator());

    // and data entry form html code is updated
    DataEntryForm dataEntryForm = manager.get(DataEntryForm.class, entryForm.getUid());
    assertNotNull(dataEntryForm);
    assertEquals("<p>{#%s}</p>".formatted(validTarget.getUid()), dataEntryForm.getHtmlCode());

    // and group is updated
    IndicatorGroup group1 = manager.get(IndicatorGroup.class, group.getUid());
    assertNotNull(group1);
    assertEquals(1, group1.getMembers().size());
    assertEquals(
        List.of(validTarget.getUid()),
        group1.getMembers().stream().map(BaseIdentifiableObject::getUid).toList());

    // and data sets are updated
    DataSet dataSet1 = manager.get(DataSet.class, dataSet.getUid());
    assertNotNull(dataSet1);
    assertEquals(1, dataSet1.getIndicators().size());
    assertTrue(dataSet1.getIndicators().contains(validTarget));

    // and sections are updated
    Section section1 = manager.get(Section.class, section.getUid());
    assertNotNull(section1);
    assertEquals(1, section1.getIndicators().size());
    assertTrue(section1.getIndicators().contains(validTarget));

    // and visualizations are updated
    Visualization visualization = manager.get(Visualization.class, viz.getUid());
    assertNotNull(visualization);
    assertEquals(2, visualization.getDataDimensionItems().size());
    assertEquals(1, visualization.getIndicators().size());
    assertTrue(visualization.getIndicators().contains(validTarget));
    assertEquals(
        2,
        visualization.getDataDimensionItems().stream()
            .filter(ddi -> ddi.getIndicator().getUid().equals(validTarget.getUid()))
            .count());

    // sorting updated
    Visualization visualization2 = manager.get(Visualization.class, viz2.getUid());
    assertNotNull(visualization2);
    Sorting sorting1 = visualization2.getSorting().get(0);
    assertEquals(validTarget.getUid(), sorting1.getDimension());

    // and config updated
    Configuration configuration = configService.getConfiguration();
    assertTrue(configuration.getInfrastructuralIndicators().getMembers().contains(validTarget));
    assertEquals(1, configuration.getInfrastructuralIndicators().getMembers().size());
  }
}
