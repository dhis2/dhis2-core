package org.hisp.dhis.analytics.table;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import static org.hisp.dhis.system.util.MathUtils.NUMERIC_LENIENT_REGEXP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class JdbcEventAnalyticsTableManager
    extends AbstractEventJdbcTableManager
{
    private static final ImmutableSet<ValueType> NO_INDEX_VAL_TYPES = ImmutableSet.of( ValueType.TEXT, ValueType.LONG_TEXT );
    
    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.EVENT;
    }
    
    @Override
    @Transactional
    public List<AnalyticsTable> getAnalyticsTables( Date earliest )
    {
        log.info( String.format( "Get tables using earliest: %s, spatial support: %b", earliest, databaseInfo.isSpatialSupport() ) );

        List<AnalyticsTable> tables = new ArrayList<>();
        
        Calendar calendar = PeriodType.getCalendar();
        
        String baseName = getTableName();
        
        List<Program> programs = idObjectManager.getAllNoAcl( Program.class );
        
        for ( Program program : programs )
        {
            List<Integer> dataYears = getDataYears( program, earliest );

            Collections.sort( dataYears );

            AnalyticsTable table = new AnalyticsTable( baseName, getDimensionColumns( program ), Lists.newArrayList(), program );
            
            for ( Integer year : dataYears )
            {                
                table.addPartitionTable( year, PartitionUtils.getStartDate( calendar, year ), PartitionUtils.getEndDate( calendar, year ) );
            }
            
            if ( table.hasPartitionTables() )
            {
                tables.add( table );
            }
        }

        return tables;
    }

    @Override
    public Set<String> getExistingDatabaseTables()
    {
        return partitionManager.getEventAnalyticsPartitions();
    }

    @Override
    protected List<String> getPartitionChecks( AnalyticsTablePartition partition )
    {
        return Lists.newArrayList(
            "yearly = '" + partition.getYear() + "'",
            "executiondate >= '" + DateUtils.getMediumDateString( partition.getStartDate() ) + "'",
            "executiondate < '" + DateUtils.getMediumDateString( partition.getEndDate() ) + "'" );
    }
    
    @Override
    protected void populateTable( AnalyticsTablePartition partition )
    {
        final Program program = partition.getMasterTable().getProgram();
        final String start = DateUtils.getMediumDateString( partition.getStartDate() );
        final String end = DateUtils.getMediumDateString( partition.getEndDate() );
        final String tableName = partition.getTempTableName();
        final String psiExecutionDate = statementBuilder.getCastToDate( "psi.executiondate" );

        String sql = "insert into " + partition.getTempTableName() + " (";

        List<AnalyticsTableColumn> columns = getDimensionColumns( program );
        
        validateDimensionColumns( columns );

        for ( AnalyticsTableColumn col : columns )
        {
            sql += col.getName() + ",";
        }

        sql = TextUtils.removeLastComma( sql ) + ") select ";

        for ( AnalyticsTableColumn col : columns )
        {
            sql += col.getAlias() + ",";
        }

        sql = TextUtils.removeLastComma( sql ) + " ";

        sql += "from programstageinstance psi " +
            "inner join programinstance pi on psi.programinstanceid=pi.programinstanceid " +
            "inner join programstage ps on psi.programstageid=ps.programstageid " +
            "inner join program pr on pi.programid=pr.programid and pi.deleted is false " +
            "inner join categoryoptioncombo ao on psi.attributeoptioncomboid=ao.categoryoptioncomboid " +
            "left join trackedentityinstance tei on pi.trackedentityinstanceid=tei.trackedentityinstanceid and tei.deleted is false " +
            "inner join organisationunit ou on psi.organisationunitid=ou.organisationunitid " +
            "left join _orgunitstructure ous on psi.organisationunitid=ous.organisationunitid " +
            "left join _organisationunitgroupsetstructure ougs on psi.organisationunitid=ougs.organisationunitid " +
                "and (cast(date_trunc('month', psi.executiondate) as date)=ougs.startdate or ougs.startdate is null) " +
            "inner join _categorystructure acs on psi.attributeoptioncomboid=acs.categoryoptioncomboid " +
            "left join _dateperiodstructure dps on " + psiExecutionDate + "=dps.dateperiod " +
            "where psi.executiondate >= '" + start + "' " + 
            "and psi.executiondate < '" + end + "' " +
            "and pr.programid=" + program.getId() + " " + 
            "and psi.organisationunitid is not null " +
            "and psi.executiondate is not null " +
            "and psi.deleted is false ";

        populateAndLog( sql, tableName );
    }

    private List<AnalyticsTableColumn> getDimensionColumns( Program program )
    {
        final String dbl = statementBuilder.getDoubleColumnType();
        final String numericClause = " and value " + statementBuilder.getRegexpMatch() + " '" + NUMERIC_LENIENT_REGEXP + "'";
        final String dateClause = " and value " + statementBuilder.getRegexpMatch() + " '" + DATE_REGEXP + "'";
        
        //TODO dateClause regular expression

        List<AnalyticsTableColumn> columns = new ArrayList<>();

        if ( program.hasCategoryCombo() )
        {
            List<DataElementCategory> categories = program.getCategoryCombo().getCategories();
            
            for ( DataElementCategory category : categories )
            {
                if ( category.isDataDimension() )
                {
                    columns.add( new AnalyticsTableColumn( quote( category.getUid() ), "character(11)", "acs." + quote( category.getUid() ), category.getCreated() ) );
                }
            }
        }

        List<OrganisationUnitLevel> levels = 
            organisationUnitService.getFilledOrganisationUnitLevels();

        List<OrganisationUnitGroupSet> orgUnitGroupSets = 
            idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );

        List<CategoryOptionGroupSet> attributeCategoryOptionGroupSets =
            categoryService.getAttributeCategoryOptionGroupSetsNoAcl();

        for ( OrganisationUnitLevel level : levels )
        {
            String column = quote( PREFIX_ORGUNITLEVEL + level.getLevel() );
            columns.add( new AnalyticsTableColumn( column, "character(11)", "ous." + column, level.getCreated() ) );
        }
        
        for ( OrganisationUnitGroupSet groupSet : orgUnitGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "ougs." + quote( groupSet.getUid() ), groupSet.getCreated() ) );
        }

        for ( CategoryOptionGroupSet groupSet : attributeCategoryOptionGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "acs." + quote( groupSet.getUid() ), groupSet.getCreated() ) );
        }

        for ( PeriodType periodType : PeriodType.getAvailablePeriodTypes() )
        {
            String column = quote( periodType.getName().toLowerCase() );
            columns.add( new AnalyticsTableColumn( column, "text", "dps." + column ) );
        }

        for ( DataElement dataElement : program.getDataElements() )
        {
            String dataType = getColumnType( dataElement.getValueType() );
            String dataClause = dataElement.isNumericType() ? numericClause : dataElement.getValueType().isDate() ? dateClause : "";
            String select = getSelectClause( dataElement.getValueType() );
            boolean skipIndex = NO_INDEX_VAL_TYPES.contains( dataElement.getValueType() ) && !dataElement.hasOptionSet();

            String sql = "(select " + select + " from trackedentitydatavalue where programstageinstanceid=psi.programstageinstanceid " +
                "and dataelementid=" + dataElement.getId() + dataClause + ") as " + quote( dataElement.getUid() );

            columns.add( new AnalyticsTableColumn( quote( dataElement.getUid() ), dataType, sql, skipIndex ) );
        }

        for ( DataElement dataElement : program.getDataElementsWithLegendSet() )
        {
            for ( LegendSet legendSet : dataElement.getLegendSets() )
            {
                String column = quote(
                    dataElement.getUid() + PartitionUtils.SEP + legendSet.getUid() );
                String select = getSelectClause( dataElement.getValueType() );

                String sql =
                    "(select l.uid from maplegend l " +
                    "inner join trackedentitydatavalue dv on l.startvalue <= " + select + " " +
                    "and l.endvalue > " + select + " " +
                    "and l.maplegendsetid=" + legendSet.getId() + " " +
                    "and dv.programstageinstanceid=psi.programstageinstanceid " +
                    "and dv.dataelementid=" + dataElement.getId() + numericClause + ") as " + column;

                columns.add( new AnalyticsTableColumn( column, "character(11)", sql ) );
            }
        }

        for ( TrackedEntityAttribute attribute : program.getNonConfidentialTrackedEntityAttributes() )
        {
            String dataType = getColumnType( attribute.getValueType() );
            String dataClause = attribute.isNumericType() ? numericClause : attribute.isDateType() ? dateClause : "";
            String select = getSelectClause( attribute.getValueType() );
            boolean skipIndex = NO_INDEX_VAL_TYPES.contains( attribute.getValueType() ) && !attribute.hasOptionSet();

            String sql = "(select " + select + " from trackedentityattributevalue where trackedentityinstanceid=pi.trackedentityinstanceid " + 
                "and trackedentityattributeid=" + attribute.getId() + dataClause + ") as " + quote( attribute.getUid() );

            columns.add( new AnalyticsTableColumn( quote( attribute.getUid() ), dataType, sql, skipIndex ) );
        }
        
        for ( TrackedEntityAttribute attribute : program.getNonConfidentialTrackedEntityAttributesWithLegendSet() )
        {
            for ( LegendSet legendSet : attribute.getLegendSets() )
            {
                String column = quote( attribute.getUid() + PartitionUtils.SEP + legendSet.getUid() );
                String select = getSelectClause( attribute.getValueType() );

                String sql =
                    "(select l.uid from maplegend l " +
                    "inner join trackedentityattributevalue av on l.startvalue <= " + select + " " +
                    "and l.endvalue > " + select + " " +
                    "and l.maplegendsetid=" + legendSet.getId() + " " +
                    "and av.trackedentityinstanceid=pi.trackedentityinstanceid " +
                    "and av.trackedentityattributeid=" + attribute.getId() + numericClause + ") as " + column;

                columns.add( new AnalyticsTableColumn( column, "character(11)", sql ) );
            }
        }

        columns.add( new AnalyticsTableColumn( quote( "psi" ), "character(11) not null", "psi.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "pi" ), "character(11) not null", "pi.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "ps" ), "character(11) not null", "ps.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "ao" ), "character(11) not null", "ao.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "enrollmentdate" ), "timestamp", "pi.enrollmentdate" ) );
        columns.add( new AnalyticsTableColumn( quote( "incidentdate" ), "timestamp", "pi.incidentdate" ) );
        columns.add( new AnalyticsTableColumn( quote( "executiondate" ), "timestamp", "psi.executiondate" ) );
        columns.add( new AnalyticsTableColumn( quote( "duedate" ), "timestamp", "psi.duedate" ) );
        columns.add( new AnalyticsTableColumn( quote( "completeddate" ), "timestamp", "psi.completeddate" ) );
        columns.add( new AnalyticsTableColumn( quote( "pistatus" ), "character(25)", "pi.status" ) );
        columns.add( new AnalyticsTableColumn( quote( "psistatus" ), "character(25)", "psi.status" ) );
        columns.add( new AnalyticsTableColumn( quote( "longitude" ), dbl, "psi.longitude" ) );
        columns.add( new AnalyticsTableColumn( quote( "latitude" ), dbl, "psi.latitude" ) );
        columns.add( new AnalyticsTableColumn( quote( "ou" ), "character(11) not null", "ou.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "ouname" ), "text not null", "ou.name" ) );
        columns.add( new AnalyticsTableColumn( quote( "oucode" ), "text", "ou.code" ) );

        if ( databaseInfo.isSpatialSupport() )
        {
            String alias = "(select ST_SetSRID(ST_MakePoint(psi.longitude, psi.latitude), 4326)) as geom";
            columns.add( new AnalyticsTableColumn( quote( "geom" ), "geometry(Point, 4326)", alias, false, "gist" ) );
        }
        
        if ( program.isRegistration() )
        {
            columns.add( new AnalyticsTableColumn( quote( "tei" ), "character(11)", "tei.uid" ) );
        }
                
        return filterDimensionColumns( columns );
    }

    private List<Integer> getDataYears( Program program, Date earliest )
    {
        String sql = 
            "select distinct(extract(year from psi.executiondate)) " +
            "from programstageinstance psi " +
            "inner join programinstance pi on psi.programinstanceid = pi.programinstanceid " +
            "where pi.programid = " + program.getId() + " " +
            "and psi.executiondate is not null " +
            "and psi.deleted is false ";

        if ( earliest != null )
        {
            sql += "and psi.executiondate >= '" + DateUtils.getMediumDateString( earliest ) + "'";
        }
        
        return jdbcTemplate.queryForList( sql, Integer.class );
    }   
}
