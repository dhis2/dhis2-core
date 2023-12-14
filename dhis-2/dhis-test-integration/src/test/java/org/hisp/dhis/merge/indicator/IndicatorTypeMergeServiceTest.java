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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeRequest;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.merge.MergeType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author david mackessy
 */
class IndicatorTypeMergeServiceTest extends TransactionalIntegrationTest {

  @Autowired private MergeService service;

  @Autowired private IdentifiableObjectManager idObjectManager;

  private IndicatorType itA;
  private IndicatorType itB;
  private IndicatorType itC;

  private final UID uidA = UID.of(BASE_IN_TYPE_UID + 'A');
  private final UID uidB = UID.of(BASE_IN_TYPE_UID + 'B');
  private final UID uidC = UID.of(BASE_IN_TYPE_UID + 'C');
  private final UID uidX = UID.of(BASE_IN_TYPE_UID + 'X');

  @Override
  public void setUpTest() {
    itA = createIndicatorType('A');
    itA.setFactor(99);
    itA.setNumber(true);
    itB = createIndicatorType('B');
    itA.setFactor(98);
    itA.setNumber(false);
    itC = createIndicatorType('C');
    idObjectManager.save(itA);
    idObjectManager.save(itB);
    idObjectManager.save(itC);
  }

  @Test
  @DisplayName(
      "Transform valid merge params, producing a valid merge request and an error free merge report")
  void testGetFromParams() {
    // given
    MergeParams params = new MergeParams();
    params.setSources(Set.of(uidA, uidB));
    params.setTarget(uidC);
    params.setDeleteSources(true);
    MergeReport mergeReport = new MergeReport(MergeType.INDICATOR_TYPE);

    // when
    MergeRequest request = service.validate(params, mergeReport);

    // then
    assertEquals(2, request.getSources().size());
    assertTrue(request.getSources().containsAll(List.of(uidA, uidB)));
    assertEquals(uidC, request.getTarget());
    assertTrue(request.isDeleteSources());
    assertFalse(mergeReport.hasErrorMessages());
  }

  @Test
  @DisplayName(
      "Transform merge params with missing sources and target, producing an empty merge request and a merge report with errors")
  void testGetFromParamsWithErrors() {
    // given
    MergeParams params = new MergeParams();
    MergeReport mergeReport = new MergeReport(MergeType.INDICATOR_TYPE);

    // when
    MergeRequest request = service.validate(params, mergeReport);

    // then
    assertRequestIsEmpty(request);
    assertTrue(mergeReport.hasErrorMessages());
    assertMatchesErrorCodes(mergeReport, Set.of(ErrorCode.E1530, ErrorCode.E1531));
    assertMatchesErrorMessages(
        mergeReport,
        Set.of(
            "At least one source indicator type must be specified",
            "Target indicator type must be specified"));
  }

  @Test
  @DisplayName(
      "Transform merge params with invalid source uid, producing an empty merge request and a merge report with an error")
  void testSourceNotFound() {
    // given
    MergeParams params = new MergeParams();
    params.setSources(Set.of(uidA, uidX));
    params.setTarget(uidC);
    MergeReport mergeReport = new MergeReport(MergeType.INDICATOR_TYPE);

    // when
    MergeRequest request = service.validate(params, mergeReport);

    // then
    assertEquals(1, request.getSources().size());
    assertEquals(BASE_IN_TYPE_UID + 'C', request.getTarget().getValue());
    assertTrue(mergeReport.hasErrorMessages());
    assertMatchesErrorCodes(mergeReport, Set.of(ErrorCode.E1533));
    assertMatchesErrorMessages(
        mergeReport, Set.of("Source indicator type does not exist: `IntY123abgX`"));
  }

  @Test
  @DisplayName(
      "Transform merge params with invalid target uid, producing an empty merge request and a merge report with error")
  void testTargetNotFound() {
    // given
    MergeParams params = new MergeParams();
    params.setSources(Set.of(uidA, uidB));
    params.setTarget(uidX);
    MergeReport mergeReport = new MergeReport(MergeType.INDICATOR_TYPE);

    // when
    MergeRequest request = service.validate(params, mergeReport);

    // then
    assertRequestIsEmpty(request);
    assertTrue(mergeReport.hasErrorMessages());
    assertMatchesErrorCodes(mergeReport, Set.of(ErrorCode.E1533));
    assertMatchesErrorMessages(
        mergeReport, Set.of("Target indicator type does not exist: `IntY123abgX`"));
  }

  @Test
  @DisplayName("Validate a valid merge request")
  void testValidate() {
    // given indicators exist and are associated with indicator types
    Indicator iA = createIndicator('A', itA);
    Indicator iB = createIndicator('B', itB);
    Indicator iC = createIndicator('C', itC);
    idObjectManager.save(iA);
    idObjectManager.save(iB);
    idObjectManager.save(iC);

    assertNotNull(idObjectManager.get(IndicatorType.class, itA.getUid()));
    assertNotNull(idObjectManager.get(IndicatorType.class, itB.getUid()));
    assertNotNull(idObjectManager.get(IndicatorType.class, itC.getUid()));
    assertEquals(itA, iA.getIndicatorType());
    assertEquals(itB, iB.getIndicatorType());
    assertEquals(itC, iC.getIndicatorType());
    MergeParams params = new MergeParams();
    params.setSources(Set.of(uidA, uidB));
    params.setTarget(uidC);
    params.setDeleteSources(true);

    // when an indicator merge request is validated
    MergeReport mergeReport = new MergeReport(MergeType.INDICATOR_TYPE);
    service.validate(params, mergeReport);

    // then
    // source indicator types exist
    assertNotNull(idObjectManager.get(IndicatorType.class, itA.getUid()));
    assertNotNull(idObjectManager.get(IndicatorType.class, itB.getUid()));
    // and the target indicator type exists
    assertNotNull(idObjectManager.get(IndicatorType.class, itC.getUid()));

    // and the merge report has no errors
    assertFalse(mergeReport.hasErrorMessages());
  }

  @Test
  @DisplayName("Validate an invalid merge request missing sources")
  void testValidateWithErrorNoSources() {
    // given
    Indicator iC = createIndicator('C', itC);
    idObjectManager.save(iC);

    assertNotNull(idObjectManager.get(IndicatorType.class, itC.getUid()));
    assertEquals(itC, iC.getIndicatorType());
    MergeParams params = new MergeParams();
    params.setSources(Set.of());
    params.setTarget(uidC);
    params.setDeleteSources(true);

    // when an indicator merge request is validated
    MergeReport mergeReport = new MergeReport(MergeType.INDICATOR_TYPE);
    MergeRequest validatedRequest = service.validate(params, mergeReport);

    // then
    // and the target indicator exists
    assertNotNull(idObjectManager.get(IndicatorType.class, itC.getUid()));

    // and the merge report has errors and the merge request is empty
    assertTrue(mergeReport.hasErrorMessages());
    assertEquals(Set.of(), validatedRequest.getSources());
    assertEquals(uidC, validatedRequest.getTarget());
    assertTrue(validatedRequest.isDeleteSources());
    assertMatchesErrorCodes(mergeReport, Set.of(ErrorCode.E1530));
    assertMatchesErrorMessages(
        mergeReport, Set.of("At least one source indicator type must be specified"));
  }

  @Test
  @DisplayName("Validate an invalid merge request missing a target")
  void testValidateWithErrorNoTarget() {
    // given indicators exist and are associated with indicator types
    Indicator iA = createIndicator('A', itA);
    Indicator iB = createIndicator('B', itB);
    idObjectManager.save(iA);
    idObjectManager.save(iB);

    assertNotNull(idObjectManager.get(IndicatorType.class, itA.getUid()));
    assertNotNull(idObjectManager.get(IndicatorType.class, itB.getUid()));
    assertEquals(itA, iA.getIndicatorType());
    assertEquals(itB, iB.getIndicatorType());
    MergeParams params = new MergeParams();
    params.setSources(Set.of(uidA, uidB));
    params.setTarget(null);
    params.setDeleteSources(true);

    // when an indicator merge request is validated
    MergeReport mergeReport = new MergeReport(MergeType.INDICATOR_TYPE);
    MergeRequest validatedRequest = service.validate(params, mergeReport);

    // then
    // and the source indicators exists
    assertNotNull(idObjectManager.get(IndicatorType.class, itA.getUid()));
    assertNotNull(idObjectManager.get(IndicatorType.class, itB.getUid()));

    // and the merge report has errors and the merge request is empty
    assertTrue(mergeReport.hasErrorMessages());
    assertRequestIsEmpty(validatedRequest);
    assertMatchesErrorCodes(mergeReport, Set.of(ErrorCode.E1531));
    assertMatchesErrorMessages(mergeReport, Set.of("Target indicator type must be specified"));
  }

  @Test
  @DisplayName("Merge indicator types and delete sources")
  void testValidMergeDeleteSources() {
    // given indicators exist and are associated with indicator types
    Indicator iA = createIndicator('A', itA);
    Indicator iB = createIndicator('B', itB);
    Indicator iC = createIndicator('C', itC);
    idObjectManager.save(iA);
    idObjectManager.save(iB);
    idObjectManager.save(iC);

    assertNotNull(idObjectManager.get(IndicatorType.class, itA.getUid()));
    assertNotNull(idObjectManager.get(IndicatorType.class, itB.getUid()));
    assertNotNull(idObjectManager.get(IndicatorType.class, itC.getUid()));
    assertEquals(itA, iA.getIndicatorType());
    assertEquals(itB, iB.getIndicatorType());
    assertEquals(itC, iC.getIndicatorType());
    MergeRequest request =
        MergeRequest.builder().sources(Set.of(uidA, uidB)).target(uidC).deleteSources(true).build();

    // when an indicator merge request is merged
    MergeReport mergeReport = new MergeReport(MergeType.INDICATOR_TYPE);
    MergeReport completeReport = service.merge(request, mergeReport);

    // then
    // source indicator types are deleted
    assertNull(idObjectManager.get(IndicatorType.class, itA.getUid()));
    assertNull(idObjectManager.get(IndicatorType.class, itB.getUid()));
    // and the target indicator type exists
    assertNotNull(idObjectManager.get(IndicatorType.class, itC.getUid()));

    // and associated source indicators are now associated to the target indicator type
    assertEquals(itC, iA.getIndicatorType());
    assertEquals(itC, iB.getIndicatorType());
    assertEquals(itC, iC.getIndicatorType());
    assertEquals(100, itC.getFactor());
    assertFalse(itC.isNumber());

    // and the merge report has no errors and contains deleted sources
    assertFalse(completeReport.hasErrorMessages());
    assertEquals(Set.of("IntY123abgA", "IntY123abgB"), completeReport.getSourcesDeleted());
  }

  @Test
  @DisplayName("Merge indicator types and do not delete sources")
  void testValidMergeKeepSources() {
    // given indicators exist and are associated with indicator types
    Indicator iA = createIndicator('A', itA);
    Indicator iB = createIndicator('B', itB);
    Indicator iC = createIndicator('C', itC);
    idObjectManager.save(iA);
    idObjectManager.save(iB);
    idObjectManager.save(iC);

    assertNotNull(idObjectManager.get(IndicatorType.class, itA.getUid()));
    assertNotNull(idObjectManager.get(IndicatorType.class, itB.getUid()));
    assertNotNull(idObjectManager.get(IndicatorType.class, itC.getUid()));
    assertEquals(itA, iA.getIndicatorType());
    assertEquals(itB, iB.getIndicatorType());
    assertEquals(itC, iC.getIndicatorType());
    MergeRequest request =
        MergeRequest.builder()
            .sources(Set.of(uidA, uidB))
            .target(uidC)
            .deleteSources(false)
            .build();

    // when an indicator merge request is merged
    MergeReport mergeReport = new MergeReport(MergeType.INDICATOR_TYPE);
    MergeReport completeReport = service.merge(request, mergeReport);

    // then
    // source indicator types are deleted
    assertNotNull(idObjectManager.get(IndicatorType.class, itA.getUid()));
    assertNotNull(idObjectManager.get(IndicatorType.class, itB.getUid()));
    // and the target indicator type exists
    assertNotNull(idObjectManager.get(IndicatorType.class, itC.getUid()));

    // and associated source indicators are now associated to the target indicator type
    assertEquals(itC, iA.getIndicatorType());
    assertEquals(itC, iB.getIndicatorType());
    assertEquals(itC, iC.getIndicatorType());
    assertEquals(100, itC.getFactor());
    assertFalse(itC.isNumber());

    // and the merge report has no errors and shows 2 deleted sources
    assertFalse(completeReport.hasErrorMessages());
    assertEquals(Set.of(), completeReport.getSourcesDeleted());
  }

  public static void assertRequestIsEmpty(MergeRequest request) {
    assertEquals(0, request.getSources().size());
    assertNull(request.getTarget());
    assertFalse(request.isDeleteSources());
  }

  private void assertMatchesErrorMessages(MergeReport mergeReport, Set<String> expected) {
    Set<String> actual =
        mergeReport.getMergeErrors().stream()
            .map(ErrorMessage::getMessage)
            .collect(Collectors.toSet());
    assertEquals(expected, actual);
  }

  private void assertMatchesErrorCodes(MergeReport mergeReport, Set<ErrorCode> expected) {
    Set<ErrorCode> actual =
        mergeReport.getMergeErrors().stream()
            .map(ErrorMessage::getErrorCode)
            .collect(Collectors.toSet());
    assertEquals(expected, actual);
  }
}
