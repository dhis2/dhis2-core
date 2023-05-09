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
package org.hisp.dhis.trackedentityfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.programstagefilter.DateFilterPeriod;
import org.hisp.dhis.programstagefilter.DatePeriodType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParamsHelper;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.trackedentityfilter.TrackedEntityFilterService" )
public class DefaultTrackedEntityFilterService
    implements TrackedEntityFilterService
{
    private final TrackedEntityFilterStore trackedEntityFilterStore;

    private final ProgramService programService;

    private final TrackedEntityAttributeService teaService;

    // -------------------------------------------------------------------------
    // TrackedEntityFilterService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long add( TrackedEntityFilter trackedEntityFilter )
    {
        trackedEntityFilterStore.save( trackedEntityFilter );
        return trackedEntityFilter.getId();
    }

    @Override
    @Transactional
    public void delete( TrackedEntityFilter trackedEntityFilter )
    {
        trackedEntityFilterStore.delete( trackedEntityFilter );
    }

    @Override
    @Transactional
    public void update( TrackedEntityFilter trackedEntityFilter )
    {
        trackedEntityFilterStore.update( trackedEntityFilter );
    }

    @Override
    @Transactional( readOnly = true )
    public TrackedEntityFilter get( long id )
    {
        return trackedEntityFilterStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityFilter> getAll()
    {
        return trackedEntityFilterStore.getAll();
    }

    @Override
    public List<String> validate( TrackedEntityFilter teiFilter )
    {
        List<String> errors = new ArrayList<>();

        if ( teiFilter.getProgram() != null && !StringUtils.isEmpty( teiFilter.getProgram().getUid() ) )
        {
            Program pr = programService.getProgram( teiFilter.getProgram().getUid() );

            if ( pr == null )
            {
                errors.add( "Program is specified but does not exist: " + teiFilter.getProgram().getUid() );
            }
        }

        EntityQueryCriteria eqc = teiFilter.getEntityQueryCriteria();

        if ( eqc == null )
        {
            return errors;
        }

        validateAttributeValueFilters( errors, eqc );

        validateDateFilterPeriods( errors, eqc );

        validateAssignedUsers( errors, eqc );

        validateOrganisationUnits( errors, eqc );

        validateOrderParams( errors, eqc );

        return errors;
    }

    private void validateDateFilterPeriods( List<String> errors, EntityQueryCriteria eqc )
    {
        validateDateFilterPeriod( errors, "EnrollmentCreatedDate", eqc.getEnrollmentCreatedDate() );
        validateDateFilterPeriod( errors, "EnrollmentIncidentDate", eqc.getEnrollmentIncidentDate() );
        validateDateFilterPeriod( errors, "EventDate", eqc.getEventDate() );
        validateDateFilterPeriod( errors, "LastUpdatedDate", eqc.getLastUpdatedDate() );
    }

    private void validateOrganisationUnits( List<String> errors, EntityQueryCriteria eqc )
    {
        if ( StringUtils.isEmpty( eqc.getOrganisationUnit() )
            && (eqc.getOuMode() == OrganisationUnitSelectionMode.SELECTED
                || eqc.getOuMode() == OrganisationUnitSelectionMode.DESCENDANTS
                || eqc.getOuMode() == OrganisationUnitSelectionMode.CHILDREN) )
        {
            errors.add( String.format( "Organisation Unit cannot be empty with %s org unit mode",
                eqc.getOuMode().toString() ) );
        }
    }

    private void validateOrderParams( List<String> errors, EntityQueryCriteria eqc )
    {
        if ( !StringUtils.isEmpty( eqc.getOrder() ) )
        {
            List<OrderCriteria> orderCriteria = OrderCriteria.fromOrderString( eqc.getOrder() );
            Map<String, TrackedEntityAttribute> attributes = teaService.getAllTrackedEntityAttributes()
                .stream().collect( Collectors.toMap( TrackedEntityAttribute::getUid, att -> att ) );
            errors.addAll(
                OrderParamsHelper.validateOrderParams( OrderParamsHelper.toOrderParams( orderCriteria ), attributes ) );
        }
    }

    private void validateAssignedUsers( List<String> errors, EntityQueryCriteria eqc )
    {
        if ( CollectionUtils.isEmpty( eqc.getAssignedUsers() )
            && eqc.getAssignedUserMode() == AssignedUserSelectionMode.PROVIDED )
        {
            errors.add( "Assigned Users cannot be empty with PROVIDED assigned user mode" );
        }
    }

    private void validateAttributeValueFilters( List<String> errors, EntityQueryCriteria eqc )
    {
        List<AttributeValueFilter> attributeValueFilters = eqc.getAttributeValueFilters();
        if ( !CollectionUtils.isEmpty( attributeValueFilters ) )
        {
            attributeValueFilters.forEach( avf -> {
                if ( StringUtils.isEmpty( avf.getAttribute() ) )
                {
                    errors.add( "Attribute Uid is missing in filter" );
                }
                else
                {
                    TrackedEntityAttribute tea = teaService.getTrackedEntityAttribute( avf.getAttribute() );
                    if ( tea == null )
                    {
                        errors.add( "No tracked entity attribute found for attribute:" + avf.getAttribute() );
                    }
                }

                validateDateFilterPeriod( errors, avf.getAttribute(), avf.getDateFilter() );
            } );
        }
    }

    private void validateDateFilterPeriod( List<String> errors, String item, DateFilterPeriod dateFilterPeriod )
    {
        if ( dateFilterPeriod == null || dateFilterPeriod.getType() == null )
        {
            return;
        }

        if ( dateFilterPeriod.getType() == DatePeriodType.ABSOLUTE
            && dateFilterPeriod.getStartDate() == null && dateFilterPeriod.getEndDate() == null )
        {
            errors.add( "Start date or end date not specified with ABSOLUTE date period type for " + item );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityFilter> get( Program program )
    {
        return trackedEntityFilterStore.get( program );
    }
}
