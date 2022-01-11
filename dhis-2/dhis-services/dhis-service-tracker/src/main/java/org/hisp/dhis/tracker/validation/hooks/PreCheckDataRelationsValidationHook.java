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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1014;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1022;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1029;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1033;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1041;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1079;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1089;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1115;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1116;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4012;
import static org.hisp.dhis.tracker.validation.hooks.RelationshipValidationUtils.getUidFromRelationshipItem;
import static org.hisp.dhis.tracker.validation.hooks.RelationshipValidationUtils.relationshipItemValueType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class PreCheckDataRelationsValidationHook
    extends AbstractTrackerDtoValidationHook
{
    private final CategoryService categoryService;

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackerImportValidationContext context,
        TrackedEntity trackedEntity )
    {
        // NOTHING TO DO HERE
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, TrackerImportValidationContext context,
        Enrollment enrollment )
    {
        Program program = context.getProgram( enrollment.getProgram() );
        OrganisationUnit organisationUnit = context.getOrganisationUnit( enrollment.getOrgUnit() );

        reporter.addErrorIf( () -> !program.isRegistration(), () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) enrollment).getUid() )
            .trackerType( ((TrackerDto) enrollment).getTrackerType() )
            .errorCode( E1014 )
            .addArg( program )
            .build() );

        if ( !programHasOrgUnit( program, organisationUnit, context.getProgramWithOrgUnitsMap() ) )
        {
            TrackerErrorReport error = TrackerErrorReport.builder()
                .uid( ((TrackerDto) enrollment).getUid() )
                .trackerType( ((TrackerDto) enrollment).getTrackerType() )
                .errorCode( E1041 )
                .addArg( context.getBundle().getIdentifier(), organisationUnit )
                .addArg( context.getBundle().getIdentifier(), program )
                .build();
            reporter.addError( error );
        }

        if ( program.getTrackedEntityType() != null
            && !program.getTrackedEntityType().getUid()
                .equals( getTrackedEntityTypeUidFromEnrollment( context, enrollment ) ) )
        {
            TrackerErrorReport error = TrackerErrorReport.builder()
                .uid( ((TrackerDto) enrollment).getUid() )
                .trackerType( ((TrackerDto) enrollment).getTrackerType() )
                .errorCode( E1022 )
                .addArg( enrollment.getTrackedEntity() )
                .addArg( context.getBundle().getIdentifier(), program )
                .build();
            reporter.addError( error );
        }
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, TrackerImportValidationContext context, Event event )
    {
        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );
        OrganisationUnit organisationUnit = context.getOrganisationUnit( event.getOrgUnit() );
        Program program = context.getProgram( event.getProgram() );

        if ( !program.getUid().equals( programStage.getProgram().getUid() ) )
        {
            TrackerErrorReport error = TrackerErrorReport.builder()
                .uid( event.getUid() )
                .trackerType( event.getTrackerType() )
                .errorCode( E1089 )
                .addArg( event )
                .addArg( context.getBundle().getIdentifier(), programStage )
                .addArg( context.getBundle().getIdentifier(), program )
                .build();
            reporter.addError( error );
        }

        if ( program.isRegistration() )
        {
            if ( StringUtils.isEmpty( event.getEnrollment() ) )
            {
                TrackerErrorReport error = TrackerErrorReport.builder()
                    .uid( event.getUid() )
                    .trackerType( event.getTrackerType() )
                    .errorCode( E1033 )
                    .addArg( event.getEvent() )
                    .build();
                reporter.addError( error );
            }
            else
            {
                String programUid = getEnrollmentProgramUidFromEvent( context, event );

                if ( !program.getUid().equals( programUid ) )
                {
                    TrackerErrorReport error = TrackerErrorReport.builder()
                        .uid( ((TrackerDto) event).getUid() )
                        .trackerType( ((TrackerDto) event).getTrackerType() )
                        .errorCode( E1079 )
                        .addArg( event )
                        .addArg( context.getBundle().getIdentifier(), program )
                        .addArg( event.getEnrollment() )
                        .build();
                    reporter.addError( error );
                }
            }
        }

        if ( !programHasOrgUnit( program, organisationUnit, context.getProgramWithOrgUnitsMap() ) )
        {
            TrackerErrorReport error = TrackerErrorReport.builder()
                .uid( event.getUid() )
                .trackerType( event.getTrackerType() )
                .errorCode( E1029 )
                .addArg( context.getBundle().getIdentifier(), organisationUnit )
                .addArg( context.getBundle().getIdentifier(), program )
                .build();
            reporter.addError( error );
        }

        validateEventCategoryCombo( reporter, context, event, program );
    }

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, TrackerImportValidationContext context,
        Relationship relationship )
    {
        validateRelationshipReference( reporter, context, relationship, relationship.getFrom() );
        validateRelationshipReference( reporter, context, relationship, relationship.getTo() );
    }

    private void validateRelationshipReference( ValidationErrorReporter reporter, TrackerImportValidationContext ctx,
        Relationship relationship,
        RelationshipItem item )
    {
        Optional<String> uid = getUidFromRelationshipItem( item );
        TrackerType trackerType = relationshipItemValueType( item );

        if ( TRACKED_ENTITY.equals( trackerType ) )
        {
            if ( uid.isPresent() && !ValidationUtils.trackedEntityInstanceExist( ctx, uid.get() ) )
            {
                TrackerErrorReport error = TrackerErrorReport.builder()
                    .uid( ((TrackerDto) relationship).getUid() )
                    .trackerType( ((TrackerDto) relationship).getTrackerType() )
                    .errorCode( E4012 )
                    .addArg( trackerType.getName() )
                    .addArg( uid.get() )
                    .build();
                reporter.addError( error );
            }
        }
        else if ( ENROLLMENT.equals( trackerType ) )
        {
            if ( uid.isPresent() && !ValidationUtils.enrollmentExist( ctx, uid.get() ) )
            {
                TrackerErrorReport error = TrackerErrorReport.builder()
                    .uid( ((TrackerDto) relationship).getUid() )
                    .trackerType( ((TrackerDto) relationship).getTrackerType() )
                    .errorCode( E4012 )
                    .addArg( trackerType.getName() )
                    .addArg( uid.get() )
                    .build();
                reporter.addError( error );
            }
        }
        else if ( EVENT.equals( trackerType ) )
        {
            if ( uid.isPresent() && !ValidationUtils.eventExist( ctx, uid.get() ) )
            {
                TrackerErrorReport error = TrackerErrorReport.builder()
                    .uid( ((TrackerDto) relationship).getUid() )
                    .trackerType( ((TrackerDto) relationship).getTrackerType() )
                    .errorCode( E4012 )
                    .addArg( trackerType.getName() )
                    .addArg( uid.get() )
                    .build();
                reporter.addError( error );
            }
        }
    }

    // TODO: This method needs some love and care, the logic here is very hard
    // to read.
    protected void validateEventCategoryCombo( ValidationErrorReporter reporter, TrackerImportValidationContext context,
        Event event, Program program )
    {
        TrackerPreheat preheat = context.getBundle().getPreheat();

        // if event has "attribute option combo" set only, fetch the aoc
        // directly
        boolean optionComboIsEmpty = StringUtils.isEmpty( event.getAttributeOptionCombo() );
        boolean categoryOptionsIsEmpty = StringUtils.isEmpty( event.getAttributeCategoryOptions() );

        CategoryOptionCombo categoryOptionCombo = null;

        if ( !optionComboIsEmpty && categoryOptionsIsEmpty )
        {
            categoryOptionCombo = context.getCategoryOptionCombo( event.getAttributeOptionCombo() );
        }
        else if ( !optionComboIsEmpty && program.getCategoryCombo() != null )
        {
            categoryOptionCombo = resolveCategoryOptions( reporter, context, event, program );
        }

        categoryOptionCombo = getDefault( event, preheat, optionComboIsEmpty, categoryOptionCombo );

        if ( categoryOptionCombo == null )
        {
            TrackerErrorReport error = TrackerErrorReport.builder()
                .uid( event.getUid() )
                .trackerType( event.getTrackerType() )
                .errorCode( E1115 )
                .addArg( event.getAttributeOptionCombo() )
                .build();
            reporter.addError( error );
        }
        else
        {
            context.cacheEventCategoryOptionCombo( event.getUid(), categoryOptionCombo );
        }
    }

    private CategoryOptionCombo resolveCategoryOptions( ValidationErrorReporter reporter,
        TrackerImportValidationContext context, Event event, Program program )
    {
        CategoryOptionCombo categoryOptionCombo;
        String attributeCategoryOptions = event.getAttributeCategoryOptions();
        CategoryCombo categoryCombo = program.getCategoryCombo();
        String cacheKey = attributeCategoryOptions + categoryCombo.getUid();

        Optional<String> cachedEventAOCProgramCC = context.getCachedEventAOCProgramCC( cacheKey );

        if ( cachedEventAOCProgramCC.isPresent() )
        {
            categoryOptionCombo = context.getCategoryOptionCombo( cachedEventAOCProgramCC.get() );
        }
        else
        {
            Set<String> categoryOptions = TextUtils
                .splitToArray( attributeCategoryOptions, TextUtils.SEMICOLON );

            categoryOptionCombo = resolveCategoryOptionCombo( reporter, context, event,
                categoryCombo, categoryOptions );

            context.putCachedEventAOCProgramCC( cacheKey,
                categoryOptionCombo != null ? categoryOptionCombo.getUid() : null );
        }
        return categoryOptionCombo;
    }

    private CategoryOptionCombo getDefault( Event event, TrackerPreheat preheat, boolean aocIsEmpty,
        CategoryOptionCombo categoryOptionCombo )
    {
        if ( categoryOptionCombo == null )
        {
            CategoryOptionCombo defaultCategoryCombo = preheat
                .getDefault( CategoryOptionCombo.class );

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
        TrackerImportValidationContext context, Event event,
        CategoryCombo programCategoryCombo, Set<String> attributeCategoryOptions )
    {
        Set<CategoryOption> categoryOptions = new HashSet<>();

        for ( String uid : attributeCategoryOptions )
        {
            CategoryOption categoryOption = context.getCategoryOption( uid );
            if ( categoryOption == null )
            {
                TrackerErrorReport error = TrackerErrorReport.builder()
                    .uid( event.getUid() )
                    .trackerType( event.getTrackerType() )
                    .errorCode( E1116 )
                    .addArg( uid )
                    .build();
                reporter.addError( error );
                return null;
            }

            categoryOptions.add( categoryOption );
        }

        CategoryOptionCombo attrOptCombo = categoryService
            .getCategoryOptionCombo( programCategoryCombo, categoryOptions );

        if ( attrOptCombo == null )
        {
            TrackerErrorReport error = TrackerErrorReport.builder()
                .uid( event.getUid() )
                .trackerType( event.getTrackerType() )
                .errorCode( TrackerErrorCode.E1117 )
                .addArg( context.getBundle().getIdentifier(), programCategoryCombo )
                .addArg( categoryOptions )
                .build();
            reporter.addError( error );
        }
        else
        {
            TrackerPreheat preheat = context.getBundle().getPreheat();
            TrackerIdentifier identifier = preheat.getIdentifiers().getCategoryOptionComboIdScheme();
            preheat.put( identifier, attrOptCombo );
        }

        return attrOptCombo;
    }

    private String getEnrollmentProgramUidFromEvent( TrackerImportValidationContext context,
        Event event )
    {
        ProgramInstance programInstance = context.getProgramInstance( event.getEnrollment() );
        if ( programInstance != null )
        {
            return programInstance.getProgram().getUid();
        }
        else
        {
            final Optional<ReferenceTrackerEntity> reference = context.getReference( event.getEnrollment() );
            if ( reference.isPresent() )
            {
                final Optional<Enrollment> enrollment = context.getBundle()
                    .getEnrollment( event.getEnrollment() );
                if ( enrollment.isPresent() )
                {
                    return enrollment.get().getProgram();
                }
            }
        }
        return null;
    }

    private String getTrackedEntityTypeUidFromEnrollment( TrackerImportValidationContext context,
        Enrollment enrollment )
    {
        final TrackedEntityInstance trackedEntityInstance = context
            .getTrackedEntityInstance( enrollment.getTrackedEntity() );
        if ( trackedEntityInstance != null )
        {
            return trackedEntityInstance.getTrackedEntityType().getUid();
        }
        else
        {
            final Optional<ReferenceTrackerEntity> reference = context.getReference( enrollment.getTrackedEntity() );
            if ( reference.isPresent() )
            {
                final Optional<TrackedEntity> tei = context.getBundle()
                    .getTrackedEntity( enrollment.getTrackedEntity() );
                if ( tei.isPresent() )
                {
                    return tei.get().getTrackedEntityType();
                }
            }
        }
        return null;
    }

    private boolean programHasOrgUnit( Program program, OrganisationUnit orgUnit,
        Map<String, List<String>> programAndOrgUnitsMap )
    {
        return programAndOrgUnitsMap.containsKey( program.getUid() )
            && programAndOrgUnitsMap.get( program.getUid() ).contains( orgUnit.getUid() );
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }

}
