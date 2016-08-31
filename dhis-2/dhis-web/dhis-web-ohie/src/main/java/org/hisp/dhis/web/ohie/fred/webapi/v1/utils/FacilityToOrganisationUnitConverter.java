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

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.web.ohie.fred.webapi.v1.domain.Facility;
import org.hisp.dhis.web.ohie.fred.webapi.v1.domain.Identifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class FacilityToOrganisationUnitConverter implements Converter<Facility, OrganisationUnit>
{
    @Autowired
    @Qualifier( "org.hisp.dhis.organisationunit.OrganisationUnitService" )
    private OrganisationUnitService organisationUnitService;

    @Autowired
    @Qualifier( "org.hisp.dhis.dataset.DataSetService" )
    private DataSetService dataSetService;

    @Override
    @SuppressWarnings( "unchecked" )
    public OrganisationUnit convert( Facility facility )
    {
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setName( facility.getName() );
        organisationUnit.setUuid( facility.getUuid() );

        if ( facility.getName() != null && facility.getName().length() > 49 )
        {
            organisationUnit.setShortName( facility.getName().substring( 0, 49 ) );
        }
        else
        {
            organisationUnit.setShortName( facility.getName() );
        }

        if ( facility.getIdentifiers() != null )
        {
            for ( Identifier identifier : facility.getIdentifiers() )
            {
                // two known system identifiers, uid and code
                if ( identifier.getAgency().equalsIgnoreCase( Identifier.DHIS2_AGENCY )
                    && identifier.getContext().equalsIgnoreCase( Identifier.DHIS2_CODE_CONTEXT ) )
                {
                    organisationUnit.setCode( identifier.getId() );
                }
                else if ( identifier.getAgency().equalsIgnoreCase( Identifier.DHIS2_AGENCY )
                    && identifier.getContext().equalsIgnoreCase( Identifier.DHIS2_UID_CONTEXT ) )
                {
                    organisationUnit.setUid( identifier.getId() );
                }
            }
        }

        organisationUnit.setFeatureType( FeatureType.POINT );

        try
        {
            GeoUtils.Coordinates coordinates = GeoUtils.parseCoordinates( facility.getCoordinates().toString() );
            organisationUnit.setCoordinates( String.format( "[%f, %f]", coordinates.lng, coordinates.lat ) );
        }
        catch ( NumberFormatException err )
        {
            organisationUnit.setCoordinates( null );
        }

        if ( facility.getProperties() != null )
        {
            String parentId = (String) facility.getProperties().get( "parent" );

            OrganisationUnit parent = organisationUnitService.getOrganisationUnit( parentId );

            if ( parent == null )
            {
                parent = organisationUnitService.getOrganisationUnitByUuid( parentId );
            }

            organisationUnit.setParent( parent );

            Collection<String> dataSets = (Collection<String>) facility.getProperties().get( "dataSets" );

            if ( dataSets != null )
            {
                for ( String uid : dataSets )
                {
                    DataSet dataSet = dataSetService.getDataSet( uid );
                    organisationUnit.getDataSets().add( dataSet );
                }
            }
        }

        return organisationUnit;
    }
}
