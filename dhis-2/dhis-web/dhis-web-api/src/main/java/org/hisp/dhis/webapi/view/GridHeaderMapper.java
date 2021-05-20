/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.view;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.analytics.event.HeaderName.NAME_PROGRAM_STAGE;

import java.util.List;
import java.util.Objects;

import org.hisp.dhis.analytics.event.HeaderName;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageStore;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Specific component responsible for overriding Grid headers based on internal
 * business rules.
 *
 * IMPORTANT: The Grid object is used and shared across many analytics features
 * and endpoints. The client/frontend also relies on the Grid objects for
 * several applications. If this component requires any changes be sure to check
 * what will be the impact.
 * 
 * @author maikel arabori
 */
@Component
@AllArgsConstructor
public class GridHeaderMapper
{
    @NonNull
    private final ProgramStageStore programStageStore;

    /**
     * This method overrides some GridHeader's based on internal rules. See
     * {@link #maybeOverrideHeaderName} to check the current rules.
     * 
     * @param grid
     */
    public void maybeOverrideHeaderNames( final Grid grid )
    {
        if ( grid != null && isNotEmpty( grid.getHeaders() ) )
        {
            final int programStageIndex = getProgramStageIndex( grid.getHeaders() );
            final boolean hasProgramStageSet = programStageIndex != -1;

            if ( hasProgramStageSet )
            {
                final ProgramStage programStage = getProgramStage( grid, programStageIndex );

                if ( programStage != null )
                {
                    final List<GridHeader> currentHeaders = grid.getHeaders();

                    currentHeaders.stream().filter( Objects::nonNull )
                        .forEachOrdered( gridHeader -> maybeOverrideHeaderName( programStage, gridHeader,
                            HeaderName.from( gridHeader.getColumn() ) ) );
                }
            }
        }
    }

    /**
     * This method (maybe) overrides the given HeaderName using the respective
     * ProgramStage label defined by the user, if any. If no label is defined by
     * the user for the given HeaderName, nothing will be overridden.
     * 
     * @param programStage the ProgramStage found in the GridHeader
     * @param gridHeader the current event GridHeader
     * @param headerName the header name to be overridden
     */
    private void maybeOverrideHeaderName( final ProgramStage programStage, final GridHeader gridHeader,
        final HeaderName headerName )
    {
        if ( headerName != null && programStage != null )
        {
            switch ( headerName )
            {
            case NAME_EVENT_DATE:
                if ( isNotBlank( programStage.getExecutionDateLabel() ) )
                {
                    gridHeader.setColumn( programStage.getExecutionDateLabel() );
                }
                break;
            case NAME_ENROLLMENT_DATE:
                if ( programStage.getProgram() != null
                    && isNotBlank( programStage.getProgram().getEnrollmentDateLabel() ) )
                {
                    gridHeader.setColumn( programStage.getProgram().getEnrollmentDateLabel() );
                }
                break;
            case NAME_INCIDENT_DATE:
                if ( programStage.getProgram() != null
                    && isNotBlank( programStage.getProgram().getIncidentDateLabel() ) )
                {
                    gridHeader.setColumn( programStage.getProgram().getIncidentDateLabel() );
                }
                break;
            default:
                // Nothing to replace.
            }
        }
    }

    /**
     * Based on the given Grid and ProgramStage index, this method load the
     * respective ProgramStage and returns it.
     * 
     * @param grid the current event grid
     * @param programStageIndex the ProgramStage index in the Grid
     * @return the respective instance of the Program Stage
     */
    private ProgramStage getProgramStage( final Grid grid, final int programStageIndex )
    {
        ProgramStage programStage = null;

        if ( isNotEmpty( grid.getRows() ) && isNotEmpty( grid.getHeaders() ) )
        {
            final boolean programStageIsPresent = programStageIndex != -1;

            if ( programStageIsPresent )
            {
                final String programStageUid = (String) grid.getRow( 0 ).get( programStageIndex );
                programStage = programStageStore.getByUid( programStageUid );
            }
        }

        return programStage;
    }

    /**
     * Extracts the index position of the ProgramStage in the GridHeader.
     *
     * @param currentHeaders the list of GridHeader
     * @return the ProgramStage index in the list
     */
    private int getProgramStageIndex( final List<GridHeader> currentHeaders )
    {
        final int notFound = -1;
        int index = 0;

        for ( final GridHeader gridHeader : currentHeaders )
        {
            if ( gridHeader != null && NAME_PROGRAM_STAGE == HeaderName.from( gridHeader.getColumn() ) )
            {
                return index;
            }

            index++;
        }

        return notFound;
    }
}
