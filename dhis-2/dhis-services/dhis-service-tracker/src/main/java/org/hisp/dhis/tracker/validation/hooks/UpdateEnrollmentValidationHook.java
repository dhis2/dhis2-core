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
public class UpdateEnrollmentValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 250;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        if ( !bundle.getImportStrategy().isUpdate() )
        {
            return Collections.emptyList();
        }

        ValidationErrorReporter errorReporter = new ValidationErrorReporter( bundle,
            UpdateEnrollmentValidationHook.class );

        TrackerPreheat preheat = bundle.getPreheat();
        User user = preheat.getUser();

        List<Enrollment> enrollments = bundle.getEnrollments();
        for ( Enrollment enrollment : enrollments )
        {
            ProgramInstance pi = preheat
                .get( TrackerIdentifier.UID, ProgramInstance.class, enrollment.getEnrollment() );
            if ( pi == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1015 )
                    .addArg( enrollment ).addArg( enrollment.getEnrollment() ) );
                continue;
            }

            List<String> errors = trackerAccessManager.canUpdate( user, pi, false );
            if ( !errors.isEmpty() )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1000 )
                    .addArg( user ).addArg( pi ) );
                continue;
            }

//            ProgramInstance programInstance = preheat
//                .getEnrollment( TrackerIdentifier.UID, enrollment.getEnrollment() );
            Program program = preheat.get( TrackerIdentifier.UID, Program.class, enrollment.getProgram() );
            TrackedEntityInstance tei = preheat
                .getTrackedEntity( TrackerIdentifier.UID, enrollment.getTrackedEntityInstance() );
            if ( !validateAttributes( errorReporter, program, enrollment, tei, bundle ) )
                continue;

            OrganisationUnit organisationUnit = preheat
                .get( TrackerIdentifier.UID, OrganisationUnit.class, enrollment.getOrgUnit() );
            // ----validateRequest
            // Enrollment(¶4.a) - can only enroll if registration!
            if ( !program.isRegistration() )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1014 )
                    .addArg( program ) );
                continue;

            }

            ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
            params.setSkipPaging( true );
            params.setProgram( program );
            params.setTrackedEntityInstance( tei );

            EnrollmentStatus status = enrollment.getStatus();
            if ( status != EnrollmentStatus.CANCELLED )
            {
                List<Enrollment> programEnrollments = getEnrollments(
                    programInstanceService.getProgramInstances( params ) );

                Set<Enrollment> activeEnrollments = programEnrollments.stream()
                    .filter( e -> e.getStatus() == EnrollmentStatus.ACTIVE )
                    .collect( Collectors.toSet() );

                // Enrollment(¶4.b.i) - When an enrollment with status COMPLETED or CANCELLED is being imported,
                // no check whether there is already some ACTIVE one is needed
                boolean isActiveEnrollment =
                    !activeEnrollments.isEmpty() && enrollment.getStatus() == EnrollmentStatus.ACTIVE;
                if ( isActiveEnrollment )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1015 )
                        .addArg( tei ).addArg( enrollment ) );
                    continue;
                }

                // Enrollment(¶4.b.ii) - The error of enrolling more than once is possible only if the imported enrollment
                // has a state other than CANCELLED
                if ( program.getOnlyEnrollOnce() )
                {
                    Set<Enrollment> activeOrCompletedEnrollments = programEnrollments.stream()
                        .filter( e -> e.getStatus() == EnrollmentStatus.ACTIVE ||
                            e.getStatus() == EnrollmentStatus.COMPLETED )
                        .collect( Collectors.toSet() );

                    if ( !activeOrCompletedEnrollments.isEmpty() )
                    {
                        errorReporter.addError( newReport( TrackerErrorCode.E1016 )
                            .addArg( tei ).addArg( program ) );
                        continue;
                    }
                }
            }
            // ----validateRequest END

            boolean isNotSameTrackedEntity = program.getTrackedEntityType() != null &&
                !program.getTrackedEntityType().equals( tei.getTrackedEntityType() );
            if ( isNotSameTrackedEntity )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1022 )
                    .addArg( tei ).addArg( program ) );

                continue;
            }

            ProgramInstance programInstance =
                createTempProgramInstance( tei, program, enrollment, organisationUnit,
                    enrollment.getEnrollment() );
            if ( !validateProgramInstance( errorReporter, program, programInstance, enrollment ) )
                continue;

            if ( !trackerOwnershipManager.hasAccess( user, tei, program ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1028 )
                    .addArg( user )
                    .addArg( tei ).addArg( program ) );
                continue;
            }

//          HOW???  validateGeo( fullReport, )

            for ( Note note : enrollment.getNotes() )
            {
                if ( CodeGenerator.isValidUid( note.getNote() ) &&
                    commentService.trackedEntityCommentExists( note.getNote() ) &&
                    !StringUtils.isEmpty( note.getValue() ) )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1029 )
                        .addArg( note.getNote() ).addArg( enrollment ) );
                    continue;
                }
            }
        }

        return errorReporter.getReportList();
    }

    private boolean validateAttributes( ValidationErrorReporter errorReporter,
        Program program,
        Enrollment enrollment,
        TrackedEntityInstance trackedEntityInstance,
        TrackerBundle bundle )
    {

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
                errorReporter.addError( newReport( TrackerErrorCode.E1007 ).addArg( errorMessage ) );
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
                    errorReporter.addError( newReport( TrackerErrorCode.E1064 ).addArg( errorMessage ) );
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
                errorReporter.addError( newReport( TrackerErrorCode.E1020 )
                    .addArg( enrollment.getEnrollmentDate() ) );
                return false;
            }
        }

        if ( !program.getSelectIncidentDatesInFuture() )
        {
            boolean incidentIsInFuture =
                Objects.nonNull( enrollment.getIncidentDate() ) && enrollment.getIncidentDate().after( new Date() );
            if ( incidentIsInFuture )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1021 )
                    .addArg( enrollment.getIncidentDate() ) );
                return false;
            }
        }

        return true;
    }

    private boolean validateProgramInstance( ValidationErrorReporter errorReporter,
        Program program,
        ProgramInstance programInstance,
        Enrollment enrollment )
    {
        boolean incidentDateIsNull = program.getDisplayIncidentDate() &&
            programInstance.getIncidentDate() == null;
        if ( incidentDateIsNull )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1023 ) );
            return false;
        }

        boolean validEnrollmentIncidentDate = programInstance.getIncidentDate() != null &&
            DateUtils.dateIsValid( DateUtils.getMediumDateString( programInstance.getIncidentDate() ) );
        if ( !validEnrollmentIncidentDate )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1024 )
                .addArg( programInstance.getIncidentDate() ) );
            return false;
        }

        boolean validEnrollmentDate = programInstance.getEnrollmentDate() != null &&
            DateUtils.dateIsValid( DateUtils.getMediumDateString( programInstance.getEnrollmentDate() ) );
        if ( !validEnrollmentDate )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1025 )
                .addArg( programInstance.getEnrollmentDate() ) );
            return false;
        }

        boolean validEnrollmentCreatedAtClientDate = enrollment.getCreatedAtClient() != null &&
            DateUtils.dateIsValid( enrollment.getCreatedAtClient() );
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
                .addArg( programInstance.getIncidentDate() ) );
            return false;
        }

        return true;
    }

    private ProgramInstance createTempProgramInstance(
        TrackedEntityInstance trackedEntityInstance,
        Program program,
        Enrollment enrollment,
        OrganisationUnit organisationUnit,
        String uid )
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( CodeGenerator.isValidUid( uid ) ? uid : CodeGenerator.generateUid() );
        programInstance.setOrganisationUnit( organisationUnit );
        programInstance.enrollTrackedEntityInstance( trackedEntityInstance, program );

        Date enrollmentDate = enrollment.getEnrollmentDate();
        Date incidentDate = enrollment.getIncidentDate();
        if ( enrollmentDate != null )
        {
            programInstance.setEnrollmentDate( enrollmentDate );
        }
        else
        {
            programInstance.setEnrollmentDate( new Date() );
        }

        if ( incidentDate != null )
        {
            programInstance.setIncidentDate( incidentDate );
        }
        else
        {
            programInstance.setIncidentDate( new Date() );
        }

        ProgramStatus programStatus = enrollment.getStatus() == EnrollmentStatus.ACTIVE ?
            ProgramStatus.ACTIVE : enrollment.getStatus() == EnrollmentStatus.COMPLETED ?
            ProgramStatus.COMPLETED :
            ProgramStatus.CANCELLED;
        programInstance.setStatus( programStatus );

        if ( programStatus == ProgramStatus.COMPLETED || programStatus == ProgramStatus.CANCELLED )
        {
            Date date = enrollment.getCompletedDate();
            if ( date == null )
            {
                date = new Date();
            }

            String user = enrollment.getCompletedBy();
            if ( user == null )
            {
                user = null; // should be from bundle...
                // importOptions.getUser().getUsername();
            }

            programInstance.setCompletedBy( user );
            programInstance.setEndDate( date );
        }

        return programInstance;
    }

    private boolean doValidationOfMandatoryAttributes( User user )
    {
        return user == null || !user.isAuthorized(
            Authorities.F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION.getAuthority() );
    }

    public List<Enrollment> getEnrollments( Iterable<ProgramInstance> programInstances )
    {
        List<Enrollment> enrollments = new ArrayList<>();
        User user = currentUserService.getCurrentUser();

        for ( ProgramInstance programInstance : programInstances )
        {
            if ( programInstance != null && trackerOwnershipManager.hasAccess( user, programInstance ) )
            {
                enrollments.add( getEnrollment( user, programInstance, true ) );
            }
        }

        return enrollments;
    }

    public Enrollment getEnrollment( User user, ProgramInstance programInstance, boolean skipOwnershipCheck )
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( programInstance.getUid() );

        List<String> errors = trackerAccessManager.canRead( user, programInstance, skipOwnershipCheck );
        if ( !errors.isEmpty() )
        {
            /// ???
            throw new IllegalQueryException( errors.toString() );
        }

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
