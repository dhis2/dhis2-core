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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
* @author Lars Helge Overland
*/
public class FavoriteDataItemUpgrader
    extends TransactionContextStartupRoutine
{
    private static final Log log = LogFactory.getLog( FavoriteDataItemUpgrader.class );

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Override
    public void executeInTransaction()
    {
        try
        {
            upgradeFavorites( ReportTable.class, "reporttable", Indicator.class, "indicator" );
            upgradeFavorites( ReportTable.class, "reporttable", DataElement.class, "dataelement" );
            upgradeFavorites( ReportTable.class, "reporttable", DataElementOperand.class, "dataelementoperand" );
            upgradeFavorites( ReportTable.class, "reporttable", DataSet.class, "dataset" );

            upgradeFavorites( Chart.class, "chart", Indicator.class, "indicator" );
            upgradeFavorites( Chart.class, "chart", DataElement.class, "dataelement" );
            upgradeFavorites( Chart.class, "chart", DataElementOperand.class, "dataelementoperand" );
            upgradeFavorites( Chart.class, "chart", DataSet.class, "dataset" );

            upgradeFavorites( MapView.class, "mapview", Indicator.class, "indicator" );
            upgradeFavorites( MapView.class, "mapview", DataElement.class, "dataelement" );
            upgradeFavorites( MapView.class, "mapview", DataElementOperand.class, "dataelementoperand" );
            upgradeFavorites( MapView.class, "mapview", DataSet.class, "dataset" );
        }
        catch ( Exception ex )
        {
            log.debug( "Error during data item upgrade of favorites, probably because upgrade was already done", ex );
            return;
        }
    }
    
    private void upgradeFavorites( Class<? extends BaseAnalyticalObject> favoriteClass, String favoriteTablename,
        Class<? extends IdentifiableObject> objectClass, String objectTablename )
    {
        String linkTablename = favoriteTablename + "_" + objectTablename + "s";
        
        String selectSql = "select " + favoriteTablename + "id, " + objectTablename + "id from " + linkTablename + " " +
            "order by " + favoriteTablename + "id, sort_order";
        
        SqlRowSet rs = jdbcTemplate.queryForRowSet( selectSql );
        
        while ( rs.next() )
        {
            int rtId = rs.getInt( 1 );
            int obId = rs.getInt( 2 );
            
            BaseAnalyticalObject favorite = idObjectManager.get( favoriteClass, rtId );
            DimensionalItemObject object = (DimensionalItemObject) idObjectManager.get( objectClass, obId );
            DataDimensionItem item = DataDimensionItem.create( object );
                        
            favorite.getDataDimensionItems().add( item );
            idObjectManager.update( favorite );
            
            log.debug( "Upgraded " + favoriteTablename + " " + favorite.getUid() + " for " + objectTablename + " " + object.getUid() );
        }
        
        String dropSql = "drop table " + linkTablename;
        
        jdbcTemplate.update( dropSql );
        
        log.info( "Update done, dropped table " + linkTablename );
    }
}
