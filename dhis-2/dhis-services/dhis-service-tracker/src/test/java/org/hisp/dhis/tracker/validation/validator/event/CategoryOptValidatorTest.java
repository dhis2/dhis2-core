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
package org.hisp.dhis.tracker.validation.validator.event;

import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
import static org.hisp.dhis.category.CategoryOption.DEFAULT_NAME;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1056;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1057;
import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
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
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.Reporter;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jim Grace
 */
@ExtendWith( MockitoExtension.class )
class CategoryOptValidatorTest extends DhisConvenienceTest
{

    @Mock
    private I18nManager i18nManager;

    @Mock
    private TrackerPreheat preheat;

    private TrackerBundle bundle;

    private static final I18nFormat I18N_FORMAT = new MockI18nFormat();

    private CategoryOptValidator validator;

    private CategoryOption catOption;

    private Category category;

    private CategoryCombo catCombo;

    private CategoryOptionCombo attOptionCombo;

    private CategoryOption defaultCatOption;

    private CategoryCombo defaultCatCombo;

    private CategoryOptionCombo defaultCatOptionCombo;

    private Program program;

    private Event event;

    private Reporter reporter;

    private final Date ONE_YEAR_BEFORE_EVENT = getDate( 2020, 1, 1 );

    private final Instant EVENT_INSTANT = getDate( 2021, 1, 1 ).toInstant();

    private final Date ONE_YEAR_AFTER_EVENT = getDate( 2022, 1, 1 );

    @BeforeEach
    public void setUp()
    {
        initServices();

        validator = new CategoryOptValidator( i18nManager );

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
        event.setProgram( MetadataIdentifier.ofUid( program ) );
        event.setOccurredAt( EVENT_INSTANT );

        User user = makeUser( "A" );

        bundle = TrackerBundle.builder()
            .user( user )
            .preheat( preheat )
            .build();

        when( preheat.getProgram( MetadataIdentifier.ofUid( program ) ) )
            .thenReturn( program );
        when( i18nManager.getI18nFormat() ).thenReturn( I18N_FORMAT );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new Reporter( idSchemes );
    }

    @Test
    void testDefaultCoc()
    {
        when( preheat.getDefault( CategoryOptionCombo.class ) ).thenReturn( defaultCatOptionCombo );
        program.setCategoryCombo( defaultCatCombo );

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void testNoCategoryOptionDates()
    {
        when( preheat.getCategoryOptionCombo( event.getAttributeOptionCombo() ) ).thenReturn( attOptionCombo );

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void testBetweenCategoryOptionDates()
    {
        when( preheat.getCategoryOptionCombo( event.getAttributeOptionCombo() ) ).thenReturn( attOptionCombo );
        catOption.setStartDate( ONE_YEAR_BEFORE_EVENT );
        catOption.setEndDate( ONE_YEAR_AFTER_EVENT );

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void testBeforeCategoryOptionStart()
    {
        when( preheat.getCategoryOptionCombo( event.getAttributeOptionCombo() ) ).thenReturn( attOptionCombo );
        catOption.setStartDate( ONE_YEAR_AFTER_EVENT );

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, E1056 );
    }

    @Test
    void testAfterCategoryOptionEnd()
    {
        when( preheat.getCategoryOptionCombo( event.getAttributeOptionCombo() ) ).thenReturn( attOptionCombo );
        catOption.setEndDate( ONE_YEAR_BEFORE_EVENT );

        validator.validate( reporter, bundle, event );

        assertHasError( reporter, event, E1057 );
    }

    @Test
    void testBeforeOpenDaysAfterCoEndDate()
    {
        when( preheat.getCategoryOptionCombo( event.getAttributeOptionCombo() ) ).thenReturn( attOptionCombo );
        catOption.setEndDate( ONE_YEAR_BEFORE_EVENT );
        program.setOpenDaysAfterCoEndDate( 400 );

        validator.validate( reporter, bundle, event );

        assertIsEmpty( reporter.getErrors() );
    }
}
