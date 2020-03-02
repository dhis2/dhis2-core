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
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EventCategoryOptValidationHook
    extends AbstractTrackerValidationHook
{
    @Autowired
    protected I18nManager i18nManager;

    @Override
    public int getOrder()
    {
        return 303;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );
        User actingUser = bundle.getPreheat().getUser();

        for ( Event event : bundle.getEvents() )
        {
            reporter.increment( event );

            ProgramStage programStage = PreheatHelper.getProgramStage( bundle, event.getProgramStage() );
            ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, event.getEnrollment() );
            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTrackedEntityInstance( bundle, event.getTrackedEntity() );
            Program program = PreheatHelper.getProgram( bundle, event.getProgram() );

            if ( program == null )
            {
                continue;
            }

            programStage = (programStage == null && program.isWithoutRegistration())
                ? program.getProgramStageByStage( 1 ) : programStage;
            if ( programStage == null )
            {
                continue;
            }

            programInstance = getProgramInstance( actingUser, programInstance, trackedEntityInstance, program );
            program = programInstance.getProgram();

            validateCategoryOptionCombo( bundle, reporter, actingUser, event, program );
        }

        return reporter.getReportList();
    }

    private void validateCategoryOptionCombo( TrackerBundle bundle, ValidationErrorReporter errorReporter
        , User actingUser, Event event, Program program )
    {
        Objects.requireNonNull( actingUser, Constants.USER_CANT_BE_NULL );
        Objects.requireNonNull( program, Constants.PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( event, Constants.EVENT_CANT_BE_NULL );

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
        accessErrors = trackerAccessManager.canWrite( actingUser, categoryOptionCombo );
        if ( !accessErrors.isEmpty() )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1058 )
                .addArg( String.join( ",", accessErrors ) ) );
        }
    }

    private CategoryOptionCombo getAttributeOptionCombo( TrackerBundle bundle, CategoryCombo categoryCombo,
        String cp, String attributeOptionCombo )
    {
        Set<String> opts = TextUtils.splitToArray( cp, TextUtils.SEMICOLON );

        if ( categoryCombo == null )
        {
            throw new IllegalQueryException( "Illegal category combo" );
        }

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

}
