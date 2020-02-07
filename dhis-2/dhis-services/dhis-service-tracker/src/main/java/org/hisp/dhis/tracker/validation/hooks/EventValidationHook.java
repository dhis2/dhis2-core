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

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.*;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
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

import java.io.IOException;
import java.util.*;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EventValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        if ( !bundle.getImportStrategy().isCreate() )
        {
            return Collections.emptyList();
        }

        ValidationErrorReporter errorReporter = new ValidationErrorReporter( bundle,
            EventValidationHook.class );

        TrackerPreheat preheat = bundle.getPreheat();
        User user = preheat.getUser();

        List<Event> events = bundle.getEvents();
        for ( Event event : events )
        {
            if ( programStageInstanceService
                .programStageInstanceExistsIncludingDeleted( event.getEvent() ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1030 )
                    .addArg( event ) );
                continue;
            }

            boolean hasEventDate = EventStatus.ACTIVE == event.getStatus() && event.getEventDate() == null;
            if ( hasEventDate )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1031 ).addArg( event ) );
                continue;
            }

            ProgramStageInstance programStageInstance = preheat.getEvent( TrackerIdentifier.UID, event.getEvent() );
            boolean programStagePointToInvalidEvent = programStageInstance == null &&
                !StringUtils.isEmpty( event.getEvent() ) &&
                !CodeGenerator.isValidUid( event.getEvent() );
            if ( programStagePointToInvalidEvent )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1030 ).addArg( event ) );
                break;
            }

            Program program = preheat
                .get( TrackerIdentifier.UID, Program.class, event.getProgram() );
            ProgramStage programStage = preheat
                .get( TrackerIdentifier.UID, ProgramStage.class, event.getProgramStage() );
            OrganisationUnit organisationUnit = preheat
                .get( TrackerIdentifier.UID, OrganisationUnit.class, event.getOrgUnit() );
            TrackedEntityInstance trackedEntityInstance = preheat
                .get( TrackerIdentifier.UID, TrackedEntityInstance.class, event.getTrackedEntityInstance() );
            ProgramInstance programInstance = preheat
                .get( TrackerIdentifier.UID, ProgramInstance.class, event.getEnrollment() );
            User assignedUser = preheat.get( TrackerIdentifier.UID, User.class, event.getAssignedUser() );

            if ( organisationUnit == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1011 ).addArg( event.getOrgUnit() ) );
                continue;
            }

            if ( program == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1034 ).addArg( event ) );
                continue;
            }

            programStage = programStage == null && program.isWithoutRegistration() ?
                program.getProgramStageByStage( 1 ) :
                programStage;

            if ( programStage == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1035 ).addArg( event ) );
                continue;
            }

            if ( program.isRegistration() )
            {
                if ( trackedEntityInstance == null )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1036 ).addArg( event ) );
                    continue;
                }

                if ( programInstance == null )
                {
                    List<ProgramInstance> activeProgramInstances = new ArrayList<>( programInstanceService
                        .getProgramInstances( trackedEntityInstance, program, ProgramStatus.ACTIVE ) );

                    if ( activeProgramInstances.isEmpty() )
                    {
                        errorReporter.addError( newReport( TrackerErrorCode.E1037 )
                            .addArg( trackedEntityInstance ).addArg( program ) );
                        continue;
                    }
                    else if ( activeProgramInstances.size() > 1 )
                    {
                        errorReporter.addError( newReport( TrackerErrorCode.E1038 )
                            .addArg( trackedEntityInstance ).addArg( program ) );
                        continue;
                    }
                    else
                    {
                        programInstance = activeProgramInstances.get( 0 );
                    }
                }

                if ( !programStage.getRepeatable() && programInstance.hasProgramStageInstance( programStage ) )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1039 ) );
                    continue;
                }
            }
            else
            {
                String cacheKey = program.getUid() + "-" + ProgramStatus.ACTIVE;
                List<ProgramInstance> activeProgramInstances = null; // getActiveProgramInstances( cacheKey, program );
                if ( activeProgramInstances.isEmpty() )
                {
                    // Create PI if it doesn't exist (should only be one)

//                    String storedBy = getValidUsername( event.getStoredBy(), null, importOptions.getUser() != null ?
//                        importOptions.getUser().getUsername() : "[Unknown]" );

                    ProgramInstance pi = new ProgramInstance();
                    pi.setEnrollmentDate( new Date() );
                    pi.setIncidentDate( new Date() );
                    pi.setProgram( program );
                    pi.setStatus( ProgramStatus.ACTIVE );
//                    pi.setStoredBy( storedBy ); ?????????

                    programInstanceService.addProgramInstance( pi );

                    activeProgramInstances.add( pi );
                }
                else if ( activeProgramInstances.size() > 1 )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1040 ).addArg( program ) );
                    continue;
                }

                /// MUTATES !!?!?!?!
                programInstance = activeProgramInstances.get( 0 );
            }

            /// MUTATES !!?!?!?!
            program = programInstance.getProgram();
            if ( programStageInstance != null )
            {
                /// MUTATES !!?!?!?!
                programStage = programStageInstance.getProgramStage();
            }

            if ( !programInstance.getProgram().hasOrganisationUnit( organisationUnit ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1041 ).addArg( organisationUnit ) );
                continue;
            }

            if ( !validateExpiryDays( errorReporter, event, program, programStageInstance ) )
                continue;

            if ( !validateGeo( errorReporter, event, programStage ) )
                continue;

            ProgramStageInstance newProgramStageInstance = new ProgramStageInstance( programInstance, programStage )
                .setOrganisationUnit( organisationUnit ).setStatus( event.getStatus() );

            List<String> errors = trackerAccessManager.canCreate( user, newProgramStageInstance, false );
            if ( !errors.isEmpty() )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1050 )
                    .addArg( user ).addArg( String.join( ",", errors ) ) );
                continue;
            }

            //// LAST VAL
            if ( !validateDates( errorReporter, event ) )
                continue;

            validateCatergoryOptionCombo( errorReporter, user, event, program );

        }

        return errorReporter.getReportList();
    }

    private boolean validateDates( ValidationErrorReporter errorReporter, Event event )
    {
        if ( event.getDueDate() != null && !DateUtils.dateIsValid( event.getDueDate() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1051 ).addArg( event.getDueDate() ) );
            return false;
        }

        if ( event.getEventDate() != null && !DateUtils.dateIsValid( event.getEventDate() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1052 ).addArg( event.getEventDate() ) );
            return false;
        }

        if ( event.getCreatedAtClient() != null && !DateUtils.dateIsValid( event.getCreatedAtClient() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1053 ).addArg( event.getCreatedAtClient() ) );
            return false;
        }

        if ( event.getLastUpdatedAtClient() != null && !DateUtils.dateIsValid( event.getLastUpdatedAtClient() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1054 ).addArg( event.getLastUpdatedAtClient() ) );
            return false;
        }

        return true;
    }

    private void validateCatergoryOptionCombo( ValidationErrorReporter errorReporter, User user,
        Event event, Program program )
    {
        List<String> errors;
        CategoryOptionCombo categoryOptionCombo = null;
        if ( (event.getAttributeCategoryOptions() != null && program.getCategoryCombo() != null)
            || event.getAttributeOptionCombo() != null )
        {
            try
            {
                /// ?????
//                    categoryOptionCombo = getAttributeOptionCombo( program.getCategoryCombo(), event.getAttributeCategoryOptions(),
//                        event.getAttributeOptionCombo(), idScheme );
            }
            catch ( IllegalQueryException ex )
            {
//                    importSummary.getConflicts().add( new ImportConflict( ex.getMessage(), event.getAttributeCategoryOptions() ) );
//                    importSummary.setStatus( ImportStatus.ERROR );
//                    return importSummary.incrementIgnored();
            }
        }
        else
        {
//           ????     categoryOptionCombo = (CategoryOptionCombo) getDefaultObject( CategoryOptionCombo.class );
        }

        if ( categoryOptionCombo != null && categoryOptionCombo.isDefault() && program.getCategoryCombo() != null &&
            !program.getCategoryCombo().isDefault() )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1055 ) );
            return;
        }

        Date executionDate = null;
        if ( event.getEventDate() != null )
        {
            executionDate = DateUtils.parseDate( event.getEventDate() );
        }
        Date dueDate = new Date();
        if ( event.getDueDate() != null )
        {
            dueDate = DateUtils.parseDate( event.getDueDate() );
        }
        Date eventDate = executionDate != null ? executionDate : dueDate;
        I18nFormat i18nFormat = i18nManager.getI18nFormat();
        for ( CategoryOption option : categoryOptionCombo.getCategoryOptions() )
        {
            if ( option.getStartDate() != null && eventDate.compareTo( option.getStartDate() ) < 0 )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1056 )
                    .addArg( i18nFormat.formatDate( eventDate ) )
                    .addArg( i18nFormat.formatDate( option.getStartDate() ) )
                    .addArg( option.getName() ) );

                return;
            }
            if ( option.getEndDate() != null && eventDate.compareTo( option.getEndDate() ) > 0 )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1057 ).addArg( event.getLastUpdatedAtClient() ) );
                return;
            }
        }

        errors = trackerAccessManager.canWrite( user, categoryOptionCombo );
        if ( !errors.isEmpty() )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1058 ).addArg( String.join( ",", errors ) ) );
            return;
        }
    }

    private boolean validateGeo( ValidationErrorReporter errorReporter,
        Event event,
        ProgramStage programStage )
    {
        if ( event.getGeometry() != null )
        {
            if ( programStage.getFeatureType().equals( FeatureType.NONE ) ||
                !programStage.getFeatureType().value().equals( event.getGeometry().getGeometryType() ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1048 )
                    .addArg( event.getGeometry().getGeometryType() )
                    .addArg( programStage.getFeatureType().value() )
                    .addArg( programStage ) );
                return false;
            }

            event.getGeometry().setSRID( GeoUtils.SRID );
        }
        else if ( event.getCoordinate() != null && event.getCoordinate().hasLatitudeLongitude() )
        {
            Coordinate coordinate = event.getCoordinate();
            try
            {
                event.setGeometry( GeoUtils.getGeoJsonPoint( coordinate.getLongitude(), coordinate.getLatitude() ) );
            }
            catch ( IOException e )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1049 ) );
                return false;
            }
        }

        return true;
    }

    private boolean validateExpiryDays( ValidationErrorReporter errorReporter,
        Event event,
        Program program,
        ProgramStageInstance programStageInstance )
    {
//        if ( importOptions == null || importOptions.getUser() == null ||
//            importOptions.getUser().isAuthorized( Authorities.F_EDIT_EXPIRED.getAuthority() ) )
//        {
//            return;
//        }

        if ( program != null )
        {
            if ( program.getCompleteEventsExpiryDays() > 0 )
            {
                if ( event.getStatus() == EventStatus.COMPLETED
                    || programStageInstance != null && programStageInstance.getStatus() == EventStatus.COMPLETED )
                {
                    Date referenceDate = null;

                    if ( programStageInstance != null )
                    {
                        referenceDate = programStageInstance.getCompletedDate();
                    }

                    else
                    {
                        if ( event.getCompletedDate() != null )
                        {
                            referenceDate = DateUtils.parseDate( event.getCompletedDate() );
                        }
                    }

                    if ( referenceDate == null )
                    {
                        errorReporter.addError( newReport( TrackerErrorCode.E1042 ).addArg( event ) );
                        return false;
                    }

                    if ( (new Date()).after(
                        DateUtils.getDateAfterAddition( referenceDate, program.getCompleteEventsExpiryDays() ) ) )
                    {
                        errorReporter.addError( newReport( TrackerErrorCode.E1043 ).addArg( event ) );
                        return false;
                    }
                }
            }

            PeriodType periodType = program.getExpiryPeriodType();

            if ( periodType != null && program.getExpiryDays() > 0 )
            {
                if ( programStageInstance != null )
                {
                    Date today = new Date();

                    if ( programStageInstance.getExecutionDate() == null )
                    {
                        errorReporter.addError( newReport( TrackerErrorCode.E1044 ).addArg( event ) );
                        return false;
                    }

                    Period period = periodType.createPeriod( programStageInstance.getExecutionDate() );

                    if ( today.after( DateUtils.getDateAfterAddition( period.getEndDate(), program.getExpiryDays() ) ) )
                    {
                        errorReporter.addError( newReport( TrackerErrorCode.E1045 ).addArg( event ) );
                        return false;
                    }
                }
                else
                {
                    String referenceDate = event.getEventDate() != null ? event.getEventDate() : event.getDueDate();
                    if ( referenceDate == null )
                    {
                        errorReporter.addError( newReport( TrackerErrorCode.E1046 ).addArg( event ) );
                        return false;
                    }

                    Period period = periodType.createPeriod( new Date() );

                    if ( DateUtils.parseDate( referenceDate ).before( period.getStartDate() ) )
                    {
                        errorReporter.addError( newReport( TrackerErrorCode.E1047 ).addArg( event ) );

                        return false;
                    }
                }
            }
        }
        return true;
    }

}
