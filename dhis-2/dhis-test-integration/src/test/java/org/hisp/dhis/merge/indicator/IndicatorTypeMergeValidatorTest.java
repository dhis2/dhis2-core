/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.merge.indicator;

import static org.hisp.dhis.merge.MergeType.INDICATOR_TYPE;
import static org.hisp.dhis.merge.indicator.IndicatorTypeMergeServiceTest.assertRequestIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.merge.MergeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author david mackessy
 */
class IndicatorTypeMergeValidatorTest extends DhisConvenienceTest {

  private IndicatorTypeMergeValidator validator;

  @BeforeEach
  public void setup() {
    validator = new IndicatorTypeMergeValidator();
  }

  @Test
  @DisplayName("Valid request results in merge report with no errors")
  void testValidRequest() {
    // given a valid merge request
    IndicatorType a = createIndicatorType('a');
    IndicatorType b = createIndicatorType('b');
    IndicatorType c = createIndicatorType('c');
    MergeReport mergeReport = new MergeReport(INDICATOR_TYPE);
    MergeRequest<IndicatorType> mergeRequest =
        MergeRequest.<IndicatorType>builder().sources(Set.of(a, b)).target(c).build();

    // when the request is validated
    MergeRequest<IndicatorType> validatedRequest = validator.validate(mergeRequest, mergeReport);

    // then the report has no errors
    assertEquals(Set.of(a, b), validatedRequest.getSources());
    assertEquals(c, validatedRequest.getTarget());
    assertFalse(mergeReport.hasErrorMessages());
    assertReportMatchesErrorCodes(mergeReport, Set.of());
    assertReportMatchesErrorMessages(mergeReport, Set.of());
    assertEquals(INDICATOR_TYPE, mergeReport.getMergeType());
  }

  @Test
  @DisplayName("Empty request results in a merge report with errors and an empty merge request")
  void testEmptyRequest() {
    // given an empty merge request
    MergeRequest<IndicatorType> mergeRequest = MergeRequest.empty();
    MergeReport mergeReport = new MergeReport(INDICATOR_TYPE);

    // when the request is validated
    MergeRequest<IndicatorType> validatedRequest = validator.validate(mergeRequest, mergeReport);

    // then the report has errors
    assertTrue(mergeReport.hasErrorMessages());
    assertRequestIsEmpty(validatedRequest);
    assertReportMatchesErrorCodes(mergeReport, Set.of(ErrorCode.E1530, ErrorCode.E1531));
    assertReportMatchesErrorMessages(
        mergeReport,
        Set.of(
            "At least one source indicator type must be specified",
            "Target indicator type must be specified"));
    assertEquals(INDICATOR_TYPE, mergeReport.getMergeType());
  }

  @Test
  @DisplayName("Empty sources results in a merge report with an error and an empty request")
  void testEmptySources() {
    // given a merge request with empty sources
    IndicatorType a = createIndicatorType('a');
    MergeRequest<IndicatorType> mergeRequest =
        MergeRequest.<IndicatorType>builder().sources(Set.of()).target(a).build();
    MergeReport mergeReport = new MergeReport(INDICATOR_TYPE);

    // when the request is validated
    MergeRequest<IndicatorType> validatedRequest = validator.validate(mergeRequest, mergeReport);

    // then the report has an error
    assertTrue(mergeReport.hasErrorMessages());
    assertRequestIsEmpty(validatedRequest);
    assertReportMatchesErrorCodes(mergeReport, Set.of(ErrorCode.E1530));
    assertReportMatchesErrorMessages(
        mergeReport, Set.of("At least one source indicator type must be specified"));
    assertEquals(INDICATOR_TYPE, mergeReport.getMergeType());
  }

  @Test
  @DisplayName("Null sources results in a merge report with an error and an empty request")
  void testNullSources() {
    // given a merge request with null sources
    IndicatorType a = createIndicatorType('a');
    MergeRequest<IndicatorType> mergeRequest =
        MergeRequest.<IndicatorType>builder().sources(null).target(a).build();
    MergeReport mergeReport = new MergeReport(INDICATOR_TYPE);

    // when the request is validated
    MergeRequest<IndicatorType> validatedRequest = validator.validate(mergeRequest, mergeReport);

    // then the report has an error
    assertTrue(mergeReport.hasErrorMessages());
    assertRequestIsEmpty(validatedRequest);
    assertReportMatchesErrorCodes(mergeReport, Set.of(ErrorCode.E1530));
    assertReportMatchesErrorMessages(
        mergeReport, Set.of("At least one source indicator type must be specified"));
    assertEquals(INDICATOR_TYPE, mergeReport.getMergeType());
  }

  @Test
  @DisplayName("Null target results in a merge report with an error and an empty request")
  void testNullTarget() {
    // given a merge request with null target
    IndicatorType a = createIndicatorType('a');
    MergeRequest<IndicatorType> mergeRequest =
        MergeRequest.<IndicatorType>builder().sources(Set.of(a)).build();
    MergeReport mergeReport = new MergeReport(INDICATOR_TYPE);

    // when the request is validated
    MergeRequest<IndicatorType> validatedRequest = validator.validate(mergeRequest, mergeReport);

    // then the report has an error
    assertTrue(mergeReport.hasErrorMessages());
    assertRequestIsEmpty(validatedRequest);
    assertReportMatchesErrorCodes(mergeReport, Set.of(ErrorCode.E1531));
    assertReportMatchesErrorMessages(
        mergeReport, Set.of("Target indicator type must be specified"));
    assertEquals(INDICATOR_TYPE, mergeReport.getMergeType());
  }

  @Test
  @DisplayName("Target in sources results in a merge report with an error and an empty request")
  void testTargetInSources() {
    // given a merge request with the target in the sources
    IndicatorType a = createIndicatorType('a');
    MergeRequest<IndicatorType> mergeRequest =
        MergeRequest.<IndicatorType>builder().sources(Set.of(a)).target(a).build();
    MergeReport mergeReport = new MergeReport(INDICATOR_TYPE);

    // when the request is validated
    MergeRequest<IndicatorType> validatedRequest = validator.validate(mergeRequest, mergeReport);

    // then the report has an error
    assertTrue(mergeReport.hasErrorMessages());
    assertRequestIsEmpty(validatedRequest);
    assertReportMatchesErrorCodes(mergeReport, Set.of(ErrorCode.E1532));
    assertReportMatchesErrorMessages(
        mergeReport, Set.of("Target indicator type cannot be a source indicator type"));
    assertEquals(INDICATOR_TYPE, mergeReport.getMergeType());
  }

  private void assertReportMatchesErrorMessages(
      MergeReport updatedReport, Set<String> expectedErrors) {
    Set<String> actualErrors =
        updatedReport.getMergeErrors().stream()
            .map(ErrorMessage::getMessage)
            .collect(Collectors.toSet());
    assertEquals(expectedErrors, actualErrors);
  }

  private void assertReportMatchesErrorCodes(
      MergeReport updatedReport, Set<ErrorCode> expectedErrors) {
    Set<ErrorCode> actualErrors =
        updatedReport.getMergeErrors().stream()
            .map(ErrorMessage::getErrorCode)
            .collect(Collectors.toSet());
    assertEquals(expectedErrors, actualErrors);
  }
}
