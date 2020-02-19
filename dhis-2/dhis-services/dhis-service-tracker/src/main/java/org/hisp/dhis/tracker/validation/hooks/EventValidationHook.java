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
import com.google.common.collect.Lists;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.*;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
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
    public int getOrder()
    {
        return 3000;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        if ( !bundle.getImportStrategy().isCreate() )
        {
            return Collections.emptyList();
        }

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle,
            EventValidationHook.class );

        TrackerPreheat preheat = bundle.getPreheat();
        User user = preheat.getUser();

        List<Event> events = bundle.getEvents();
        for ( Event event : events )
        {
            reporter.increment( event );

            boolean exists = programStageInstanceService.programStageInstanceExistsIncludingDeleted( event.getEvent() );
            if ( bundle.getImportStrategy().isCreate() && exists )
            {
                reporter.addError( newReport( TrackerErrorCode.E1030 )
                    .addArg( event ) );
                continue;
            }
            else if ( bundle.getImportStrategy().isUpdate() && !exists )
            {
                reporter.addError( newReport( TrackerErrorCode.E1032 )
                    .addArg( event ) );
                continue;
            }

            if ( EventStatus.ACTIVE == event.getStatus() && event.getEventDate() == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1031 )
                    .addArg( event ) );
                continue;
            }

            ProgramStageInstance programStageInstance = PreheatHelper
                .getProgramStageInstance( bundle, event.getEvent() );

            boolean validId = isValidId( bundle.getIdentifier(), event.getEvent() );
            if ( programStageInstance == null && validId )
            {
                reporter.addError( newReport( TrackerErrorCode.E1071 )
                    .addArg( event ) );
            }

            ProgramStage programStage = PreheatHelper.getProgramStage( bundle, event.getProgramStage() );
            ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, event.getEnrollment() );
            OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, event.getOrgUnit() );
            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTrackedEntityInstance( bundle, event.getTrackedEntityInstance() );

            Program program = PreheatHelper.getProgram( bundle, event.getProgram() );

            if ( organisationUnit == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1011 ).addArg( event.getOrgUnit() ) );
                continue;
            }

            if ( program == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1034 ).addArg( event ) );
                continue;
            }

            programStage = (programStage == null && program.isWithoutRegistration())
                ? program.getProgramStageByStage( 1 ) : programStage;
            if ( programStage == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1035 ).addArg( event ) );
                continue;
            }

            if ( program.isRegistration() )
            {
                if ( trackedEntityInstance == null )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1036 )
                        .addArg( event ) );
                    continue;
                }

                if ( programInstance == null )
                {
                    List<ProgramInstance> activeProgramInstances = new ArrayList<>( programInstanceService
                        .getProgramInstances( trackedEntityInstance, program, ProgramStatus.ACTIVE ) );

                    if ( activeProgramInstances.isEmpty() )
                    {
                        reporter.addError( newReport( TrackerErrorCode.E1037 )
                            .addArg( trackedEntityInstance )
                            .addArg( program ) );
                        continue;
                    }
                    else if ( activeProgramInstances.size() > 1 )
                    {
                        reporter.addError( newReport( TrackerErrorCode.E1038 )
                            .addArg( trackedEntityInstance )
                            .addArg( program ) );
                        continue;
                    }
                    else
                    {
                        programInstance = activeProgramInstances.get( 0 );
                    }
                }

                if ( !programStage.getRepeatable() && programInstance.hasProgramStageInstance( programStage ) )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1039 ) );
                    continue;
                }
            }
            else
            {

                // NOTE: This is cached in the prev. event importer? What do we do here?
                List<ProgramInstance> activeProgramInstances = programInstanceService
                    .getProgramInstances( program, ProgramStatus.ACTIVE );
                if ( activeProgramInstances.isEmpty() )
                {
                    ProgramInstance pi = new ProgramInstance();
                    pi.setEnrollmentDate( new Date() );
                    pi.setIncidentDate( new Date() );
                    pi.setProgram( program );
                    pi.setStatus( ProgramStatus.ACTIVE );
                    pi.setStoredBy( user.getUsername() );

                    programInstance = pi;
                }
                else if ( activeProgramInstances.size() > 1 )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1040 )
                        .addArg( program ) );
                    continue;
                }
                else
                {
                    programInstance = activeProgramInstances.get( 0 );
                }
            }

            program = programInstance.getProgram();

            if ( programStageInstance != null )
            {
                programStage = programStageInstance.getProgramStage();
            }

            if ( !programInstance.getProgram().hasOrganisationUnit( organisationUnit ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1041 ).addArg( organisationUnit ) );
                continue;
            }

            if ( program != null )
            {
                validateExpiryDays( reporter, event, program, programStageInstance );
            }

            validateGeo( reporter,
                event.getGeometry(),
                event.getCoordinate() != null ? event.getCoordinate().getCoordinateString() : null,
                programStage.getFeatureType() );

            ProgramStageInstance newProgramStageInstance = new ProgramStageInstance( programInstance, programStage )
                .setOrganisationUnit( organisationUnit ).setStatus( event.getStatus() );

            List<String> errors = trackerAccessManager.canCreate( user, newProgramStageInstance, false );
            if ( !errors.isEmpty() )
            {
                reporter.addError( newReport( TrackerErrorCode.E1050 )
                    .addArg( user )
                    .addArg( String.join( ",", errors ) ) );
                continue;
            }

            if ( !validateDates( reporter, event ) )
            {
                continue;
            }

            validateCategoryOptionCombo( bundle, reporter, user, event, program );

        }

        return reporter.getReportList();
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

    private CategoryOptionCombo getAttributeOptionCombo( TrackerBundle bundle, CategoryCombo categoryCombo,
        String cp, String attributeOptionCombo )
    {
        Set<String> opts = TextUtils.splitToArray( cp, TextUtils.SEMICOLON );

        if ( categoryCombo == null )
        {
            throw new IllegalQueryException( "Illegal category combo" );
        }

        // ---------------------------------------------------------------------
        // Attribute category options validation
        // ---------------------------------------------------------------------

        CategoryOptionCombo attrOptCombo = null;

        if ( opts != null )
        {
            Set<CategoryOption> categoryOptions = new HashSet<>();

            for ( String uid : opts )
            {
                CategoryOption categoryOption = bundle.getPreheat()
                    .get( bundle.getIdentifier(), CategoryOption.class, uid );

                if ( categoryOption == null )
                {
                    throw new IllegalQueryException( "Illegal category option identifier: " + uid );
                }

                categoryOptions.add( categoryOption );
            }

            List<String> options = Lists.newArrayList( opts );
            Collections.sort( options );

            String cacheKey = categoryCombo.getUid() + "-" + Joiner.on( "-" ).join( options );
            attrOptCombo = bundle.getPreheat().get( bundle.getIdentifier(), CategoryOptionCombo.class, cacheKey );

            if ( attrOptCombo == null )
            {
                throw new IllegalQueryException(
                    "Attribute option combo does not exist for given category combo and category options" );
            }
        }
        else if ( attributeOptionCombo != null )
        {
            attrOptCombo = bundle.getPreheat()
                .get( bundle.getIdentifier(), CategoryOptionCombo.class, attributeOptionCombo );
        }

        // ---------------------------------------------------------------------
        // Fall back to default category option combination
        // ---------------------------------------------------------------------

        if ( attrOptCombo == null )
        {
            attrOptCombo = (CategoryOptionCombo) bundle.getPreheat().getDefaults().get( CategoryOptionCombo.class );
        }

        if ( attrOptCombo == null )
        {
            throw new IllegalQueryException( "Default attribute option combo does not exist" );
        }

        return attrOptCombo;
    }

    private void validateCategoryOptionCombo( TrackerBundle bundle, ValidationErrorReporter errorReporter
        , User user, Event event, Program program )
    {

        // NOTE: Morten H. & Stian. Abyot : How do we solve this in the new importer?
//        CategoryOptionCombo categoryOptionCombo;
//        if ( (event.getAttributeCategoryOptions() != null
//            && program.getCategoryCombo() != null)
//            || event.getAttributeOptionCombo() != null )
//        {
//            try
//            {
//                categoryOptionCombo = getAttributeOptionCombo( bundle,
//                    program.getCategoryCombo(),
//                    event.getAttributeCategoryOptions(),
//                    event.getAttributeOptionCombo() );
//            }
//            catch ( IllegalQueryException e )
//            {
//                errorReporter.addError( newReport( TrackerErrorCode.E1072 )
//                    .addArg( event.getAttributeCategoryOptions() )
//                    .addArg( e.getMessage() ) );
//                return;
//            }
//        }
//        else
//        {
//            categoryOptionCombo = (CategoryOptionCombo) bundle.getPreheat().getDefaults()
//                .get( CategoryOptionCombo.class );
//        }

        CategoryOptionCombo categoryOptionCombo = (CategoryOptionCombo) bundle.getPreheat().getDefaults()
            .get( CategoryOptionCombo.class );
        if ( categoryOptionCombo == null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1055 ) );
            return;
        }

        if ( categoryOptionCombo.isDefault()
            && program.getCategoryCombo() != null
            && !program.getCategoryCombo().isDefault() )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1055 ) );
            return;
        }

        // NOTE: How to best get current date into iso format?
        Date eventDate = DateUtils.parseDate( ObjectUtils
            .firstNonNull( event.getEventDate(), event.getDueDate(), DateUtils.getIso8601( new Date() ) ) );

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
                errorReporter.addError( newReport( TrackerErrorCode.E1057 ).
                    addArg( event.getLastUpdatedAtClient() ) );
                return;
            }
        }

        List<String> accessErrors;
        accessErrors = trackerAccessManager.canWrite( user, categoryOptionCombo );
        if ( !accessErrors.isEmpty() )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1058 )
                .addArg( String.join( ",", accessErrors ) ) );
        }
    }

    private void validateExpiryDays( ValidationErrorReporter errorReporter,
        Event event,
        Program program,
        ProgramStageInstance programStageInstance )
    {
//        if ( importOptions == null || importOptions.getUser() == null ||
//            importOptions.getUser().isAuthorized( Authorities.F_EDIT_EXPIRED.getAuthority() ) )
//        {
//            return;
//        }

        if ( program == null )
        {
            return;
        }

        if ( program.getCompleteEventsExpiryDays() > 0 )
        {
            if ( EventStatus.COMPLETED == event.getStatus()
                || (programStageInstance != null && EventStatus.COMPLETED == programStageInstance.getStatus()) )
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
                    errorReporter.addError( newReport( TrackerErrorCode.E1042 )
                        .addArg( event ) );
                }

                if ( (new Date()).after(
                    DateUtils.getDateAfterAddition( referenceDate, program.getCompleteEventsExpiryDays() ) ) )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1043 )
                        .addArg( event ) );
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
                }

                Period period = periodType.createPeriod( programStageInstance.getExecutionDate() );

                if ( today.after( DateUtils.getDateAfterAddition( period.getEndDate(), program.getExpiryDays() ) ) )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1045 ).addArg( event ) );
                }
            }
            else
            {
                String referenceDate = event.getEventDate() != null ? event.getEventDate() : event.getDueDate();
                if ( referenceDate == null )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1046 ).addArg( event ) );
                }

                Period period = periodType.createPeriod( new Date() );

                if ( DateUtils.parseDate( referenceDate ).before( period.getStartDate() ) )
                {
                    errorReporter.addError( newReport( TrackerErrorCode.E1047 ).addArg( event ) );

                }
            }
        }

    }

}
