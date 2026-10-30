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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.test.TrackerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryOptionComboValidatorTest extends TrackerTestBase {

  private static final String PROGRAM_UID = "PROGRAM_UID";

  private static final String ORG_UNIT_ID = "ORG_UNIT_ID";

  private static final String TE_TYPE_ID = "TE_TYPE_ID";

  private static final UID ENROLLMENT_ID = UID.generate();

  private org.hisp.dhis.tracker.imports.validation.validator.enrollment.CategoryOptionComboValidator
      validator;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    validator =
        new org.hisp.dhis.tracker.imports.validation.validator.enrollment
            .CategoryOptionComboValidator();

    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void shouldSucceedWhenEnrollmentAOCNotSetAndProgramHasDefaultCC() {
    OrganisationUnit orgUnit = organisationUnit(ORG_UNIT_ID);
    Program program = programWithRegistration(PROGRAM_UID, orgUnit);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);

    setUpDefaultCategoryCombo(program);
    Enrollment enrollment = enrollmentBuilder(program, null);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenWhenEnrollmentAOCNotSetAndProgramHasNonDefaultCC() {
    OrganisationUnit orgUnit = setupOrgUnit();
    Program program = setupProgram(orgUnit);
    program.setEnrollmentCategoryCombo(categoryCombo());
    Enrollment enrollment = enrollmentBuilder(program, null);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1055);
  }

  @Test
  void shouldSucceedWhenEnrollmentAOCIsSetAndProgramHasNoCC() {
    OrganisationUnit orgUnit = setupOrgUnit();
    Program program = setupProgram(orgUnit);
    program.setEnrollmentCategoryCombo(null);

    CategoryCombo cc = categoryCombo();
    program.setEnrollmentCategoryCombo(cc);
    CategoryOptionCombo aoc = firstCategoryOptionCombo(cc);
    when(preheat.getCategoryOptionCombo(MetadataIdentifier.ofUid(aoc))).thenReturn(aoc);

    Enrollment enrollment = enrollmentBuilder(program, aoc.getUid());

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldSucceedWhenEnrollmentAOCIsSetAndProgramHasDefaultCC() {
    OrganisationUnit orgUnit = setupOrgUnit();
    Program program = setupProgram(orgUnit);

    CategoryCombo defaultCC = defaultCategoryCombo();
    program.setEnrollmentCategoryCombo(defaultCC);
    CategoryOptionCombo defaultAOC = firstCategoryOptionCombo(defaultCC);
    when(preheat.getCategoryOptionCombo(MetadataIdentifier.ofUid(defaultAOC)))
        .thenReturn(defaultAOC);

    Enrollment enrollment = enrollmentBuilder(program, defaultAOC.getUid());

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenEnrollmentAOCIsSetProgramHasDefaultCCAndAOCIsNotFound() {
    OrganisationUnit orgUnit = setupOrgUnit();
    Program program = setupProgram(orgUnit);
    program.setEnrollmentCategoryCombo(defaultCategoryCombo());

    String unknownAocId = CodeGenerator.generateUid();
    when(preheat.getCategoryOptionCombo(MetadataIdentifier.ofUid(unknownAocId))).thenReturn(null);

    Enrollment enrollment = enrollmentBuilder(program, unknownAocId);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1115);
  }

  @Test
  void shouldFailWhenEnrollmentAOCIsSetProgramHasNonDefaultCCAndAOCIsNotFound() {
    OrganisationUnit orgUnit = setupOrgUnit();
    Program program = setupProgram(orgUnit);
    program.setEnrollmentCategoryCombo(categoryCombo());

    String unknownAocId = CodeGenerator.generateUid();
    when(preheat.getCategoryOptionCombo(MetadataIdentifier.ofUid(unknownAocId))).thenReturn(null);

    Enrollment enrollment = enrollmentBuilder(program, unknownAocId);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1115);
  }

  @Test
  void shouldFailWhenEnrollmentAOCIsSetToNonDefaultAOCNotInProgramCC() {
    OrganisationUnit orgUnit = setupOrgUnit();
    Program program = setupProgram(orgUnit);
    program.setCategoryCombo(categoryCombo('A'));

    CategoryOptionCombo aoc = firstCategoryOptionCombo(categoryCombo('B'));
    when(preheat.getCategoryOptionCombo(MetadataIdentifier.ofUid(aoc))).thenReturn(aoc);

    Enrollment enrollment = enrollmentBuilder(program, aoc.getUid());

    validator.validate(reporter, bundle, enrollment);

    assertEquals(1, reporter.getErrors().size());
    assertTrue(
        reporter.hasErrorReport(
            r ->
                r.getErrorCode() == ValidationCode.E1130
                    && r.getMessage().contains(aoc.getUid())
                    && r.getMessage().contains(program.getEnrollmentCategoryCombo().getUid())));
  }

  @Test
  void shouldFailWhenEnrollmentAOCIsSetToDefaultAOCNotInProgramCC() {
    OrganisationUnit orgUnit = setupOrgUnit();
    Program program = setupProgram(orgUnit);
    program.setEnrollmentCategoryCombo(categoryCombo('A'));

    CategoryOptionCombo defaultAOC = firstCategoryOptionCombo(defaultCategoryCombo());
    when(preheat.getCategoryOptionCombo(MetadataIdentifier.ofUid(defaultAOC)))
        .thenReturn(defaultAOC);

    Enrollment enrollment = enrollmentBuilder(program, defaultAOC.getUid());

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, ValidationCode.E1055);
  }

  @Test
  void shouldFailWhenEnrollmentAOCIsSetToAOCNotInProgramCCAndProgramHasDefaultCC() {
    OrganisationUnit orgUnit = setupOrgUnit();
    Program program = setupProgram(orgUnit);
    program.setCategoryCombo(defaultCategoryCombo());

    CategoryOptionCombo aoc = firstCategoryOptionCombo(categoryCombo('B'));
    when(preheat.getCategoryOptionCombo(MetadataIdentifier.ofUid(aoc))).thenReturn(aoc);

    Enrollment enrollment = enrollmentBuilder(program, aoc.getUid());

    validator.validate(reporter, bundle, enrollment);

    assertEquals(1, reporter.getErrors().size());
    assertTrue(
        reporter.hasErrorReport(
            r ->
                r.getErrorCode() == ValidationCode.E1130
                    && r.getMessage().contains(aoc.getUid())
                    && r.getMessage().contains(program.getEnrollmentCategoryCombo().getUid())));
  }

  @Test
  void shouldFailWhenEnrollmentAOCIsSetButNotPartOfProgramCC() {
    OrganisationUnit orgUnit = setupOrgUnit();
    Program program = setupProgram(orgUnit);
    program.setCategoryCombo(defaultCategoryCombo());

    CategoryCombo defaultCC = defaultCategoryCombo();
    program.setEnrollmentCategoryCombo(defaultCC);
    when(preheat.getCategoryOptionCombo(MetadataIdentifier.ofUid((String) null))).thenReturn(null);
    when(preheat.getDefault(CategoryOptionCombo.class)).thenReturn(null);

    Enrollment enrollment = enrollmentBuilder(program, null);

    validator.validate(reporter, bundle, enrollment);

    assertEquals(1, reporter.getErrors().size());
    assertTrue(
        reporter.hasErrorReport(
            r ->
                r.getErrorCode() == ValidationCode.E1129
                    && r.getMessage().contains(program.getEnrollmentCategoryCombo().getUid())));
  }

  private void setUpDefaultCategoryCombo(Program program) {
    CategoryCombo defaultCC = defaultCategoryCombo();
    program.setCategoryCombo(defaultCC);
    CategoryOptionCombo defaultAOC = firstCategoryOptionCombo(defaultCC);
    when(preheat.getDefault(CategoryOptionCombo.class)).thenReturn(defaultAOC);
  }

  private OrganisationUnit organisationUnit(String uid) {
    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    organisationUnit.setUid(uid);
    return organisationUnit;
  }

  private Program programWithRegistration(String uid, OrganisationUnit orgUnit) {
    return program(uid, ProgramType.WITH_REGISTRATION, 'A', orgUnit, trackedEntityType(TE_TYPE_ID));
  }

  private Program program(
      String uid,
      ProgramType type,
      char uniqueCharacter,
      OrganisationUnit orgUnit,
      TrackedEntityType teType) {
    Program program = createProgram(uniqueCharacter);
    program.setUid(uid);
    program.setProgramType(type);
    program.setOrganisationUnits(Sets.newHashSet(orgUnit));
    program.setTrackedEntityType(teType);
    program.setEnrollmentCategoryCombo(defaultCategoryCombo());
    return program;
  }

  private TrackedEntityType trackedEntityType(String uid) {
    return trackedEntityType(uid, 'A');
  }

  private TrackedEntityType trackedEntityType(String uid, char uniqueChar) {
    TrackedEntityType trackedEntityType = createTrackedEntityType(uniqueChar);
    trackedEntityType.setUid(uid);
    return trackedEntityType;
  }

  private Program setupProgram(OrganisationUnit orgUnit) {
    Program program = programWithRegistration(PROGRAM_UID, orgUnit);
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(program);

    return program;
  }

  private OrganisationUnit setupOrgUnit() {
    return organisationUnit(ORG_UNIT_ID);
  }

  private CategoryCombo defaultCategoryCombo() {
    CategoryOption co = new CategoryOption(CategoryOption.DEFAULT_NAME);
    co.setAutoFields();
    assertTrue(co.isDefault(), "tests rely on this CO being the default one");
    Category ca = createCategory('A', co);
    CategoryCombo cc = createCategoryCombo('A', ca);
    cc.setName(CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME);
    assertTrue(cc.isDefault(), "tests rely on this CC being the default one");
    cc.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    CategoryOptionCombo aoc = createCategoryOptionCombo(cc, co);
    aoc.setName(CategoryOptionCombo.DEFAULT_NAME);
    assertTrue(aoc.isDefault(), "tests rely on this AOC being the default one");
    cc.setOptionCombos(Sets.newHashSet(aoc));
    return cc;
  }

  private CategoryCombo categoryCombo() {
    return categoryCombo('A');
  }

  private CategoryCombo categoryCombo(char uniqueIdentifier) {
    CategoryOption co1 = createCategoryOption(uniqueIdentifier);
    CategoryOption co2 = createCategoryOption(uniqueIdentifier);
    Category ca = createCategory(uniqueIdentifier, co1, co2);
    CategoryCombo cc = createCategoryCombo(uniqueIdentifier, ca);
    cc.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    CategoryOptionCombo aoc1 = createCategoryOptionCombo(cc, co1);
    CategoryOptionCombo aoc2 = createCategoryOptionCombo(cc, co2);
    cc.setOptionCombos(Sets.newHashSet(aoc1, aoc2));
    return cc;
  }

  private CategoryOptionCombo firstCategoryOptionCombo(CategoryCombo categoryCombo) {
    assertNotNull(categoryCombo.getOptionCombos());
    assertFalse(categoryCombo.getOptionCombos().isEmpty());

    return categoryCombo.getSortedOptionCombos().get(0);
  }

  private Enrollment enrollmentBuilder(Program program, String categoryOptionCombo) {
    return Enrollment.builder()
        .enrollment(UID.generate())
        .program(MetadataIdentifier.ofUid(program))
        .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_ID))
        .attributeOptionCombo(MetadataIdentifier.ofUid(categoryOptionCombo))
        .enrollment(ENROLLMENT_ID)
        .build();
  }
}
