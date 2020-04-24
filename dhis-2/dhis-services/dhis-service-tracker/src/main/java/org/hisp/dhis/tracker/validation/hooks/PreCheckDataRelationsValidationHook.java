package org.hisp.dhis.tracker.validation.hooks;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.google.api.client.util.Lists;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckDataRelationsValidationHook
    extends AbstractTrackerDtoValidationHook
{

    @Autowired
    private CategoryService categoryService;

    @Override
    public int getOrder()
    {
        return 4;
    }

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter,
        TrackedEntity trackedEntity )
    {
        // NOTHING TO DO HERE
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext validationContext = reporter.getValidationContext();
        TrackerImportStrategy strategy = validationContext.getStrategy( enrollment );
        TrackerBundle bundle = validationContext.getBundle();

        Program program = PreheatHelper.getProgram( bundle, enrollment.getProgram() );

        if ( !program.isRegistration() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1014 )
                .addArg( program ) );
        }

        TrackedEntityInstance tei = PreheatHelper.getTei( bundle, enrollment.getTrackedEntity() );

        if ( tei == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1068 )
                .addArg( enrollment.getTrackedEntity() ) );
        }

        if ( tei != null && program.getTrackedEntityType() != null
            && !program.getTrackedEntityType().equals( tei.getTrackedEntityType() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1022 )
                .addArg( tei )
                .addArg( program ) );
        }

        //TODO: This dont make sense
//        ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, enrollment.getEnrollment() );
//        if ( !bundle.getImportStrategy().isCreateOrCreateAndUpdate() && programInstance == null )
//        {
//            reporter.addError( newReport( TrackerErrorCode.E1015 )
//                .addArg( enrollment )
//                .addArg( enrollment.getEnrollment() ) );
//        }
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext validationContext = reporter.getValidationContext();
        TrackerImportStrategy strategy = validationContext.getStrategy( event );
        TrackerBundle bundle = validationContext.getBundle();

        Program program = PreheatHelper.getProgram( bundle, event.getProgram() );

        if ( program.isRegistration() )
        {
            if ( PreheatHelper.getTei( bundle, event.getTrackedEntity() ) == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1036 )
                    .addArg( event ) );
            }

            if ( strategy.isCreate() )
            {
                ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, event.getEnrollment() );
                ProgramStage programStage = PreheatHelper.getProgramStage( bundle, event.getProgramStage() );

                if ( programStage != null && programInstance != null &&
                    !programStage.getRepeatable() && programInstance.hasProgramStageInstance( programStage ) )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1039 ) );
                }
            }
        }

        checkCategoryCombo( reporter, bundle, event, program );
    }

    protected void checkCategoryCombo( ValidationErrorReporter reporter, TrackerBundle bundle,
        Event event, Program program )
    {

        boolean programHasCatCombo = program.getCategoryCombo() != null;

        // if event has "attribute option combo" set only, fetch the aoc directly
        boolean aocNotEmpty = StringUtils.isNotEmpty( event.getAttributeOptionCombo() );
        boolean acoNotEmpty = StringUtils.isNotEmpty( event.getAttributeCategoryOptions() );

        if ( aocNotEmpty && !acoNotEmpty )
        {
            CategoryOptionCombo coc = PreheatHelper.getCategoryOptionCombo( bundle, event.getAttributeOptionCombo() );
            if ( coc == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1115 )
                    .addArg( event.getAttributeOptionCombo() ) );
            }
            else
            {
                PreheatHelper.cacheEventCategoryOptionCombo( bundle, event, coc );
            }
        }
        // if event has no "attribute option combo", fetch the default aoc
        else if ( !aocNotEmpty && !acoNotEmpty && programHasCatCombo )
        {
            CategoryOptionCombo coc = (CategoryOptionCombo) bundle.getPreheat().getDefaults()
                .get( CategoryOptionCombo.class );

            if ( coc == null )
            {
                //TODO: is this possible? i.e no default COC?
                reporter.addError( newReport( TrackerErrorCode.E1115 )
                    .addArg( "<NO DEFAULT COC>" ) );
            }
            else
            {
                PreheatHelper.cacheEventCategoryOptionCombo( bundle, event, coc );
            }
        }
        else if ( aocNotEmpty && acoNotEmpty && programHasCatCombo )
        {
            CategoryOptionCombo coc = resolveCategoryOptionCombo( reporter, bundle,
                program.getCategoryCombo(), event.getAttributeCategoryOptions(),
                event.getAttributeOptionCombo() );
            if ( coc == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1115 )
                    .addArg( event.getAttributeOptionCombo() ) );
            }
            else
            {
                PreheatHelper.cacheEventCategoryOptionCombo( bundle, event, coc );
            }
        }//TODO: What about if event.getAttributeCategoryOptions() != acoNotEmpty BUT event.getAttributeOptionCombo() IS?
    }

    private CategoryOptionCombo resolveCategoryOptionCombo( ValidationErrorReporter reporter, TrackerBundle bundle,
        CategoryCombo categoryCombo, String attributeCategoryOptions, String attributeOptionCombo )
    {
        Set<String> opts = TextUtils.splitToArray( attributeCategoryOptions, TextUtils.SEMICOLON );

        CategoryOptionCombo attrOptCombo = null;

        if ( opts != null )
        {
            Set<CategoryOption> categoryOptions = new HashSet<>();

            for ( String uid : opts )
            {
                CategoryOption categoryOption = PreheatHelper.getCategoryOption( bundle, uid );
                if ( categoryOption == null )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1116 )
                        .addArg( uid ) );
                    return null;
                }

                categoryOptions.add( categoryOption );
            }

            List<String> options = Lists.newArrayList( opts );
            Collections.sort( options );

            attrOptCombo = categoryService.getCategoryOptionCombo( categoryCombo, categoryOptions );

            if ( attrOptCombo == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1117 )
                    .addArg( categoryCombo )
                    .addArg( categoryOptions ) );
                return null;
            }
        }
        else if ( attributeOptionCombo != null )
        {
            attrOptCombo = PreheatHelper.getCategoryOptionCombo( bundle, attributeOptionCombo );
        }

        if ( attrOptCombo == null )
        {
            attrOptCombo = (CategoryOptionCombo) bundle.getPreheat().getDefaults().get( CategoryOptionCombo.class );
        }

        if ( attrOptCombo == null )
        {
            //TODO: is this possible? i.e no default COC?
            reporter.addError( newReport( TrackerErrorCode.E1115 )
                .addArg( "<NO DEFAULT COC>" ) );
        }

        return attrOptCombo;
    }
}
