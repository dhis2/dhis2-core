package org.hisp.dhis.tracker.validation.hooks;

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
import org.hisp.dhis.tracker.TrackerErrorCode;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.validation.ValidationHookErrorReporter;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

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
        ValidationHookErrorReporter errorReporter = new ValidationHookErrorReporter( bundle,
            EventValidationHook.class );

        if ( !bundle.getImportStrategy().isCreate() )
        {
            //skip
            return errorReporter.getReportList();
        }

        TrackerPreheat preheat = bundle.getPreheat();
        User user = preheat.getUser();

        List<Event> events = bundle.getEvents();
        for ( Event event : events )
        {
            if ( programStageInstanceService
                .programStageInstanceExistsIncludingDeleted( event.getEvent() ) )
            {
                errorReporter.raiseError( TrackerErrorCode.E1030,
                    event.getEvent() );
                continue;
            }

            boolean hasEventDate = EventStatus.ACTIVE == event.getStatus() && event.getEventDate() == null;
            if ( hasEventDate )
            {
                errorReporter.raiseError( TrackerErrorCode.E1031, event.getEvent() );
                continue;
            }

            ProgramStageInstance programStageInstance = preheat.getEvent( TrackerIdentifier.UID, event.getEvent() );
            boolean programStagePointToInvalidEvent = programStageInstance == null &&
                !StringUtils.isEmpty( event.getEvent() ) &&
                !CodeGenerator.isValidUid( event.getEvent() );
            if ( programStagePointToInvalidEvent )
            {
                errorReporter.raiseError( TrackerErrorCode.E1030, event.getEvent() );
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
                errorReporter.raiseError( TrackerErrorCode.E1033, event.getOrgUnit() );
                continue;
            }

            if ( program == null )
            {
                errorReporter.raiseError( TrackerErrorCode.E1034, event.getProgram() );
                continue;
            }

            programStage = programStage == null && program.isWithoutRegistration() ?
                program.getProgramStageByStage( 1 ) :
                programStage;

            if ( programStage == null )
            {
                errorReporter.raiseError( TrackerErrorCode.E1035, event.getProgramStage() );
                continue;
            }

            if ( program.isRegistration() )
            {
                if ( trackedEntityInstance == null )
                {
                    errorReporter.raiseError( TrackerErrorCode.E1036, event.getTrackedEntityInstance() );
                    continue;
                }

                if ( programInstance == null )
                {
                    List<ProgramInstance> activeProgramInstances = new ArrayList<>( programInstanceService
                        .getProgramInstances( trackedEntityInstance, program, ProgramStatus.ACTIVE ) );

                    if ( activeProgramInstances.isEmpty() )
                    {
                        errorReporter.raiseError( TrackerErrorCode.E1037, trackedEntityInstance.getUid(),
                            program.getUid() );
                        continue;
                    }
                    else if ( activeProgramInstances.size() > 1 )
                    {
                        errorReporter.raiseError( TrackerErrorCode.E1038, trackedEntityInstance.getUid(),
                            program.getUid() );
                        continue;
                    }
                    else
                    {
                        programInstance = activeProgramInstances.get( 0 );
                    }
                }

                if ( !programStage.getRepeatable() && programInstance.hasProgramStageInstance( programStage ) )
                {
                    errorReporter.raiseError( TrackerErrorCode.E1039 );
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
                    errorReporter.raiseError( TrackerErrorCode.E1040, program.getUid() );
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
                errorReporter.raiseError( TrackerErrorCode.E1041, organisationUnit.getUid() );
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
                errorReporter.raiseError( TrackerErrorCode.E1050, errors );
                continue;
            }

            //// LAST VAL
            if ( !validateDates( errorReporter, event ) )
                continue;

            validateCatergoryOptionCombo( errorReporter, user, event, program );

        }

        return errorReporter.getReportList();
    }

    private boolean validateDates( ValidationHookErrorReporter errorReporter, Event event )
    {
        if ( event.getDueDate() != null && !DateUtils.dateIsValid( event.getDueDate() ) )
        {
            errorReporter.raiseError( TrackerErrorCode.E1051, event.getDueDate() );
            return false;
        }

        if ( event.getEventDate() != null && !DateUtils.dateIsValid( event.getEventDate() ) )
        {
            errorReporter.raiseError( TrackerErrorCode.E1052, event.getEventDate() );
            return false;
        }

        if ( event.getCreatedAtClient() != null && !DateUtils.dateIsValid( event.getCreatedAtClient() ) )
        {
            errorReporter.raiseError( TrackerErrorCode.E1053, event.getCreatedAtClient() );
            return false;
        }

        if ( event.getLastUpdatedAtClient() != null && !DateUtils.dateIsValid( event.getLastUpdatedAtClient() ) )
        {
            errorReporter.raiseError( TrackerErrorCode.E1054, event.getLastUpdatedAtClient() );
            return false;
        }

        return true;
    }

    private void validateCatergoryOptionCombo( ValidationHookErrorReporter errorReporter, User user,
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
            errorReporter.raiseError( TrackerErrorCode.E1055 );
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
                errorReporter.raiseError( TrackerErrorCode.E1056, i18nFormat.formatDate( eventDate ),
                    i18nFormat.formatDate( option.getStartDate() ), option.getName() );
                return;
            }
            if ( option.getEndDate() != null && eventDate.compareTo( option.getEndDate() ) > 0 )
            {
                errorReporter.raiseError( TrackerErrorCode.E1057, event.getLastUpdatedAtClient() );
                return;
            }
        }

        errors = trackerAccessManager.canWrite( user, categoryOptionCombo );
        if ( !errors.isEmpty() )
        {
            errorReporter.raiseError( TrackerErrorCode.E1058, errors );
            return;
        }
    }

    private boolean validateGeo( ValidationHookErrorReporter errorReporter,
        Event event,
        ProgramStage programStage )
    {
        if ( event.getGeometry() != null )
        {
            if ( programStage.getFeatureType().equals( FeatureType.NONE ) ||
                !programStage.getFeatureType().value().equals( event.getGeometry().getGeometryType() ) )
            {
                errorReporter.raiseError( TrackerErrorCode.E1048, event.getGeometry().getGeometryType(),
                    programStage.getFeatureType().value(), programStage.getUid() );
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
                errorReporter.raiseError( TrackerErrorCode.E1049 );
                return false;
            }
        }

        return true;
    }

    private boolean validateExpiryDays( ValidationHookErrorReporter errorReporter,
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
                        errorReporter.raiseError( TrackerErrorCode.E1042, program.getUid() );
                        return false;
                    }

                    if ( (new Date()).after(
                        DateUtils.getDateAfterAddition( referenceDate, program.getCompleteEventsExpiryDays() ) ) )
                    {
                        errorReporter.raiseError( TrackerErrorCode.E1043, program.getUid() );
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
                        errorReporter.raiseError( TrackerErrorCode.E1044, program.getUid() );
                        return false;
                    }

                    Period period = periodType.createPeriod( programStageInstance.getExecutionDate() );

                    if ( today.after( DateUtils.getDateAfterAddition( period.getEndDate(), program.getExpiryDays() ) ) )
                    {
                        errorReporter.raiseError( TrackerErrorCode.E1045, program.getUid() );
                        return false;
                    }
                }
                else
                {
                    String referenceDate = event.getEventDate() != null ? event.getEventDate() : event.getDueDate();
                    if ( referenceDate == null )
                    {
                        errorReporter.raiseError( TrackerErrorCode.E1046, program.getUid() );
                        return false;
                    }

                    Period period = periodType.createPeriod( new Date() );

                    if ( DateUtils.parseDate( referenceDate ).before( period.getStartDate() ) )
                    {
                        errorReporter.raiseError( TrackerErrorCode.E1047, program.getUid() );
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
