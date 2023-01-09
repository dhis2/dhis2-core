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
package org.hisp.dhis.programstageworkinglistdefinition;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.util.ValidationUtil.validateAttributeValueFilters;
import static org.hisp.dhis.util.ValidationUtil.validateDateFilterPeriod;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.programstagefilter.EventDataFilter;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service( "org.hisp.dhis.programstageworkinglist.ProgramStageWorkingListDefinitionService" )
public class DefaultProgramStageWorkingListDefinitionService
    implements ProgramStageWorkingListDefinitionService
{

    private final ProgramStageWorkingListDefinitionStore programStageWorkingListDefinitionStore;

    private final ProgramService programService;

    private final ProgramStageService programStageService;

    private final OrganisationUnitService organisationUnitService;

    private final TrackedEntityAttributeService teaService;

    private final DataElementService dataElementService;

    public DefaultProgramStageWorkingListDefinitionService(
        ProgramStageWorkingListDefinitionStore programStageWorkingListDefinitionStore, ProgramService programService,
        ProgramStageService programStageService, OrganisationUnitService organisationUnitService,
        TrackedEntityAttributeService teaService, DataElementService dataElementService )
    {
        checkNotNull( programStageWorkingListDefinitionStore );
        checkNotNull( programService );
        checkNotNull( programStageService );

        this.programStageWorkingListDefinitionStore = programStageWorkingListDefinitionStore;
        this.programService = programService;
        this.programStageService = programStageService;
        this.organisationUnitService = organisationUnitService;
        this.teaService = teaService;
        this.dataElementService = dataElementService;
    }

    @Override
    public long add( ProgramStageWorkingListDefinition programStageWorkingListDefinition )
    {
        programStageWorkingListDefinitionStore.save( programStageWorkingListDefinition );
        return programStageWorkingListDefinition.getId();
    }

    @Override
    public void delete( ProgramStageWorkingListDefinition programStageWorkingListDefinition )
    {
        programStageWorkingListDefinitionStore.delete( programStageWorkingListDefinition );
    }

    @Override
    public void update( ProgramStageWorkingListDefinition programStageWorkingListDefinition )
    {
        programStageWorkingListDefinitionStore.update( programStageWorkingListDefinition );
    }

    @Override
    public ProgramStageWorkingListDefinition get( long id )
    {
        return programStageWorkingListDefinitionStore.get( id );
    }

    @Override
    public List<ProgramStageWorkingListDefinition> getAll()
    {
        return programStageWorkingListDefinitionStore.getAll();
    }

    @Override
    public List<String> validate( ProgramStageWorkingListDefinition programStageWorkingListDefinition )
    {
        List<String> errors = new ArrayList<>();

        validateProgram( errors, programStageWorkingListDefinition );
        validateProgramStage( errors, programStageWorkingListDefinition );
        validateName( errors, programStageWorkingListDefinition );

        ProgramStageQueryCriteria queryCriteria = programStageWorkingListDefinition.getProgramStageQueryCriteria();

        if ( queryCriteria == null )
        {
            return errors;
        }

        validateDateFilterPeriods( errors, queryCriteria );
        validateAssignedUsers( errors, queryCriteria );
        validateOrganisationUnit( errors, queryCriteria );
        validateAttributeValueFilters( errors, queryCriteria.getAttributeValueFilters(), teaService );
        validateDataFilters( errors, queryCriteria );

        return errors;
    }

    private void validateProgram( List<String> errors,
        ProgramStageWorkingListDefinition programStageWorkingListDefinition )
    {
        if ( programStageWorkingListDefinition.getProgram() == null )
        {
            errors.add( "No program specified for the working list definition." );
        }
        else
        {
            Program pr = programService.getProgram( programStageWorkingListDefinition.getProgram().getUid() );

            if ( pr == null )
            {
                errors.add(
                    "Program is specified but does not exist: " + programStageWorkingListDefinition.getProgram() );
            }
        }
    }

    private void validateProgramStage( List<String> errors,
        ProgramStageWorkingListDefinition programStageWorkingListDefinition )
    {
        if ( programStageWorkingListDefinition.getProgramStage() == null )
        {
            errors.add( "No program stage specified for the working list definition." );
        }
        else
        {
            ProgramStage ps = programStageService
                .getProgramStage( programStageWorkingListDefinition.getProgramStage().getUid() );

            if ( ps == null )
            {
                errors.add( "Program stage is specified but does not exist: "
                    + programStageWorkingListDefinition.getProgramStage() );
            }
        }
    }

    private void validateName( List<String> errors,
        ProgramStageWorkingListDefinition programStageWorkingListDefinition )
    {
        if ( programStageWorkingListDefinition.getName() == null
            || programStageWorkingListDefinition.getName().isEmpty() )
        {
            errors.add( "No name specified for the working list definition." );
        }
    }

    private void validateDateFilterPeriods( List<String> errors, ProgramStageQueryCriteria queryCriteria )
    {
        validateDateFilterPeriod( errors, "EnrollmentCreatedDate", queryCriteria.getEnrolledAt() );
        validateDateFilterPeriod( errors, "EnrollmentIncidentDate", queryCriteria.getEnrollmentOccurredAt() );
        validateDateFilterPeriod( errors, "EventDate", queryCriteria.getEventCreatedAt() );
        validateDateFilterPeriod( errors, "EventScheduledDate", queryCriteria.getScheduledAt() );
    }

    private void validateAssignedUsers( List<String> errors, ProgramStageQueryCriteria queryCriteria )
    {
        if ( CollectionUtils.isEmpty( queryCriteria.getAssignedUsers() )
            && queryCriteria.getAssignedUserMode() == AssignedUserSelectionMode.PROVIDED )
        {
            errors.add( "Assigned Users cannot be empty with PROVIDED assigned user mode" );
        }
    }

    private void validateOrganisationUnit( List<String> errors, ProgramStageQueryCriteria queryCriteria )
    {
        if ( queryCriteria.getOrgUnit() != null )
        {
            OrganisationUnit ou = organisationUnitService.getOrganisationUnit( queryCriteria.getOrgUnit() );
            if ( ou == null )
            {
                errors.add( "Org unit is specified but does not exist: " + queryCriteria.getOrgUnit() );
                return;
            }
        }

        if ( StringUtils.isEmpty( queryCriteria.getOrgUnit() )
            && (queryCriteria.getOuMode() == OrganisationUnitSelectionMode.SELECTED
                || queryCriteria.getOuMode() == OrganisationUnitSelectionMode.DESCENDANTS
                || queryCriteria.getOuMode() == OrganisationUnitSelectionMode.CHILDREN) )
        {
            errors.add( String.format( "Organisation Unit cannot be empty with %s org unit mode",
                queryCriteria.getOuMode().toString() ) );
        }
    }

    private void validateDataFilters( List<String> errors, ProgramStageQueryCriteria queryCriteria )
    {
        List<EventDataFilter> dataFilters = queryCriteria.getDataFilters();
        if ( !CollectionUtils.isEmpty( dataFilters ) )
        {
            dataFilters.forEach( avf -> {
                if ( StringUtils.isEmpty( avf.getDataItem() ) )
                {
                    errors.add( "Data item Uid is missing in filter" );
                }
                else
                {
                    DataElement dataElement = dataElementService.getDataElement( avf.getDataItem() );
                    if ( dataElement == null )
                    {
                        errors.add( "No data element found for item:" + avf.getDataItem() );
                    }
                }

                validateDateFilterPeriod( errors, avf.getDataItem(), avf.getDateFilter() );
            } );
        }
    }
}
