/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.DATE;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.jdbc.batchhandler.MappingBatchHandler;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.quick.JdbcConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populates the analytics_ownership_[programuid] table which is joined for
 * tracker analytics queries to find the owning organisation unit at the start
 * or end of the query date range.
 *
 * @author Jim Grace
 */
@Slf4j
@Service( "org.hisp.dhis.analytics.OwnershipAnalyticsTableManager" )
public class JdbcOwnershipAnalyticsTableManager
    extends AbstractEventJdbcTableManager
{
    private final JdbcConfiguration jdbcConfiguration;

    public JdbcOwnershipAnalyticsTableManager( IdentifiableObjectManager idObjectManager,
        OrganisationUnitService organisationUnitService, CategoryService categoryService,
        SystemSettingManager systemSettingManager, DataApprovalLevelService dataApprovalLevelService,
        ResourceTableService resourceTableService, AnalyticsTableHookService tableHookService,
        StatementBuilder statementBuilder, PartitionManager partitionManager, DatabaseInfo databaseInfo,
        JdbcTemplate jdbcTemplate, JdbcConfiguration jdbcConfiguration )
    {
        super( idObjectManager, organisationUnitService, categoryService, systemSettingManager,
            dataApprovalLevelService, resourceTableService, tableHookService, statementBuilder, partitionManager,
            databaseInfo, jdbcTemplate );
        this.jdbcConfiguration = jdbcConfiguration;
    }

    private static final List<AnalyticsTableColumn> FIXED_COLS = List.of(
        new AnalyticsTableColumn( quote( "teiuid" ), CHARACTER_11, "tei.uid" ),
        new AnalyticsTableColumn( quote( "startdate" ), DATE, "o.owndate" ),
        new AnalyticsTableColumn( quote( "enddate" ), DATE, "null" ),
        new AnalyticsTableColumn( quote( "ou" ), CHARACTER_11, NOT_NULL, "ou.uid" ) );

    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.OWNERSHIP;
    }

    @Override
    @Transactional
    public List<AnalyticsTable> getAnalyticsTables( AnalyticsTableUpdateParams params )
    {
        return params.isLatestUpdate() ? emptyList() : getRegularAnalyticsTables();
    }

    /**
     * Creates a list of {@link AnalyticsTable} for each program.
     *
     * @return a list of {@link AnalyticsTableUpdateParams}.
     */
    private List<AnalyticsTable> getRegularAnalyticsTables()
    {
        return idObjectManager.getAllNoAcl( Program.class ).stream()
            .map( p -> new AnalyticsTable( getAnalyticsTableType(), getDimensionColumns(), emptyList(), p ) )
            .collect( toList() );
    }

    @Override
    protected List<String> getPartitionChecks( AnalyticsTablePartition partition )
    {
        return emptyList();
    }

    @Override
    protected void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition )
    {
        Program program = partition.getMasterTable().getProgram();

        if ( program.getProgramType() == WITHOUT_REGISTRATION )
        {
            return; // Builds an empty table, but it may be joined in queries.
        }

        String sql = getInputSql( params, program );

        log.debug( "Populate {} with SQL: '{}'", partition.getTempTableName(), sql );

        Timer timer = new SystemTimer().start();

        populateTableInternal( partition, sql );

        log.info( "Populate {} in: {}", partition.getTempTableName(), timer.stop().toString() );
    }

    private void populateTableInternal( AnalyticsTablePartition partition, String sql )
    {
        List<String> columnNames = getDimensionColumns().stream()
            .map( AnalyticsTableColumn::getName )
            .collect( toList() );

        MappingBatchHandler batchHandler = MappingBatchHandler.builder()
            .jdbcConfiguration( jdbcConfiguration )
            .tableName( partition.getTempTableName() )
            .columns( columnNames ).build();

        batchHandler.init();

        JdbcOwnershipWriter writer = JdbcOwnershipWriter.getInstance( batchHandler );

        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );

        int queryRowCount = 0;

        while ( rowSet.next() )
        {
            Map<String, Object> row = getRowMap( columnNames, rowSet );

            writer.write( row );

            queryRowCount++;
        }

        log.info( "OwnershipAnalytics query row count was {} for {}", queryRowCount, partition.getTempTableName() );

        writer.flush();
    }

    private String getInputSql( AnalyticsTableUpdateParams params, Program program )
    {
        // SELECT clause

        StringBuilder sb = new StringBuilder( "select " );

        for ( AnalyticsTableColumn col : getDimensionColumns() )
        {
            sb.append( col.getAlias() ).append( "," );
        }

        sb.deleteCharAt( sb.length() - 1 ); // Remove the final ','.

        // FROM clausetestImportEventInProgramStageSuccessWithWarningRaised

        // Gets no more than one row per ownership start day. If there are
        // multiple enrollments and/or owner changes, gets the last change
        // before midnight that starts the ownership day.

        return sb.append( " from (" +
            "select pi.trackedentityinstanceid, pi.enrollmentDate as owndate, pi.organisationunitid " +
            "from programinstance pi " +
            "where pi.programid=" + program.getId() + " " +
            "and pi.trackedentityinstanceid is not null " +
            "and pi.organisationunitid is not null " +
            "and pi.lastupdated <= '" + getLongDateString( params.getStartTime() ) + "' " +
            "union " +
            "select poh.trackedentityinstanceid, poh.startdate as owndate, poh.organisationunitid " +
            "from programownershiphistory poh " +
            "where poh.programid=" + program.getId() + " " +
            "and poh.trackedentityinstanceid is not null " +
            "and poh.organisationunitid is not null " +
            ") o " +
            "inner join trackedentityinstance tei on o.trackedentityinstanceid=tei.trackedentityinstanceid " +
            "and tei.deleted is false " +
            "inner join organisationunit ou on o.organisationunitid=ou.organisationunitid " +
            "left join _orgunitstructure ous on o.organisationunitid=ous.organisationunitid " +
            "left join _organisationunitgroupsetstructure ougs on o.organisationunitid=ougs.organisationunitid " +
            "order by tei.uid, o.owndate" ).toString();
    }

    private Map<String, Object> getRowMap( List<String> columnNames, SqlRowSet rowSet )
    {
        Map<String, Object> rowMap = new HashMap<>();

        for ( int i = 0; i < columnNames.size(); i++ )
        {
            rowMap.put( columnNames.get( i ), rowSet.getObject( i + 1 ) );
        }

        return rowMap;
    }

    /**
     * Returns dimensional analytics table columns.
     *
     * @return a list of {@link AnalyticsTableColumn}.
     */
    private List<AnalyticsTableColumn> getDimensionColumns()
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        columns.addAll( addOrganisationUnitLevels() );
        columns.addAll( addOrganisationUnitGroupSets() );

        columns.addAll( getFixedColumns() );

        return filterDimensionColumns( columns );
    }

    @Override
    public List<AnalyticsTableColumn> getFixedColumns()
    {
        return FIXED_COLS;
    }
}
