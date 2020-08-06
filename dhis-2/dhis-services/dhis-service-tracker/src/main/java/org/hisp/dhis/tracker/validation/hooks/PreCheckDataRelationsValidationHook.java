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
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
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
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckDataRelationsValidationHook
    extends AbstractTrackerDtoValidationHook
{
    private final ProgramInstanceService programInstanceService;

    private final CategoryService categoryService;

    public PreCheckDataRelationsValidationHook( TrackedEntityAttributeService teAttrService,
        ProgramInstanceService programInstanceService, CategoryService categoryService )
    {
        super( teAttrService );

        checkNotNull( categoryService );

        this.programInstanceService = programInstanceService;
        this.categoryService = categoryService;
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
                validateHasEnrollments( reporter, event );

                validateNotMultipleEvents( reporter, event );
            }
        }

        validateEventCategoryCombo( reporter, event, program );
    }

    private void validateHasEnrollments( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext ctx = reporter.getValidationContext();
        Program program = ctx.getProgram( event.getProgram() );

        if ( program.isRegistration() )
        {
            ProgramInstance programInstance = ctx.getProgramInstance( event.getEnrollment() );

            if ( programInstance == null )
            {
                TrackedEntityInstance tei = ctx.getTrackedEntityInstance( event.getTrackedEntity() );

                List<ProgramInstance> programInstances = ctx.getEventToProgramInstancesMap()
                    .getOrDefault( event.getUid(), new ArrayList<>() );

                final int count = programInstances.size();

                if ( count == 0 )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1037 )
                        .addArg( tei )
                        .addArg( program ) );
                }
                else if ( count > 1 )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1038 )
                        .addArg( tei )
                        .addArg( program ) );
                }
                else
                {
                    // FIXME: we probably need to take in consideration the idScheme
                    event.setEnrollment( programInstances.get( 0 ).getUid() );
                }
            }
        }
        else
        {
            User user = ctx.getBundle().getUser();

            ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
            params.setProgram( program );
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
            params.setUser( user );

            params.setTrackedEntityInstance( null );

            int count = programInstanceService.countProgramInstances( params );

            if ( count > 1 )
            {
                // TODO: this also needs to be changed to match original code.
                reporter.addError( newReport( TrackerErrorCode.E1040 ).addArg( program ) );
            }
        }
    }

    private void validateNotMultipleEvents( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        ProgramInstance programInstance = context.getProgramInstance( event.getEnrollment() );
        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );

        if ( programStage != null && programInstance != null
            && !programStage.getRepeatable()
            && programInstance.hasProgramStageInstance( programStage ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1039 ).addArg( programStage ) );
        }
    }

    //TODO: This method needs some love and care, the logic here is very hard to read.
    protected void validateEventCategoryCombo( ValidationErrorReporter reporter,
        Event event, Program program )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerPreheat preheat = reporter.getValidationContext().getBundle().getPreheat();

        // if event has "attribute option combo" set only, fetch the aoc directly
        boolean optionComboIsEmpty = StringUtils.isEmpty( event.getAttributeOptionCombo() );
        boolean categoryOptionsIsEmpty = StringUtils.isEmpty( event.getAttributeCategoryOptions() );

        CategoryOptionCombo categoryOptionCombo = null;

        if ( !optionComboIsEmpty && categoryOptionsIsEmpty )
        {
            categoryOptionCombo = context.getCategoryOptionCombo( event.getAttributeOptionCombo() );
        }
        else if ( !optionComboIsEmpty && program.getCategoryCombo() != null )
        {
            categoryOptionCombo = resolveCategoryOptions( reporter, event, program, context );
        }

        categoryOptionCombo = getDefault( event, preheat, optionComboIsEmpty, categoryOptionCombo );

        if ( categoryOptionCombo == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1115 )
                .addArg( event.getAttributeOptionCombo() ) );
        }
        else
        {
            reporter.getValidationContext()
                .cacheEventCategoryOptionCombo( event.getUid(), categoryOptionCombo );

        }
    }

    private CategoryOptionCombo resolveCategoryOptions( ValidationErrorReporter reporter, Event event, Program program,
        TrackerImportValidationContext context )
    {
        CategoryOptionCombo categoryOptionCombo;
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
        return categoryOptionCombo;
    }

    private CategoryOptionCombo getDefault( Event event, TrackerPreheat preheat, boolean aocIsEmpty,
        CategoryOptionCombo categoryOptionCombo )
    {
        if ( categoryOptionCombo == null )
        {
            CategoryOptionCombo defaultCategoryCombo = (CategoryOptionCombo) preheat.getDefaults()
                .get( CategoryOptionCombo.class );

            if ( defaultCategoryCombo != null && !aocIsEmpty )
            {
                String uid = defaultCategoryCombo.getUid();
                if ( uid.equals( event.getAttributeOptionCombo() ) )
                {
                    categoryOptionCombo = defaultCategoryCombo;
                }
            }
            else if ( defaultCategoryCombo != null )
            {
                categoryOptionCombo = defaultCategoryCombo;
            }
        }

        return categoryOptionCombo;
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
