package org.hisp.dhis.web.ohie.fred.webapi.v1.utils;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.web.ohie.fred.webapi.v1.controller.FacilityController;
import org.hisp.dhis.web.ohie.fred.webapi.v1.domain.Facility;
import org.hisp.dhis.web.ohie.fred.webapi.v1.domain.Identifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class OrganisationUnitToFacilityConverter implements Converter<OrganisationUnit, Facility>
{
    @Override
    public Facility convert( OrganisationUnit organisationUnit )
    {
        Facility facility = new Facility();
        facility.setUuid( organisationUnit.getUuid() );
        facility.setName( organisationUnit.getDisplayName() );
        facility.setCreatedAt( organisationUnit.getCreated() );
        facility.setUpdatedAt( organisationUnit.getLastUpdated() );

        try
        {
            facility.setHref( linkTo( FacilityController.class ).slash( organisationUnit.getUuid() ).toString() );
        }
        catch ( IllegalStateException ignored )
        {
        }

        if ( organisationUnit.getFeatureType() != null && organisationUnit.isPoint() && organisationUnit.getCoordinates() != null )
        {
            try
            {
                GeoUtils.Coordinates coordinates = GeoUtils.parseCoordinates( organisationUnit.getCoordinates() );

                facility.getCoordinates().add( coordinates.lng );
                facility.getCoordinates().add( coordinates.lat );
            }
            catch ( NumberFormatException ignored )
            {
            }
        }

        if ( organisationUnit.getParent() != null )
        {
            facility.getProperties().put( "parent", organisationUnit.getParent().getUid() );
        }

        Identifier identifier = new Identifier();

        identifier.setAgency( Identifier.DHIS2_AGENCY );
        identifier.setContext( Identifier.DHIS2_UID_CONTEXT );
        identifier.setId( organisationUnit.getUid() );

        facility.getIdentifiers().add( identifier );

        if ( organisationUnit.getCode() != null )
        {
            identifier = new Identifier();
            identifier.setAgency( Identifier.DHIS2_AGENCY );
            identifier.setContext( Identifier.DHIS2_CODE_CONTEXT );
            identifier.setId( organisationUnit.getCode() );

            facility.getIdentifiers().add( identifier );
        }

        // make sure that dataSets always come in the same order. This is a must for safe ETag generation.
        List<DataSet> ouDataSets = new ArrayList<>( organisationUnit.getDataSets() );
        Collections.sort( ouDataSets, new IdentifiableObjectNameComparator() );

        List<String> dataSets = new ArrayList<>();

        for ( DataSet dataSet : ouDataSets )
        {
            dataSets.add( dataSet.getUid() );
        }

        if ( !dataSets.isEmpty() )
        {
            facility.getProperties().put( "dataSets", dataSets );
        }

        facility.getProperties().put( "level", organisationUnit.getLevel() );

        for ( OrganisationUnitGroup group : organisationUnit.getGroups() )
        {
            if ( group.getGroupSet() == null )
            {
                continue;
            }

            String name = group.getGroupSet().getName();

            if ( name.equalsIgnoreCase( "Ownership" ) || name.equalsIgnoreCase( "Type" ) )
            {
                facility.getProperties().put( name.toLowerCase(), group.getName() );
            }
        }

        return facility;
    }
}
