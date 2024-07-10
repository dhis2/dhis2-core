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
package org.hisp.dhis.tracker.validation.validator.enrollment;

import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertNoErrors;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.Reporter;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.hisp.dhis.tracker.validation.service.attribute.TrackedAttributeValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luca Cambi <luca@dhis2.org>
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AttributeValidatorTest {

  @InjectMocks private AttributeValidator validator;

  @Mock private Enrollment enrollment;

  @Mock private Program program;

  @Mock private TrackerPreheat preheat;

  @Mock private DhisConfigurationProvider dhisConfigurationProvider;

  @Mock private TrackedAttributeValidationService teAttrService;

  private TrackerBundle bundle;

  @Mock private TrackedEntityInstance trackedEntityInstance;

  private static final String trackedEntity = "trackedEntity";

  private static final String trackedAttribute = "attribute";

  private static final String trackedAttribute1 = "attribute1";

  private static final String trackedAttributeP = "attributeP";

  private TrackedEntityAttribute trackedEntityAttribute;

  private TrackedEntityAttribute trackedEntityAttribute1;

  private TrackedEntityAttribute trackedEntityAttributeP;

  private Reporter reporter;

  @BeforeEach
  public void setUp() {

    trackedEntityAttribute =
        new TrackedEntityAttribute("name", "description", ValueType.TEXT, false, false);
    trackedEntityAttribute.setUid(trackedAttribute);

    trackedEntityAttribute1 =
        new TrackedEntityAttribute("name1", "description1", ValueType.TEXT, false, false);
    trackedEntityAttribute1.setUid(trackedAttribute1);

    trackedEntityAttributeP =
        new TrackedEntityAttribute("percentage", "percent", ValueType.PERCENTAGE, false, false);
    trackedEntityAttributeP.setUid(trackedAttributeP);

    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getProgram((MetadataIdentifier) any())).thenReturn(program);
    when(enrollment.getProgram()).thenReturn(MetadataIdentifier.ofUid("program"));
    when(preheat.getTrackedEntityAttribute(MetadataIdentifier.ofUid(trackedAttribute)))
        .thenReturn(trackedEntityAttribute);
    when(preheat.getTrackedEntityAttribute(MetadataIdentifier.ofUid(trackedAttribute1)))
        .thenReturn(trackedEntityAttribute1);
    when(preheat.getTrackedEntityAttribute(MetadataIdentifier.ofUid(trackedAttributeP)))
        .thenReturn(trackedEntityAttributeP);

    when(dhisConfigurationProvider.getEncryptionStatus())
        .thenReturn(EncryptionStatus.MISSING_ENCRYPTION_PASSWORD);

    String uid = CodeGenerator.generateUid();
    when(enrollment.getUid()).thenReturn(uid);
    when(enrollment.getEnrollment()).thenReturn(uid);
    when(enrollment.getTrackerType()).thenCallRealMethod();
    enrollment.setTrackedEntity(trackedEntity);

    bundle = TrackerBundle.builder().preheat(preheat).build();

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void shouldPassValidationWhenCreatingEnrollmentAndMandatoryAttributeIsPresentOnlyInTE() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);
    when(trackedEntityInstance.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Arrays.asList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntityInstance),
                    new TrackedEntityAttributeValue(
                        trackedEntityAttribute1, trackedEntityInstance))));
    bundle.setStrategy(enrollment, TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, enrollment);

    assertNoErrors(reporter);
  }

  @Test
  void
      shouldReturnErrorWhenUpdatingEnrollmentAndMandatoryFieldIsNotPresentInEnrollmentOrInTrackedEntityOrInDB() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(trackedEntityInstance.getTrackedEntityAttributeValues()).thenReturn(Set.of());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);
    bundle.setStrategy(enrollment, TrackerImportStrategy.UPDATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1018);
  }

  @Test
  void
      shouldReturnNoErrorWhenUpdatingEnrollmentAndMandatoryFieldIsNotPresentInEnrollmentButPresentInTrackedEntity() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();

    Attribute attribute1 =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute1))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(trackedEntityInstance.getTrackedEntityAttributeValues()).thenReturn(Set.of());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);
    bundle.setStrategy(enrollment, TrackerImportStrategy.UPDATE);
    bundle.setTrackedEntities(
        List.of(
            org.hisp.dhis.tracker.domain.TrackedEntity.builder()
                .trackedEntity(enrollment.getTrackedEntity())
                .attributes(List.of(attribute, attribute1))
                .build()));

    validator.validate(reporter, bundle, enrollment);

    assertNoErrors(reporter);
  }

  @Test
  void
      shouldReturnNoErrorWhenUpdatingEnrollmentAndMandatoryFieldIsNotPresentInEnrollmentButPresentInDB() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);
    when(trackedEntityInstance.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                List.of(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntityInstance),
                    new TrackedEntityAttributeValue(
                        trackedEntityAttribute1, trackedEntityInstance))));
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);
    bundle.setStrategy(enrollment, TrackerImportStrategy.UPDATE);

    validator.validate(reporter, bundle, enrollment);

    assertNoErrors(reporter);
  }

  @Test
  void shouldFailValidationWhenCreatingEnrollmentAndValueIsNotPresentAndAttributeIsMandatory() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);
    when(trackedEntityInstance.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Collections.singletonList(
                    new TrackedEntityAttributeValue(
                        trackedEntityAttribute, trackedEntityInstance))));
    bundle.setStrategy(enrollment, TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1018);
  }

  @Test
  void shouldFailValidationWhenCreatingEnrollmentAndValueIsNullAndAttributeIsMandatory() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();
    Attribute attribute1 =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute1))
            .valueType(ValueType.TEXT)
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Arrays.asList(attribute, attribute1));
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);
    when(trackedEntityInstance.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Arrays.asList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntityInstance),
                    new TrackedEntityAttributeValue(
                        trackedEntityAttribute1, trackedEntityInstance))));
    bundle.setStrategy(enrollment, TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1076);
  }

  @Test
  void shouldFailValidationWhenUpdatingEnrollmentAndValueIsNullAndAttributeIsMandatory() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();
    Attribute attribute1 =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute1))
            .valueType(ValueType.TEXT)
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Arrays.asList(attribute, attribute1));
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);
    when(trackedEntityInstance.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Arrays.asList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntityInstance),
                    new TrackedEntityAttributeValue(
                        trackedEntityAttribute1, trackedEntityInstance))));
    bundle.setStrategy(enrollment, TrackerImportStrategy.UPDATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1076);
  }

  @Test
  void shouldPassValidationWhenValueIsNullAndAttributeIsNotMandatory() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();
    Attribute attribute1 =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute1))
            .valueType(ValueType.TEXT)
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, false)));

    when(enrollment.getAttributes()).thenReturn(Arrays.asList(attribute, attribute1));
    when(trackedEntityInstance.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Arrays.asList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntityInstance),
                    new TrackedEntityAttributeValue(
                        trackedEntityAttribute1, trackedEntityInstance))));
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailValidationWhenValueIsInvalidPercentage() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();
    Attribute attribute1 =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttributeP))
            .valueType(ValueType.PERCENTAGE)
            .value("1000")
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttributeP, false, false)));

    when(enrollment.getAttributes()).thenReturn(Arrays.asList(attribute, attribute1));
    when(trackedEntityInstance.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Arrays.asList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntityInstance),
                    new TrackedEntityAttributeValue(
                        trackedEntityAttributeP, trackedEntityInstance))));
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1085);
  }

  @Test
  void
      shouldFailValidationWhenCreatingEnrollmentAndValueIsNullAndAttributeIsMandatoryAndAttributeNotExistsInTei() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();
    Attribute attribute1 =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute1))
            .valueType(ValueType.TEXT)
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Arrays.asList(attribute, attribute1));
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);
    when(trackedEntityInstance.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Collections.singletonList(
                    new TrackedEntityAttributeValue(
                        trackedEntityAttribute, trackedEntityInstance))));
    bundle.setStrategy(enrollment, TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, enrollment);

    assertAll(
        () -> assertHasError(reporter, enrollment, ValidationCode.E1076),
        () -> assertHasError(reporter, enrollment, ValidationCode.E1018));
  }

  @Test
  void
      shouldFailValidationWhenUpdatingEnrollmentAndValueIsNullAndAttributeIsMandatoryAndAttributeNotExistsInTei() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();
    Attribute attribute1 =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(trackedAttribute1))
            .valueType(ValueType.TEXT)
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Arrays.asList(attribute, attribute1));
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);
    when(trackedEntityInstance.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Collections.singletonList(
                    new TrackedEntityAttributeValue(
                        trackedEntityAttribute, trackedEntityInstance))));
    bundle.setStrategy(enrollment, TrackerImportStrategy.UPDATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1076);
  }

  @Test
  void shouldFailValidationWhenAttributeIsNotPresentInDB() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid("invalidAttribute"))
            .valueType(ValueType.TEXT)
            .value("value")
            .build();

    when(program.getProgramAttributes()).thenReturn(Collections.emptyList());

    when(enrollment.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(trackedEntityInstance.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Collections.singletonList(
                    new TrackedEntityAttributeValue(
                        trackedEntityAttribute, trackedEntityInstance))));
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntityInstance);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1006);
  }
}
