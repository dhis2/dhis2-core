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

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeProcessor;
import org.hisp.dhis.merge.MergeType;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author david mackessy
 */
class IndicatorMergeProcessorTest extends SingleSetupIntegrationTestBase {
  @Autowired private MergeProcessor indicatorMergeProcessor;

  @Autowired private IdentifiableObjectManager manager;

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

    //    dataSet = createDataSet('a');
    //    dataSet.setIndicators(Set.of(validSource1, validSource2));

    manager.save(validSource1);
    manager.save(validSource2);
    manager.save(validTarget);
    manager.save(indicatorWithSourceNumerator);
    manager.save(indicatorWithSourceDenominator);
    manager.save(entryForm);

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

    // and data sets
    //    DataSet dataSet1 = manager.get(DataSet.class, dataSet.getUid());
    //    assertNotNull(dataSet1);
    //    assertEquals(1, dataSet1.getIndicators().size());
  }
}
