package org.hisp.dhis.oum.action.organisationunit;

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

import com.opensymphony.xwork2.Action;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.comparator.AttributeSortOrderComparator;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.system.util.AttributeUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.system.util.ValidationUtils.coordinateIsValid;

/**
 * @author Torgeir Lorange Ostby
 */
public class GetOrganisationUnitAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private CalendarService calendarService;

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private OrganisationUnit organisationUnit;

    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    private int numberOfChildren;

    public int getNumberOfChildren()
    {
        return numberOfChildren;
    }

    private List<DataSet> availableDataSets;

    public List<DataSet> getAvailableDataSets()
    {
        return availableDataSets;
    }

    private List<DataSet> dataSets;

    public List<DataSet> getDataSets()
    {
        return dataSets;
    }

    private List<OrganisationUnitGroupSet> groupSets;

    public List<OrganisationUnitGroupSet> getGroupSets()
    {
        return groupSets;
    }

    private List<Attribute> attributes;

    public List<Attribute> getAttributes()
    {
        return attributes;
    }

    public Map<Integer, String> attributeValues = new HashMap<>();

    public Map<Integer, String> getAttributeValues()
    {
        return attributeValues;
    }

    private boolean point;

    public boolean isPoint()
    {
        return point;
    }

    private String longitude;

    public String getLongitude()
    {
        return longitude;
    }

    private String latitude;

    public String getLatitude()
    {
        return latitude;
    }

    private String openingDate;

    public String getOpeningDate()
    {
        return openingDate;
    }

    private String closedDate;

    public String getClosedDate()
    {
        return closedDate;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        organisationUnit = organisationUnitService.getOrganisationUnit( id );

        numberOfChildren = organisationUnit.getChildren().size();

        availableDataSets = new ArrayList<>( idObjectManager.getAllNoAcl( DataSet.class ) );
        availableDataSets.removeAll( organisationUnit.getDataSets() );

        dataSets = new ArrayList<>( organisationUnit.getDataSets() );

        groupSets = new ArrayList<>( organisationUnitGroupService.getCompulsoryOrganisationUnitGroupSetsWithMembers() );

        attributes = new ArrayList<>( attributeService.getAttributes( OrganisationUnit.class ) );

        attributeValues = AttributeUtils.getAttributeValueMap( organisationUnit.getAttributeValues() );

        Collections.sort( availableDataSets, IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( dataSets, IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( groupSets, IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( attributes, AttributeSortOrderComparator.INSTANCE );

        // ---------------------------------------------------------------------
        // Allow update only if org unit does not have polygon coordinates
        // ---------------------------------------------------------------------

        point = organisationUnit.getCoordinates() == null || coordinateIsValid( organisationUnit.getCoordinates() );
        longitude = ValidationUtils.getLongitude( organisationUnit.getCoordinates() );
        latitude = ValidationUtils.getLatitude( organisationUnit.getCoordinates() );

        if ( organisationUnit.getOpeningDate() != null )
        {
            DateTimeUnit dateTimeUnit = calendarService.getSystemCalendar().fromIso( organisationUnit.getOpeningDate() );
            openingDate = calendarService.getSystemCalendar().formattedDate( dateTimeUnit );
        }

        if ( organisationUnit.getClosedDate() != null )
        {
            DateTimeUnit dateTimeUnit = calendarService.getSystemCalendar().fromIso( organisationUnit.getClosedDate() );
            closedDate = calendarService.getSystemCalendar().formattedDate( dateTimeUnit );
        }

        return SUCCESS;
    }
}
