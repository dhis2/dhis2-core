/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
import static org.hisp.dhis.category.CategoryOption.DEFAULT_NAME;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1058;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1059;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.mock.MockI18nFormat;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.test.TrackerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryOptValidatorTest extends TrackerTestBase {

  @Mock private I18nManager i18nManager;

  @Mock private TrackerPreheat preheat;

  private TrackerBundle bundle;

  private static final I18nFormat I18N_FORMAT = new MockI18nFormat();

  private CategoryOptValidator validator;

  private CategoryOption catOption;

  private CategoryCombo catCombo;

  private CategoryOptionCombo attOptionCombo;

  private CategoryCombo defaultCatCombo;

  private CategoryOptionCombo defaultCatOptionCombo;

  private Program program;

  private Enrollment enrollment;

  private Reporter reporter;

  private static final Date ONE_YEAR_BEFORE_ENROLLMENT = getDate(2020, 1, 1);

  private static final Instant ENROLLMENT_INSTANT = getDate(2021, 1, 1).toInstant();

  private static final Date ONE_YEAR_AFTER_ENROLLMENT = getDate(2022, 1, 1);

  @BeforeEach
  public void setUp() {
    initServices();

    validator = new CategoryOptValidator(i18nManager);

    catOption = createCategoryOption('A');
    Category category = createCategory('A', catOption);
    catCombo = createCategoryCombo('A', category);
    attOptionCombo = createCategoryOptionCombo(catCombo, catOption);

    defaultCatCombo = new CategoryCombo();
    defaultCatCombo.setName(DEFAULT_CATEGORY_COMBO_NAME);
    CategoryOption defaultCatOption = new CategoryOption();
    defaultCatOption.setName(DEFAULT_NAME);
    defaultCatOptionCombo = createCategoryOptionCombo(defaultCatCombo, defaultCatOption);

    program = createProgram('A');
    program.setCategoryCombo(defaultCatCombo);
    program.setEnrollmentCategoryCombo(catCombo);

    enrollment =
        Enrollment.builder()
            .enrollment(UID.generate())
            .program(MetadataIdentifier.ofUid(program))
            .attributeOptionCombo(MetadataIdentifier.ofUid(attOptionCombo))
            .enrolledAt(ENROLLMENT_INSTANT)
            .build();

    bundle = TrackerBundle.builder().preheat(preheat).build();

    when(preheat.getProgram(MetadataIdentifier.ofUid(program))).thenReturn(program);
    when(i18nManager.getI18nFormat()).thenReturn(I18N_FORMAT);

    reporter = new Reporter(TrackerIdSchemeParams.builder().build());
  }

  @Test
  void shouldPassWhenProgramEnrollmentCategoryComboIsDefault() {
    program.setCategoryCombo(catCombo);
    program.setEnrollmentCategoryCombo(defaultCatCombo);
    when(preheat.getDefault(CategoryOptionCombo.class)).thenReturn(defaultCatOptionCombo);
    catOption.setEndDate(ONE_YEAR_BEFORE_ENROLLMENT);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenCategoryOptionHasNoDates() {
    when(preheat.getCategoryOptionCombo(enrollment.getAttributeOptionCombo()))
        .thenReturn(attOptionCombo);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldPassWhenEnrolledAtIsBetweenCategoryOptionDates() {
    when(preheat.getCategoryOptionCombo(enrollment.getAttributeOptionCombo()))
        .thenReturn(attOptionCombo);
    catOption.setStartDate(ONE_YEAR_BEFORE_ENROLLMENT);
    catOption.setEndDate(ONE_YEAR_AFTER_ENROLLMENT);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void shouldFailWhenEnrolledAtIsBeforeCategoryOptionStartDate() {
    when(preheat.getCategoryOptionCombo(enrollment.getAttributeOptionCombo()))
        .thenReturn(attOptionCombo);
    catOption.setStartDate(ONE_YEAR_AFTER_ENROLLMENT);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1058);
  }

  @Test
  void shouldFailWhenEnrolledAtIsAfterCategoryOptionEndDate() {
    when(preheat.getCategoryOptionCombo(enrollment.getAttributeOptionCombo()))
        .thenReturn(attOptionCombo);
    catOption.setEndDate(ONE_YEAR_BEFORE_ENROLLMENT);

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1059);
  }

  @Test
  void shouldPassWhenEnrolledAtIsWithinOpenDaysAfterCoEndDate() {
    when(preheat.getCategoryOptionCombo(enrollment.getAttributeOptionCombo()))
        .thenReturn(attOptionCombo);
    catOption.setEndDate(ONE_YEAR_BEFORE_ENROLLMENT);
    program.setOpenDaysAfterCoEndDate(400);

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }
}
