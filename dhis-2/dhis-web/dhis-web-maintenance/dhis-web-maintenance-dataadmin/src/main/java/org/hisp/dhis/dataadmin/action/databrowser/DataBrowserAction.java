package org.hisp.dhis.dataadmin.action.databrowser;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.databrowser.DataBrowserGridService;
import org.hisp.dhis.databrowser.MetaValue;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.util.SessionUtils;

/**
 * @author espenjac, joakibj, briane, eivinhb
 */
public class DataBrowserAction
    extends ActionPagingSupport<Grid>
{
    private static final Log log = LogFactory.getLog( DataBrowserAction.class );
    
    private static final String KEY_DATABROWSERGRID = "dataBrowserGridResults";
    private static final String TRUE = "on";
    private static final String EMPTY = "";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private DataBrowserGridService dataBrowserGridService;

    public void setDataBrowserGridService( DataBrowserGridService dataBrowserGridService )
    {
        this.dataBrowserGridService = dataBrowserGridService;
    }

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private OrganisationUnitGroupService organisationUnitGroupService;

    public void setOrganisationUnitGroupService( OrganisationUnitGroupService organisationUnitGroupService )
    {
        this.organisationUnitGroupService = organisationUnitGroupService;
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

    // -------------------------------------------------------------------------
    // I18n & I18nFormat
    // -------------------------------------------------------------------------

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
    // Parameters
    // -------------------------------------------------------------------------

    private Grid grid;

    private String mode;

    private String toDate;

    private String fromDate;

    private String fromToDate;

    private String periodTypeId;

    private String parent;

    private String tmpParent;

    private String orgunitid;

    private String selectedUnitChanger;

    private String dataElementName;

    private String drillDownCheckBox;

    private String showZeroCheckBox;

    private OrganisationUnit selectedUnit;

    private boolean isSummary;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    public void setMode( String mode )
    {
        this.mode = mode;
    }

    public void setToDate( String toDate )
    {
        this.toDate = toDate;
    }

    public void setFromDate( String fromDate )
    {
        this.fromDate = fromDate;
    }

    public void setPeriodTypeId( String periodTypeId )
    {
        this.periodTypeId = periodTypeId;
    }

    public void setParent( String parent )
    {
        this.parent = parent;
    }

    public void setSelectedUnitChanger( String selectedUnitChanger )
    {
        this.selectedUnitChanger = selectedUnitChanger.trim();
    }

    public void setOrgunitid( String orgunitid )
    {
        this.orgunitid = orgunitid;
    }

    public void setDrillDownCheckBox( String drillDownCheckBox )
    {
        this.drillDownCheckBox = drillDownCheckBox;
    }

    public void setShowZeroCheckBox( String showZeroCheckBox )
    {
        this.showZeroCheckBox = showZeroCheckBox;
    }

    public void setSummary( boolean isSummary )
    {
        this.isSummary = isSummary;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    public boolean isSummary()
    {
        return isSummary;
    }

    public Grid getGrid()
    {
        return grid;
    }

    public Collection<DataElementGroup> getDataElementGroups()
    {
        return dataElementService.getAllDataElementGroups();
    }

    public String getMode()
    {
        return mode;
    }

    public String getToDate()
    {
        return toDate;
    }

    public String getFromDate()
    {
        return fromDate;
    }

    public String getFromToDate()
    {
        return fromToDate;
    }

    public String getPeriodTypeId()
    {
        return periodTypeId;
    }

    public String getParent()
    {
        return parent;
    }

    public String getOrgunitid()
    {
        return orgunitid;
    }

    public String getTmpParent()
    {
        return tmpParent;
    }

    public String getDataElementName()
    {
        return dataElementName;
    }

    public String getShowZeroCheckBox()
    {
        return showZeroCheckBox;
    }

    public String getParentName()
    {
        if ( mode.equals( "OU" ) )
        {
            return selectedUnit.getName();
        }

        if ( parent == null )
        {
            return EMPTY;
        }

        if ( mode.equals( "DS" ) )
        {
            return dataSetService.getDataSet( Integer.parseInt( parent ) ).getName();
        }

        if ( mode.equals( "OUG" ) )
        {
            return organisationUnitGroupService.getOrganisationUnitGroup( Integer.parseInt( parent ) ).getName();
        }

        if ( mode.equals( "DEG" ) )
        {
            return dataElementService.getDataElementGroup( Integer.parseInt( parent ) ).getName();
        }

        return EMPTY;
    }

    public String getCurrentParentsParent()
    {
        try
        {
            return selectedUnit.getParent().getName();
        }
        catch ( Exception e )
        {
            return EMPTY;
        }
    }

    public List<OrganisationUnit> getCurrentChildren()
    {
        Set<OrganisationUnit> tmp = selectedUnit.getChildren();
        List<OrganisationUnit> list = new ArrayList<>();

        for ( OrganisationUnit o : tmp )
        {
            if ( o.getChildren().size() > 0 )
            {
                list.add( o );
            }
        }
        Collections.sort( list, IdentifiableObjectNameComparator.INSTANCE );

        return list;
    }

    public List<OrganisationUnit> getBreadCrumbOrgUnit()
    {
        List<OrganisationUnit> myList = new ArrayList<>();

        boolean loop = true;
        OrganisationUnit currentOrgUnit = selectedUnit;

        while ( loop )
        {
            myList.add( currentOrgUnit );

            if ( currentOrgUnit.getParent() == null )
            {
                loop = false;
            }
            else
            {
                currentOrgUnit = currentOrgUnit.getParent();
            }
        }
        Collections.reverse( myList );

        return myList;
    }

    public List<Object> getMetaValues()
    {
        return grid.getColumn( 0 );
    }

    public Map<Integer, List<Object>> getMetaValueMaps()
    {
        Map<Integer, List<Object>> maps = new Hashtable<>();

        for ( List<Object> row : grid.getRows() )
        {
            if ( !row.isEmpty() && row.size() > 1 )
            {
                maps.put( ((MetaValue) row.get( 0 )).getId(), row.subList( 1, row.size() ) );
            }
        }

        return maps;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        // ---------------------------------------------------------------------
        // Validation
        // ---------------------------------------------------------------------

        boolean isZeroAdded = (showZeroCheckBox != null) && showZeroCheckBox.equals( TRUE );

        // Check if the second selected date is later than the first
        
        if ( StringUtils.trimToNull( fromDate ) == null && StringUtils.trimToNull( toDate ) == null )
        {
            if ( DateUtils.checkDates( fromDate, toDate ) )
            {
                return ERROR;
            }
        }

        // If set, change the current selected unit
        
        if ( selectedUnitChanger != null )
        {
            selectionTreeManager.setSelectedOrganisationUnit( organisationUnitService.getOrganisationUnit( Integer
                .parseInt( selectedUnitChanger ) ) );
        }

        selectedUnit = selectionTreeManager.getReloadedSelectedOrganisationUnit();

        // Checks if the selected unit is a leaf node, if so add parent
        
        if ( parent == null && mode.equals( "OU" ) && selectedUnit != null && selectedUnit.getChildren().size() == 0 )
        {
            parent = selectedUnit.getId() + EMPTY;
        }

        PeriodType periodType = periodService.reloadPeriodType( periodService.getPeriodTypeByName( periodTypeId ) );        
        
        // ---------------------------------------------------------------------
        // Data set mode
        // ---------------------------------------------------------------------

        log.info( "Mode: " + mode + ", parent: " + parent );
        
        if ( mode.equals( "DS" ) )
        {
            if ( parent != null )
            {
                Integer parentInt = Integer.parseInt( parent );

                grid = dataBrowserGridService.getCountDataElementsForDataSetInPeriod( parentInt, fromDate, toDate,
                    periodType, format, isZeroAdded );
            }
            else
            {
                grid = dataBrowserGridService.getDataSetsInPeriod( fromDate, toDate, periodType, format, isZeroAdded );
            }

            this.setSummary( true );
        }
        
        // ---------------------------------------------------------------------
        // Data element group mode
        // ---------------------------------------------------------------------

        else if ( mode.equals( "DEG" ) )
        {
            if ( parent != null )
            {
                Integer parentInt = Integer.parseInt( parent );

                grid = dataBrowserGridService.getCountDataElementsForDataElementGroupInPeriod( parentInt, fromDate,
                    toDate, periodType, format, isZeroAdded );
            }
            else
            {
                grid = dataBrowserGridService.getDataElementGroupsInPeriod( fromDate, toDate, periodType, format,
                    isZeroAdded );
            }

            this.setSummary( true );
        }

        // ---------------------------------------------------------------------
        // Organisation unit group mode
        // ---------------------------------------------------------------------
        
        else if ( mode.equals( "OUG" ) )
        {
            if ( parent != null )
            {
                Integer parentInt = Integer.parseInt( parent );
                grid = dataBrowserGridService.getCountDataElementGroupsForOrgUnitGroupInPeriod( parentInt, fromDate,
                    toDate, periodType, format, isZeroAdded );
            }
            else
            {
                grid = dataBrowserGridService.getOrgUnitGroupsInPeriod( fromDate, toDate, periodType, format,
                    isZeroAdded );
            }

            this.setSummary( true );
        }

        // ---------------------------------------------------------------------
        // Organisation unit mode
        // ---------------------------------------------------------------------
        
        else if ( mode.equals( "OU" ) )
        {
            selectedUnit = selectionTreeManager.getSelectedOrganisationUnit();

            if ( (drillDownCheckBox != null) && drillDownCheckBox.equals( TRUE ) )
            {
                parent = String.valueOf( selectedUnit.getId() );
            }

            if ( parent != null )
            {
                Integer parentInt = Integer.parseInt( parent );

                // Show data values entered for this specified unit only
                
                grid = dataBrowserGridService.getRawDataElementsForOrgUnitInPeriod( parentInt, fromDate, toDate,
                    periodType, format, isZeroAdded );

                this.setSummary( false );
            }
            else if ( selectedUnit != null )
            {
                // Show the summary values for the immediate and descendant units of the specified unit
                
                grid = dataBrowserGridService.getOrgUnitsInPeriod( selectedUnit.getId(), fromDate, toDate, periodType,
                    null, format, isZeroAdded );

                this.setSummary( true );
            }
            else
            {
                return ERROR;
            }
        }
        else
        {
            return ERROR;
        }

        setGridTitle();

        convertColumnNames( grid );

        doPaging();

        // Set DataBrowserTable variable for PDF export
        
        SessionUtils.setSessionVar( KEY_DATABROWSERGRID, grid );

        return SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * This is a helper method for populating a list of converted column names
     * 
     * @param DataBrowserTable
     */
    private void convertColumnNames( Grid grid )
    {
        PeriodType periodType = periodService.getPeriodTypeByName( periodTypeId );

        for ( GridHeader col : grid.getVisibleHeaders() )
        {
            col.setName( dataBrowserGridService.convertDate( periodType, col.getName(), i18n, format ) );
        }
    }

    private void setGridTitle()
    {
        grid.setTitle( i18n.getString( mappingMode( mode ) )
            + (mode.equals( "OU" ) == true ? " - " + getParentName() : EMPTY) );
        grid.setSubtitle( i18n.getString( "from_date" ) + ": " + fromDate + " " + i18n.getString( "to_date" ) + ": "
            + toDate + ", " + i18n.getString( "period_type" ) + ": " + i18n.getString( periodTypeId ) );
    }

    private void doPaging()
    {
        this.paging = this.createPaging( grid.getHeight() );

        grid.limitGrid( paging.getStartPos(), paging.getEndPos() );
    }

    private String mappingMode( String mode )
    {
        if ( mode.equals( "DS" ) )
        {
            return "data_sets";
        }
        else if ( mode.equals( "DEG" ) )
        {
            return "data_element_groups";
        }
        else if ( mode.equals( "OU" ) )
        {
            return "organisation_units";
        }
        else if ( mode.equals( "OUG" ) )
        {
            return "organisation_unit_groups";
        }

        return mode;
    }
}
