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
package org.hisp.dhis.programstagefilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 *
 */
@Service( "org.hisp.dhis.programstagefilter.ProgramStageInstanceFilterService" )
@Transactional( readOnly = true )
public class DefaultProgramStageInstanceFilterService implements ProgramStageInstanceFilterService
{

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramService programService;

    private ProgramStageService programStageService;

    private OrganisationUnitService organisationUnitService;

    public DefaultProgramStageInstanceFilterService( ProgramService programService,
        ProgramStageService programStageService,
        OrganisationUnitService organisationUnitService )
    {
        this.programService = programService;
        this.programStageService = programStageService;
        this.organisationUnitService = organisationUnitService;
    }

    // -------------------------------------------------------------------------
    // ProgramStageInstanceFilterService implementation
    // -------------------------------------------------------------------------

    @Override
    public List<String> validate( ProgramStageInstanceFilter programStageInstanceFilter )
    {
        List<String> errors = new ArrayList<>();

        if ( programStageInstanceFilter.getProgram() == null )
        {
            errors.add( "Program should be specified for event filters." );
        }
        else
        {
            Program pr = programService.getProgram( programStageInstanceFilter.getProgram() );

            if ( pr == null )
            {
                errors.add( "Program is specified but does not exist: " + programStageInstanceFilter.getProgram() );
            }
        }

        if ( programStageInstanceFilter.getProgramStage() != null )
        {
            ProgramStage ps = programStageService.getProgramStage( programStageInstanceFilter.getProgramStage() );
            if ( ps == null )
            {
                errors.add(
                    "Program stage is specified but does not exist: " + programStageInstanceFilter.getProgramStage() );
            }
        }

        EventQueryCriteria eventQC = programStageInstanceFilter.getEventQueryCriteria();
        if ( eventQC != null )
        {
            if ( eventQC.getOrganisationUnit() != null )
            {
                OrganisationUnit ou = organisationUnitService.getOrganisationUnit( eventQC.getOrganisationUnit() );
                if ( ou == null )
                {
                    errors.add( "Org unit is specified but does not exist: " + eventQC.getOrganisationUnit() );
                }
            }
            if ( eventQC.getAssignedUserMode() != null && eventQC.getAssignedUsers() != null
                && !eventQC.getAssignedUsers().isEmpty()
                && !eventQC.getAssignedUserMode().equals( AssignedUserSelectionMode.PROVIDED ) )
            {
                errors.add( "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED" );
            }

            if ( eventQC.getEvents() != null && !eventQC.getEvents().isEmpty() && eventQC.getDataFilters() != null
                && !eventQC.getDataFilters().isEmpty() )
            {
                errors.add( "Event UIDs and filters can not be specified at the same time" );
            }

            if ( eventQC.getDisplayColumnOrder() != null && eventQC.getDisplayColumnOrder().size() > 0
                && (new HashSet<String>( eventQC.getDisplayColumnOrder() )).size() < eventQC.getDisplayColumnOrder()
                    .size() )
            {
                errors.add( "Event query criteria can not have duplicate column ordering fields" );
            }
        }

        return errors;
    }

}