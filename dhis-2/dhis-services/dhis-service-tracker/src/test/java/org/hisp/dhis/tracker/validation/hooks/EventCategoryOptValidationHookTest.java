/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
import static org.hisp.dhis.category.CategoryOption.DEFAULT_NAME;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.ValidationMode.FULL;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1055;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1056;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1057;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.mock.MockI18nFormat;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Jim Grace
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class EventCategoryOptValidationHookTest extends DhisConvenienceTest
{

    @Mock
    private I18nManager i18nManager;

    @Mock
    private TrackerImportValidationContext validationContext;

    private static final I18nFormat I18N_FORMAT = new MockI18nFormat();

    private EventCategoryOptValidationHook hook;

    private CategoryOption catOption;

    private Category category;

    private CategoryCombo catCombo;

    private CategoryOptionCombo attOptionCombo;

    private CategoryOption defaultCatOption;

    private CategoryCombo defaultCatCombo;

    private CategoryOptionCombo defaultCatOptionCombo;

    private Program program;

    private Event event;

    private ValidationErrorReporter reporter;

    private final Date ONE_YEAR_BEFORE_EVENT = getDate( 2020, 1, 1 );

    private final Instant EVENT_INSTANT = getDate( 2021, 1, 1 ).toInstant();

    private final Date ONE_YEAR_AFTER_EVENT = getDate( 2022, 1, 1 );

    private final int OPEN_DAYS_AFTER_CO_END_DATE = 400;

    @BeforeEach
    public void setUp()
    {
        initServices();

        hook = new EventCategoryOptValidationHook( i18nManager );

        catOption = createCategoryOption( 'A' );

        category = createCategory( 'A', catOption );

        catCombo = createCategoryCombo( 'A', category );

        attOptionCombo = createCategoryOptionCombo( catCombo, catOption );

        defaultCatCombo = new CategoryCombo();
        defaultCatCombo.setName( DEFAULT_CATEGORY_COMBO_NAME );

        defaultCatOption = new CategoryOption();
        defaultCatOption.setName( DEFAULT_NAME );

        defaultCatOptionCombo = createCategoryOptionCombo( defaultCatCombo, defaultCatOption );

        program = createProgram( 'A' );
        program.setCategoryCombo( catCombo );

        event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setProgram( program.getUid() );
        event.setOccurredAt( EVENT_INSTANT );

        User user = createUser( 'A' );

        TrackerBundle bundle = TrackerBundle.builder()
            .user( user )
            .validationMode( FULL )
            .build();

        when( validationContext.getBundle() ).thenReturn( bundle );

        when( validationContext.getProgram( program.getUid() ) )
            .thenReturn( program );

        when( i18nManager.getI18nFormat() )
            .thenReturn( I18N_FORMAT );

        reporter = new ValidationErrorReporter( validationContext, event );
    }

    @Test
    void testDefaultCoc()
    {
        // given
        program.setCategoryCombo( defaultCatCombo );

        // when
        when( validationContext.getCachedEventCategoryOptionCombo( any() ) )
            .thenReturn( defaultCatOptionCombo );

        hook.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void testDefaultCocWithNonDefaultCatCombo()
    {
        // given
        program.setCategoryCombo( catCombo );

        // when
        when( validationContext.getCachedEventCategoryOptionCombo( any() ) )
            .thenReturn( defaultCatOptionCombo );

        hook.validateEvent( reporter, event );

        // then
        hasTrackerError( reporter, E1055, EVENT, event.getUid() );
    }

    @Test
    void testNoCategoryOptionDates()
    {
        // when
        when( validationContext.getCachedEventCategoryOptionCombo( any() ) )
            .thenReturn( attOptionCombo );

        hook.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void testBetweenCategoryOptionDates()
    {
        // given
        catOption.setStartDate( ONE_YEAR_BEFORE_EVENT );
        catOption.setEndDate( ONE_YEAR_AFTER_EVENT );

        // when
        when( validationContext.getCachedEventCategoryOptionCombo( any() ) )
            .thenReturn( attOptionCombo );

        hook.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void testBeforeCategoryOptionStart()
    {
        // given
        catOption.setStartDate( ONE_YEAR_AFTER_EVENT );

        // when
        when( validationContext.getCachedEventCategoryOptionCombo( any() ) )
            .thenReturn( attOptionCombo );

        hook.validateEvent( reporter, event );

        // then
        hasTrackerError( reporter, E1056, EVENT, event.getUid() );
    }

    @Test
    void testAfterCategoryOptionEnd()
    {
        // given
        catOption.setEndDate( ONE_YEAR_BEFORE_EVENT );

        // when
        when( validationContext.getCachedEventCategoryOptionCombo( any() ) )
            .thenReturn( attOptionCombo );

        hook.validateEvent( reporter, event );

        // then
        hasTrackerError( reporter, E1057, EVENT, event.getUid() );
    }

    @Test
    void testBeforeOpenDaysAfterCoEndDate()
    {
        // given
        catOption.setEndDate( ONE_YEAR_BEFORE_EVENT );
        program.setOpenDaysAfterCoEndDate( OPEN_DAYS_AFTER_CO_END_DATE );

        // when
        when( validationContext.getCachedEventCategoryOptionCombo( any() ) )
            .thenReturn( attOptionCombo );

        hook.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
    }
}
