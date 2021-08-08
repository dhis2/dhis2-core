/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.analytics.table;

import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_50;
import static org.hisp.dhis.analytics.ColumnDataType.DOUBLE;
import static org.hisp.dhis.analytics.ColumnDataType.GEOMETRY;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.ColumnDataType.TIMESTAMP;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.IndexType;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * @author Markus Bekken
 */
@Service( "org.hisp.dhis.analytics.EnrollmentAnalyticsTableManager" )
public class JdbcEnrollmentAnalyticsTableManager
    extends AbstractEventJdbcTableManager
{
    public JdbcEnrollmentAnalyticsTableManager( IdentifiableObjectManager idObjectManager,
        OrganisationUnitService organisationUnitService, CategoryService categoryService,
        SystemSettingManager systemSettingManager, DataApprovalLevelService dataApprovalLevelService,
        ResourceTableService resourceTableService, AnalyticsTableHookService tableHookService,
        StatementBuilder statementBuilder, PartitionManager partitionManager, DatabaseInfo databaseInfo,
        JdbcTemplate jdbcTemplate )
    {
        super( idObjectManager, organisationUnitService, categoryService, systemSettingManager,
            dataApprovalLevelService, resourceTableService, tableHookService, statementBuilder, partitionManager,
            databaseInfo, jdbcTemplate );
    }

    private static final List<AnalyticsTableColumn> FIXED_COLS = ImmutableList.of(
        new AnalyticsTableColumn( quote( "pi" ), CHARACTER_11, NOT_NULL, "pi.uid" ),
        new AnalyticsTableColumn( quote( "enrollmentdate" ), TIMESTAMP, "pi.enrollmentdate" ),
        new AnalyticsTableColumn( quote( "incidentdate" ), TIMESTAMP, "pi.incidentdate" ),
        new AnalyticsTableColumn( quote( "completeddate" ), TIMESTAMP,
            "case pi.status when 'COMPLETED' then pi.enddate end" ),
        new AnalyticsTableColumn( quote( "enrollmentstatus" ), CHARACTER_50, "pi.status" ),
        new AnalyticsTableColumn( quote( "longitude" ), DOUBLE,
            "CASE WHEN 'POINT' = GeometryType(pi.geometry) THEN ST_X(pi.geometry) ELSE null END" ),
        new AnalyticsTableColumn( quote( "latitude" ), DOUBLE,
            "CASE WHEN 'POINT' = GeometryType(pi.geometry) THEN ST_Y(pi.geometry) ELSE null END" ),
        new AnalyticsTableColumn( quote( "ou" ), CHARACTER_11, NOT_NULL, "ou.uid" ),
        new AnalyticsTableColumn( quote( "ouname" ), TEXT, NOT_NULL, "ou.name" ),
        new AnalyticsTableColumn( quote( "oucode" ), TEXT, "ou.code" ),
        new AnalyticsTableColumn( quote( "pigeometry" ), GEOMETRY, "pi.geometry" )
            .withIndexType( IndexType.GIST ) );

    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.ENROLLMENT;
    }

    @Override
    @Transactional
    public List<AnalyticsTable> getAnalyticsTables( AnalyticsTableUpdateParams params )
    {
        return params.isLatestUpdate() ? new ArrayList<>() : getRegularAnalyticsTables( params );
    }

    /**
     * Creates a list of {@link AnalyticsTable} for each program.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @return a list of {@link AnalyticsTableUpdateParams}.
     */
    private List<AnalyticsTable> getRegularAnalyticsTables( AnalyticsTableUpdateParams params )
    {
        List<AnalyticsTable> tables = new UniqueArrayList<>();

        List<Program> programs = idObjectManager.getAllNoAcl( Program.class );

        for ( Program program : programs )
        {
            AnalyticsTable table = new AnalyticsTable( getAnalyticsTableType(), getDimensionColumns( program ),
                Lists.newArrayList(), program );

            tables.add( table );
        }

        return tables;
    }

    @Override
    protected List<String> getPartitionChecks( AnalyticsTablePartition partition )
    {
        return Lists.newArrayList();
    }

    @Override
    protected String getPartitionColumn()
    {
        return null;
    }

    @Override
    protected void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition )
    {
        final Program program = partition.getMasterTable().getProgram();

        String fromClause = "from programinstance pi " +
            "inner join program pr on pi.programid=pr.programid " +
            "left join trackedentityinstance tei on pi.trackedentityinstanceid=tei.trackedentityinstanceid " +
            "and tei.deleted is false " +
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

        populateTableInternal( partition, getDimensionColumns( program ), fromClause );
    }

    private List<AnalyticsTableColumn> getDimensionColumns( Program program )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        columns.addAll( addOrganisationUnitLevels() );
        columns.addAll( addOrganisationUnitGroupSets() );
        columns.addAll( addPeriodTypeColumns( "dps" ) );
        columns.addAll( addTrackedEntityAttributes( program ) );
        columns.addAll( getFixedColumns() );

        if ( program.isRegistration() )
        {
            columns.add( new AnalyticsTableColumn( quote( "tei" ), CHARACTER_11, "tei.uid" ) );
        }

        return filterDimensionColumns( columns );
    }

    @Override
    public List<AnalyticsTableColumn> getFixedColumns()
    {
        return FIXED_COLS;
    }
}
