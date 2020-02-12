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

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.*;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class AddEnrollmentValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 200;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        if ( !bundle.getImportStrategy().isCreate() )
        {
            return Collections.emptyList();
        }

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle,
            AddEnrollmentValidationHook.class );

        TrackerPreheat preheat = bundle.getPreheat();
        User actingUser = preheat.getUser();

        List<Enrollment> enrollments = bundle.getEnrollments();
        for ( Enrollment enrollment : enrollments )
        {
            reporter.increment( enrollment );

            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTrackedEntityInstance( bundle, enrollment.getTrackedEntityInstance() );

            if ( trackedEntityInstance == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1020 )
                    .addArg( enrollment.getTrackedEntityInstance() ) );
                continue;
            }

            Program program = PreheatHelper.getProgram( bundle, enrollment.getProgram() );
            OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, enrollment.getOrgUnit() );

            // Validate progam!=null && orgunit != null?

            // Missmatch program tracked entity type - trackedEntityInstance  tracked entity type
            boolean isNotSameTrackedEntity = program.getTrackedEntityType() != null &&
                !program.getTrackedEntityType().equals( trackedEntityInstance.getTrackedEntityType() );

            if ( isNotSameTrackedEntity )
            {
                reporter.addError( newReport( TrackerErrorCode.E1022 )
                    .addArg( trackedEntityInstance )
                    .addArg( program ) );

                continue;
            }

            ProgramInstance programInstance = new ProgramInstance( program, trackedEntityInstance, organisationUnit );

            if ( !trackerOwnershipManager.hasAccess( actingUser, trackedEntityInstance, program ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1028 )
                    .addArg( trackedEntityInstance )
                    .addArg( program ) );
                continue;
            }

//            ProgramInstance programInstance = preheat.getEnrollment( TrackerIdentifier.UID, enrollment.getEnrollment() );?????

            List<String> errors = trackerAccessManager.canCreate( actingUser, programInstance, false );
            if ( !errors.isEmpty() )
            {
                reporter.addError( newReport( TrackerErrorCode.E1000 )
                    .addArg( actingUser )
                    .addArg( String.join( ",", errors ) ) );
                continue;
            }

            // ----validateRequest
            // Enrollment(¶4.a) - can only enroll if registration!
            if ( !program.isRegistration() )
            {
                reporter.addError( newReport( TrackerErrorCode.E1014 )
                    .addArg( program ) );
                continue;

            }

            if ( !validateAttributes( reporter, program, enrollment, trackedEntityInstance, bundle ) )
            {
                continue;
            }

            if ( EnrollmentStatus.CANCELLED != enrollment.getStatus() )
            {
                // Enrollment(¶4.b.i) - When an enrollment with status != CANCELLED is being imported,
                // Check to make sure we don’t have any existing active enrollments for TEI + Program combination

                ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
                params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
                params.setSkipPaging( true );
                params.setProgram( program );
                params.setTrackedEntityInstance( trackedEntityInstance );

                List<ProgramInstance> programInstances = programInstanceService.getProgramInstances( params );

                // Sort out only the programs the importing user has access too...
                // Stian, Morten H.  NOTE: How will this affect validation? If there is a conflict here but importing user is not allowed to know,
                // should import still be possible?
                List<Enrollment> programEnrollments = filterEnrollmentsUserAsAccessTo( actingUser, programInstances );

                // Enrollment(¶4.b.ii) - The error of enrolling more than once is possible only if the imported enrollment
                // has a state other than CANCELLED...
                if ( program.getOnlyEnrollOnce() )
                {
                    Set<Enrollment> activeOrCompletedEnrollments = programEnrollments.stream()
                        .filter( programEnrollment -> EnrollmentStatus.ACTIVE == programEnrollment.getStatus()
                            || EnrollmentStatus.COMPLETED == programEnrollment.getStatus() )
                        .collect( Collectors.toSet() );

                    if ( !activeOrCompletedEnrollments.isEmpty() )
                    {
                        reporter.addError( newReport( TrackerErrorCode.E1016 )
                            .addArg( trackedEntityInstance )
                            .addArg( program ) );
                        continue;
                    }
                }
                else if ( EnrollmentStatus.ACTIVE == enrollment.getStatus() )
                {
                    // Optimize: this check/filter can be done in fetch above?
                    Set<Enrollment> activeEnrollments = programEnrollments.stream()
                        .filter( programEnrollment -> EnrollmentStatus.ACTIVE == programEnrollment.getStatus() )
                        .collect( Collectors.toSet() );

                    if ( !activeEnrollments.isEmpty() )
                    {
                        //Error: TrackedEntityInstance already has an active enrollment in another program...
                        reporter.addError( newReport( TrackerErrorCode.E1015 )
                            .addArg( trackedEntityInstance )
                            .addArg( program ) );
                        continue;
                    }
                }
            }
            // ----validateRequest END

            // Validate dates...
            ProgramInstance tempProgramInstance =
                createTempProgramInstance( trackedEntityInstance, program, enrollment, organisationUnit, actingUser );

            if ( !validateProgramInstance( reporter, program, tempProgramInstance, enrollment ) )
            {
                continue;
            }

//          HOW???  validateGeo( fullReport, )

            // note validation....
            for ( Note note : enrollment.getNotes() )
            {
                if ( CodeGenerator.isValidUid( note.getNote() )
                    && commentService.trackedEntityCommentExists( note.getNote() )
                    && !StringUtils.isEmpty( note.getValue() ) )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1028 )
                        .addArg( enrollment )
                        .addArg( note.getNote() ) );
                    continue;
                }
            }
        }

        return reporter.getReportList();
    }

    private boolean validateProgramInstance( ValidationErrorReporter errorReporter,
        Program program,
        ProgramInstance programInstance,
        Enrollment enrollment )
    {
        boolean incidentDateIsNull = program.getDisplayIncidentDate() & programInstance.getIncidentDate() == null;
        if ( incidentDateIsNull )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1023 ) );
            return false;
        }

        String incidentDate = DateUtils.getMediumDateString( programInstance.getIncidentDate() );
        boolean validEnrollmentIncidentDate =
            programInstance.getIncidentDate() != null && DateUtils.dateIsValid( incidentDate );

        if ( !validEnrollmentIncidentDate )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1024 )
                .addArg( programInstance.getIncidentDate() ) );
            return false;
        }

        String enrollmentDate = DateUtils.getMediumDateString( programInstance.getEnrollmentDate() );
        boolean validEnrollmentDate =
            programInstance.getEnrollmentDate() != null && DateUtils.dateIsValid( enrollmentDate );

        if ( !validEnrollmentDate )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1025 )
                .addArg( programInstance.getEnrollmentDate() ) );
            return false;
        }

        boolean validEnrollmentCreatedAtClientDate =
            enrollment.getCreatedAtClient() != null && DateUtils.dateIsValid( enrollment.getCreatedAtClient() );

        if ( !validEnrollmentCreatedAtClientDate )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1026 )
                .addArg( programInstance.getCreatedAtClient() ) );
            return false;
        }

        boolean validLastUpdatedAtClientDate =
            enrollment.getLastUpdatedAtClient() != null && DateUtils.dateIsValid( enrollment.getLastUpdatedAtClient() );

        if ( !validLastUpdatedAtClientDate )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1027 )
                .addArg( enrollment.getLastUpdatedAtClient() ) );
            return false;
        }

        return true;
    }

    private ProgramInstance createTempProgramInstance( TrackedEntityInstance trackedEntityInstance, Program program,
        Enrollment enrollment, OrganisationUnit organisationUnit, User user )
    {
        // throw/raise error if uid is invalid ?
        String uid = CodeGenerator.isValidUid( enrollment.getEnrollment() )
            ? enrollment.getEnrollment() : CodeGenerator.generateUid();

        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( uid );
        programInstance.setOrganisationUnit( organisationUnit );
        programInstance.enrollTrackedEntityInstance( trackedEntityInstance, program );

        programInstance
            .setEnrollmentDate( enrollment.getEnrollmentDate() != null ? enrollment.getEnrollmentDate() : new Date() );
        programInstance
            .setIncidentDate( enrollment.getIncidentDate() != null ? enrollment.getIncidentDate() : new Date() );

        ProgramStatus programStatus = EnrollmentStatus.ACTIVE == enrollment.getStatus()
            ? ProgramStatus.ACTIVE : EnrollmentStatus.COMPLETED == enrollment.getStatus()
            ? ProgramStatus.COMPLETED : ProgramStatus.CANCELLED;

        programInstance.setStatus( programStatus );

        if ( (ProgramStatus.COMPLETED == programStatus) || (programStatus == ProgramStatus.CANCELLED) )
        {
            programInstance.setCompletedBy(
                enrollment.getCompletedBy() == null ? user.getUsername() : enrollment.getCompletedBy() );

            programInstance.setEndDate(
                enrollment.getCompletedDate() == null ? new Date() : enrollment.getCompletedDate() );
        }

        return programInstance;
    }

    private boolean validateAttributes( ValidationErrorReporter errorReporter,
        Program program,
        Enrollment enrollment,
        TrackedEntityInstance trackedEntityInstance,
        TrackerBundle bundle )
    {
//        Program program = getProgram( importOptions.getIdSchemes(), enrollment.getProgram() );
//        org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance(
//            enrollment.getTrackedEntityInstance() );

        Map<TrackedEntityAttribute, Boolean> mandatoryMap = Maps.newHashMap();
        Map<String, String> attributeValueMap = Maps.newHashMap();

        for ( ProgramTrackedEntityAttribute programTrackedEntityAttribute : program.getProgramAttributes() )
        {
            mandatoryMap
                .put( programTrackedEntityAttribute.getAttribute(), programTrackedEntityAttribute.isMandatory() );
        }

        // ignore attributes which do not belong to this program
        trackedEntityInstance.getTrackedEntityAttributeValues().stream().
            filter( value -> mandatoryMap.containsKey( value.getAttribute() ) ).
            forEach( value -> attributeValueMap.put( value.getAttribute().getUid(), value.getValue() ) );

        for ( Attribute attribute : enrollment.getAttributes() )
        {
            attributeValueMap.put( attribute.getAttribute(), attribute.getValue() );
//            validateAttributeType( attribute, importOptions );
//            TrackedEntityAttribute teAttribute = getTrackedEntityAttribute( importOptions.getIdSchemes(), attribute.getAttribute() );
            TrackedEntityAttribute teAttribute = bundle.getPreheat()
                .get( TrackerIdentifier.UID, TrackedEntityAttribute.class, attribute.getAttribute() );
            if ( teAttribute == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1017 ) );
                return false;
            }

            String errorMessage = teAttrService.validateValueType( teAttribute, attribute.getValue() );
            if ( errorMessage != null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1017 ).addArg( errorMessage ) );
                return false;
            }

        }

        TrackedEntityInstance instance =
            trackedEntityInstanceService.getTrackedEntityInstance( enrollment.getTrackedEntityInstance() );

        for ( TrackedEntityAttribute trackedEntityAttribute : mandatoryMap.keySet() )
        {
            Boolean mandatory = mandatoryMap.get( trackedEntityAttribute );

            boolean hasMissingAttribute = mandatory && doValidationOfMandatoryAttributes( bundle.getUser() ) &&
                !attributeValueMap.containsKey( trackedEntityAttribute.getUid() );
            if ( hasMissingAttribute )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1018 ).addArg( trackedEntityAttribute ) );
                return false;
            }

            if ( trackedEntityAttribute.isUnique() )
            {
                String errorMessage = teAttrService.validateAttributeUniquenessWithinScope(
                    trackedEntityAttribute,
                    attributeValueMap.get( trackedEntityAttribute.getUid() ),
                    trackedEntityInstance.getUid(), instance.getOrganisationUnit() );
                if ( errorMessage != null )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1007 ).addArg( errorMessage ) );
                    return false;
                }
            }

            attributeValueMap.remove( trackedEntityAttribute.getUid() );
        }

        if ( !attributeValueMap.isEmpty() )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1019 )
                .addArg( Joiner.on( "," ).withKeyValueSeparator( "=" ).join( attributeValueMap ) ) );
            return false;
        }

        if ( !program.getSelectEnrollmentDatesInFuture() )
        {
            boolean enrollmentIsInFuture = Objects.nonNull( enrollment.getEnrollmentDate() ) &&
                enrollment.getEnrollmentDate().after( new Date() );
            if ( enrollmentIsInFuture )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1020 ).addArg( enrollment.getEnrollmentDate() ) );
                return false;
            }
        }

        if ( !program.getSelectIncidentDatesInFuture() )
        {
            boolean incidentIsInFuture =
                Objects.nonNull( enrollment.getIncidentDate() ) && enrollment.getIncidentDate().after( new Date() );
            if ( incidentIsInFuture )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1021 ).addArg( enrollment.getIncidentDate() ) );
                return false;
            }
        }

        return true;
    }

    private boolean doValidationOfMandatoryAttributes( User user )
    {
        return user == null || !user.isAuthorized(
            Authorities.F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION.getAuthority() );
    }

    public List<Enrollment> filterEnrollmentsUserAsAccessTo( User actingUser,
        Iterable<ProgramInstance> programInstances )
    {
        List<Enrollment> enrollments = new ArrayList<>();

        for ( ProgramInstance programInstance : programInstances )
        {
            if ( programInstance != null && trackerOwnershipManager.hasAccess( actingUser, programInstance ) )
            {
                List<String> errors = trackerAccessManager.canRead( actingUser, programInstance, true );
                if ( errors.isEmpty() )
                {
                    enrollments.add( getEnrollmentFromProgramInstance( programInstance ) );
                }
                else
                {
                    /// ??? // what shall we do here?, this exception seems out of place...
                    throw new IllegalQueryException( errors.toString() );
                }
            }
        }

        return enrollments;
    }

    public Enrollment getEnrollmentFromProgramInstance( ProgramInstance programInstance )
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( programInstance.getUid() );

        if ( programInstance.getEntityInstance() != null )
        {
            enrollment.setTrackedEntityType( programInstance.getEntityInstance().getTrackedEntityType().getUid() );
            enrollment.setTrackedEntityInstance( programInstance.getEntityInstance().getUid() );
        }

        if ( programInstance.getOrganisationUnit() != null )
        {
            enrollment.setOrgUnit( programInstance.getOrganisationUnit().getUid() );
            enrollment.setOrgUnitName( programInstance.getOrganisationUnit().getName() );
        }

        if ( programInstance.getGeometry() != null )
        {
            enrollment.setGeometry( programInstance.getGeometry() );

            if ( programInstance.getProgram().getFeatureType().equals( FeatureType.POINT ) )
            {
                com.vividsolutions.jts.geom.Coordinate co = programInstance.getGeometry().getCoordinate();
                enrollment.setCoordinate( new Coordinate( co.x, co.y ) );
            }
        }

        enrollment.setCreated( DateUtils.getIso8601NoTz( programInstance.getCreated() ) );
        enrollment.setCreatedAtClient( DateUtils.getIso8601NoTz( programInstance.getCreatedAtClient() ) );
        enrollment.setLastUpdated( DateUtils.getIso8601NoTz( programInstance.getLastUpdated() ) );
        enrollment.setLastUpdatedAtClient( DateUtils.getIso8601NoTz( programInstance.getLastUpdatedAtClient() ) );
        enrollment.setProgram( programInstance.getProgram().getUid() );
        enrollment.setStatus( EnrollmentStatus.fromProgramStatus( programInstance.getStatus() ) );
        enrollment.setEnrollmentDate( programInstance.getEnrollmentDate() );
        enrollment.setIncidentDate( programInstance.getIncidentDate() );
        enrollment.setFollowup( programInstance.getFollowup() );
        enrollment.setCompletedDate( programInstance.getEndDate() );
        enrollment.setCompletedBy( programInstance.getCompletedBy() );
        enrollment.setStoredBy( programInstance.getStoredBy() );
        enrollment.setDeleted( programInstance.isDeleted() );

        List<TrackedEntityComment> comments = programInstance.getComments();

        for ( TrackedEntityComment comment : comments )
        {
            Note note = new Note();

            note.setNote( comment.getUid() );
            note.setValue( comment.getCommentText() );
            note.setStoredBy( comment.getCreator() );
            note.setStoredDate( DateUtils.getIso8601NoTz( comment.getCreated() ) );

            enrollment.getNotes().add( note );
        }

        return enrollment;
    }

    private boolean checkAssigned( Attribute attribute, List<String> oldFileValues )
    {
        FileResource fileResource = fileResourceService.getFileResource( attribute.getValue() );
        return fileResource != null && fileResource.isAssigned() && !oldFileValues.contains( attribute.getValue() );
    }

}
