package org.hisp.dhis.databrowser.jdbc;

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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.databrowser.DataBrowserGridStore;
import org.hisp.dhis.databrowser.MetaValue;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.grid.ListGrid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * @author joakibj, martinwa, briane, eivinhb
 * @version $Id JDBCDataBrowserStore.java 2010-04-06 jpp, ddhieu$
 */
public class JDBCDataBrowserStore
    implements DataBrowserGridStore
{
    private static final Log log = LogFactory.getLog( JDBCDataBrowserStore.class );
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private StatementBuilder statementBuilder;

    public void setStatementBuilder( StatementBuilder statementBuilder )
    {
        this.statementBuilder = statementBuilder;
    }

    // -------------------------------------------------------------------------
    // DataBrowserStore implementation
    // -------------------------------------------------------------------------

    @Override
    public Grid getDataSetsBetweenPeriods( List<Integer> betweenPeriodIds, PeriodType periodType, boolean isZeroAdded )
    {
        StringBuffer sqlsb = new StringBuffer();

        sqlsb.append( "(SELECT d.datasetid AS ID, d.name AS DataSet, COUNT(*) AS counts_of_aggregated_values " );
        sqlsb.append( "FROM datavalue dv " );
        sqlsb.append( "JOIN datasetmembers dsm ON (dv.dataelementid = dsm.dataelementid) " );
        sqlsb.append( "JOIN dataset d ON (d.datasetid = dsm.datasetid) " );
        sqlsb.append( "WHERE dv.periodid IN " + splitListHelper( betweenPeriodIds ) + " " );
        sqlsb.append( "AND d.periodtypeid=" + periodType.getId() + " " );
        sqlsb.append( "GROUP BY d.datasetid, d.name " );
        sqlsb.append( "ORDER BY counts_of_aggregated_values DESC)" );

        // Gets all the dataSets in a period with a count attached to the
        // dataSet. The table returned has 2 columns.

        Grid dataSetGrid = new ListGrid();

        dataSetGrid.addHeader( new GridHeader( "drilldown_data_set", false, false ) );
        dataSetGrid.addHeader( new GridHeader( "counts_of_aggregated_values", false, false ) );

        populateGrid( dataSetGrid, sqlsb.toString(), isZeroAdded, jdbcTemplate );

        return dataSetGrid;
    }

    @Override
    public Grid getDataElementGroupsBetweenPeriods( List<Integer> betweenPeriodIds, boolean isZeroAdded )
    {
        StringBuffer sqlsb = new StringBuffer();

        sqlsb.append( "(SELECT d.dataelementgroupid AS ID, d.name AS DataElementGroup, COUNT(*) AS counts_of_aggregated_values " );
        sqlsb.append( "FROM datavalue dv " );
        sqlsb.append( "JOIN dataelementgroupmembers degm ON (dv.dataelementid = degm.dataelementid)" );
        sqlsb.append( "JOIN dataelementgroup d ON (d.dataelementgroupid = degm.dataelementgroupid) " );
        sqlsb.append( "WHERE dv.periodid IN " + splitListHelper( betweenPeriodIds ) + " " );
        sqlsb.append( "GROUP BY d.dataelementgroupid, d.name " );
        sqlsb.append( "ORDER BY counts_of_aggregated_values DESC)" );

        Grid gridDEG = new ListGrid();

        gridDEG.addHeader( new GridHeader( "drilldown_data_element_group", false, false ) );
        gridDEG.addHeader( new GridHeader( "counts_of_aggregated_values", false, false ) );

        populateGrid( gridDEG, sqlsb.toString(), isZeroAdded, jdbcTemplate );

        return gridDEG;
    }

    @Override
    public Grid getOrgUnitGroupsBetweenPeriods( List<Integer> betweenPeriodIds, boolean isZeroAdded )
    {
        StringBuffer sqlsb = new StringBuffer();

        sqlsb.append( "(SELECT oug.orgunitgroupid, oug.name AS OrgUnitGroup, COUNT(*) AS counts_of_aggregated_values " );
        sqlsb.append( "FROM orgunitgroup oug " );
        sqlsb.append( "JOIN orgunitgroupmembers ougm ON oug.orgunitgroupid = ougm.orgunitgroupid " );
        sqlsb.append( "JOIN organisationunit ou ON  ougm.organisationunitid = ou.organisationunitid " );
        sqlsb.append( "JOIN datavalue dv ON ou.organisationunitid = dv.sourceid " );
        sqlsb.append( "WHERE dv.periodid IN " + splitListHelper( betweenPeriodIds ) + " " );
        sqlsb.append( "GROUP BY oug.orgunitgroupid, oug.name " );
        sqlsb.append( "ORDER BY counts_of_aggregated_values DESC) " );

        Grid gridOUG = new ListGrid();

        gridOUG.addHeader( new GridHeader( "drilldown_orgunit_group", false, false ) );
        gridOUG.addHeader( new GridHeader( "counts_of_aggregated_values", false, false ) );

        populateGrid( gridOUG, sqlsb.toString(), isZeroAdded, jdbcTemplate );

        return gridOUG;
    }

    // -------------------------------------------------------------------------
    // Advance - Set structure
    // -------------------------------------------------------------------------

    @Override
    public void setDataElementStructureForDataSet( Grid grid, Integer dataSetId, List<Integer> metaIds )
    {
        StringBuffer sqlsb = new StringBuffer();

        sqlsb.append( "(SELECT de.dataelementid, de.name AS DataElement " );
        sqlsb.append( "FROM dataelement de " );
        sqlsb.append( "JOIN datasetmembers dsm ON (de.dataelementid = dsm.dataelementid) " );
        sqlsb.append( "WHERE dsm.datasetid = '" + dataSetId + "' " );
        sqlsb.append( "ORDER BY de.name) " );

        grid.addHeader( new GridHeader( "drilldown_data_element", false, false ) );
        populateMetaStructure( grid, sqlsb.toString(), metaIds, jdbcTemplate );
    }

    @Override
    public void setDataElementStructureForDataElementGroup( Grid grid, Integer dataElementGroupId, List<Integer> metaIds )
    {
        StringBuffer sqlsb = new StringBuffer();

        sqlsb.append( "(SELECT de.dataelementid, de.name AS DataElement " );
        sqlsb.append( "FROM dataelement de " );
        sqlsb.append( "JOIN dataelementgroupmembers degm ON (de.dataelementid = degm.dataelementid) " );
        sqlsb.append( "WHERE degm.dataelementgroupid = '" + dataElementGroupId + "' " );
        sqlsb.append( "GROUP BY de.dataelementid, de.name " );
        sqlsb.append( "ORDER BY de.name) " );

        grid.addHeader( new GridHeader( "drilldown_data_element", false, false ) );
        populateMetaStructure( grid, sqlsb.toString(), metaIds, jdbcTemplate );
    }

    @Override
    public void setDataElementGroupStructureForOrgUnitGroup( Grid grid, Integer orgUnitGroupId, List<Integer> metaIds )
    {
        StringBuffer sqlsb = new StringBuffer();

        sqlsb.append( "(SELECT deg.dataelementgroupid, deg.name AS DataElementGroup " );
        sqlsb.append( "FROM dataelementgroup deg " );
        sqlsb.append( "JOIN dataelementgroupmembers degm ON deg.dataelementgroupid = degm.dataelementgroupid " );
        sqlsb.append( "JOIN datavalue dv ON degm.dataelementid = dv.dataelementid " );
        sqlsb.append( "JOIN organisationunit ou ON dv.sourceid = ou.organisationunitid " );
        sqlsb.append( "JOIN orgunitgroupmembers ougm ON ou.organisationunitid = ougm.organisationunitid " );
        sqlsb.append( "WHERE ougm.orgunitgroupid = '" + orgUnitGroupId + "' " );
        sqlsb.append( "GROUP BY deg.dataelementgroupid, deg.name " );
        sqlsb.append( "ORDER BY deg.name ASC) " );

        grid.addHeader( new GridHeader( "drilldown_data_element_group", false, false ) );
        populateMetaStructure( grid, sqlsb.toString(), metaIds, jdbcTemplate );

    }

    @Override
    public void setStructureForOrgUnit( Grid grid, Integer orgUnitParent, List<Integer> metaIds )
    {
        StringBuffer sqlsb = new StringBuffer();

        sqlsb.append( "(SELECT o.organisationunitid, o.name AS OrganisationUnit " );
        sqlsb.append( "FROM organisationunit o " );
        sqlsb.append( "WHERE o.parentid = '" + orgUnitParent + "' " );
        sqlsb.append( "ORDER BY o.name)" );

        grid.addHeader( new GridHeader( "drilldown_orgunit", false, false ) );
        populateMetaStructure( grid, sqlsb.toString(), metaIds, jdbcTemplate );
    }

    @Override
    public void setDataElementStructureForOrgUnit( Grid grid, Integer orgUnitId, List<Integer> metaIds )
    {
        StringBuffer sqlsb = new StringBuffer();

        sqlsb.append( statementBuilder.queryDataElementStructureForOrgUnit() );

        grid.addHeader( new GridHeader( "drilldown_data_element", false, false ) );
        populateMetaStructure( grid, sqlsb.toString(), metaIds, jdbcTemplate );
    }

    // -------------------------------------------------------------------------
    // Advance - Set count
    // -------------------------------------------------------------------------

    @Override
    public Integer setCountDataElementsForDataSetBetweenPeriods( Grid grid, Integer dataSetId, PeriodType periodType,
        List<Integer> betweenPeriodIds, List<Integer> metaIds, boolean isZeroAdded )
    {
        // Here we uses a for loop to create one big sql statement using UNION.
        // This is done because the count and GROUP BY parts of this query can't
        // be done in another way. The alternative to this method is to actually
        // query the database as many time than betweenPeriodIds.size() tells.
        // But the overhead cost of doing that is bigger than the creation of
        // this UNION query.

        StringBuffer sqlsb = new StringBuffer();

        int i = 0;
        for ( Integer periodId : betweenPeriodIds )
        {
            i++;

            sqlsb.append( "(SELECT de.dataelementid, de.name AS dataelement, COUNT(*) AS counts_of_aggregated_values, p.periodid AS PeriodId, p.startdate AS ColumnHeader " );
            sqlsb.append( "FROM dataelement de JOIN datavalue dv ON (de.dataelementid = dv.dataelementid) " );
            sqlsb.append( "JOIN datasetmembers dsm ON (de.dataelementid = dsm.dataelementid) " );
            sqlsb.append( "JOIN dataset ds ON (dsm.datasetid = ds.datasetid) " );
            sqlsb.append( "JOIN period p ON (dv.periodid = p.periodid) " );
            sqlsb.append( "WHERE dsm.datasetid = '" + dataSetId + "' " );
            sqlsb.append( "AND ds.periodtypeid = '" + periodType.getId() + "' " );
            sqlsb.append( "AND dv.periodid = '" + periodId + "' " );
            sqlsb.append( "GROUP BY de.dataelementid, de.name, p.periodid, p.startDate)" );

            sqlsb.append( i == betweenPeriodIds.size() ? "ORDER BY ColumnHeader" : " UNION " );
        }

        return populateGridAdvanced( grid, sqlsb.toString(), metaIds, isZeroAdded, jdbcTemplate );
    }

    @Override
    public Integer setCountDataElementsForDataElementGroupBetweenPeriods( Grid grid, Integer dataElementGroupId,
        List<Integer> betweenPeriodIds, List<Integer> metaIds, boolean isZeroAdded )
    {
        StringBuffer sqlsb = new StringBuffer();

        int i = 0;
        for ( Integer periodid : betweenPeriodIds )
        {
            i++;

            sqlsb.append( "(SELECT de.dataelementid, de.name AS DataElement, COUNT(dv.value) AS counts_of_aggregated_values, p.periodid AS PeriodId, p.startDate AS ColumnHeader " );
            sqlsb.append( "FROM dataelement de JOIN datavalue dv ON (de.dataelementid = dv.dataelementid) " );
            sqlsb.append( "JOIN dataelementgroupmembers degm ON (de.dataelementid = degm.dataelementid) " );
            sqlsb.append( "JOIN period p ON (dv.periodid = p.periodid) " );
            sqlsb.append( "WHERE degm.dataelementgroupid = '" + dataElementGroupId + "' " );
            sqlsb.append( "AND dv.periodid = '" + periodid + "' " );
            sqlsb.append( "GROUP BY de.dataelementid, de.name, p.periodid, p.startDate) " );

            sqlsb.append( i == betweenPeriodIds.size() ? "ORDER BY ColumnHeader" : " UNION " );
        }

        return populateGridAdvanced( grid, sqlsb.toString(), metaIds, isZeroAdded, jdbcTemplate );
    }

    @Override
    public Integer setCountDataElementGroupsForOrgUnitGroupBetweenPeriods( Grid grid, Integer orgUnitGroupId,
        List<Integer> betweenPeriodIds, List<Integer> metaIds, boolean isZeroAdded )
    {
        StringBuffer sqlsb = new StringBuffer();

        int i = 0;
        for ( Integer periodid : betweenPeriodIds )
        {
            i++;

            sqlsb.append( "(SELECT deg.dataelementgroupid, deg.name, COUNT(dv.value) AS counts_of_aggregated_values, p.periodid AS PeriodId, p.startdate AS ColumnHeader " );
            sqlsb.append( "FROM dataelementgroup AS deg " );
            sqlsb.append( "INNER JOIN dataelementgroupmembers AS degm ON deg.dataelementgroupid = degm.dataelementgroupid " );
            sqlsb.append( "INNER JOIN datavalue AS dv ON degm.dataelementid = dv.dataelementid " );
            sqlsb.append( "INNER JOIN period AS p ON dv.periodid = p.periodid " );
            sqlsb.append( "INNER JOIN organisationunit AS ou ON dv.sourceid = ou.organisationunitid " );
            sqlsb.append( "INNER JOIN orgunitgroupmembers AS ougm ON ou.organisationunitid = ougm.organisationunitid " );
            sqlsb.append( "WHERE p.periodid =  '" + periodid + "' AND ougm.orgunitgroupid =  '" + orgUnitGroupId + "' " );
            sqlsb.append( "GROUP BY deg.dataelementgroupid,deg.name,p.periodid,p.startdate) " );
            sqlsb.append( i == betweenPeriodIds.size() ? "ORDER BY ColumnHeader" : " UNION " );
        }

        return populateGridAdvanced( grid, sqlsb.toString(), metaIds, isZeroAdded, jdbcTemplate );
    }

    @Override
    public Integer setCountOrgUnitsBetweenPeriods( Grid grid, Integer orgUnitParent, List<Integer> betweenPeriodIds,
        Integer maxLevel, List<Integer> metaIds, boolean isZeroAdded )
    {
        StringBuffer sql = new StringBuffer();

        boolean valid = this.setUpQueryForDrillDownDescendants( sql, orgUnitParent, betweenPeriodIds,
            maxLevel );

        return valid ? populateGridAdvanced( grid, sql.toString(), metaIds, isZeroAdded, jdbcTemplate ) : 0;

    }

    @Override
    public Integer setRawDataElementsForOrgUnitBetweenPeriods( Grid grid, Integer orgUnitId,
        List<Integer> betweenPeriodIds, List<Integer> metaIds, boolean isZeroAdded )
    {
        StringBuffer sqlsb = new StringBuffer();

        sqlsb.append( statementBuilder.queryRawDataElementsForOrgUnitBetweenPeriods( orgUnitId, betweenPeriodIds ) );

        return populateGridAdvanced( grid, sqlsb.toString(), metaIds, isZeroAdded, jdbcTemplate );
    }

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    private static void populateMetaStructure( Grid grid, final String sql, List<Integer> metaIds, JdbcTemplate jdbcTemplate )
    {
        log.info( "Meta SQL: " + sql );
        
        SqlRowSet resultSet = jdbcTemplate.queryForRowSet( sql );

        while ( resultSet.next() )
        {
            Integer metaId = resultSet.getInt( 1 );
            String metaName = resultSet.getString( 2 );

            metaIds.add( metaId );
            grid.addRow().addValue( new MetaValue( metaId, metaName ) );
        }
    }

    private static void setHeaderStructure( Grid grid, SqlRowSet resultSet, List<Integer> headerIds, boolean isZeroAdded )
    {
        Integer headerId = null;
        String headerName = null;

        while ( resultSet.next() )
        {
            headerId = resultSet.getInt( 4 );
            headerName = resultSet.getString( 5 );

            GridHeader header = new GridHeader( headerName, headerId + "", String.class.getName(), false, false );

            if ( !headerIds.contains( headerId ) )
            {
                headerIds.add( headerId );
                grid.addHeader( header );

                for ( List<Object> row : grid.getRows() )
                {
                    row.add( isZeroAdded ? "0" : "" );
                }
            }
        }
    }

    private static void populateGrid( Grid grid, final String sql, boolean isZeroAdded, JdbcTemplate jdbcTemplate )
    {
        log.info( "Grid SQL: " + sql );
        
        SqlRowSet resultSet = jdbcTemplate.queryForRowSet( sql );

        while ( resultSet.next() )
        {
            MetaValue metaValue = new MetaValue( resultSet.getInt( 1 ), resultSet.getString( 2 ) );

            grid.addRow().addValue( metaValue ).addValue( checkValue( resultSet.getString( 3 ), isZeroAdded ) );
        }
    }

    private static int populateGridAdvanced( Grid grid, final String sql, List<Integer> metaIds, boolean isZeroAdded,
        JdbcTemplate jdbcTemplate )
    {
        int countRows = 0;
        int oldWidth = grid.getWidth();

        log.info( "Advanced SQL: " + sql );
        
        SqlRowSet rs = jdbcTemplate.queryForRowSet( sql );

        List<Integer> headerIds = new ArrayList<>();
        setHeaderStructure( grid, rs, headerIds, isZeroAdded );

        if ( !rs.first() )
        {
            return countRows;
        }

        rs.beforeFirst();

        while ( rs.next() )
        {
            int rowIndex = metaIds.indexOf( rs.getInt( 1 ) );
            int columnIndex = headerIds.indexOf( rs.getInt( 4 ) ) + oldWidth;

            grid.getRow( rowIndex ).set( columnIndex, checkValue( rs.getString( 3 ), isZeroAdded ) );

            countRows++;
        }
        
        return countRows;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private static String checkValue( String value, boolean isZeroAdded )
    {
        if ( value == null )
        {
            return "null";
        }
        return (value.equals( "0" ) && !isZeroAdded) ? "" : value;
    }
    
    /**
     * Splits a list of integers by by comma. Use this method if you have a list
     * that will be used in f.ins. a WHERE xxx IN (list) clause in SQL.
     * 
     * @param List <Integer> list of Integers
     * @return the list as a string splitted by a comma.
     */
    private String splitListHelper( List<Integer> list )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "(" );

        for ( Integer i : list )
        {
            sb.append( i ).append( "," );  
        }

        return sb.substring( 0, sb.length() - ",".length() ).concat( ")" );
    }

    private boolean setUpQueryForDrillDownDescendants( StringBuffer sb, Integer orgUnitSelected,
        List<Integer> betweenPeriodIds, Integer maxLevel )
    {
        if ( maxLevel == null )
        {
            maxLevel = organisationUnitService.getNumberOfOrganisationalLevels();
        }

        int curLevel = organisationUnitService.getOrganisationUnit( orgUnitSelected ).getLevel();
        int loopSize = betweenPeriodIds.size();

        String descendantQuery = this.setUpQueryGetDescendants( curLevel, maxLevel, orgUnitSelected );

        if ( !descendantQuery.isEmpty() )
        {
            int i = 0;

            for ( Integer periodid : betweenPeriodIds )
            {
                i++;
                /**
                 * Get all descendant level data for all orgunits under the
                 * selected, grouped by the next immediate children of the
                 * selected orgunit Looping through each period UNION construct
                 * appears to be faster with an index placed on periodid's
                 * rather than joining on periodids and then performing the
                 * aggregation step.
                 * 
                 */
                sb.append( " SELECT a.parentid,a.name AS organisationunit,COUNT(*),p.periodid,p.startdate AS columnheader" );
                sb.append( " FROM datavalue dv" );
                sb.append( " INNER JOIN (SELECT DISTINCT x.parentid,x.childid,ou.name FROM(" + descendantQuery + ") x" );
                sb.append( " INNER JOIN organisationunit ou ON x.parentid=ou.organisationunitid) a ON dv.sourceid=a.childid" );
                sb.append( " INNER JOIN period p ON dv.periodid=p.periodid" );
                sb.append( " WHERE dv.periodid=" + periodid );
                sb.append( " GROUP BY a.parentid,a.name,p.periodid,p.startdate" );
                sb.append( i < loopSize ? " UNION " : "" );

            }

            sb.append( " ORDER BY columnheader,organisationunit" );

            return true;
        }

        return false;
    }

    private String setUpQueryGetDescendants( int curLevel, int maxLevel, Integer orgUnitSelected )
    {
        Integer childLevel = curLevel + 1;
        Integer diffLevel = maxLevel - curLevel;

        // The immediate child level can probably be combined into the for loop
        // but we need to clarify whether the selected unit should be present,
        // and if so, how?

        StringBuilder desc_query = new StringBuilder();

        // Loop through each of the descendants until the diff level is reached
        for ( int j = 0; j < diffLevel; j++ )
        {
            desc_query.append( j != 0 ? " UNION " : "" );
            desc_query.append( "SELECT DISTINCT idlevel" + (childLevel) + " AS parentid," );
            desc_query.append( "idlevel" + (childLevel + j) + " AS childid" );
            desc_query.append( " FROM _orgunitstructure" );
            desc_query.append( " WHERE idlevel" + (curLevel) + "='" + orgUnitSelected + "'" );
            desc_query.append( " AND idlevel" + (childLevel + j) + "<>0" );
        }

        return desc_query.toString();
    }
}
