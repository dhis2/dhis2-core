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
package org.hisp.dhis.tracker.programrule.executor.enrollment;

import static org.hisp.dhis.tracker.programrule.ProgramRuleIssue.error;
import static org.hisp.dhis.tracker.validation.ValidationCode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetMandatoryFieldExecutorTest extends DhisConvenienceTest {
  private static final String ACTIVE_ENROLLMENT_ID = "ActiveEnrollmentUid";

  private static final String COMPLETED_ENROLLMENT_ID = "CompletedEnrollmentUid";

  private static final String DATA_ELEMENT_ID = "DataElementId";

  private static final String ATTRIBUTE_ID = "AttributeId";

  private static final String ATTRIBUTE_CODE = "AttributeCode";

  private static final String TEI_ID = "TeiId";

  private static final String ATTRIBUTE_VALUE = "23.0";

  private static ProgramStage firstProgramStage;

  private static ProgramStage secondProgramStage;

  private static DataElement dataElementA;

  private static DataElement dataElementB;

  private TrackedEntityAttribute attribute;

  private final SetMandatoryFieldExecutor executor =
      new SetMandatoryFieldExecutor("RULE_ATTRIBUTE", ATTRIBUTE_ID);

  private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @BeforeEach
  void setUpTest() {
    firstProgramStage = createProgramStage('A', 0);
    firstProgramStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    dataElementA = createDataElement('A');
    dataElementA.setUid(DATA_ELEMENT_ID);
    ProgramStageDataElement programStageDataElementA =
        createProgramStageDataElement(firstProgramStage, dataElementA, 0);
    firstProgramStage.setProgramStageDataElements(Set.of(programStageDataElementA));
    secondProgramStage = createProgramStage('B', 0);
    secondProgramStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    dataElementB = createDataElement('B');
    ProgramStageDataElement programStageDataElementB =
        createProgramStageDataElement(secondProgramStage, dataElementB, 0);
    secondProgramStage.setProgramStageDataElements(Set.of(programStageDataElementB));

    attribute = createTrackedEntityAttribute('A');
    attribute.setUid(ATTRIBUTE_ID);
    attribute.setCode(ATTRIBUTE_CODE);

    bundle = TrackerBundle.builder().build();
    bundle.setPreheat(preheat);
  }

  @Test
  void shouldReturnNoErrorWhenMandatoryFieldIsPresentForEnrollment() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getTrackedEntityAttribute(ATTRIBUTE_ID)).thenReturn(attribute);
    bundle.setEnrollments(List.of(getEnrollmentWithMandatoryAttributeSet()));

    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, getEnrollmentWithMandatoryAttributeSet());

    assertFalse(error.isPresent());
  }

  @Test
  void shouldReturnNoErrorWhenMandatoryFieldIsPresentForEnrollmentUsingIdSchemeCode() {
    TrackerIdSchemeParams idSchemes =
        TrackerIdSchemeParams.builder().idScheme(TrackerIdSchemeParam.CODE).build();
    when(preheat.getIdSchemes()).thenReturn(idSchemes);
    when(preheat.getTrackedEntityAttribute(ATTRIBUTE_ID)).thenReturn(attribute);
    bundle.setEnrollments(List.of(getEnrollmentWithMandatoryAttributeSet(idSchemes)));

    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, getEnrollmentWithMandatoryAttributeSet(idSchemes));

    assertFalse(error.isPresent());
  }

  @Test
  void shouldReturnAnErrorWhenMandatoryFieldIsNotPresentForEnrollment() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getTrackedEntityAttribute(ATTRIBUTE_ID)).thenReturn(attribute);
    bundle.setEnrollments(
        List.of(
            getEnrollmentWithMandatoryAttributeSet(), getEnrollmentWithMandatoryAttributeNOTSet()));

    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, getEnrollmentWithMandatoryAttributeSet());

    assertFalse(error.isPresent());

    error = executor.executeRuleAction(bundle, getEnrollmentWithMandatoryAttributeNOTSet());

    assertTrue(error.isPresent());
    assertEquals(error("RULE_ATTRIBUTE", E1306, ATTRIBUTE_ID), error.get());
  }

  private Enrollment getEnrollmentWithMandatoryAttributeSet(TrackerIdSchemeParams idSchemes) {
    return Enrollment.builder()
        .enrollment(ACTIVE_ENROLLMENT_ID)
        .trackedEntity(TEI_ID)
        .status(EnrollmentStatus.ACTIVE)
        .attributes(getAttributes(idSchemes))
        .build();
  }

  private Enrollment getEnrollmentWithMandatoryAttributeSet() {
    return Enrollment.builder()
        .enrollment(ACTIVE_ENROLLMENT_ID)
        .trackedEntity(TEI_ID)
        .status(EnrollmentStatus.ACTIVE)
        .attributes(getAttributes())
        .build();
  }

  private Enrollment getEnrollmentWithMandatoryAttributeNOTSet() {
    return Enrollment.builder()
        .enrollment(COMPLETED_ENROLLMENT_ID)
        .trackedEntity(TEI_ID)
        .status(EnrollmentStatus.COMPLETED)
        .build();
  }

  private List<Attribute> getAttributes(TrackerIdSchemeParams idSchemes) {
    return List.of(getAttribute(idSchemes));
  }

  private List<Attribute> getAttributes() {
    return List.of(getAttribute());
  }

  private Attribute getAttribute(TrackerIdSchemeParams idSchemes) {
    return Attribute.builder()
        .attribute(idSchemes.toMetadataIdentifier(attribute))
        .value(ATTRIBUTE_VALUE)
        .build();
  }

  private Attribute getAttribute() {
    return Attribute.builder()
        .attribute(MetadataIdentifier.ofUid(ATTRIBUTE_ID))
        .value(ATTRIBUTE_VALUE)
        .build();
  }
}
