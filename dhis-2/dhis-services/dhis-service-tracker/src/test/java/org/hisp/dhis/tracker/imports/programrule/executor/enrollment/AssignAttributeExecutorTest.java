/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.imports.programrule.executor.enrollment;

import static org.hisp.dhis.tracker.imports.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.imports.programrule.IssueType.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.IssueType;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AssignAttributeExecutorTest extends TestBase {

  private static final UID TRACKED_ENTITY_ID = UID.generate();

  private static final UID FIRST_ENROLLMENT_ID = UID.generate();

  private static final UID SECOND_ENROLLMENT_ID = UID.generate();

  private static final UID ATTRIBUTE_UID = UID.of("h4w96yEMlzO");

  private static final UID RULE_UID = UID.of("TvctPPhpD8u");

  private static final String ATTRIBUTE_CODE = "AttributeCode";

  private static final String TE_ATTRIBUTE_OLD_VALUE = "10.0";

  private static final String TE_ATTRIBUTE_NEW_VALUE = "24.0";

  private static TrackedEntityAttribute attributeA;

  private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private SystemSettingsProvider settingsProvider;
  @Mock private SystemSettings settings;

  @BeforeEach
  void setUpTest() {
    attributeA = createTrackedEntityAttribute('A');
    attributeA.setUid(ATTRIBUTE_UID.getValue());
    attributeA.setCode(ATTRIBUTE_CODE);
    attributeA.setValueType(ValueType.NUMBER);
    when(preheat.getTrackedEntityAttribute(attributeA.getUid())).thenReturn(attributeA);
    bundle = TrackerBundle.builder().build();
    bundle.setPreheat(preheat);
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    when(settings.getRuleEngineAssignOverwrite()).thenReturn(false);
  }

  @Test
  void shouldAssignAttributeValueForEnrollmentsWhenAttributeIsEmpty() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    List<TrackedEntity> trackedEntities = List.of(getTrackedEntitiesWithAttributeNOTSet());
    Enrollment enrollmentWithAttributeNOTSet = getEnrollmentWithAttributeNOTSet();
    List<Enrollment> enrollments = List.of(enrollmentWithAttributeNOTSet);
    bundle.setTrackedEntities(trackedEntities);
    bundle.setEnrollments(enrollments);

    AssignAttributeExecutor executor =
        new AssignAttributeExecutor(
            settingsProvider,
            RULE_UID,
            TE_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_UID,
            enrollmentWithAttributeNOTSet.getAttributes());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, enrollmentWithAttributeNOTSet);

    Optional<Attribute> attribute = findAttributeByUid(bundle, SECOND_ENROLLMENT_ID, ATTRIBUTE_UID);

    assertAttributeWasAssignedAndWarningIsPresent(TE_ATTRIBUTE_NEW_VALUE, attribute, warning);
  }

  @Test
  void shouldNotAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresent() {
    Enrollment enrollmentWithAttributeSet = getEnrollmentWithAttributeSet();
    List<Enrollment> enrollments = List.of(enrollmentWithAttributeSet);
    bundle.setEnrollments(enrollments);

    AssignAttributeExecutor executor =
        new AssignAttributeExecutor(
            settingsProvider,
            RULE_UID,
            TE_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_UID,
            enrollmentWithAttributeSet.getAttributes());

    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, enrollmentWithAttributeSet);

    Optional<Attribute> attribute = findAttributeByUid(bundle, FIRST_ENROLLMENT_ID, ATTRIBUTE_UID);

    assertAttributeWasNotAssignedAndErrorIsPresent(TE_ATTRIBUTE_OLD_VALUE, attribute, error);
  }

  @Test
  void shouldNotAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentUsingIdSchemeCode() {
    TrackerIdSchemeParams idSchemes =
        TrackerIdSchemeParams.builder().idScheme(TrackerIdSchemeParam.CODE).build();
    when(preheat.getTrackedEntityAttribute(ATTRIBUTE_UID.getValue())).thenReturn(attributeA);
    Enrollment enrollmentWithAttributeSet = getEnrollmentWithAttributeSet(idSchemes);
    List<Enrollment> enrollments = List.of(enrollmentWithAttributeSet);
    bundle.setEnrollments(enrollments);

    AssignAttributeExecutor executor =
        new AssignAttributeExecutor(
            settingsProvider,
            RULE_UID,
            TE_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_UID,
            enrollmentWithAttributeSet.getAttributes());

    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, enrollmentWithAttributeSet);

    Optional<Attribute> attribute =
        findAttributeByCode(bundle, FIRST_ENROLLMENT_ID, ATTRIBUTE_CODE);

    assertAttributeWasNotAssignedAndErrorIsPresent(TE_ATTRIBUTE_OLD_VALUE, attribute, error);
  }

  @Test
  void shouldNotAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentInTei() {
    Enrollment enrollmentWithAttributeNOTSet = getEnrollmentWithAttributeNOTSet();
    List<Enrollment> enrollments = List.of(enrollmentWithAttributeNOTSet);
    List<TrackedEntity> trackedEntities = List.of(getTrackedEntitiesWithAttributeSet());
    bundle.setEnrollments(enrollments);
    bundle.setTrackedEntities(trackedEntities);

    AssignAttributeExecutor executor =
        new AssignAttributeExecutor(
            settingsProvider,
            RULE_UID,
            TE_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_UID,
            getTrackedEntitiesWithAttributeSet().getAttributes());

    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, enrollmentWithAttributeNOTSet);

    Optional<Attribute> teAttribute =
        findTeiAttributeByUid(bundle, TRACKED_ENTITY_ID, ATTRIBUTE_UID);
    Optional<Attribute> enrollmentAttribute =
        findAttributeByUid(bundle, SECOND_ENROLLMENT_ID, ATTRIBUTE_UID);

    assertFalse(enrollmentAttribute.isPresent());
    assertAttributeWasNotAssignedAndErrorIsPresent(TE_ATTRIBUTE_OLD_VALUE, teAttribute, error);
  }

  @Test
  void
      shouldAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentInTeiAndCanBeOverwritten() {
    when(settings.getRuleEngineAssignOverwrite()).thenReturn(true);
    Enrollment enrollmentWithAttributeNOTSet = getEnrollmentWithAttributeNOTSet();
    List<Enrollment> enrollments = List.of(enrollmentWithAttributeNOTSet);
    List<TrackedEntity> trackedEntities = List.of(getTrackedEntitiesWithAttributeSet());
    bundle.setEnrollments(enrollments);
    bundle.setTrackedEntities(trackedEntities);

    AssignAttributeExecutor executor =
        new AssignAttributeExecutor(
            settingsProvider,
            RULE_UID,
            TE_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_UID,
            enrollmentWithAttributeNOTSet.getAttributes());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, enrollmentWithAttributeNOTSet);

    Optional<Attribute> teAttribute =
        findTeiAttributeByUid(bundle, TRACKED_ENTITY_ID, ATTRIBUTE_UID);
    Optional<Attribute> enrollmentAttribute =
        findAttributeByUid(bundle, SECOND_ENROLLMENT_ID, ATTRIBUTE_UID);

    assertFalse(enrollmentAttribute.isPresent());
    assertAttributeWasAssignedAndWarningIsPresent(TE_ATTRIBUTE_NEW_VALUE, teAttribute, warning);
  }

  @Test
  void shouldAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentAndHasTheSameValue() {
    Enrollment enrollmentWithAttributeSetSameValue = getEnrollmentWithAttributeSetSameValue();
    List<Enrollment> enrollments = List.of(enrollmentWithAttributeSetSameValue);
    bundle.setEnrollments(enrollments);

    AssignAttributeExecutor executor =
        new AssignAttributeExecutor(
            settingsProvider,
            RULE_UID,
            TE_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_UID,
            enrollmentWithAttributeSetSameValue.getAttributes());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, enrollmentWithAttributeSetSameValue);

    Optional<Attribute> attribute = findAttributeByUid(bundle, FIRST_ENROLLMENT_ID, ATTRIBUTE_UID);

    assertAttributeWasAssignedAndWarningIsPresent(TE_ATTRIBUTE_NEW_VALUE, attribute, warning);
  }

  @Test
  void
      shouldAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentAndSystemSettingToOverwriteIsTrue() {
    Enrollment enrollmentWithAttributeSet = getEnrollmentWithAttributeSet();
    List<Enrollment> enrollments = List.of(enrollmentWithAttributeSet);
    bundle.setEnrollments(enrollments);
    when(settings.getRuleEngineAssignOverwrite()).thenReturn(true);

    AssignAttributeExecutor executor =
        new AssignAttributeExecutor(
            settingsProvider,
            RULE_UID,
            TE_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_UID,
            enrollmentWithAttributeSet.getAttributes());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, enrollmentWithAttributeSet);

    Optional<Attribute> attribute = findAttributeByUid(bundle, FIRST_ENROLLMENT_ID, ATTRIBUTE_UID);

    assertAttributeWasAssignedAndWarningIsPresent(TE_ATTRIBUTE_NEW_VALUE, attribute, warning);
  }

  private Optional<Attribute> findTeiAttributeByUid(
      TrackerBundle bundle, UID teUid, UID attributeUid) {
    TrackedEntity te = bundle.findTrackedEntityByUid(teUid).get();
    return te.getAttributes().stream()
        .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofUid(attributeUid.getValue())))
        .findAny();
  }

  private Optional<Attribute> findAttributeByUid(
      TrackerBundle bundle, UID enrollmentUid, UID attributeUid) {
    Enrollment enrollment = bundle.findEnrollmentByUid(enrollmentUid).get();
    return enrollment.getAttributes().stream()
        .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofUid(attributeUid.getValue())))
        .findAny();
  }

  private Optional<Attribute> findAttributeByCode(
      TrackerBundle bundle, UID enrollmentUid, String attributeCode) {
    Enrollment enrollment = bundle.findEnrollmentByUid(enrollmentUid).get();
    return enrollment.getAttributes().stream()
        .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofCode(attributeCode)))
        .findAny();
  }

  private void assertAttributeWasAssignedAndWarningIsPresent(
      String attributeValue, Optional<Attribute> attribute, Optional<ProgramRuleIssue> warning) {
    assertAttributeWasAssignedAndValidationIsPresent(attributeValue, attribute, warning, WARNING);
  }

  private void assertAttributeWasNotAssignedAndErrorIsPresent(
      String attributeValue, Optional<Attribute> attribute, Optional<ProgramRuleIssue> error) {
    assertAttributeWasAssignedAndValidationIsPresent(attributeValue, attribute, error, ERROR);
  }

  private void assertAttributeWasAssignedAndValidationIsPresent(
      String attributeValue,
      Optional<Attribute> attribute,
      Optional<ProgramRuleIssue> warning,
      IssueType issueType) {
    assertTrue(attribute.isPresent());
    assertEquals(attributeValue, attribute.get().getValue());
    assertTrue(warning.isPresent());
    assertEquals(issueType, warning.get().getIssueType());
  }

  private Enrollment getEnrollmentWithAttributeSet() {
    return Enrollment.builder()
        .enrollment(FIRST_ENROLLMENT_ID)
        .trackedEntity(UID.generate())
        .status(EnrollmentStatus.ACTIVE)
        .attributes(getAttributes())
        .build();
  }

  private Enrollment getEnrollmentWithAttributeSet(TrackerIdSchemeParams idSchemes) {
    return Enrollment.builder()
        .enrollment(FIRST_ENROLLMENT_ID)
        .status(EnrollmentStatus.ACTIVE)
        .attributes(getAttributes(idSchemes))
        .build();
  }

  private Enrollment getEnrollmentWithAttributeSetSameValue() {
    return Enrollment.builder()
        .enrollment(FIRST_ENROLLMENT_ID)
        .trackedEntity(UID.generate())
        .status(EnrollmentStatus.ACTIVE)
        .attributes(getAttributesSameValue())
        .build();
  }

  private TrackedEntity getTrackedEntitiesWithAttributeSet() {
    return TrackedEntity.builder()
        .trackedEntity(TRACKED_ENTITY_ID)
        .attributes(getAttributes())
        .build();
  }

  private TrackedEntity getTrackedEntitiesWithAttributeNOTSet() {
    return TrackedEntity.builder().trackedEntity(TRACKED_ENTITY_ID).build();
  }

  private Enrollment getEnrollmentWithAttributeNOTSet() {
    return Enrollment.builder()
        .enrollment(SECOND_ENROLLMENT_ID)
        .status(EnrollmentStatus.COMPLETED)
        .trackedEntity(TRACKED_ENTITY_ID)
        .build();
  }

  private List<Attribute> getAttributes(TrackerIdSchemeParams idSchemes) {
    Attribute attribute =
        Attribute.builder()
            .attribute(idSchemes.toMetadataIdentifier(attributeA))
            .value(TE_ATTRIBUTE_OLD_VALUE)
            .build();
    return List.of(attribute);
  }

  private List<Attribute> getAttributes() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(ATTRIBUTE_UID.getValue()))
            .value(TE_ATTRIBUTE_OLD_VALUE)
            .build();
    return List.of(attribute);
  }

  private List<Attribute> getAttributesSameValue() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(ATTRIBUTE_UID.getValue()))
            .value(TE_ATTRIBUTE_NEW_VALUE)
            .build();
    return List.of(attribute);
  }
}
