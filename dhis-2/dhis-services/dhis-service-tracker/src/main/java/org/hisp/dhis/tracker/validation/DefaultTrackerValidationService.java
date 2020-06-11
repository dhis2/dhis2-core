package org.hisp.dhis.tracker.validation;

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

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hisp.dhis.tracker.validation.TrackerImportValidationConfig.VALIDATION_ORDER;
import static org.hisp.dhis.tracker.validation.TrackerImportValidationConfig.VALIDATION_ORDER_MAP;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service
public class DefaultTrackerValidationService
    implements TrackerValidationService
{
    private List<TrackerValidationHook> validationHooks = new ArrayList<>();

    @Autowired( required = false )
    public void setValidationHooks( List<TrackerValidationHook> validationHooks )
    {
        this.validationHooks = validationHooks;

        if ( !VALIDATION_ORDER
            .containsAll(
                validationHooks.stream().map( TrackerValidationHook::getClass ).collect( Collectors.toList() ) ) )
        {
            String orderList = VALIDATION_ORDER.stream().map( Class::getName )
                .collect( Collectors.joining( "," ) );

            String internList = validationHooks.stream().map( i -> i.getClass().getName() )
                .collect( Collectors.joining( "," ) );

            throw new RuntimeException(
                String.format(
                    "ValidationConfig.class is missing a validation hook in the validation order list, please add it to the list. Order list: %s, service list: %s",
                    orderList, internList ) );
        }

        this.validationHooks.sort( Comparator.comparingInt( o -> VALIDATION_ORDER_MAP.get( o.getClass() ) ) );
    }

    @Override
    public TrackerValidationReport validate( TrackerBundle bundle )
    {
        TrackerValidationReport validationReport = new TrackerValidationReport();

        User user = bundle.getUser();

        if ( (user == null || user.isSuper()) && ValidationMode.SKIP == bundle.getValidationMode() )
        {
            log.warn( "Skipping validation for metadata import by user '" +
                bundle.getUsername() + "'. Not recommended." );
            return validationReport;
        }

        TrackerImportValidationContext context = new TrackerImportValidationContext( bundle );

        try
        {
            for ( TrackerValidationHook hook : validationHooks )
            {
                if ( hook.isEnabled() )
                {
                    List<TrackerErrorReport> errors = hook.validate( context );
                    if ( !errors.isEmpty() )
                    {
                        validationReport.add( errors );
                    }
                }
            }
        }
        catch ( ValidationFailFastException e )
        {
            validationReport.add( e.getErrors() );
        }

        if ( validationReport.getErrorReports().size() > 0 )
        {
            printReport( validationReport );
        }

        return validationReport;
    }

    protected void printReport( TrackerValidationReport report )
    {
        for ( TrackerErrorReport errorReport : report.getErrorReports() )
        {
            log.error( errorReport.toString() );
        }
    }
}
