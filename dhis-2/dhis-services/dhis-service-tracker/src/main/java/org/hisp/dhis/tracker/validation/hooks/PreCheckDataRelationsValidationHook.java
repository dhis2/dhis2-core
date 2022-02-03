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
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
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
        OrganisationUnit organisationUnit = context.getOrganisationUnit( enrollment.getOrgUnit() );

        reporter.addErrorIf( () -> !program.isRegistration(), enrollment, E1014, program );

        if ( !programHasOrgUnit( program, organisationUnit, context.getProgramWithOrgUnitsMap() ) )
        {
            reporter.addError( enrollment, E1041, organisationUnit, program );
        }

        if ( program.getTrackedEntityType() != null
            && !program.getTrackedEntityType().getUid()
                .equals( getTrackedEntityTypeUidFromEnrollment( context, enrollment ) ) )
        {
            reporter.addError( enrollment, E1022, enrollment.getTrackedEntity(), program );
        }
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );
        OrganisationUnit organisationUnit = context.getOrganisationUnit( event.getOrgUnit() );
        Program program = context.getProgram( event.getProgram() );

        if ( !program.getUid().equals( programStage.getProgram().getUid() ) )
        {
            reporter.addError( event, E1089, event, programStage, program );
        }

        if ( program.isRegistration() )
        {
            if ( StringUtils.isEmpty( event.getEnrollment() ) )
            {
                reporter.addError( event, E1033, event.getEvent() );
            }
            else
            {
                String programUid = getEnrollmentProgramUidFromEvent( context, event );

                if ( !program.getUid().equals( programUid ) )
                {
                    reporter.addError( event, E1079, event, program, event.getEnrollment() );
                }
            }
        }

        if ( !programHasOrgUnit( program, organisationUnit, context.getProgramWithOrgUnitsMap() ) )
        {
            reporter.addError( event, E1029, organisationUnit, program );
        }

        validateEventCategoryCombo( reporter, event, program );
    }

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        validateRelationshipReference( reporter, relationship, relationship.getFrom() );
        validateRelationshipReference( reporter, relationship, relationship.getTo() );
    }

    private void validateRelationshipReference( ValidationErrorReporter reporter, Relationship relationship,
        RelationshipItem item )
    {
        Optional<String> uid = getUidFromRelationshipItem( item );
        TrackerType trackerType = relationshipItemValueType( item );

        TrackerImportValidationContext ctx = reporter.getValidationContext();

        if ( TRACKED_ENTITY.equals( trackerType ) )
        {
            if ( uid.isPresent() && !ValidationUtils.trackedEntityInstanceExist( ctx, uid.get() ) )
            {
                reporter.addError( relationship, E4012, trackerType.getName(), uid.get() );
            }
        }
        else if ( ENROLLMENT.equals( trackerType ) )
        {
            if ( uid.isPresent() && !ValidationUtils.enrollmentExist( ctx, uid.get() ) )
            {
                reporter.addError( relationship, E4012, trackerType.getName(), uid.get() );
            }
        }
        else if ( EVENT.equals( trackerType ) )
        {
            if ( uid.isPresent() && !ValidationUtils.eventExist( ctx, uid.get() ) )
            {
                reporter.addError( relationship, E4012, trackerType.getName(), uid.get() );
            }
        }
    }

    // TODO: This method needs some love and care, the logic here is very hard
    // to read.
    protected void validateEventCategoryCombo( ValidationErrorReporter reporter,
        Event event, Program program )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerPreheat preheat = reporter.getValidationContext().getBundle().getPreheat();

        // if event has "attribute option combo" set only, fetch the aoc
        // directly
        boolean optionComboIsEmpty = StringUtils.isEmpty( event.getAttributeOptionCombo() );
        boolean categoryOptionsIsEmpty = StringUtils.isEmpty( event.getAttributeCategoryOptions() );

        CategoryOptionCombo categoryOptionCombo = null;

        if ( !optionComboIsEmpty && categoryOptionsIsEmpty )
        {
            categoryOptionCombo = preheat.getCategoryOptionCombo( event.getAttributeOptionCombo() );
        }
        else if ( !optionComboIsEmpty && program.getCategoryCombo() != null )
        {
            categoryOptionCombo = resolveCategoryOptions( reporter, event, program, context );
        }

        categoryOptionCombo = getDefault( event, preheat, optionComboIsEmpty, categoryOptionCombo );

        if ( categoryOptionCombo == null )
        {
            reporter.addError( event, E1115, event.getAttributeOptionCombo() );
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
            categoryOptionCombo = context.getBundle().getPreheat()
                .getCategoryOptionCombo( cachedEventAOCProgramCC.get() );
        }
        else
        {
            Set<String> categoryOptions = TextUtils
                .splitToArray( attributeCategoryOptions, TextUtils.SEMICOLON );

            categoryOptionCombo = resolveCategoryOptionCombo( reporter, event,
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

    private CategoryOptionCombo resolveCategoryOptionCombo( ValidationErrorReporter reporter, Event event,
        CategoryCombo programCategoryCombo, Set<String> attributeCategoryOptions )
    {
        Set<CategoryOption> categoryOptions = new HashSet<>();

        TrackerPreheat preheat = reporter.getValidationContext().getBundle().getPreheat();
        for ( String uid : attributeCategoryOptions )
        {
            CategoryOption categoryOption = preheat.getCategoryOption( uid );
            if ( categoryOption == null )
            {
                reporter.addError( event, E1116, uid );
                return null;
            }

            categoryOptions.add( categoryOption );
        }

        CategoryOptionCombo attrOptCombo = categoryService
            .getCategoryOptionCombo( programCategoryCombo, categoryOptions );

        if ( attrOptCombo == null )
        {
            reporter.addError( event, TrackerErrorCode.E1117, programCategoryCombo,
                categoryOptions );
        }
        else
        {
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
