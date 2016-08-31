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
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.ouwt.manager.OrganisationUnitSelectionManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author Torgeir Lorange Ostby
 */
public class AddOrganisationUnitAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private CalendarService calendarService;

    private OrganisationUnitSelectionManager selectionManager;

    public void setSelectionManager( OrganisationUnitSelectionManager selectionManager )
    {
        this.selectionManager = selectionManager;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String shortName;

    public void setShortName( String shortName )
    {
        this.shortName = shortName;
    }

    private String description;

    public void setDescription( String description )
    {
        this.description = description;
    }

    private String code;

    public void setCode( String code )
    {
        this.code = code;
    }

    private String openingDate;

    public void setOpeningDate( String openingDate )
    {
        this.openingDate = openingDate;
    }

    private String comment;

    public void setComment( String comment )
    {
        this.comment = comment;
    }

    private String longitude;

    public void setLongitude( String longitude )
    {
        this.longitude = longitude;
    }

    private String latitude;

    public void setLatitude( String latitude )
    {
        this.latitude = latitude;
    }

    private String url;

    public void setUrl( String url )
    {
        this.url = url;
    }

    private String contactPerson;

    public void setContactPerson( String contactPerson )
    {
        this.contactPerson = contactPerson;
    }

    private String address;

    public void setAddress( String address )
    {
        this.address = address;
    }

    private String email;

    public void setEmail( String email )
    {
        this.email = email;
    }

    private String phoneNumber;

    public void setPhoneNumber( String phoneNumber )
    {
        this.phoneNumber = phoneNumber;
    }

    private Collection<String> dataSets = new HashSet<>();

    public void setDataSets( Collection<String> dataSets )
    {
        this.dataSets = dataSets;
    }

    private Collection<String> selectedGroups = new HashSet<>();

    public void setSelectedGroups( Collection<String> selectedGroups )
    {
        this.selectedGroups = selectedGroups;
    }

    private Integer organisationUnitId;

    public Integer getOrganisationUnitId()
    {
        return organisationUnitId;
    }

    private List<String> jsonAttributeValues;

    public void setJsonAttributeValues( List<String> jsonAttributeValues )
    {
        this.jsonAttributeValues = jsonAttributeValues;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        OrganisationUnit parent = selectionManager.getSelectedOrganisationUnit();

        if ( parent == null )
        {
            // -----------------------------------------------------------------
            // If no unit is selected, the parent is the parent of the roots
            // -----------------------------------------------------------------

            parent = selectionManager.getRootOrganisationUnitsParent();
        }

        // ---------------------------------------------------------------------
        // Create organisation unit
        // ---------------------------------------------------------------------

        DateTimeUnit isoOpeningDate = calendarService.getSystemCalendar().toIso( openingDate );

        OrganisationUnit organisationUnit = new OrganisationUnit();

        organisationUnit.setName( StringUtils.trimToNull( name ) );
        organisationUnit.setShortName( StringUtils.trimToNull( shortName ) );
        organisationUnit.setCode( StringUtils.trimToNull( code ) );
        organisationUnit.setOpeningDate( isoOpeningDate.toJdkCalendar().getTime() );
        organisationUnit.setDescription( description );
        organisationUnit.setComment( StringUtils.trimToNull( comment ) );
        organisationUnit.setUrl( StringUtils.trimToNull( url ) );
        organisationUnit.setParent( parent );
        organisationUnit.setContactPerson( StringUtils.trimToNull( contactPerson ) );
        organisationUnit.setAddress( StringUtils.trimToNull( address ) );
        organisationUnit.setEmail( StringUtils.trimToNull( email ) );
        organisationUnit.setPhoneNumber( StringUtils.trimToNull( phoneNumber ) );

        if ( parent != null )
        {
            parent.getChildren().add( organisationUnit );
        }

        if ( jsonAttributeValues != null )
        {
            attributeService.updateAttributeValues( organisationUnit, jsonAttributeValues );
        }

        // ---------------------------------------------------------------------
        // Set coordinates and feature type to point if valid
        // ---------------------------------------------------------------------

        if ( longitude != null && latitude != null )
        {
            String coordinates = ValidationUtils.getCoordinate( longitude, latitude );

            if ( ValidationUtils.coordinateIsValid( coordinates ) )
            {
                organisationUnit.setCoordinates( coordinates );
                organisationUnit.setFeatureType( FeatureType.POINT );
            }
        }

        // ---------------------------------------------------------------------
        // Must persist org unit before adding data sets because association are
        // updated on both sides (and this side is inverse)
        // ---------------------------------------------------------------------

        organisationUnitId = organisationUnitService.addOrganisationUnit( organisationUnit );

        for ( String id : dataSets )
        {
            organisationUnit.addDataSet( manager.getNoAcl( DataSet.class, Integer.parseInt( id ) ) );
        }

        for ( String id : selectedGroups )
        {
            OrganisationUnitGroup group = manager.getNoAcl( OrganisationUnitGroup.class, Integer.parseInt( id ) );

            if ( group != null )
            {
                group.addOrganisationUnit( organisationUnit );
                manager.updateNoAcl( group );
            }
        }

        organisationUnitService.updateOrganisationUnit( organisationUnit );

        organisationUnitService.updateOrganisationUnitVersion();

        return SUCCESS;
    }
}
