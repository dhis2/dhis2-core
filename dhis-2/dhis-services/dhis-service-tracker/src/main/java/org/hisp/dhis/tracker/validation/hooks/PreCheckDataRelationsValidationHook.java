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
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class PreCheckDataRelationsValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter,
        TrackerBundle bundle, TrackedEntity trackedEntity )
    {
        // NOTHING TO DO HERE
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        Program program = reporter.getBundle().getPreheat().getProgram( enrollment.getProgram() );
        OrganisationUnit organisationUnit = reporter.getBundle().getPreheat()
            .getOrganisationUnit( enrollment.getOrgUnit() );

        reporter.addErrorIf( () -> !program.isRegistration(), enrollment, E1014, program );

        TrackerPreheat preheat = reporter.getBundle().getPreheat();
        if ( programDoesNotHaveOrgUnit( program, organisationUnit, preheat.getProgramWithOrgUnitsMap() ) )
        {
            reporter.addError( enrollment, E1041, organisationUnit, program );
        }

        validateTrackedEntityTypeMatchesPrograms( reporter, program, enrollment );
    }

    private boolean programDoesNotHaveOrgUnit( Program program, OrganisationUnit orgUnit,
        Map<String, List<String>> programAndOrgUnitsMap )
    {
        return !programAndOrgUnitsMap.containsKey( program.getUid() )
            || !programAndOrgUnitsMap.get( program.getUid() ).contains( orgUnit.getUid() );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, TrackerBundle bundle, Event event )
    {
        ProgramStage programStage = reporter.getBundle().getPreheat().getProgramStage( event.getProgramStage() );
        OrganisationUnit organisationUnit = reporter.getBundle().getPreheat().getOrganisationUnit( event.getOrgUnit() );
        Program program = reporter.getBundle().getPreheat().getProgram( event.getProgram() );

        validateProgramStageInProgram( reporter, event, programStage, program );
        validateRegistrationProgram( reporter, event, program );
        validateProgramHasOrgUnit( reporter, event, organisationUnit, program );
        validateEventCategoryOptionCombo( reporter, event, program );
    }

    private void validateProgramStageInProgram( ValidationErrorReporter reporter, Event event,
        ProgramStage programStage, Program program )
    {
        if ( !program.getUid().equals( programStage.getProgram().getUid() ) )
        {
            reporter.addError( event, E1089, event, programStage, program );
        }
    }

    private void validateRegistrationProgram( ValidationErrorReporter reporter, Event event, Program eventProgram )
    {
        if ( eventProgram.isRegistration() )
        {
            if ( StringUtils.isEmpty( event.getEnrollment() ) )
            {
                reporter.addError( event, E1033, event.getEvent() );
            }
            else
            {
                Program enrollmentProgram = getEnrollmentProgramFromEvent( reporter.getBundle(), event );

                if ( !eventProgram.equals( enrollmentProgram ) )
                {
                    reporter.addError( event, E1079, event, eventProgram, event.getEnrollment() );
                }
            }
        }
    }

    private void validateProgramHasOrgUnit( ValidationErrorReporter reporter, Event event,
        OrganisationUnit organisationUnit, Program program )
    {
        TrackerPreheat preheat = reporter.getBundle().getPreheat();
        if ( programDoesNotHaveOrgUnit( program, organisationUnit, preheat.getProgramWithOrgUnitsMap() ) )
        {
            reporter.addError( event, E1029, organisationUnit, program );
        }
    }

    private void validateEventCategoryOptionCombo( ValidationErrorReporter reporter,
        Event event, Program program )
    {
        boolean isValid = validateAttributeOptionComboExists( reporter, event );
        isValid = validateCategoryOptionsExist( reporter, event ) && isValid;
        isValid = validateDefaultProgramCategoryCombo( reporter, event, program ) && isValid;
        if ( !isValid )
        {
            // no need to do the next validations concerning relationships
            // between the AOC id, COs and the program CC
            // since not all AOC, COs exist or the AOC, COs violate programs
            // default CC rules
            return;
        }

        CategoryOptionCombo aoc = resolveAttributeOptionCombo( reporter, event, program );
        // We should have an AOC by this point. Either the event has no AOC and
        // no COs and its program has the default
        // category combo, or we have an event.AOC. If the payload did not
        // contain an AOC, the preheat and preprocessor should
        // have found/set it.
        if ( !validateAttributeOptionComboFound( reporter, event, program, aoc ) )
            return;
        if ( !validateAttributeOptionComboIsInProgramCategoryCombo( reporter, event, program ) )
            return;
        validateAttributeOptionComboMatchesCategoryOptions( reporter, event, program, aoc );
    }

    private boolean validateAttributeOptionComboExists( ValidationErrorReporter reporter, Event event )
    {
        if ( hasNoAttributeOptionComboSet( event ) )
        {
            return true;
        }

        CategoryOptionCombo categoryOptionCombo = reporter.getBundle().getPreheat()
            .getCategoryOptionCombo( event.getAttributeOptionCombo() );
        if ( categoryOptionCombo == null )
        {
            reporter.addError( event, E1115, event.getAttributeOptionCombo() );
            return false;
        }
        return true;
    }

    private boolean hasNoAttributeOptionComboSet( Event event )
    {
        return event.getAttributeOptionCombo().isBlank();
    }

    private boolean validateCategoryOptionsExist( ValidationErrorReporter reporter, Event event )
    {
        if ( hasNoAttributeCategoryOptionsSet( event ) )
        {
            return true;
        }

        boolean allCOsExist = true;
        TrackerPreheat preheat = reporter.getBundle().getPreheat();
        for ( MetadataIdentifier id : event.getAttributeCategoryOptions() )
        {
            if ( preheat.getCategoryOption( id ) == null )
            {
                reporter.addError( event, E1116, id );
                allCOsExist = false;
            }
        }
        return allCOsExist;
    }

    private boolean hasNoAttributeCategoryOptionsSet( Event event )
    {
        return event.getAttributeCategoryOptions().isEmpty();
    }

    /**
     * Validates that the event program has the default category combo if no AOC
     * or COs are given or if a default AOC is given.
     *
     * @param reporter validation error reporter
     * @param event event to validate
     * @param program event program from the preheat
     * @return return true if event program is default with valid aoc and co
     *         combinations
     */
    private boolean validateDefaultProgramCategoryCombo( ValidationErrorReporter reporter, Event event,
        Program program )
    {
        if ( hasNoAttributeOptionComboSet( event ) &&
            hasNoAttributeCategoryOptionsSet( event ) &&
            !program.getCategoryCombo().isDefault() )
        {
            reporter.addError( event, TrackerErrorCode.E1055 );
            return false;
        }
        CategoryOptionCombo aoc = reporter.getBundle().getPreheat()
            .getCategoryOptionCombo( event.getAttributeOptionCombo() );
        if ( hasAttributeOptionComboSet( event ) &&
            aoc != null && aoc.getCategoryCombo().isDefault() &&
            !program.getCategoryCombo().isDefault() )
        {
            reporter.addError( event, TrackerErrorCode.E1055 );
            return false;
        }

        return true;
    }

    private boolean hasAttributeOptionComboSet( Event event )
    {
        return !hasNoAttributeOptionComboSet( event );
    }

    private boolean validateAttributeOptionComboIsInProgramCategoryCombo( ValidationErrorReporter reporter, Event event,
        Program program )
    {
        if ( hasNoAttributeOptionComboSet( event ) )
        {
            return true;
        }

        CategoryOptionCombo aoc = reporter.getBundle().getPreheat()
            .getCategoryOptionCombo( event.getAttributeOptionCombo() );
        if ( !program.getCategoryCombo().equals( aoc.getCategoryCombo() ) )
        {
            reporter.addError( event, TrackerErrorCode.E1054, event.getAttributeOptionCombo(),
                program.getCategoryCombo() );
            return false;
        }

        return true;
    }

    private CategoryOptionCombo resolveAttributeOptionCombo( ValidationErrorReporter reporter, Event event,
        Program program )
    {

        TrackerPreheat preheat = reporter.getBundle().getPreheat();
        CategoryOptionCombo aoc;
        if ( hasNoAttributeOptionComboSet( event ) && program.getCategoryCombo().isDefault() )
        {
            aoc = preheat.getDefault( CategoryOptionCombo.class );
        }
        else
        {
            // Note: there is a potential case when there are multiple AOCs in
            // the default CC
            // this should not happen, but it's technically possible. In this
            // case with event.AOC provided,
            // stick to the given AOC in the payload instead of
            // preheat.getDefault( CategoryOptionCombo.class )
            aoc = preheat.getCategoryOptionCombo( event.getAttributeOptionCombo() );
        }
        return aoc;
    }

    private Set<CategoryOption> getCategoryOptions( TrackerPreheat preheat, Event event )
    {

        Set<CategoryOption> categoryOptions = new HashSet<>();
        for ( MetadataIdentifier id : event.getAttributeCategoryOptions() )
        {
            categoryOptions.add( preheat.getCategoryOption( id ) );
        }
        return categoryOptions;
    }

    private boolean validateAttributeOptionComboFound( ValidationErrorReporter reporter, Event event, Program program,
        CategoryOptionCombo aoc )
    {
        if ( aoc != null )
        {
            return true;
        }

        addAOCAndCOCombinationError( event, reporter, program );
        return false;
    }

    private void validateAttributeOptionComboMatchesCategoryOptions( ValidationErrorReporter reporter, Event event,
        Program program,
        CategoryOptionCombo aoc )
    {
        if ( hasNoAttributeCategoryOptionsSet( event ) )
        {
            return;
        }

        if ( isNotAOCForCOs( reporter.getBundle().getPreheat(), event, aoc ) )
        {
            addAOCAndCOCombinationError( event, reporter, program );
        }

    }

    private void addAOCAndCOCombinationError( Event event, ValidationErrorReporter reporter, Program program )
    {
        // we used the program CC in finding the AOC id, if the AOC id was not
        // provided in the payload
        if ( hasNoAttributeOptionComboSet( event ) )
        {
            reporter.addError( event, TrackerErrorCode.E1117, program.getCategoryCombo(),
                event.getAttributeCategoryOptions().stream().map( MetadataIdentifier::getIdentifierOrAttributeValue )
                    .collect( Collectors.toSet() ).toString() );
        }
        else
        {
            reporter.addError( event, TrackerErrorCode.E1117,
                event.getAttributeOptionCombo(),
                event.getAttributeCategoryOptions().stream().map( MetadataIdentifier::getIdentifierOrAttributeValue )
                    .collect( Collectors.toSet() ).toString() );
        }
    }

    private boolean isNotAOCForCOs( TrackerPreheat preheat, Event event, CategoryOptionCombo aoc )
    {
        return !isAOCForCOs( aoc, getCategoryOptions( preheat, event ) );
    }

    private boolean isAOCForCOs( CategoryOptionCombo aoc, Set<CategoryOption> categoryOptions )
    {
        return aoc.getCategoryOptions().containsAll( categoryOptions )
            && aoc.getCategoryOptions().size() == categoryOptions.size();
    }

    private Program getEnrollmentProgramFromEvent( TrackerBundle bundle, Event event )
    {
        ProgramInstance programInstance = bundle.getProgramInstance( event.getEnrollment() );
        if ( programInstance != null )
        {
            return programInstance.getProgram();
        }
        else
        {
            final Optional<ReferenceTrackerEntity> reference = bundle.getPreheat()
                .getReference( event.getEnrollment() );
            if ( reference.isPresent() )
            {
                final Optional<Enrollment> enrollment = bundle.getEnrollment( event.getEnrollment() );
                if ( enrollment.isPresent() )
                {
                    return bundle.getPreheat().getProgram( enrollment.get().getProgram() );
                }
            }
        }
        return null;
    }

    private void validateTrackedEntityTypeMatchesPrograms( ValidationErrorReporter reporter, Program program,
        Enrollment enrollment )
    {

        if ( program.getTrackedEntityType() == null )
        {
            return;
        }

        if ( !trackedEntityTypesMatch( reporter.getBundle(), program, enrollment ) )
        {
            reporter.addError( enrollment, E1022, enrollment.getTrackedEntity(), program );
        }
    }

    private boolean trackedEntityTypesMatch( TrackerBundle bundle, Program program, Enrollment enrollment )
    {
        final TrackedEntityInstance trackedEntityInstance = bundle
            .getTrackedEntityInstance( enrollment.getTrackedEntity() );
        if ( trackedEntityInstance != null )
        {
            return program.getTrackedEntityType().getUid()
                .equals( trackedEntityInstance.getTrackedEntityType().getUid() );
        }

        final Optional<ReferenceTrackerEntity> reference = bundle.getPreheat()
            .getReference( enrollment.getTrackedEntity() );
        if ( reference.isPresent() )
        {
            final Optional<TrackedEntity> tei = bundle.getTrackedEntity( enrollment.getTrackedEntity() );
            if ( tei.isPresent() )
            {
                return tei.get().getTrackedEntityType().isEqualTo( program.getTrackedEntityType() );
            }
        }

        return false;
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

        if ( TRACKED_ENTITY.equals( trackerType ) )
        {
            if ( uid.isPresent() && !ValidationUtils.trackedEntityInstanceExist( reporter.getBundle(), uid.get() ) )
            {
                reporter.addError( relationship, E4012, trackerType.getName(), uid.get() );
            }
        }
        else if ( ENROLLMENT.equals( trackerType ) )
        {
            if ( uid.isPresent() && !ValidationUtils.enrollmentExist( reporter.getBundle(), uid.get() ) )
            {
                reporter.addError( relationship, E4012, trackerType.getName(), uid.get() );
            }
        }
        else if ( EVENT.equals( trackerType ) && uid.isPresent()
            && !ValidationUtils.eventExist( reporter.getBundle(), uid.get() ) )
        {
            reporter.addError( relationship, E4012, trackerType.getName(), uid.get() );
        }
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }

}
