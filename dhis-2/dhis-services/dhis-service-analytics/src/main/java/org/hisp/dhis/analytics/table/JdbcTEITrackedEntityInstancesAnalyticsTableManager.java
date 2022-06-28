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
import static com.google.common.collect.Lists.newArrayList;
import static org.hisp.dhis.analytics.ColumnDataType.BOOLEAN;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.INTEGER;
import static org.hisp.dhis.analytics.ColumnDataType.JSONB;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_1200;
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

import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.IndexType;
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

@Service( "org.hisp.dhis.analytics.TEITrackedEntityInstanceAnalyticsTableManager" )
public class JdbcTEITrackedEntityInstancesAnalyticsTableManager extends AbstractJdbcTableManager
{
    private final TrackedEntityTypeService trackedEntityTypeService;

    private final ProgramService programService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    public JdbcTEITrackedEntityInstancesAnalyticsTableManager( IdentifiableObjectManager idObjectManager,
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
        new AnalyticsTableColumn( quote( "trackedentityinstanceuid" ), CHARACTER_11, NOT_NULL, "tei.uid" ) );

    /**
     * Returns the {@link AnalyticsTableType} of analytics table which this
     * manager handles.
     *
     * @return type of analytics table.
     */
    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.TRACKED_ENTITY_INSTANCE_TEI;
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

                CollectionUtils.emptyIfNull( programsByTetUid.get( tet.getUid() ) )
                    .forEach( program -> columns.add(
                        new AnalyticsTableColumn(
                            quote( program.getUid() ), BOOLEAN,
                            " exists(\n" +
                                "        (select 1 from programinstance pi_0, program p_0\n" +
                                "                 where pi_0.trackedentityinstanceid = tei.trackedentityinstanceid\n" +
                                "                 and p_0.programid = pi_0.programid\n" +
                                "                 and p_0.uid='" + program.getUid() + "'))" ) ) );

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

                columns.add(
                    new AnalyticsTableColumn(
                        quote( "enrollments" ), JSONB,
                        "(SELECT JSON_AGG(\n" +
                            "                    JSON_BUILD_OBJECT(\n" +
                            "                            'programUid', p_0.uid,\n" +
                            "                            'programInstanceUid', pi_0.uid,\n" +
                            "                            'enrollmentDate', pi_0.enrollmentdate,\n" +
                            "                            'incidentDate', pi_0.incidentdate,\n" +
                            "                            'endDate', pi_0.enddate,\n" +
                            "                            'events', (SELECT JSON_AGG(\n" +
                            "                                                      JSON_BUILD_OBJECT(\n" +
                            "                                                              'programStageUid', ps_0.uid,\n"
                            +
                            "                                                              'programStageInstanceUid', psi_0.uid,\n"
                            +
                            "                                                              'executionDate', psi_0.executiondate,\n"
                            +
                            "                                                              'dueDate', psi_0.duedate,\n"
                            +
                            "                                                              'eventDataValues', psi_0.eventdatavalues)\n"
                            +
                            "                                                  )\n" +
                            "                                       FROM programstageinstance psi_0,\n" +
                            "                                            programstage ps_0\n" +
                            "                                       WHERE psi_0.programinstanceid = pi_0.programinstanceid\n"
                            +
                            "                                         AND ps_0.programstageid = psi_0.programstageid))\n"
                            +
                            "                )\n" +
                            "     FROM programinstance pi_0,\n" +
                            "          program p_0\n" +
                            "     WHERE pi_0.trackedentityinstanceid = tei.trackedentityinstanceid\n" +
                            "       AND p_0.programid = pi_0.programid)" )
                                .withIndexType( IndexType.GIN ) );

                return new AnalyticsTable( getAnalyticsTableType(), columns,
                    newArrayList(), tet );
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
        return newArrayList();
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
            if ( col.isVirtual() )
            {
                continue;
            }

            sql += col.getName() + ",";
        }

        sql = TextUtils.removeLastComma( sql ) + ") SELECT ";

        for ( AnalyticsTableColumn col : columns )
        {
            if ( col.isVirtual() )
            {
                continue;
            }

            sql += col.getAlias() + ",";
        }

        sql = TextUtils.removeLastComma( sql ) +
            " FROM trackedentityinstance tei " +
            " LEFT JOIN programinstance pi on pi.trackedentityinstanceid = tei.trackedentityinstanceid" +
            " LEFT JOIN program p on p.programid = pi.programid" +
            " LEFT JOIN programstageinstance psi on psi.programinstanceid = pi.programinstanceid" +
            " LEFT JOIN programstage ps on ps.programstageid = psi.programstageid" +
            " WHERE tei.trackedentitytypeid = " + partition.getMasterTable().getTrackedEntityType().getId() +
            " GROUP BY " +
            FIXED_COLS.stream()
                .map( AnalyticsTableColumn::getAlias )
                .filter( alias -> !"enrollments".equals( alias ) )
                .collect( Collectors.joining( "," ) );

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
