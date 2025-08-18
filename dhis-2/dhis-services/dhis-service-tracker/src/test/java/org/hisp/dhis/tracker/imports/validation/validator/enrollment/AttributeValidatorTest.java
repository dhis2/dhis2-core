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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.util.Constant;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

  private TrackerBundle bundle;

  @Mock private TrackedEntity trackedEntity;

  private static final String TRACKED_ATTRIBUTE = "attribute";

  private static final String TRACKED_ATTRIBUTE_1 = "attribute1";

  private static final String TRACKED_ATTRIBUTE_P = "attributeP";

  private TrackedEntityAttribute trackedEntityAttribute;

  private TrackedEntityAttribute trackedEntityAttribute1;

  private TrackedEntityAttribute trackedEntityAttributeP;

  private Reporter reporter;

  @BeforeEach
  public void setUp() {

    trackedEntityAttribute =
        new TrackedEntityAttribute("name", "description", ValueType.TEXT, false, false);
    trackedEntityAttribute.setUid(TRACKED_ATTRIBUTE);

    trackedEntityAttribute1 =
        new TrackedEntityAttribute("name1", "description1", ValueType.TEXT, false, false);
    trackedEntityAttribute1.setUid(TRACKED_ATTRIBUTE_1);

    trackedEntityAttributeP =
        new TrackedEntityAttribute("percentage", "percent", ValueType.PERCENTAGE, false, false);
    trackedEntityAttributeP.setUid(TRACKED_ATTRIBUTE_P);

    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getProgram((MetadataIdentifier) any())).thenReturn(program);
    when(enrollment.getProgram()).thenReturn(MetadataIdentifier.ofUid("program"));
    when(preheat.getTrackedEntityAttribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE)))
        .thenReturn(trackedEntityAttribute);
    when(preheat.getTrackedEntityAttribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE_1)))
        .thenReturn(trackedEntityAttribute1);
    when(preheat.getTrackedEntityAttribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE_P)))
        .thenReturn(trackedEntityAttributeP);

    when(dhisConfigurationProvider.getEncryptionStatus())
        .thenReturn(EncryptionStatus.MISSING_ENCRYPTION_PASSWORD);

    UID uid = UID.generate();
    when(enrollment.getUid()).thenReturn(uid);
    when(enrollment.getEnrollment()).thenReturn(uid);
    when(enrollment.getTrackerType()).thenCallRealMethod();
    enrollment.setTrackedEntity(UID.generate());

    bundle = TrackerBundle.builder().preheat(preheat).build();

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  static Stream<Arguments> validImageFormats() {
    return Constant.VALID_IMAGE_FORMATS.stream().map(Arguments::of);
  }

  static Stream<Arguments> invalidImageFormats() {
    Set<String> invalidCandidates = Set.of("exe", "dat", "pdf", "docx");
    return invalidCandidates.stream()
        .filter(fmt -> !Constant.VALID_IMAGE_FORMATS.contains(fmt))
        .map(Arguments::of);
  }

  @Test
  void shouldPassValidationWhenCreatingEnrollmentAndMandatoryAttributeIsPresentOnlyInTE() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE))
            .value("value")
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(trackedEntity.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Arrays.asList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntity),
                    new TrackedEntityAttributeValue(trackedEntityAttribute1, trackedEntity))));
    when(enrollment.getTrackedEntity()).thenReturn(UID.generate());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntity);
    bundle.setStrategy(enrollment, TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, enrollment);

    assertNoErrors(reporter);
  }

  @Test
  void
      shouldReturnErrorWhenUpdatingEnrollmentAndMandatoryFieldIsNotPresentInEnrollmentOrInTrackedEntityOrInDB() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE))
            .value("value")
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(trackedEntity.getTrackedEntityAttributeValues()).thenReturn(Set.of());
    when(enrollment.getTrackedEntity()).thenReturn(UID.generate());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntity);
    bundle.setStrategy(enrollment, TrackerImportStrategy.UPDATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1018);
  }

  @Test
  void
      shouldReturnNoErrorWhenUpdatingEnrollmentAndMandatoryFieldIsNotPresentInEnrollmentButPresentInTrackedEntity() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE))
            .value("value")
            .build();

    Attribute attribute1 =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE_1))
            .value("value")
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(trackedEntity.getTrackedEntityAttributeValues()).thenReturn(Set.of());
    when(enrollment.getTrackedEntity()).thenReturn(UID.generate());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntity);
    bundle.setStrategy(enrollment, TrackerImportStrategy.UPDATE);
    bundle.setTrackedEntities(
        List.of(
            org.hisp.dhis.tracker.imports.domain.TrackedEntity.builder()
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
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE))
            .value("value")
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(trackedEntity.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                List.of(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntity),
                    new TrackedEntityAttributeValue(trackedEntityAttribute1, trackedEntity))));
    when(enrollment.getTrackedEntity()).thenReturn(UID.generate());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntity);
    bundle.setStrategy(enrollment, TrackerImportStrategy.UPDATE);

    validator.validate(reporter, bundle, enrollment);

    assertNoErrors(reporter);
  }

  @Test
  void shouldFailValidationWhenCreatingEnrollmentAndValueIsNotPresentAndAttributeIsMandatory() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE))
            .value("value")
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(trackedEntity.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Collections.singletonList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntity))));
    when(enrollment.getTrackedEntity()).thenReturn(UID.generate());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntity);
    bundle.setStrategy(enrollment, TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1018);
  }

  @Test
  void shouldFailValidationWhenCreatingEnrollmentAndValueIsNullAndAttributeIsMandatory() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE))
            .value("value")
            .build();
    Attribute attribute1 =
        Attribute.builder().attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE_1)).build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Arrays.asList(attribute, attribute1));
    when(trackedEntity.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Arrays.asList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntity),
                    new TrackedEntityAttributeValue(trackedEntityAttribute1, trackedEntity))));
    when(enrollment.getTrackedEntity()).thenReturn(UID.generate());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntity);
    bundle.setStrategy(enrollment, TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1076);
  }

  @Test
  void shouldFailValidationWhenUpdatingEnrollmentAndValueIsNullAndAttributeIsMandatory() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE))
            .value("value")
            .build();
    Attribute attribute1 =
        Attribute.builder().attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE_1)).build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Arrays.asList(attribute, attribute1));
    when(trackedEntity.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Arrays.asList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntity),
                    new TrackedEntityAttributeValue(trackedEntityAttribute1, trackedEntity))));
    when(enrollment.getTrackedEntity()).thenReturn(UID.generate());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntity);
    bundle.setStrategy(enrollment, TrackerImportStrategy.UPDATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1076);
  }

  @Test
  void shouldPassValidationWhenValueIsNullAndAttributeIsNotMandatory() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE))
            .value("value")
            .build();
    Attribute attribute1 =
        Attribute.builder().attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE_1)).build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, false)));

    when(enrollment.getAttributes()).thenReturn(Arrays.asList(attribute, attribute1));
    when(trackedEntity.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Arrays.asList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntity),
                    new TrackedEntityAttributeValue(trackedEntityAttribute1, trackedEntity))));
    when(enrollment.getTrackedEntity()).thenReturn(UID.generate());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntity);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailValidationWhenValueIsInvalidPercentage() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE))
            .value("value")
            .build();
    Attribute attribute1 =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE_P))
            .value("1000")
            .build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttributeP, false, false)));

    when(enrollment.getAttributes()).thenReturn(Arrays.asList(attribute, attribute1));
    when(trackedEntity.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Arrays.asList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntity),
                    new TrackedEntityAttributeValue(trackedEntityAttributeP, trackedEntity))));
    when(enrollment.getTrackedEntity()).thenReturn(UID.generate());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntity);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1007);
  }

  @Test
  void
      shouldFailValidationWhenCreatingEnrollmentAndValueIsNullAndAttributeIsMandatoryAndAttributeNotExistsInTei() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE))
            .value("value")
            .build();
    Attribute attribute1 =
        Attribute.builder().attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE_1)).build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Arrays.asList(attribute, attribute1));
    when(trackedEntity.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Collections.singletonList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntity))));
    when(enrollment.getTrackedEntity()).thenReturn(UID.generate());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntity);
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
            .attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE))
            .value("value")
            .build();
    Attribute attribute1 =
        Attribute.builder().attribute(MetadataIdentifier.ofUid(TRACKED_ATTRIBUTE_1)).build();

    when(program.getProgramAttributes())
        .thenReturn(
            Arrays.asList(
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute, false, true),
                new ProgramTrackedEntityAttribute(program, trackedEntityAttribute1, false, true)));

    when(enrollment.getAttributes()).thenReturn(Arrays.asList(attribute, attribute1));
    when(trackedEntity.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Collections.singletonList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntity))));
    when(enrollment.getTrackedEntity()).thenReturn(UID.generate());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntity);
    bundle.setStrategy(enrollment, TrackerImportStrategy.UPDATE);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1076);
  }

  @Test
  void shouldFailValidationWhenAttributeIsNotPresentInDB() {
    Attribute attribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid("invalidAttribute"))
            .value("value")
            .build();

    when(program.getProgramAttributes()).thenReturn(Collections.emptyList());

    when(enrollment.getAttributes()).thenReturn(Collections.singletonList(attribute));
    when(trackedEntity.getTrackedEntityAttributeValues())
        .thenReturn(
            new HashSet<>(
                Collections.singletonList(
                    new TrackedEntityAttributeValue(trackedEntityAttribute, trackedEntity))));
    when(enrollment.getTrackedEntity()).thenReturn(UID.generate());
    when(preheat.getTrackedEntity(enrollment.getTrackedEntity())).thenReturn(trackedEntity);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1006);
  }

  @ParameterizedTest(name = "Should pass when format={0}")
  @MethodSource("validImageFormats")
  void shouldPassValidationWhenImageFileResourceHasValidFormat(String format) {
    runImageValidationTest(format, true);
  }

  @ParameterizedTest(name = "Should fail when format={0}")
  @MethodSource("invalidImageFormats")
  void shouldFailValidationWhenImageFileResourceHasInvalidFormat(String format) {
    runImageValidationTest(format, false);
  }

  private void runImageValidationTest(String format, boolean shouldPass) {
    String fileResUid = CodeGenerator.generateUid();
    TrackedEntityAttribute imageAttr =
        new TrackedEntityAttribute(
            "profilePic", "User profile image", ValueType.IMAGE, false, false);
    imageAttr.setUid("imageAttrUid");

    Attribute imageAttribute =
        Attribute.builder()
            .attribute(MetadataIdentifier.ofUid(imageAttr.getUid()))
            .value(fileResUid)
            .build();

    FileResource fileResource = mock(FileResource.class);
    when(fileResource.getUid()).thenReturn(fileResUid);
    when(fileResource.getFormat()).thenReturn(format);
    when(fileResource.getContentMd5()).thenReturn("file");
    when(preheat.get(FileResource.class, fileResUid)).thenReturn(fileResource);

    when(program.getProgramAttributes())
        .thenReturn(
            java.util.Collections.singletonList(
                new ProgramTrackedEntityAttribute(program, imageAttr, false, false)));

    when(preheat.getTrackedEntityAttribute(MetadataIdentifier.ofUid("imageAttrUid")))
        .thenReturn(imageAttr);

    when(enrollment.getAttributes())
        .thenReturn(java.util.Collections.singletonList(imageAttribute));
    bundle.setStrategy(enrollment, TrackerImportStrategy.CREATE);

    validator.validate(reporter, bundle, enrollment);

    if (shouldPass) {
      assertNoErrors(reporter);
    } else {
      assertHasError(reporter, enrollment, ValidationCode.E1007);
    }
  }
}
