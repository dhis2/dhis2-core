package org.hisp.dhis.webapi.controller.organisationunit;

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

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.filter.OrganisationUnitPolygonCoveringCoordinateFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author James Chang <jamesbchang@gmail.com>
 */
@Controller
@RequestMapping( value = OrganisationUnitLocationController.RESOURCE_PATH )
public class OrganisationUnitLocationController
{
    public static final String RESOURCE_PATH = "/organisationUnitLocations";

    private static final String ORGUNIGROUP_SYMBOL = "orgUnitGroupSymbol";

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private RenderService renderService;

    /**
     * Get Organisation Units within a distance from a location
     */
    @RequestMapping( value = "/withinRange", method = RequestMethod.GET, produces = { "*/*", "application/json" } )
    public void getEntitiesWithinRange(
        @RequestParam Double longitude,
        @RequestParam Double latitude,
        @RequestParam Double distance,
        @RequestParam( required = false ) String orgUnitGroupSetId, HttpServletResponse response ) throws Exception
    {
        List<OrganisationUnit> entityList = new ArrayList<>(
            organisationUnitService.getOrganisationUnitWithinDistance( longitude, latitude, distance ) );

        for ( OrganisationUnit organisationUnit : entityList )
        {
            Set<AttributeValue> attributeValues = organisationUnit.getAttributeValues();
            attributeValues.clear();

            if ( orgUnitGroupSetId != null )
            {
                for ( OrganisationUnitGroup organisationUnitGroup : organisationUnit.getGroups() )
                {
                    if ( organisationUnitGroup.getGroupSet() != null )
                    {
                        OrganisationUnitGroupSet orgunitGroupSet = organisationUnitGroup.getGroupSet();

                        if ( orgunitGroupSet.getUid().compareTo( orgUnitGroupSetId ) == 0 )
                        {
                            AttributeValue attributeValue = new AttributeValue();
                            // attributeValue.setAttribute( new Attribute( ORGUNIGROUP_SYMBOL, ORGUNIGROUP_SYMBOL ) );
                            attributeValue.setAttribute( new Attribute( ORGUNIGROUP_SYMBOL, ValueType.TEXT ) );
                            attributeValue.setValue( organisationUnitGroup.getSymbol() );
                            attributeValues.add( attributeValue );
                        }
                    }
                }
            }

            organisationUnit.setAttributeValues( attributeValues );

            // Clear out all data not needed for this task

            organisationUnit.removeAllDataSets();
            organisationUnit.removeAllUsers();
            organisationUnit.removeAllOrganisationUnitGroups();
        }

        renderService.toJson( response.getOutputStream(), entityList );
    }

    /**
     * Get lowest level Org Units that includes the location in their polygon shape.
     */
    @RequestMapping( value = "/orgUnitByLocation", method = RequestMethod.GET, produces = { "*/*", "application/json" } )
    public void getParentByLocation(
        @RequestParam Double longitude,
        @RequestParam Double latitude,
        @RequestParam( required = false ) String topOrgUnit,
        @RequestParam( required = false ) Integer targetLevel, HttpServletResponse response ) throws Exception
    {
        List<OrganisationUnit> entityList = new ArrayList<>(
            organisationUnitService.getOrganisationUnitByCoordinate( longitude, latitude, topOrgUnit, targetLevel ) );

        // Remove unrelated details and output in JSON format

        for ( OrganisationUnit organisationUnit : entityList )
        {
            Set<AttributeValue> attributeValues = organisationUnit.getAttributeValues();
            attributeValues.clear();
            organisationUnit.removeAllDataSets();
            organisationUnit.removeAllUsers();
            organisationUnit.removeAllOrganisationUnitGroups();
        }

        renderService.toJson( response.getOutputStream(), entityList );
    }

    /**
     * Check if the location lies within the organisation unit boundary
     */
    @RequestMapping( value = "/locationWithinOrgUnitBoundary", method = RequestMethod.GET, produces = { "*/*", "application/json" } )
    public void checkLocationWithinOrgUnit( @RequestParam String orgUnitUid,
        @RequestParam Double longitude, @RequestParam Double latitude, HttpServletResponse response ) throws Exception
    {
        boolean withinOrgUnit = false;

        List<OrganisationUnit> organisationUnits = new ArrayList<>();
        organisationUnits.add( organisationUnitService.getOrganisationUnit( orgUnitUid ) );
        FilterUtils.filter( organisationUnits, new OrganisationUnitPolygonCoveringCoordinateFilter( longitude, latitude ) );

        if ( !organisationUnits.isEmpty() )
        {
            withinOrgUnit = true;
        }

        renderService.toJson( response.getOutputStream(), withinOrgUnit );
    }
}
