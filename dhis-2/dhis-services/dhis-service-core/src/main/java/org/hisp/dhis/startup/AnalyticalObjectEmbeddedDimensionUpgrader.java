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
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.CategoryOptionGroupSetDimension;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementGroupSetDimension;
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
import org.springframework.util.Assert;

/**
* @author Lars Helge Overland
*/
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
        BiConsumer<BaseDimensionalEmbeddedObject, AnalyticalObject> dataElementGroupSetConsumer = ( embeddedDimension, analyticalObject ) -> {
            DataElementGroupSetDimension dimension = new DataElementGroupSetDimension();
            dimension.setDimension( (DataElementGroupSet) embeddedDimension.getDimension() );
            dimension.setItems( DimensionalObjectUtils.asTypedList( embeddedDimension.getItems() ) );
            analyticalObject.addDataElementGroupSetDimension( dimension );
        };
        
        BiConsumer<BaseDimensionalEmbeddedObject, AnalyticalObject> orgUnitGroupSetConsumer = ( embeddedDimension, analyticalObject ) -> {
            OrganisationUnitGroupSetDimension dimension = new OrganisationUnitGroupSetDimension();
            dimension.setDimension( (OrganisationUnitGroupSet) embeddedDimension.getDimension() );
            dimension.setItems( DimensionalObjectUtils.asTypedList( embeddedDimension.getItems() ) );
            analyticalObject.addOrganisationUnitGroupSetDimension( dimension );
        };
        
        BiConsumer<BaseDimensionalEmbeddedObject, AnalyticalObject> categoryOptionGroupSetConsumer = ( embeddedDimension, analyticalObject ) -> {
            CategoryOptionGroupSetDimension dimension = new CategoryOptionGroupSetDimension();
            dimension.setDimension( (CategoryOptionGroupSet) embeddedDimension.getDimension() );
            dimension.setItems( DimensionalObjectUtils.asTypedList( embeddedDimension.getItems() ) );
            analyticalObject.addCategoryOptionGroupSetDimension( dimension );
        };
        
        try
        {
            upgradeGrupSetDimensions( "reporttable", "orgunitgroupset", "orgunitgroup", ReportTable.class, OrganisationUnitGroupSet.class, OrganisationUnitGroup.class, orgUnitGroupSetConsumer );
            upgradeGrupSetDimensions( "reporttable", "dataelementgroupset", "dataelementgroup", ReportTable.class, DataElementGroupSet.class, DataElementGroup.class, dataElementGroupSetConsumer );
            upgradeGrupSetDimensions( "reporttable", "categoryoptiongroupset", "categoryoptiongroup", ReportTable.class, CategoryOptionGroupSet.class, CategoryOptionGroup.class, categoryOptionGroupSetConsumer );
    
            upgradeGrupSetDimensions( "chart", "orgunitgroupset", "orgunitgroup", Chart.class, OrganisationUnitGroupSet.class, OrganisationUnitGroup.class, orgUnitGroupSetConsumer );
            upgradeGrupSetDimensions( "chart", "dataelementgroupset", "dataelementgroup", Chart.class, DataElementGroupSet.class, DataElementGroup.class, dataElementGroupSetConsumer );
            upgradeGrupSetDimensions( "chart", "categoryoptiongroupset", "categoryoptiongroup", Chart.class, CategoryOptionGroupSet.class, CategoryOptionGroup.class, categoryOptionGroupSetConsumer );
            
            upgradeGrupSetDimensions( "eventreport", "orgunitgroupset", "orgunitgroup", EventReport.class, OrganisationUnitGroupSet.class, OrganisationUnitGroup.class, orgUnitGroupSetConsumer );
            upgradeGrupSetDimensions( "eventchart", "orgunitgroupset", "orgunitgroup", EventChart.class, OrganisationUnitGroupSet.class, OrganisationUnitGroup.class, orgUnitGroupSetConsumer );
        }
        catch ( Exception ex )
        {
            log.debug( "Error during group set dimensions upgrade of favorite, probably because upgrade was already done", ex );
            return;
        }        
    }

    @SuppressWarnings("unchecked")
    private void upgradeGrupSetDimensions( String favorite, String dimension, String item, 
        Class<? extends AnalyticalObject> favoriteClazz, Class<? extends DimensionalObject> dimensionClass, Class<? extends DimensionalItemObject> itemClass,
        BiConsumer<BaseDimensionalEmbeddedObject, AnalyticalObject> consumer )
    {
        String groupSetSqlPattern = 
            "select distinct d.{favorite}id, gsm.{dimension}id " +
            "from {favorite}_{item}s d " +
            "inner join {dimension}members gsm on d.{item}id=gsm.{item}id";

        String groupSetSql = TextUtils.replace( groupSetSqlPattern, "{favorite}", favorite, "{dimension}", dimension, "{item}", item );
        
        log.debug( String.format( "Group set SQL: %s", groupSetSql ) );
        
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

            AnalyticalObject analyticalObject = idObjectManager.get( favoriteClazz, favoriteId );
            DimensionalObject groupSet = idObjectManager.get( dimensionClass, dimensionId );

            Assert.notNull( analyticalObject, String.format( "Analytical object not found: %s, class: %s", favoriteId, favoriteClazz ) );
            Assert.notNull( groupSet, String.format( "Group set not found: %s, class: %s", dimensionId, dimensionClass ) );
            
            String groupSql = TextUtils.replace( groupSqlPattern, "{favorite}", favorite, "{dimension}", dimension, 
                "{item}", item, "{favoriteId}", String.valueOf( favoriteId ), "{dimensionId}", String.valueOf( dimensionId ) );
            
            log.debug( String.format( "Group SQL: %s", groupSql ) );
            
            SqlRowSet groupRs = jdbcTemplate.queryForRowSet( groupSql );

            List<Integer> groupIds = new ArrayList<>();
            
            while ( groupRs.next() )
            {
                groupIds.add( groupRs.getInt( 1 ) );
            }

            List<DimensionalItemObject> groups = (List<DimensionalItemObject>) idObjectManager.getById( itemClass, groupIds );
            
            Assert.notNull( groups, "Groups cannot be null" );
            
            BaseDimensionalEmbeddedObject embeddedDimension = new BaseDimensionalEmbeddedObject( groupSet, groups );
            
            consumer.accept( embeddedDimension, analyticalObject );
            
            idObjectManager.update( analyticalObject );
            
            log.info( String.format( "Added %s group set dimension: %s with groups: %d for favorite: %s", 
                favorite, groupSet.getUid(), groups.size(), analyticalObject.getUid() ) );
        }
        
        String dropSql = TextUtils.replace( "drop table {favorite}_{item}s", "{favorite}", favorite, "{item}", item );
        
        jdbcTemplate.update( dropSql );
        
        log.info( String.format( "Dropped table, update done for favorite: %s and dimension: %s", favorite, dimension ) );
    }
    
    class BaseDimensionalEmbeddedObject
    {
        private DimensionalObject dimension;
        private List<DimensionalItemObject> items = new ArrayList<>();
        
        public BaseDimensionalEmbeddedObject( DimensionalObject dimension, List<DimensionalItemObject> items )
        {
            this.dimension = dimension;
            this.items = items;
        }

        public DimensionalObject getDimension()
        {
            return dimension;
        }

        public List<DimensionalItemObject> getItems()
        {
            return items;
        }
    }
}
