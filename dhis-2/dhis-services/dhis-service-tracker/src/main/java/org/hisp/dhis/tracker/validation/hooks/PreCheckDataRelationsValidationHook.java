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
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
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
        TrackerImportValidationContext context = reporter.getValidationContext();

        Program program = context.getProgram( enrollment.getProgram() );

        if ( !program.isRegistration() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1014 )
                .addArg( program ) );
        }

        TrackedEntityInstance tei = context.getTrackedEntityInstance( enrollment.getTrackedEntity() );

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
//        ProgramInstance programInstance = context.getProgramInstance(  enrollment.getEnrollment() );
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
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( event );

        Program program = context.getProgram( event.getProgram() );

        if ( program.isRegistration() )
        {
            if ( context.getTrackedEntityInstance( event.getTrackedEntity() ) == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1036 )
                    .addArg( event ) );
            }

            if ( strategy.isCreate() )
            {
                ProgramInstance programInstance = context.getProgramInstance( event.getEnrollment() );
                ProgramStage programStage = context.getProgramStage( event.getProgramStage() );

                if ( programStage != null && programInstance != null
                    && !programStage.getRepeatable()
                    && programInstance.hasProgramStageInstance( programStage ) )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1039 ) );
                }
            }
        }

        validateEventCategoryCombo( reporter, event, program );
    }

    protected void validateEventCategoryCombo( ValidationErrorReporter reporter,
        Event event, Program program )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        // if event has "attribute option combo" set only, fetch the aoc directly
        boolean aocEmpty = StringUtils.isEmpty( event.getAttributeOptionCombo() );
        boolean acoEmpty = StringUtils.isEmpty( event.getAttributeCategoryOptions() );

        CategoryOptionCombo categoryOptionCombo = (CategoryOptionCombo) reporter.getValidationContext().getBundle()
            .getPreheat().getDefaults().get( CategoryOptionCombo.class );

        if ( !aocEmpty && acoEmpty )
        {
            categoryOptionCombo = context.getCategoryOptionCombo( event.getAttributeOptionCombo() );
        }
        else if ( !aocEmpty && !acoEmpty && program.getCategoryCombo() != null )
        {
            String attributeCategoryOptions = event.getAttributeCategoryOptions();
            CategoryCombo categoryCombo = program.getCategoryCombo();
            String cacheKey = attributeCategoryOptions + categoryCombo.getUid();

            Optional<String> cachedEventAOCProgramCC = reporter.getValidationContext()
                .getCachedEventAOCProgramCC( cacheKey );

            if ( cachedEventAOCProgramCC.isPresent() )
            {
                categoryOptionCombo = context.getCategoryOptionCombo( cachedEventAOCProgramCC.get() );
            }
            else
            {
                Set<String> categoryOptions = TextUtils
                    .splitToArray( attributeCategoryOptions, TextUtils.SEMICOLON );

                categoryOptionCombo = resolveCategoryOptionCombo( reporter,
                    categoryCombo, categoryOptions );

                reporter.getValidationContext().putCachedEventAOCProgramCC( cacheKey,
                    categoryOptionCombo != null ? categoryOptionCombo.getUid() : null );
            }
        }

        if ( categoryOptionCombo == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1115 )
                .addArg( event.getAttributeOptionCombo() ) );
        }
        else
        {
            reporter.getValidationContext()
                .cacheEventCategoryOptionCombo( event.getEvent(), categoryOptionCombo.getUid() );
        }
    }

    private CategoryOptionCombo resolveCategoryOptionCombo( ValidationErrorReporter reporter,
        CategoryCombo programCategoryCombo, Set<String> attributeCategoryOptions )
    {
        Set<CategoryOption> categoryOptions = new HashSet<>();

        for ( String uid : attributeCategoryOptions )
        {
            CategoryOption categoryOption = reporter.getValidationContext().getCategoryOption( uid );
            if ( categoryOption == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1116 )
                    .addArg( uid ) );
                return null;
            }

            categoryOptions.add( categoryOption );
        }

        CategoryOptionCombo attrOptCombo = categoryService
            .getCategoryOptionCombo( programCategoryCombo, categoryOptions );

        if ( attrOptCombo == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1117 )
                .addArg( programCategoryCombo )
                .addArg( categoryOptions ) );
        }
        else
        {
            TrackerPreheat preheat = reporter.getValidationContext().getBundle().getPreheat();
            TrackerIdentifier identifier = preheat.getIdentifiers().getCategoryOptionComboIdScheme();
            preheat.put( identifier, attrOptCombo );
        }

        return attrOptCombo;
    }
}
