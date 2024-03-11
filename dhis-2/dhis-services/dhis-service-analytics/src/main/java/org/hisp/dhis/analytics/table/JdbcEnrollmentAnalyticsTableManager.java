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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.hisp.dhis.system.util.DateUtils.getLongDateString;
import static org.hisp.dhis.system.util.MathUtils.NUMERIC_LENIENT_REGEXP;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;

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

        String baseName = getTableName();

        for ( Program program : programs )
        {
            AnalyticsTable table = new AnalyticsTable( baseName, getDimensionColumns( program ), Lists.newArrayList(), program );

            tables.add( table );
        }

        return tables;
    }

    @Override
    public Set<String> getExistingDatabaseTables()
    {
        return new HashSet<>();
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
            columns.add( new AnalyticsTableColumn( column, "text", "dps." + column ) );
        }

        for ( TrackedEntityAttribute attribute : program.getNonConfidentialTrackedEntityAttributes() )
        {
            String dataType = getColumnType( attribute.getValueType() );
            String dataClause = attribute.isNumericType() ? numericClause : attribute.isDateType() ? dateClause : "";
            String select = getSelectClause( attribute.getValueType() );
            boolean skipIndex = NO_INDEX_VAL_TYPES.contains( attribute.getValueType() ) && !attribute.hasOptionSet();

            String sql = "(select " + select + " from trackedentityattributevalue "
                + "where trackedentityinstanceid=pi.trackedentityinstanceid " + "and trackedentityattributeid="
                + attribute.getId() + dataClause + ") " + addClosingParentheses( select ) + " as "
                + quote( attribute.getUid() );

            columns.add( new AnalyticsTableColumn( quote( attribute.getUid() ), dataType, sql, skipIndex ) );
        }

        columns.add( new AnalyticsTableColumn( quote( "pi" ), "character(11) not null", "pi.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "enrollmentdate" ), "timestamp", "pi.enrollmentdate" ) );
        columns.add( new AnalyticsTableColumn( quote( "incidentdate" ), "timestamp", "pi.incidentdate" ) );
        columns.add( new AnalyticsTableColumn( quote( "completeddate" ), "timestamp", "case pi.status when 'COMPLETED' then pi.enddate end" ) );
        columns.add( new AnalyticsTableColumn( quote( "enrollmentstatus" ), "character(50)", "pi.status" ) );
        columns.add( new AnalyticsTableColumn( quote( "longitude" ), dbl, "ST_X(pi.geometry)" ) );
        columns.add( new AnalyticsTableColumn( quote( "latitude" ), dbl, "ST_Y(pi.geometry)" ) );
        columns.add( new AnalyticsTableColumn( quote( "ou" ), "character(11) not null", "ou.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "ouname" ), "text not null", "ou.name" ) );
        columns.add( new AnalyticsTableColumn( quote( "oucode" ), "text", "ou.code" ) );

        columns.add( new AnalyticsTableColumn( quote( "geom" ), "geometry", "pi.geometry", false, "gist" ) );

        if ( program.isRegistration() )
        {
            columns.add( new AnalyticsTableColumn( quote( "tei" ), "character(11)", "tei.uid" ) );
        }

        return filterDimensionColumns( columns );
    }
}
