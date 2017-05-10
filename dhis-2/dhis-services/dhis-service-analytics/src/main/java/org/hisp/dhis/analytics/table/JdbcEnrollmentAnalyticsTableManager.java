package org.hisp.dhis.analytics.table;

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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
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
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.hisp.dhis.commons.util.TextUtils.removeLast;
import static org.hisp.dhis.program.ProgramIndicator.DB_SEPARATOR_ID;
import static org.hisp.dhis.system.util.MathUtils.NUMERIC_LENIENT_REGEXP;

/**
 * @author Markus Bekken
 */
public class JdbcEnrollmentAnalyticsTableManager
    extends AbstractEventJdbcTableManager
{
    private static final Set<ValueType> NO_INDEX_VAL_TYPES = ImmutableSet.of( ValueType.TEXT, ValueType.LONG_TEXT );
    
    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.ENROLLMENT;
    }
    
    @Override
    @Transactional
    public List<AnalyticsTable> getTables( Date earliest )
    {
        return getTables();
    }
    
    @Override
    public Set<String> getExistingDatabaseTables()
    {
        return new HashSet<>();
    }
    
    private List<AnalyticsTable> getTables() 
    {
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
    protected void populateTable( AnalyticsTable table )
    {
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
            "left join trackedentityinstance tei on pi.trackedentityinstanceid=tei.trackedentityinstanceid and tei.deleted is false " +
            "inner join organisationunit ou on pi.organisationunitid=ou.organisationunitid " +
            "left join _orgunitstructure ous on pi.organisationunitid=ous.organisationunitid " +
            "left join _organisationunitgroupsetstructure ougs on pi.organisationunitid=ougs.organisationunitid " +
            "left join _dateperiodstructure dps on " + piEnrollmentDate + "=dps.dateperiod " +
            "where pr.programid=" + table.getProgram().getId() + " " + 
            "and pi.organisationunitid is not null " +
            "and pi.incidentdate is not null " +
            "and pi.deleted is false ";

        populateAndLog( sql, tableName );
    }

    @Override
    protected List<AnalyticsTableColumn> getDimensionColumns( AnalyticsTable table )
    {
        final String dbl = statementBuilder.getDoubleColumnType();
        final String numericClause = " and value " + statementBuilder.getRegexpMatch() + " '" + NUMERIC_LENIENT_REGEXP + "'";
        final String dateClause = " and value " + statementBuilder.getRegexpMatch() + " '" + DATE_REGEXP + "'";

        List<AnalyticsTableColumn> columns = new ArrayList<>();

        List<OrganisationUnitLevel> levels = 
            organisationUnitService.getFilledOrganisationUnitLevels();

        List<OrganisationUnitGroupSet> orgUnitGroupSets = 
            idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );

        for ( OrganisationUnitLevel level : levels )
        {
            String column = quote( PREFIX_ORGUNITLEVEL + level.getLevel() );
            columns.add( new AnalyticsTableColumn( column, "character(11)", "ous." + column, level.getCreated() ) );
        }
        
        for ( OrganisationUnitGroupSet groupSet : orgUnitGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "ougs." + quote( groupSet.getUid() ), groupSet.getCreated() ) );
        }

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

                String sql = "(select " + select + " from trackedentitydatavalue tedv " + 
                    "inner join programstageinstance psi on psi.programstageinstanceid = tedv.programstageinstanceid " + 
                    "where psi.executiondate is not null " + 
                    "and psi.deleted is false " + 
                    "and psi.programinstanceid=pi.programinstanceid " +
                    dataClause + " " +
                    "and tedv.dataelementid=" + dataElement.getId() + " " +
                    "and psi.programstageid=" + programStage.getId() + " " +
                    "order by psi.executiondate desc " +
                    "limit 1) as " + quote( programStage.getUid() + DB_SEPARATOR_ID + dataElement.getUid() );

                columns.add( new AnalyticsTableColumn( quote( programStage.getUid() + DB_SEPARATOR_ID + dataElement.getUid() ), dataType, sql, skipIndex ) ); 
            }
        }

        for ( TrackedEntityAttribute attribute : table.getProgram().getNonConfidentialTrackedEntityAttributes() )
        {
            String dataType = getColumnType( attribute.getValueType() );
            String dataClause = attribute.isNumericType() ? numericClause : attribute.isDateType() ? dateClause : "";
            String select = getSelectClause( attribute.getValueType() );
            boolean skipIndex = NO_INDEX_VAL_TYPES.contains( attribute.getValueType() ) && !attribute.hasOptionSet();

            String sql = "(select " + select + " from trackedentityattributevalue " +
                "where trackedentityinstanceid=pi.trackedentityinstanceid " + 
                "and trackedentityattributeid=" + attribute.getId() + dataClause + ") as " + quote( attribute.getUid() );

            columns.add( new AnalyticsTableColumn( quote( attribute.getUid() ), dataType, sql, skipIndex ) );
        }
        
        AnalyticsTableColumn pi = new AnalyticsTableColumn( quote( "pi" ), "character(11) not null", "pi.uid" );
        AnalyticsTableColumn erd = new AnalyticsTableColumn( quote( "enrollmentdate" ), "timestamp", "pi.enrollmentdate" );
        AnalyticsTableColumn id = new AnalyticsTableColumn( quote( "incidentdate" ), "timestamp", "pi.incidentdate" );
        
        final String executionDateSql = "(select psi.executionDate from programstageinstance psi " +
            "where psi.programinstanceid=pi.programinstanceid " + 
            "and psi.executiondate is not null " + 
            "and psi.deleted is false " +
            "order by psi.executiondate desc " +
            "limit 1) as " + quote( "executiondate" );        
        AnalyticsTableColumn ed = new AnalyticsTableColumn( quote( "executiondate" ), "timestamp", executionDateSql );
        
        final String dueDateSql = "(select psi.duedate from programstageinstance psi " + 
            "where psi.programinstanceid = pi.programinstanceid " + 
            "and psi.duedate is not null " + 
            "and psi.deleted is false " +
            "order by psi.duedate desc " +
            "limit 1) as " + quote( "duedate" );        
        AnalyticsTableColumn dd = new AnalyticsTableColumn( quote( "duedate" ), "timestamp", dueDateSql );
        
        AnalyticsTableColumn cd = new AnalyticsTableColumn( quote( "completeddate" ), "timestamp", "case status when 'COMPLETED' then enddate end" );
        AnalyticsTableColumn es = new AnalyticsTableColumn( quote( "enrollmentstatus" ), "character(50)", "pi.status" );
        AnalyticsTableColumn longitude = new AnalyticsTableColumn( quote( "longitude" ), dbl, "pi.longitude" );
        AnalyticsTableColumn latitude = new AnalyticsTableColumn( quote( "latitude" ), dbl, "pi.latitude" );
        AnalyticsTableColumn ou = new AnalyticsTableColumn( quote( "ou" ), "character(11) not null", "ou.uid" );
        AnalyticsTableColumn oun = new AnalyticsTableColumn( quote( "ouname" ), "character varying(230) not null", "ou.name" );
        AnalyticsTableColumn ouc = new AnalyticsTableColumn( quote( "oucode" ), "character varying(50)", "ou.code" );

        columns.addAll( Lists.newArrayList( pi, erd, id, ed, es, dd, cd, longitude, latitude, ou, oun, ouc ) );

        if ( databaseInfo.isSpatialSupport() )
        {
            String alias = "(select ST_SetSRID(ST_MakePoint(pi.longitude, pi.latitude), 4326)) as geom";
            columns.add( new AnalyticsTableColumn( quote( "geom" ), "geometry(Point, 4326)", alias, false, "gist" ) );
        }
        
        if ( table.hasProgram() && table.getProgram().isRegistration() )
        {
            columns.add( new AnalyticsTableColumn( quote( "tei" ), "character(11)", "tei.uid" ) );
        }
        
        return filterDimensionColumns( columns );
    }    
}
