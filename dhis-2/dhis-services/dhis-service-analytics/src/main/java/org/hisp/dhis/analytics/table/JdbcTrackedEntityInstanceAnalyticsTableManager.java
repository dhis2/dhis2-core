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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.INTEGER;
import static org.hisp.dhis.analytics.ColumnDataType.JSONB;
import static org.hisp.dhis.analytics.ColumnDataType.TIMESTAMP;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_1200;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_255;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@Service( "org.hisp.dhis.analytics.TrackedEntityInstanceAnalyticsTableManager" )
public class JdbcTrackedEntityInstanceAnalyticsTableManager extends AbstractJdbcTableManager
{
    private final TrackedEntityTypeService trackedEntityTypeService;

    private final ProgramService programService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    public JdbcTrackedEntityInstanceAnalyticsTableManager( IdentifiableObjectManager idObjectManager,
        OrganisationUnitService organisationUnitService,
        CategoryService categoryService, SystemSettingManager systemSettingManager,
        DataApprovalLevelService dataApprovalLevelService, ResourceTableService resourceTableService,
        AnalyticsTableHookService tableHookService, StatementBuilder statementBuilder,
        PartitionManager partitionManager, DatabaseInfo databaseInfo, JdbcTemplate jdbcTemplate,
        TrackedEntityTypeService trackedEntityTypeService, ProgramService programService,
        TrackedEntityAttributeService trackedEntityAttributeService )
    {
        super( idObjectManager, organisationUnitService, categoryService, systemSettingManager,
            dataApprovalLevelService, resourceTableService,
            tableHookService, statementBuilder, partitionManager, databaseInfo, jdbcTemplate );

        checkNotNull( trackedEntityAttributeService );
        this.trackedEntityAttributeService = trackedEntityAttributeService;

        checkNotNull( programService );
        this.programService = programService;

        checkNotNull( trackedEntityTypeService );
        this.trackedEntityTypeService = trackedEntityTypeService;
    }

    private static final List<AnalyticsTableColumn> FIXED_COLS = ImmutableList.of(
        new AnalyticsTableColumn( quote( "trackedentityinstanceid" ), INTEGER, NOT_NULL,
            "tei.trackedentityinstanceid" ),
        new AnalyticsTableColumn( quote( "programuid" ), CHARACTER_11, NOT_NULL, "p.uid" ),
        new AnalyticsTableColumn( quote( "programname" ), VARCHAR_255, NOT_NULL, "p.name" ),
        new AnalyticsTableColumn( quote( "trackedentityinstanceuid" ), CHARACTER_11, NOT_NULL, "tei.uid" ),
        new AnalyticsTableColumn( quote( "programstageuid" ), CHARACTER_11, NOT_NULL, "ps.uid" ),
        new AnalyticsTableColumn( quote( "programinstanceuid" ), CHARACTER_11, NOT_NULL, "pi.uid" ),
        new AnalyticsTableColumn( quote( "programstageinstanceuid" ), CHARACTER_11, NOT_NULL, "psi.uid" ),
        new AnalyticsTableColumn( quote( "enrollmentdate" ), TIMESTAMP, "pi.enrollmentdate" ),
        new AnalyticsTableColumn( quote( "enddate" ), TIMESTAMP, "pi.enddate" ),
        new AnalyticsTableColumn( quote( "incidentdate" ), TIMESTAMP, "pi.incidentdate" ),
        new AnalyticsTableColumn( quote( "executiondate" ), TIMESTAMP, "psi.executiondate" ),
        new AnalyticsTableColumn( quote( "duedate" ), TIMESTAMP, "psi.duedate" ),
        new AnalyticsTableColumn( quote( "eventdatavalues" ), JSONB, "psi.eventdatavalues" ) );

    /**
     * Returns the {@link AnalyticsTableType} of analytics table which this
     * manager handles.
     *
     * @return type of analytics table.
     */
    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.TRACKED_ENTITY_INSTANCE;
    }

    /**
     * Returns a {@link AnalyticsTable} with a list of yearly
     * {@link AnalyticsTablePartition}.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @return the analytics table with partitions.
     */
    @Override
    @Transactional
    public List<AnalyticsTable> getAnalyticsTables( AnalyticsTableUpdateParams params )
    {
        List<TrackedEntityType> trackedEntityTypes = trackedEntityTypeService.getAllTrackedEntityType();
        Map<String, List<Program>> programsByTetUid = programService.getAllPrograms().stream()
            .filter( program -> Objects.nonNull( program.getTrackedEntityType() ) )
            .collect( Collectors.groupingBy(
                o -> o.getTrackedEntityType().getUid() ) );

        return trackedEntityTypes
            .stream()
            .map( tet -> {
                List<AnalyticsTableColumn> columns = new ArrayList<>( getFixedColumns() );

                List<TrackedEntityAttribute> trackedEntityAttributes = new ArrayList<>();

                if ( programsByTetUid.containsKey( tet.getUid() ) )
                {
                    trackedEntityAttributes = getAllTrackedEntityAttributes( tet,
                        programsByTetUid.get( tet.getUid() ) );
                }

                columns.addAll( trackedEntityAttributes.stream()
                    .map( BaseIdentifiableObject::getUid )
                    .distinct()
                    .map( teaUid -> new AnalyticsTableColumn(
                        quote( teaUid ), VARCHAR_1200,
                        " (SELECT teavin.value from trackedentityattribute teain " +
                            " INNER JOIN trackedentityattributevalue teavin ON teain.trackedentityattributeid = teavin.trackedentityattributeid "
                            +
                            " WHERE tei.trackedentityinstanceid = teavin.trackedentityinstanceid AND teain.uid = '" +
                            teaUid + "')" ) )
                    .collect( Collectors.toList() ) );

                return new AnalyticsTable( getAnalyticsTableType(), columns,
                    Lists.newArrayList(), tet );
            } ).collect( Collectors.toList() );
    }

    private List<TrackedEntityAttribute> getAllTrackedEntityAttributes( TrackedEntityType trackedEntityType,
        List<Program> programs )
    {
        List<TrackedEntityAttribute> trackedEntityAttributes = new ArrayList<>();

        trackedEntityAttributes.addAll( trackedEntityAttributeService.getProgramTrackedEntityAttributes( programs ) );

        trackedEntityAttributes.addAll(
            Optional.of( trackedEntityType )
                .map( TrackedEntityType::getTrackedEntityAttributes )
                .orElse( Collections.emptyList() ) );

        return trackedEntityAttributes;
    }

    /**
     * Checks if the database content is in valid state for analytics table
     * generation.
     *
     * @return null if valid, a descriptive string if invalid.
     */
    @Override
    public String validState()
    {
        return null;
    }

    /**
     * Returns a list of non-dynamic {@link AnalyticsTableColumn}.
     *
     * @return a List of {@link AnalyticsTableColumn}.
     */
    @Override
    public List<AnalyticsTableColumn> getFixedColumns()
    {
        return FIXED_COLS;
    }

    /**
     * Returns a list of table checks (constraints) for the given analytics
     * table partition.
     *
     * @param partition the {@link AnalyticsTablePartition}.
     */
    @Override
    protected List<String> getPartitionChecks( AnalyticsTablePartition partition )
    {
        return Lists.newArrayList();
    }

    /**
     * Returns the partition column name for the analytics table type, or null
     * if the table type is not partitioned.
     */
    @Override
    protected String getPartitionColumn()
    {
        return null;
    }

    /**
     * Populates the given analytics table.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @param partition the {@link AnalyticsTablePartition} to populate.
     */
    @Override
    protected void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition )
    {
        List<AnalyticsTableColumn> columns = partition.getMasterTable().getDimensionColumns();

        List<AnalyticsTableColumn> values = partition.getMasterTable().getValueColumns();

        validateDimensionColumns( columns );

        String sql = "insert into " + partition.getTempTableName() + " (";

        for ( AnalyticsTableColumn col : ListUtils.union( columns, values ) )
        {
            sql += col.getName() + ",";
        }

        sql = TextUtils.removeLastComma( sql ) + ") SELECT DISTINCT ";

        for ( AnalyticsTableColumn col : columns )
        {
            sql += col.getAlias() + ",";
        }

        sql = TextUtils.removeLastComma( sql ) +
            " FROM programinstance pi " +
            " INNER JOIN program p ON pi.programid = p.programid " +
            " INNER JOIN programstageinstance psi ON psi.programinstanceid = pi.programinstanceid " +
            " INNER JOIN programstage ps ON ps.programid = p.programid AND psi.programstageid = ps.programstageid " +
            " INNER JOIN trackedentityinstance tei ON pi.trackedentityinstanceid = tei.trackedentityinstanceid ";

        invokeTimeAndLog( sql, partition.getTempTableName() );
    }

    /**
     * Indicates whether data was created or updated for the given time range
     * since last successful "latest" table partition update.
     *
     * @param startDate the start date.
     * @param endDate the end date.
     * @return true if updated data exists.
     */
    @Override
    protected boolean hasUpdatedLatestData( Date startDate, Date endDate )
    {
        return false;
    }
}
