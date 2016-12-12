package org.hisp.dhis.analytics.table;
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

import static org.hisp.dhis.commons.util.TextUtils.removeLast;
import static org.hisp.dhis.system.util.MathUtils.NUMERIC_LENIENT_REGEXP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class JdbcEnrollmentAnalyticsTableManager
    extends AbstractJdbcTableManager
{
    private static final Set<ValueType> NO_INDEX_VAL_TYPES = ImmutableSet.of( ValueType.TEXT, ValueType.LONG_TEXT );
    public static final String SEP = "_";
    
    @Override
    @Transactional
    public List<AnalyticsTable> getTables( Date earliest )
    {
        return getTables();
    }
    
    private List<AnalyticsTable> getTables() {
        List<AnalyticsTable> tables = new UniqueArrayList<>();
        List<Program> programs = idObjectManager.getAllNoAcl( Program.class );

        String baseName = getTableName();
        
        for ( Program program : programs )
        {
            AnalyticsTable table = new AnalyticsTable( baseName, null, null, program );
            List<AnalyticsTableColumn> dimensionColumns = getDimensionColumns( table );
            table.setDimensionColumns( dimensionColumns );
            tables.add( table );
        }
        
        return tables;
    }
    
    @Override
    public String getTableName()
    {
        return ENROLLMENT_ANALYTICS_TABLE_NAME;
    }

    @Override
    public void createTable( AnalyticsTable table )
    {
        final String tableName = table.getTempTableName();

        final String sqlDrop = "drop table " + tableName;

        executeSilently( sqlDrop );

        String sqlCreate = "create table " + tableName + " (";

        List<AnalyticsTableColumn> columns = getDimensionColumns( table );
        
        validateDimensionColumns( columns );
        
        for ( AnalyticsTableColumn col : columns )
        {
            sqlCreate += col.getName() + " " + col.getDataType() + ",";
        }

        sqlCreate = removeLast( sqlCreate, 1 ) + ") ";

        sqlCreate += statementBuilder.getTableOptions( false );

        log.info( "Creating table: " + tableName + ", columns: " + columns.size() );
        
        log.debug( "Create SQL: " + sqlCreate );
        
        jdbcTemplate.execute( sqlCreate );
    }


    @Override
    public List<Integer> getDataYears( Date earliest )
    {
        return null;
    }

    @Override
    protected List<AnalyticsTableColumn> getDimensionColumns( AnalyticsTable table )
    {
        final String dbl = statementBuilder.getDoubleColumnType();
        final String numericClause = " and value " + statementBuilder.getRegexpMatch() + " '" + NUMERIC_LENIENT_REGEXP + "'";
        final String dateClause = " and value " + statementBuilder.getRegexpMatch() + " '" + DATE_REGEXP + "'";

        List<AnalyticsTableColumn> columns = new ArrayList<>();

        /* TODO: must figure out categorystructure
        if ( table.getProgram().hasCategoryCombo() )
        {
            List<DataElementCategory> categories = table.getProgram().getCategoryCombo().getCategories();
            
            for ( DataElementCategory category : categories )
            {
                columns.add( new AnalyticsTableColumn( quote( category.getUid() ), "character(11)", "acs." + quote( category.getUid() ) ) );
            }
        }*/

        List<OrganisationUnitLevel> levels = 
            organisationUnitService.getFilledOrganisationUnitLevels();

        List<OrganisationUnitGroupSet> orgUnitGroupSets = 
            idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );

        //List<CategoryOptionGroupSet> attributeCategoryOptionGroupSets =
        //    categoryService.getAttributeCategoryOptionGroupSetsNoAcl();

        for ( OrganisationUnitLevel level : levels )
        {
            String column = quote( PREFIX_ORGUNITLEVEL + level.getLevel() );
            columns.add( new AnalyticsTableColumn( column, "character(11)", "ous." + column ) );
        }
        
        for ( OrganisationUnitGroupSet groupSet : orgUnitGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "ougs." + quote( groupSet.getUid() ) ) );
        }

        /* TODO: must figure out category structure
        for ( CategoryOptionGroupSet groupSet : attributeCategoryOptionGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "acs." + quote( groupSet.getUid() ) ) );
        }*/

        for ( PeriodType periodType : PeriodType.getAvailablePeriodTypes() )
        {
            String column = quote( periodType.getName().toLowerCase() );
            columns.add( new AnalyticsTableColumn( column, "character varying(15)", "dps." + column ) );
        }

        for ( ProgramStage programStage : table.getProgram().getProgramStages() )
        {
            for( ProgramStageDataElement programStageDataElement : 
                programStage.getProgramStageDataElements() )
            {
                DataElement dataElement = programStageDataElement.getDataElement();
                ValueType valueType = dataElement.getValueType();
                String dataType = getColumnType( valueType );
                String dataClause = dataElement.isNumericType() ? numericClause : dataElement.getValueType().isDate() ? dateClause : "";
                String select = getSelectClause( valueType );
                boolean skipIndex = NO_INDEX_VAL_TYPES.contains( dataElement.getValueType() ) && !dataElement.hasOptionSet();

                //Only this has been changed so far:
                String sql = "( SELECT " + select + " FROM trackedentitydatavalue tedv " + 
                             "JOIN programstageinstance psi " + 
                             "ON psi.programstageinstanceid = tedv.programstageinstanceid " + 
                             "WHERE psi.executiondate is not null " + 
                             "AND psi.deleted is not true " + 
                             "AND psi.programinstanceid = pi.programinstanceid " +
                             dataClause + 
                             " AND tedv.dataelementid = " + dataElement.getId() + 
                             " AND psi.programStageId = " + programStage.getId() +
                             " ORDER BY psi.executiondate desc " +
                             "LIMIT 1 ) as " + quote( programStage.getUid() + SEP + dataElement.getUid() );


                columns.add( new AnalyticsTableColumn( quote( programStage.getUid() + SEP + dataElement.getUid() ), dataType, sql, skipIndex ) ); 
            }
        }

        /* TODO: REIMPLEMENT
        for ( DataElement dataElement : table.getProgram().getDataElementsWithLegendSet() )
        {
            String column = quote( dataElement.getUid() + PartitionUtils.SEP + dataElement.getLegendSet().getUid() );
            String select = getSelectClause( dataElement.getValueType() );
            
            String sql = "(select l.uid from maplegend l inner join maplegendsetmaplegend lsl on l.maplegendid=lsl.maplegendid " +
                "inner join trackedentitydatavalue dv on l.startvalue <= " + select + " and l.endvalue > " + select + " " +
                "and lsl.legendsetid=" + dataElement.getLegendSet().getId() + " and dv.programstageinstanceid=psi.programstageinstanceid " + 
                "and dv.dataelementid=" + dataElement.getId() + numericClause + ") as " + column;
                
            columns.add( new AnalyticsTableColumn( column, "character(11)", sql ) );
        }*/

        //Unchanged
        for ( TrackedEntityAttribute attribute : table.getProgram().getNonConfidentialTrackedEntityAttributes() )
        {
            String dataType = getColumnType( attribute.getValueType() );
            String dataClause = attribute.isNumericType() ? numericClause : attribute.isDateType() ? dateClause : "";
            String select = getSelectClause( attribute.getValueType() );
            boolean skipIndex = NO_INDEX_VAL_TYPES.contains( attribute.getValueType() ) && !attribute.hasOptionSet();

            String sql = "(select " + select + " from trackedentityattributevalue where trackedentityinstanceid=pi.trackedentityinstanceid " + 
                "and trackedentityattributeid=" + attribute.getId() + dataClause + ") as " + quote( attribute.getUid() );

            columns.add( new AnalyticsTableColumn( quote( attribute.getUid() ), dataType, sql, skipIndex ) );
        }
/*      TODO: REIMPLEMENT
        for ( TrackedEntityAttribute attribute : table.getProgram().getNonConfidentialTrackedEntityAttributesWithLegendSet() )
        {
            String column = quote( attribute.getUid() + PartitionUtils.SEP + attribute.getLegendSet().getUid() );
            String select = getSelectClause( attribute.getValueType() );
            
            String sql = "(select l.uid from maplegend l inner join maplegendsetmaplegend lsl on l.maplegendid=lsl.maplegendid " +
                "inner join trackedentityattributevalue av on l.startvalue <= " + select + " and l.endvalue > " + select + " " +
                "and lsl.legendsetid=" + attribute.getLegendSet().getId() + " and av.trackedentityinstanceid=pi.trackedentityinstanceid " +
                "and av.trackedentityattributeid=" + attribute.getId() + numericClause + ") as " + column;
            
            columns.add( new AnalyticsTableColumn( column, "character(11)", sql ) );
        }*/

        AnalyticsTableColumn pi = new AnalyticsTableColumn( quote( "pi" ), "character(11) not null", "pi.uid" );
        AnalyticsTableColumn erd = new AnalyticsTableColumn( quote( "enrollmentdate" ), "timestamp", "pi.enrollmentdate" );
        AnalyticsTableColumn id = new AnalyticsTableColumn( quote( "incidentdate" ), "timestamp", "pi.incidentdate" );
        //PSI columns fallback in enrollment analytics
        String executionDateSql = "( SELECT psi.executionDate FROM programstageinstance psi " + 
            "JOIN programinstance pi " + 
            "ON psi.programinstanceid = pi.programinstanceid " + 
            "WHERE psi.executiondate is not null " + 
            "AND psi.deleted is not true " +
            "ORDER BY psi.executiondate desc " +
            "LIMIT 1 ) as " + quote( "executiondate" );
        AnalyticsTableColumn ed = new AnalyticsTableColumn( quote( "executiondate" ), "timestamp", executionDateSql );
        String dueDateSql = "( SELECT psi.duedate FROM programstageinstance psi " + 
            "JOIN programinstance pi " + 
            "ON psi.programinstanceid = pi.programinstanceid " + 
            "WHERE psi.duedate is not null " + 
            "AND psi.deleted is not true " +
            "ORDER BY psi.duedate desc " +
            "LIMIT 1 ) as " + quote( "duedate" );
        AnalyticsTableColumn dd = new AnalyticsTableColumn( quote( "duedate" ), "timestamp", dueDateSql );
        //Completed date is interpreted as enrollment completed date
        String completedDateSql = "( SELECT psi.completeddate FROM programstageinstance psi " + 
            "JOIN programinstance pi " + 
            "ON psi.programinstanceid = pi.programinstanceid " + 
            "WHERE psi.completeddate is not null " + 
            "AND psi.deleted is not true " +
            "ORDER BY psi.completeddate desc " +
            "LIMIT 1 ) as " + quote( "completeddate" );
        AnalyticsTableColumn cd = new AnalyticsTableColumn( quote( "completeddate" ), "timestamp", "CASE status WHEN 'COMPLETED' THEN enddate END" );
        //AnalyticsTableColumn es = new AnalyticsTableColumn( quote( "psistatus" ), "character(25)", "psi.status" );
        String longitudeSql = "( SELECT psi.longitude FROM programstageinstance psi " + 
            "JOIN programinstance pi " + 
            "ON psi.programinstanceid = pi.programinstanceid " + 
            "WHERE psi.executiondate is not null " + 
            "AND psi.deleted is not true " +
            "ORDER BY psi.executiondate desc " +
            "LIMIT 1 ) as " + quote( "longitude" );
        AnalyticsTableColumn longitude = new AnalyticsTableColumn( quote( "longitude" ), dbl, longitudeSql );
        String latitudeSql = "( SELECT psi.latitude FROM programstageinstance psi " + 
            "JOIN programinstance pi " + 
            "ON psi.programinstanceid = pi.programinstanceid " + 
            "WHERE psi.executiondate is not null " + 
            "AND psi.deleted is not true " +
            "ORDER BY psi.executiondate desc " +
            "LIMIT 1 ) as " + quote( "latitude" );
        AnalyticsTableColumn latitude = new AnalyticsTableColumn( quote( "latitude" ), dbl, latitudeSql );
        AnalyticsTableColumn ou = new AnalyticsTableColumn( quote( "ou" ), "character(11) not null", "ou.uid" );
        AnalyticsTableColumn oun = new AnalyticsTableColumn( quote( "ouname" ), "character varying(230) not null", "ou.name" );
        AnalyticsTableColumn ouc = new AnalyticsTableColumn( quote( "oucode" ), "character varying(50)", "ou.code" );

        //columns.addAll( Lists.newArrayList( psi, pi, ps, erd, id, ed, dd, cd, es, longitude, latitude, ou, oun, ouc ) );
        columns.addAll( Lists.newArrayList( pi, erd, id, ed, dd, cd, longitude, latitude, ou, oun, ouc ) );

        if ( databaseInfo.isSpatialSupport() )
        {
            String alias = "(select ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)) as geom";
            columns.add( new AnalyticsTableColumn( quote( "geom" ), "geometry(Point, 4326)", alias, false, "gist" ) );
        }
        
        if ( table.hasProgram() && table.getProgram().isRegistration() )
        {
            columns.add( new AnalyticsTableColumn( quote( "tei" ), "character(11)", "tei.uid" ) );
        }
                
        return columns;
    }
    
    // ***************************************************************************************
    //UNCHANGED HELPERS COPIED FORM EVENT ANALYTICS BELOW HERE - can be moved into central codes.
    
    @Override
    public Future<?> applyAggregationLevels( ConcurrentLinkedQueue<AnalyticsTable> tables,
        Collection<String> dataElements, int aggregationLevel )
    {
        return null; // Not relevant
    }

    @Override
    public Future<?> vacuumTablesAsync( ConcurrentLinkedQueue<AnalyticsTable> tables )
    {
        return null; // Not relevant
    }
    
    @Override
    public String validState()
    {
        boolean hasData = jdbcTemplate.queryForRowSet( "select dataelementid from trackedentitydatavalue limit 1" ).next();
        
        if ( !hasData )
        {
            return "No events exist, not updating event analytics tables";
        }
        
        return null;
    }
    
    /**
     * Returns the database column type based on the given value type. For boolean
     * values, 1 means true, 0 means false and null means no value.
     */
    private String getColumnType( ValueType valueType )
    {
        if ( Double.class.equals( valueType.getJavaClass() ) || Integer.class.equals( valueType.getJavaClass() ) )
        {
            return statementBuilder.getDoubleColumnType();
        }
        else if ( Boolean.class.equals( valueType.getJavaClass() ) )
        {
            return "integer";
        }
        else if ( Date.class.equals( valueType.getJavaClass() ) )
        {
            return "timestamp";
        }
        else
        {
            return "text";
        }
    }
    
    /**
     * Returns the select clause, potentially with a cast statement, based on the
     * given value type.
     */
    private String getSelectClause( ValueType valueType )
    {
        if ( Double.class.equals( valueType.getJavaClass() ) || Integer.class.equals( valueType.getJavaClass() ) )
        {
            return "cast(value as " + statementBuilder.getDoubleColumnType() + ")";
        }
        else if ( Boolean.class.equals( valueType.getJavaClass() ) )
        {
            return "case when value = 'true' then 1 when value = 'false' then 0 else null end";
        }
        else if ( Date.class.equals( valueType.getJavaClass() ) )
        {
            return "cast(value as timestamp)";
        }
        else
        {
            return "value";
        }
    }

    @Override
    public Set<String> getExistingDatabaseTables()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void populateTable( AnalyticsTable table )
    {

        final String start = DateUtils.getMediumDateString( DateUtils.getMinimumDate() );
        final String end = DateUtils.getMediumDateString( DateUtils.getMaximumDate() );
        final String tableName = table.getTempTableName();
        final String piEnrollmentDate = statementBuilder.getCastToDate( "pi.enrollmentdate" );

        String sql = "insert into " + table.getTempTableName() + " (";

        List<AnalyticsTableColumn> columns = getDimensionColumns( table );
        
        validateDimensionColumns( columns );

        for ( AnalyticsTableColumn col : columns )
        {
            sql += col.getName() + ",";
        }

        sql = removeLast( sql, 1 ) + ") select ";

        for ( AnalyticsTableColumn col : columns )
        {
            sql += col.getAlias() + ",";
        }

        sql = removeLast( sql, 1 ) + " ";

        sql += "from programinstance pi " +
            "inner join program pr on pi.programid=pr.programid " +
            "left join trackedentityinstance tei on pi.trackedentityinstanceid=tei.trackedentityinstanceid " +
            "inner join organisationunit ou on pi.organisationunitid=ou.organisationunitid " +
            "left join _orgunitstructure ous on pi.organisationunitid=ous.organisationunitid " +
            "left join _organisationunitgroupsetstructure ougs on pi.organisationunitid=ougs.organisationunitid " +
            //"left join _categorystructure acs on psi.attributeoptioncomboid=acs.categoryoptioncomboid " +
            "left join _dateperiodstructure dps on " + piEnrollmentDate + "=dps.dateperiod " +
            "where pi.incidentdate >= '" + start + "' " + 
            "and pi.incidentdate <= '" + end + "' " +
            "and pr.programid=" + table.getProgram().getId() + " " + 
            "and pi.organisationunitid is not null " +
            "and pi.incidentdate is not null";

        populateAndLog( sql, tableName );
    }

}
