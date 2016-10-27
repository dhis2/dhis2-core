package org.hisp.dhis.dxf2.dataset;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.util.DateUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author Halvdan Hoem Grelland
 */
public interface CompleteDataSetRegistrationService
{
    ExportParams getFromUrl( Set<String> dataSets, Set<String> orgUnits, Set<String> orgUnitGroups, Set<String> periods,
        Date startDate, Date endDate, boolean includeChildren, Date created, String createdDuration, Integer limit, IdSchemes idSchemes );

    default void validate( ExportParams params ) throws IllegalQueryException
    {
        Consumer<String> error = message -> { throw new IllegalQueryException( message ); };

        if ( params == null )
        {
            throw new IllegalArgumentException( "ExportParams must be non-null" );
        }

        if ( params.getDataSets().isEmpty() )
        {
            error.accept( "At least one data set must be specified" );
        }

        if ( !params.hasPeriods() && !params.hasStartEndDate() && !params.hasCreated() && !params.hasCreatedDuration() )
        {
            error.accept( "At least one valid period, start/end dates, created or created duration must be specified" );
        }

        if ( params.hasPeriods() && params.hasStartEndDate() )
        {
            error.accept( "Both periods and start/end date cannot be specified" );
        }

        if ( params.hasStartEndDate() && params.getStartDate().after( params.getEndDate() ) )
        {
            error.accept( "Start date must be before end date" );
        }

        if ( params.hasCreatedDuration() && DateUtils.getDuration( params.getCreatedDuration() ) == null )
        {
            error.accept( "Duration is not valid: " + params.getCreatedDuration() );
        }

        if ( !params.hasOrganisationUnits() && !params.hasOrganisationUnitGroups() )
        {
            error.accept( "At least one valid organisation unit or organisation unit group must be specified" );
        }

        if ( params.isIncludeChildren() && params.hasOrganisationUnitGroups() )
        {
            error.accept( "Children cannot be included for organisation unit groups" );
        }

        if ( params.isIncludeChildren() && !params.hasOrganisationUnits() )
        {
            error.accept( "At least one organisation unit must be specified when children are included" );
        }

        if ( params.hasLimit() && params.getLimit() < 0 )
        {
            error.accept( "Limit cannot be less than zero: " + params.getLimit() );
        }
    }

    void decideAccess( ExportParams params ) throws IllegalQueryException;

    void writeCompleteDataSetRegistrationsXml( ExportParams params, OutputStream out );

    void writeCompleteDataSetRegistrationsJson( ExportParams params, OutputStream out );

    ImportSummary saveCompleteDataSetRegistrationsXml( InputStream in );

    ImportSummary saveCompleteDataSetRegistrationsXml( InputStream in, ImportOptions importOptions );

    ImportSummary saveCompleteDataSetRegistrationsXml( InputStream in, ImportOptions importOptions, TaskId taskId );

    ImportSummary saveCompleteDataSetRegistrationsJson( InputStream in );

    ImportSummary saveCompleteDataSetRegistrationsJson( InputStream in, ImportOptions importOptions );

    ImportSummary saveCompleteDataSetRegistrationsJson( InputStream in, ImportOptions importOptions, TaskId taskId );
}
