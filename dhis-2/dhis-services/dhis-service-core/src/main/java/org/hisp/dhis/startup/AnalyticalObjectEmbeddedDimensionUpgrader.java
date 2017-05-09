package org.hisp.dhis.startup;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetDimension;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public class AnalyticalObjectEmbeddedDimensionUpgrader
    extends TransactionContextStartupRoutine
{
    private static final Log log = LogFactory.getLog( AnalyticalObjectEmbeddedDimensionUpgrader.class );
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private IdentifiableObjectManager idObjectManager;


    @Override
    public void executeInTransaction()
    {
        try
        {
            upgradeGrupSetDimensions( "reporttable", "orgunitgroupset", "orgunitgroup", ReportTable.class );
            upgradeGrupSetDimensions( "chart", "orgunitgroupset", "orgunitgroup", Chart.class );
            upgradeGrupSetDimensions( "eventreport", "orgunitgroupset", "orgunitgroup", EventReport.class );
            upgradeGrupSetDimensions( "eventchart", "orgunitgroupset", "orgunitgroup", EventChart.class );
        }
        catch ( Exception ex )
        {
            log.info( "Error during group set dimensions upgrade of favorites, probably beacuse upgrade is done", ex );
            return;
        }
        
    }

    private void upgradeGrupSetDimensions( String favorite, String dimension, String item, Class<? extends AnalyticalObject> clazz )
    {
        String groupSetSqlPattern = 
            "select distinct d.{favorite}id, gsm.{dimension}id " +
            "from {favorite}_{item}s d " +
            "inner join {dimension}members gsm on d.{item}id=gsm.{item}id";

        String groupSetSql = TextUtils.replace( groupSetSqlPattern, "{favorite}", favorite, "{dimension}", dimension, "{item}", item );
        
        log.info( String.format( "Dimension SQL: %s", groupSetSql ) );
        
        String groupSqlPattern =
            "select d.{item}id " +
            "from {favorite}_{item}s d " +
            "inner join {dimension}members gsm on d.{item}id=gsm.{item}id " +
            "where d.{favorite}id={favoriteId} " +
            "and gsm.{dimension}id={dimensionId} " +
            "order by d.sort_order";
        
        SqlRowSet groupSetRs = jdbcTemplate.queryForRowSet( groupSetSql );
        
        while ( groupSetRs.next() )
        {
            int favoriteId = groupSetRs.getInt( 1 );
            int dimensionId = groupSetRs.getInt( 2 );
            
            AnalyticalObject analyticalObject = idObjectManager.get( clazz, favoriteId );
            
            String groupSql = TextUtils.replace( groupSqlPattern, "{favorite}", favorite, "{dimension}", dimension, 
                "{item}", item, "{favoriteId}", String.valueOf( favoriteId ), "{dimensionId}", String.valueOf( dimensionId ) );
            
            SqlRowSet groupRs = jdbcTemplate.queryForRowSet( groupSql );

            List<OrganisationUnitGroup> groups = new ArrayList<>();
            
            while ( groupRs.next() )
            {
                int gId = groupRs.getInt( 1 );
                
                OrganisationUnitGroup group = idObjectManager.get( OrganisationUnitGroup.class, gId );                
                groups.add( group );
            }
            
            OrganisationUnitGroupSet groupSet = idObjectManager.get( OrganisationUnitGroupSet.class, dimensionId );
            
            OrganisationUnitGroupSetDimension dim = new OrganisationUnitGroupSetDimension();
            dim.setDimension( groupSet );
            dim.setItems( groups );
            
            analyticalObject.addOrganisationUnitGroupSetDimension( dim );
            
            idObjectManager.update( analyticalObject );
            
            log.info( String.format( "Added %s group set dimension: %s with groups: %d for favorite: %s", 
                favorite, groupSet.getUid(), groups.size(), analyticalObject.getUid() ) );
        }
        
        String dropSql = TextUtils.replace( "drop table {favorite}_{item}s", "{favorite}", favorite, "{item}", item );
        
        jdbcTemplate.update( dropSql );
        
        log.info( String.format( "Dropped link table, update done for %s", favorite ) );
    }
}
