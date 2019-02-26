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

import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_50;
import static org.hisp.dhis.analytics.ColumnDataType.DOUBLE;
import static org.hisp.dhis.analytics.ColumnDataType.GEOMETRY;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.ColumnDataType.TIMESTAMP;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.api.util.DateUtils.getLongDateString;
import static org.hisp.dhis.system.util.MathUtils.NUMERIC_LENIENT_REGEXP;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.ColumnDataType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

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
    public List<AnalyticsTable> getAnalyticsTables( Date earliest )
    {
        List<AnalyticsTable> tables = new UniqueArrayList<>();
        List<Program> programs = idObjectManager.getAllNoAcl( Program.class );

        for ( Program program : programs )
        {
            AnalyticsTable table = new AnalyticsTable( getAnalyticsTableType(), getDimensionColumns( program ), Lists.newArrayList(), program );

            tables.add( table );
        }

        return tables;
    }

    @Override
    public Set<String> getExistingDatabaseTables()
    {
        return partitionManager.getAnalyticsPartitions( AnalyticsTableType.ENROLLMENT );
    }

    @Override
    protected List<String> getPartitionChecks( AnalyticsTablePartition partition )
    {
        return Lists.newArrayList();
    }

    @Override
    protected void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition )
    {
        final Program program = partition.getMasterTable().getProgram();
        final String tableName = partition.getTempTableName();

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

        sql += "from programinstance pi " +
            "inner join program pr on pi.programid=pr.programid " +
            "left join trackedentityinstance tei on pi.trackedentityinstanceid=tei.trackedentityinstanceid and tei.deleted is false " +
            "inner join organisationunit ou on pi.organisationunitid=ou.organisationunitid " +
            "left join _orgunitstructure ous on pi.organisationunitid=ous.organisationunitid " +
            "left join _organisationunitgroupsetstructure ougs on pi.organisationunitid=ougs.organisationunitid " +
                "and (cast(date_trunc('month', pi.enrollmentdate) as date)=ougs.startdate or ougs.startdate is null) " +
            "left join _dateperiodstructure dps on cast(pi.enrollmentdate as date)=dps.dateperiod " +
            "where pr.programid=" + program.getId() + " " +
            "and pi.organisationunitid is not null " +
            "and pi.lastupdated <= '" + getLongDateString( params.getStartTime() ) + "' " +
            "and pi.incidentdate is not null " +
            "and pi.deleted is false ";

        populateAndLog( sql, tableName );
    }

    private List<AnalyticsTableColumn> getDimensionColumns( Program program )
    {
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
            columns.add( new AnalyticsTableColumn( column, CHARACTER_11, "ous." + column ).withCreated( level.getCreated() ) );
        }

        for ( OrganisationUnitGroupSet groupSet : orgUnitGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), CHARACTER_11, "ougs." + quote( groupSet.getUid() ) ).withCreated( groupSet.getCreated() ) );
        }

        for ( PeriodType periodType : PeriodType.getAvailablePeriodTypes() )
        {
            String column = quote( periodType.getName().toLowerCase() );
            columns.add( new AnalyticsTableColumn( column, TEXT, "dps." + column ) );
        }

        for ( TrackedEntityAttribute attribute : program.getNonConfidentialTrackedEntityAttributes() )
        {
            ColumnDataType dataType = getColumnType( attribute.getValueType() );
            String dataClause = attribute.isNumericType() ? numericClause : attribute.isDateType() ? dateClause : "";
            String select = getSelectClause( attribute.getValueType(), "value" );
            boolean skipIndex = NO_INDEX_VAL_TYPES.contains( attribute.getValueType() ) && !attribute.hasOptionSet();

            String sql = "(select " + select + " from trackedentityattributevalue " +
                "where trackedentityinstanceid=pi.trackedentityinstanceid " +
                "and trackedentityattributeid=" + attribute.getId() + dataClause + ") as " + quote( attribute.getUid() );

            columns.add( new AnalyticsTableColumn( quote( attribute.getUid() ), dataType, sql ).withSkipIndex( skipIndex ) );
        }

        columns.add( new AnalyticsTableColumn( quote( "pi" ), CHARACTER_11, NOT_NULL, "pi.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "enrollmentdate" ), TIMESTAMP, "pi.enrollmentdate" ) );
        columns.add( new AnalyticsTableColumn( quote( "incidentdate" ), TIMESTAMP, "pi.incidentdate" ) );
        columns.add( new AnalyticsTableColumn( quote( "completeddate" ), TIMESTAMP, "case pi.status when 'COMPLETED' then pi.enddate end" ) );
        columns.add( new AnalyticsTableColumn( quote( "enrollmentstatus" ), CHARACTER_50, "pi.status" ) );
        columns.add( new AnalyticsTableColumn( quote( "longitude" ), DOUBLE, "ST_X(pi.geometry)" ) );
        columns.add( new AnalyticsTableColumn( quote( "latitude" ), DOUBLE, "ST_Y(pi.geometry)" ) );
        columns.add( new AnalyticsTableColumn( quote( "ou" ), CHARACTER_11, NOT_NULL, "ou.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "ouname" ), TEXT, NOT_NULL, "ou.name" ) );
        columns.add( new AnalyticsTableColumn( quote( "oucode" ), TEXT, "ou.code" ) );

        columns.add( new AnalyticsTableColumn( quote( "pigeometry" ), GEOMETRY, "pi.geometry" ).withIndexType( "gist" ) );

        if ( program.isRegistration() )
        {
            columns.add( new AnalyticsTableColumn( quote( "tei" ), CHARACTER_11, "tei.uid" ) );
        }

        return filterDimensionColumns( columns );
    }
}
