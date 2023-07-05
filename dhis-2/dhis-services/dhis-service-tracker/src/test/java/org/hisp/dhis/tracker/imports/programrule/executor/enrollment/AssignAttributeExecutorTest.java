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
package org.hisp.dhis.tracker.imports.programrule.executor.enrollment;

import static org.hisp.dhis.tracker.imports.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.imports.programrule.IssueType.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.EnrollmentStatus;
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
class AssignAttributeExecutorTest extends DhisConvenienceTest {

  private static final String TRACKED_ENTITY_ID = "TrackedEntityUid";

  private static final String FIRST_ENROLLMENT_ID = "ActiveEnrollmentUid";

  private static final String SECOND_ENROLLMENT_ID = "CompletedEnrollmentUid";

  private static final String ATTRIBUTE_ID = "AttributeId";

  private static final String ATTRIBUTE_CODE = "AttributeCode";

  private static final String TEI_ATTRIBUTE_OLD_VALUE = "10.0";

  private static final String TEI_ATTRIBUTE_NEW_VALUE = "24.0";

  private static TrackedEntityAttribute attributeA;

  private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @Mock private SystemSettingManager systemSettingManager;

  @BeforeEach
  void setUpTest() {
    attributeA = createTrackedEntityAttribute('A');
    attributeA.setUid(ATTRIBUTE_ID);
    attributeA.setCode(ATTRIBUTE_CODE);
    attributeA.setValueType(ValueType.NUMBER);
    when(preheat.getTrackedEntityAttribute(attributeA.getUid())).thenReturn(attributeA);
    bundle = TrackerBundle.builder().build();
    bundle.setPreheat(preheat);
    when(systemSettingManager.getBooleanSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE))
        .thenReturn(Boolean.FALSE);
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
            systemSettingManager,
            "",
            TEI_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_ID,
            enrollmentWithAttributeNOTSet.getAttributes());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, enrollmentWithAttributeNOTSet);

    Optional<Attribute> attribute = findAttributeByUid(bundle, SECOND_ENROLLMENT_ID, ATTRIBUTE_ID);

    assertAttributeWasAssignedAndWarningIsPresent(TEI_ATTRIBUTE_NEW_VALUE, attribute, warning);
  }

  @Test
  void shouldNotAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresent() {
    Enrollment enrollmentWithAttributeSet = getEnrollmentWithAttributeSet();
    List<Enrollment> enrollments = List.of(enrollmentWithAttributeSet);
    bundle.setEnrollments(enrollments);

    AssignAttributeExecutor executor =
        new AssignAttributeExecutor(
            systemSettingManager,
            "",
            TEI_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_ID,
            enrollmentWithAttributeSet.getAttributes());

    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, enrollmentWithAttributeSet);

    Optional<Attribute> attribute = findAttributeByUid(bundle, FIRST_ENROLLMENT_ID, ATTRIBUTE_ID);

    assertAttributeWasNotAssignedAndErrorIsPresent(TEI_ATTRIBUTE_OLD_VALUE, attribute, error);
  }

  @Test
  void shouldNotAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentUsingIdSchemeCode() {
    TrackerIdSchemeParams idSchemes =
        TrackerIdSchemeParams.builder().idScheme(TrackerIdSchemeParam.CODE).build();
    when(preheat.getTrackedEntityAttribute(ATTRIBUTE_ID)).thenReturn(attributeA);
    Enrollment enrollmentWithAttributeSet = getEnrollmentWithAttributeSet(idSchemes);
    List<Enrollment> enrollments = List.of(enrollmentWithAttributeSet);
    bundle.setEnrollments(enrollments);

    AssignAttributeExecutor executor =
        new AssignAttributeExecutor(
            systemSettingManager,
            "",
            TEI_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_ID,
            enrollmentWithAttributeSet.getAttributes());

    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, enrollmentWithAttributeSet);

    Optional<Attribute> attribute =
        findAttributeByCode(bundle, FIRST_ENROLLMENT_ID, ATTRIBUTE_CODE);

    assertAttributeWasNotAssignedAndErrorIsPresent(TEI_ATTRIBUTE_OLD_VALUE, attribute, error);
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
            systemSettingManager,
            "",
            TEI_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_ID,
            getTrackedEntitiesWithAttributeSet().getAttributes());

    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, enrollmentWithAttributeNOTSet);

    Optional<Attribute> teiAttribute =
        findTeiAttributeByUid(bundle, TRACKED_ENTITY_ID, ATTRIBUTE_ID);
    Optional<Attribute> enrollmentAttribute =
        findAttributeByUid(bundle, SECOND_ENROLLMENT_ID, ATTRIBUTE_ID);

    assertFalse(enrollmentAttribute.isPresent());
    assertAttributeWasNotAssignedAndErrorIsPresent(TEI_ATTRIBUTE_OLD_VALUE, teiAttribute, error);
  }

  @Test
  void
      shouldAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentInTeiAndCanBeOverwritten() {
    when(systemSettingManager.getBooleanSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE))
        .thenReturn(Boolean.TRUE);
    Enrollment enrollmentWithAttributeNOTSet = getEnrollmentWithAttributeNOTSet();
    List<Enrollment> enrollments = List.of(enrollmentWithAttributeNOTSet);
    List<TrackedEntity> trackedEntities = List.of(getTrackedEntitiesWithAttributeSet());
    bundle.setEnrollments(enrollments);
    bundle.setTrackedEntities(trackedEntities);

    AssignAttributeExecutor executor =
        new AssignAttributeExecutor(
            systemSettingManager,
            "",
            TEI_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_ID,
            enrollmentWithAttributeNOTSet.getAttributes());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, enrollmentWithAttributeNOTSet);

    Optional<Attribute> teiAttribute =
        findTeiAttributeByUid(bundle, TRACKED_ENTITY_ID, ATTRIBUTE_ID);
    Optional<Attribute> enrollmentAttribute =
        findAttributeByUid(bundle, SECOND_ENROLLMENT_ID, ATTRIBUTE_ID);

    assertFalse(enrollmentAttribute.isPresent());
    assertAttributeWasAssignedAndWarningIsPresent(TEI_ATTRIBUTE_NEW_VALUE, teiAttribute, warning);
  }

  @Test
  void shouldAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentAndHasTheSameValue() {
    Enrollment enrollmentWithAttributeSetSameValue = getEnrollmentWithAttributeSetSameValue();
    List<Enrollment> enrollments = List.of(enrollmentWithAttributeSetSameValue);
    bundle.setEnrollments(enrollments);

    AssignAttributeExecutor executor =
        new AssignAttributeExecutor(
            systemSettingManager,
            "",
            TEI_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_ID,
            enrollmentWithAttributeSetSameValue.getAttributes());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, enrollmentWithAttributeSetSameValue);

    Optional<Attribute> attribute = findAttributeByUid(bundle, FIRST_ENROLLMENT_ID, ATTRIBUTE_ID);

    assertAttributeWasAssignedAndWarningIsPresent(TEI_ATTRIBUTE_NEW_VALUE, attribute, warning);
  }

  @Test
  void
      shouldAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentAndSystemSettingToOverwriteIsTrue() {
    Enrollment enrollmentWithAttributeSet = getEnrollmentWithAttributeSet();
    List<Enrollment> enrollments = List.of(enrollmentWithAttributeSet);
    bundle.setEnrollments(enrollments);
    when(systemSettingManager.getBooleanSetting(SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE))
        .thenReturn(Boolean.TRUE);

    AssignAttributeExecutor executor =
        new AssignAttributeExecutor(
            systemSettingManager,
            "",
            TEI_ATTRIBUTE_NEW_VALUE,
            ATTRIBUTE_ID,
            enrollmentWithAttributeSet.getAttributes());

    Optional<ProgramRuleIssue> warning =
        executor.executeRuleAction(bundle, enrollmentWithAttributeSet);

    Optional<Attribute> attribute = findAttributeByUid(bundle, FIRST_ENROLLMENT_ID, ATTRIBUTE_ID);

    assertAttributeWasAssignedAndWarningIsPresent(TEI_ATTRIBUTE_NEW_VALUE, attribute, warning);
  }

  private Optional<Attribute> findTeiAttributeByUid(
      TrackerBundle bundle, String teiUid, String attributeUid) {
    TrackedEntity tei = bundle.findTrackedEntityByUid(teiUid).get();
    return tei.getAttributes().stream()
        .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofUid(attributeUid)))
        .findAny();
  }

  private Optional<Attribute> findAttributeByUid(
      TrackerBundle bundle, String enrollmentUid, String attributeUid) {
    Enrollment enrollment = bundle.findEnrollmentByUid(enrollmentUid).get();
    return enrollment.getAttributes().stream()
        .filter(at -> at.getAttribute().equals(MetadataIdentifier.ofUid(attributeUid)))
        .findAny();
  }

  private Optional<Attribute> findAttributeByCode(
      TrackerBundle bundle, String enrollmentUid, String attributeCode) {
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
            .value(TEI_ATTRIBUTE_OLD_VALUE)
            .build();
    return List.of(attribute);
  }

  private List<Attribute> getAttributes() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(ATTRIBUTE_ID))
            .value(TEI_ATTRIBUTE_OLD_VALUE)
            .build();
    return List.of(attribute);
  }

  private List<Attribute> getAttributesSameValue() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(ATTRIBUTE_ID))
            .value(TEI_ATTRIBUTE_NEW_VALUE)
            .build();
    return List.of(attribute);
  }
}
