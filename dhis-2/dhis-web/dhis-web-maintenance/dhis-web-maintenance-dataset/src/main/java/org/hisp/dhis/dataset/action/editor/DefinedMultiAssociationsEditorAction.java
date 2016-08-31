package org.hisp.dhis.dataset.action.editor;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.databrowser.MetaValue;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;

import com.opensymphony.xwork2.Action;

/**
 * @author Dang Duy Hieu
 * @version $Id$
 */
public class DefinedMultiAssociationsEditorAction
    implements Action
{
    private static final String SEPERATE = " - ";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    // -------------------------------------------------------------------------
    // I18n
    // -------------------------------------------------------------------------

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    // -------------------------------------------------------------------------
    // Parameters
    // -------------------------------------------------------------------------

    private Integer orgUnitId;

    private Integer[] dataSetIds;

    private Boolean[] statuses;

    private boolean checked;

    private OrganisationUnit source;

    private List<MetaValue> metaItems = new ArrayList<>();

    private Map<Integer, String> itemMaps = new HashMap<>();

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    public void setOrgUnitId( Integer orgUnitId )
    {
        this.orgUnitId = orgUnitId;
    }

    public void setDataSetIds( Integer[] dataSetIds )
    {
        this.dataSetIds = dataSetIds;
    }

    public void setStatuses( Boolean[] statuses )
    {
        this.statuses = statuses;
    }

    public boolean isChecked()
    {
        return checked;
    }

    public void setChecked( boolean checked )
    {
        this.checked = checked;
    }

    public List<MetaValue> getMetaItems()
    {
        return metaItems;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    public Map<Integer, String> getItemMaps()
    {
        return itemMaps;
    }

    public OrganisationUnit getSource()
    {
        return source;
    }

    // -------------------------------------------------------------------------
    // Action implement
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        String title = "";

        if ( checked )
        {
            title = i18n.getString( "assigned" ) + SEPERATE;
        }
        else
        {
            title = i18n.getString( "unassigned" ) + SEPERATE;
        }

        if ( dataSetIds.length == statuses.length )
        {
            source = organisationUnitService.getOrganisationUnit( orgUnitId );

            for ( int i = 0; i < dataSetIds.length; i++ )
            {
                DataSet dataSet = dataSetService.getDataSet( dataSetIds[i] );

                itemMaps.put( i, title + dataSet.getName() + SEPERATE + source.getName() );
                
                metaItems.add( new MetaValue( orgUnitId, dataSet.getId() + "", String.valueOf( checked ) ) );

                if ( (checked && !statuses[i]) )
                {
                    dataSet.getSources().add( source );

                    dataSetService.updateDataSet( dataSet );
                }
                else if ( (!checked && statuses[i]) )
                {
                    dataSet.getSources().remove( source );

                    dataSetService.updateDataSet( dataSet );
                }
            }
        }

        return SUCCESS;
    }
}
