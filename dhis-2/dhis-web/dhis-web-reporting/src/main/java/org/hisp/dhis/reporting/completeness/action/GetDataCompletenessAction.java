package org.hisp.dhis.reporting.completeness.action;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ServiceProvider;
import org.hisp.dhis.completeness.DataSetCompletenessResult;
import org.hisp.dhis.completeness.DataSetCompletenessService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.util.SessionUtils;

import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 */
public class GetDataCompletenessAction
    implements Action
{
    private static final String KEY_DATA_COMPLETENESS = "dataSetCompletenessResults";

    private static final String DEFAULT_TYPE = "html";

    private static final String TITLE_SEP = " - ";
    
    private static final String EMPTY = "";
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ServiceProvider<DataSetCompletenessService> serviceProvider;

    public void setServiceProvider( ServiceProvider<DataSetCompletenessService> serviceProvider )
    {
        this.serviceProvider = serviceProvider;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private SelectionTreeManager selectionTreeManager;

    public void setSelectionTreeManager( SelectionTreeManager selectionTreeManager )
    {
        this.selectionTreeManager = selectionTreeManager;
    }

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }
    
    private I18nFormat format;

    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String periodId;

    public void setPeriodId( String periodId )
    {
        this.periodId = periodId;
    }

    private Integer dataSetId;

    public void setDataSetId( Integer dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    private String criteria;

    public void setCriteria( String criteria )
    {
        this.criteria = criteria;
    }

    private String type;

    public void setType( String type )
    {
        this.type = type;
    }
    
    private Set<Integer> groupId = new HashSet<>();

    public void setGroupId( Set<Integer> groupId )
    {
        this.groupId = groupId;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private Grid grid;

    public Grid getGrid()
    {
        return grid;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        Grid _grid = (Grid) SessionUtils.getSessionVar( KEY_DATA_COMPLETENESS );

        // Use last grid for any format except HTML
        
        if ( _grid != null && type != null && !type.equals( DEFAULT_TYPE ) )
        {
            grid = _grid;

            return type;
        }
        else
        {
            OrganisationUnit selectedUnit = selectionTreeManager.getReloadedSelectedOrganisationUnit();

            if ( periodId == null || selectedUnit == null || criteria == null )
            {
                return INPUT;
            }
            else
            {
                Period period = periodService.reloadPeriod( PeriodType.getPeriodFromIsoString( periodId ) );
                Integer _periodId = period.getId();

                DataSet dataSet = null;
                List<DataSetCompletenessResult> mainResults = new ArrayList<>();
                List<DataSetCompletenessResult> footerResults = new ArrayList<>();

                DataSetCompletenessService completenessService = serviceProvider.provide( criteria );

                if ( dataSetId != null && dataSetId != 0 ) // One ds for one ou
                {
                    mainResults = new ArrayList<>( completenessService.getDataSetCompleteness(
                        _periodId, getIdentifiers( selectedUnit.getChildren() ), dataSetId, groupId ) );

                    footerResults = new ArrayList<>(
                        completenessService.getDataSetCompleteness( _periodId, Arrays.asList( selectedUnit.getId() ),
                            dataSetId, groupId ) );

                    dataSet = dataSetService.getDataSet( dataSetId );
                }
                else // All ds for children of one ou               
                {
                    mainResults = new ArrayList<>( completenessService.getDataSetCompleteness(
                        _periodId, selectedUnit.getId(), groupId ) );
                }

                grid = getGrid( mainResults, footerResults, selectedUnit, dataSet, period );

                SessionUtils.setSessionVar( KEY_DATA_COMPLETENESS, grid );
            }

            return type != null ? type : DEFAULT_TYPE;
        }
    }

    private Grid getGrid( List<DataSetCompletenessResult> mainResults, List<DataSetCompletenessResult> footerResults,
        OrganisationUnit unit, DataSet dataSet, Period period )
    {
        String title = 
            ( unit != null ? unit.getName() : EMPTY ) + 
            ( dataSet != null ? TITLE_SEP + dataSet.getName() : EMPTY ) +
            ( period != null ? TITLE_SEP + format.formatPeriod( period ) : EMPTY );

        Grid grid = new ListGrid().setTitle( title );

        grid.addHeader( new GridHeader( i18n.getString( "name" ), false, true ) );
        grid.addHeader( new GridHeader( i18n.getString( "actual_reports" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "expected_reports" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "percent" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "reports_on_time" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "percent_on_time" ), false, false ) );

        for ( DataSetCompletenessResult result : mainResults )
        {
            addRow( grid, result );
        }

        if ( grid.getWidth() >= 4 )
        {
            grid.sortGrid( 4, 1 );
        }

        for ( DataSetCompletenessResult result : footerResults )
        {
            addRow( grid, result );
        }

        return grid;
    }

    private void addRow( Grid grid, DataSetCompletenessResult result )
    {
        grid.addRow();
        grid.addValue( result.getName() );
        grid.addValue( result.getRegistrations() );
        grid.addValue( result.getSources() );
        grid.addValue( result.getPercentage() );
        grid.addValue( result.getRegistrationsOnTime() );
        grid.addValue( result.getPercentageOnTime() );
    }
}
