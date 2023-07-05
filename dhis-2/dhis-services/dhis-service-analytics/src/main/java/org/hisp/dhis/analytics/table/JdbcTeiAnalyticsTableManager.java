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

import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.analytics.ColumnDataType.BOOLEAN;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.DOUBLE;
import static org.hisp.dhis.analytics.ColumnDataType.GEOMETRY;
import static org.hisp.dhis.analytics.ColumnDataType.INTEGER;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.ColumnDataType.TIMESTAMP;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_1200;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_255;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_50;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NULL;
import static org.hisp.dhis.analytics.IndexType.GIST;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.EXPORTABLE_EVENT_STATUSES;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.DisplayNameUtils.getDisplayName;
import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;
import static org.hisp.dhis.util.DateUtils.getLongDateString;
import static org.springframework.util.Assert.notNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.analytics.AnalyticsExportSettings;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component( "org.hisp.dhis.analytics.TeiAnalyticsTableManager" )
public class JdbcTeiAnalyticsTableManager extends AbstractJdbcTableManager
{
    private static final String PROGRAMS_BY_TET_KEY = "programsByTetUid";

    private static final String ALL_TET_ATTRIBUTES = "allTetAttributes";

    private final TrackedEntityTypeService trackedEntityTypeService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    public JdbcTeiAnalyticsTableManager( IdentifiableObjectManager idObjectManager,
        OrganisationUnitService organisationUnitService, CategoryService categoryService,
        SystemSettingManager systemSettingManager, DataApprovalLevelService dataApprovalLevelService,
        ResourceTableService resourceTableService, AnalyticsTableHookService tableHookService,
        StatementBuilder statementBuilder, PartitionManager partitionManager, DatabaseInfo databaseInfo,
        JdbcTemplate jdbcTemplate, TrackedEntityTypeService trackedEntityTypeService,
        TrackedEntityAttributeService trackedEntityAttributeService, AnalyticsExportSettings settings,
        PeriodDataProvider periodDataProvider )
    {
        super( idObjectManager, organisationUnitService, categoryService, systemSettingManager,
            dataApprovalLevelService, resourceTableService, tableHookService, statementBuilder, partitionManager,
            databaseInfo, jdbcTemplate, settings, periodDataProvider );

        notNull( trackedEntityAttributeService, "trackedEntityAttributeService cannot be null" );
        this.trackedEntityAttributeService = trackedEntityAttributeService;

        notNull( trackedEntityTypeService, "trackedEntityTypeService cannot be null" );
        this.trackedEntityTypeService = trackedEntityTypeService;
    }

    private static final List<AnalyticsTableColumn> GROUP_BY_COLS = List.of(
        new AnalyticsTableColumn( quote( "trackedentityinstanceid" ), INTEGER, NOT_NULL,
            "tei.trackedentityinstanceid" ),
        new AnalyticsTableColumn( quote( "trackedentityinstanceuid" ), CHARACTER_11, NOT_NULL, "tei.uid" ),
        new AnalyticsTableColumn( quote( "created" ), TIMESTAMP, "tei.created" ),
        new AnalyticsTableColumn( quote( "lastupdated" ), TIMESTAMP, "tei.lastupdated" ),
        new AnalyticsTableColumn( quote( "inactive" ), BOOLEAN, "tei.inactive" ),
        new AnalyticsTableColumn( quote( "createdatclient" ), TIMESTAMP, "tei.createdatclient" ),
        new AnalyticsTableColumn( quote( "lastupdatedatclient" ), TIMESTAMP, "tei.lastupdatedatclient" ),
        new AnalyticsTableColumn( quote( "lastsynchronized" ), TIMESTAMP, "tei.lastsynchronized" ),
        new AnalyticsTableColumn( quote( "geometry" ), GEOMETRY, "tei.geometry" ).withIndexType( GIST ),
        new AnalyticsTableColumn( quote( "longitude" ), DOUBLE,
            "case when 'POINT' = GeometryType(tei.geometry) then ST_X(tei.geometry) else null end" ),
        new AnalyticsTableColumn( quote( "latitude" ), DOUBLE,
            "case when 'POINT' = GeometryType(tei.geometry) then ST_Y(tei.geometry) else null end" ),
        new AnalyticsTableColumn( quote( "featuretype" ), VARCHAR_255, NULL, "tei.featuretype" ),
        new AnalyticsTableColumn( quote( "coordinates" ), TEXT, NULL, "tei.coordinates" ),
        new AnalyticsTableColumn( quote( "storedby" ), VARCHAR_255, "tei.storedby" ),
        new AnalyticsTableColumn( quote( "potentialduplicate" ), BOOLEAN, NULL, "tei.potentialduplicate" ),
        new AnalyticsTableColumn( quote( "uidlevel1" ), CHARACTER_11, NULL, "ous.uidlevel1" ),
        new AnalyticsTableColumn( quote( "uidlevel2" ), CHARACTER_11, NULL, "ous.uidlevel2" ),
        new AnalyticsTableColumn( quote( "uidlevel3" ), CHARACTER_11, NULL, "ous.uidlevel3" ),
        new AnalyticsTableColumn( quote( "uidlevel4" ), CHARACTER_11, NULL, "ous.uidlevel4" ),
        new AnalyticsTableColumn( quote( "ou" ), CHARACTER_11, NULL, "ou.uid" ),
        new AnalyticsTableColumn( quote( "ouname" ), VARCHAR_255, NULL, "ou.name" ),
        new AnalyticsTableColumn( quote( "oucode" ), VARCHAR_50, NULL, "ou.code" ),
        new AnalyticsTableColumn( quote( "oulevel" ), INTEGER, NULL, "ous.level" ) );

    private static final List<AnalyticsTableColumn> NON_GROUP_BY_COLS = List.of(
        new AnalyticsTableColumn( quote( "createdbyusername" ), VARCHAR_255,
            "tei.createdbyuserinfo ->> 'username' as createdbyusername" ),
        new AnalyticsTableColumn( quote( "createdbyname" ), VARCHAR_255,
            "tei.createdbyuserinfo ->> 'firstName' as createdbyname" ),
        new AnalyticsTableColumn( quote( "createdbylastname" ), VARCHAR_255,
            "tei.createdbyuserinfo ->> 'surname' as createdbylastname" ),
        new AnalyticsTableColumn( quote( "createdbydisplayname" ), VARCHAR_255,
            getDisplayName( "createdbyuserinfo", "tei", "createdbydisplayname" ) ),
        new AnalyticsTableColumn( quote( "lastupdatedbyusername" ), VARCHAR_255,
            "tei.lastupdatedbyuserinfo ->> 'username' as lastupdatedbyusername" ),
        new AnalyticsTableColumn( quote( "lastupdatedbyname" ), VARCHAR_255,
            "tei.lastupdatedbyuserinfo ->> 'firstName' as lastupdatedbyname" ),
        new AnalyticsTableColumn( quote( "lastupdatedbylastname" ), VARCHAR_255,
            "tei.lastupdatedbyuserinfo ->> 'surname' as lastupdatedbylastname" ),
        new AnalyticsTableColumn( quote( "lastupdatedbydisplayname" ), VARCHAR_255,
            getDisplayName( "lastupdatedbyuserinfo", "tei", "lastupdatedbydisplayname" ) ) );

    /**
     * Returns the {@link AnalyticsTableType} of analytics table which this
     * manager handles.
     *
     * @return type of analytics table.
     */
    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return TRACKED_ENTITY_INSTANCE;
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
        Map<String, List<Program>> programsByTetUid = getProgramsByTetUid( params );

        params.addExtraParam( "", PROGRAMS_BY_TET_KEY, programsByTetUid );

        return trackedEntityTypeService.getAllTrackedEntityType()
            .stream()
            .map( tet -> new AnalyticsTable( getAnalyticsTableType(), getTableColumns( params, tet ),
                emptyList(), tet ) )
            .collect( toList() );
    }

    private Map<String, List<Program>> getProgramsByTetUid( AnalyticsTableUpdateParams params )
    {
        List<Program> programs = params.isSkipPrograms() ? idObjectManager.getAllNoAcl( Program.class )
            .stream()
            .filter( p -> !params.getSkipPrograms().contains( p.getUid() ) )
            .collect( toList() ) : idObjectManager.getAllNoAcl( Program.class );

        return programs.stream()
            .filter( program -> Objects.nonNull( program.getTrackedEntityType() ) )
            .collect( groupingBy(
                o -> o.getTrackedEntityType().getUid() ) );
    }

    @SuppressWarnings( "unchecked" )
    private List<AnalyticsTableColumn> getTableColumns( AnalyticsTableUpdateParams params,
        TrackedEntityType tet )
    {
        Map<String, List<Program>> programsByTetUid = (Map<String, List<Program>>) params.getExtraParam( "",
            PROGRAMS_BY_TET_KEY );

        List<AnalyticsTableColumn> columns = new ArrayList<>( getFixedColumns() );

        // Review this logic, it could result in many columns
        CollectionUtils.emptyIfNull( programsByTetUid.get( tet.getUid() ) )
            .forEach( program -> columns.add( new AnalyticsTableColumn( quote( program.getUid() ), BOOLEAN,
                " exists(select 1 from programinstance pi_0"
                    + " where pi_0.trackedentityinstanceid = tei.trackedentityinstanceid"
                    + " and pi_0.programid = " + program.getId() + ")" ) ) );

        List<TrackedEntityAttribute> trackedEntityAttributes = new ArrayList<>();

        if ( programsByTetUid.containsKey( tet.getUid() ) )
        {
            trackedEntityAttributes = getAllTrackedEntityAttributes( tet,
                programsByTetUid.get( tet.getUid() ) );
        }

        params.addExtraParam( tet.getUid(), ALL_TET_ATTRIBUTES, trackedEntityAttributes );

        columns.addAll( trackedEntityAttributes.stream()
            .map( tea -> new AnalyticsTableColumn( quote( tea.getUid() ), VARCHAR_1200,
                "\"" + tea.getUid() + "\".value" ) )
            .collect( toList() ) );

        return columns;
    }

    private List<TrackedEntityAttribute> getAllTrackedEntityAttributes( TrackedEntityType trackedEntityType,
        List<Program> programs )
    {
        return Stream.concat(
            /* all attributes of programs */
            trackedEntityAttributeService.getProgramTrackedEntityAttributes( programs ).stream(),
            /* all attributes of the trackedEntityType */
            CollectionUtils.emptyIfNull( trackedEntityType.getTrackedEntityAttributes() ).stream() )
            .distinct()
            .collect( toList() );
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
        List<AnalyticsTableColumn> allFixedColumns = new ArrayList<>( GROUP_BY_COLS );
        allFixedColumns.add( getOrganisationUnitNameHierarchyColumn() );
        allFixedColumns.addAll( NON_GROUP_BY_COLS );

        return allFixedColumns;
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
        return emptyList();
    }

    /**
     * Populates the given analytics table.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @param partition the {@link AnalyticsTablePartition} to populate.
     */
    @Override
    @SuppressWarnings( "unchecked" )
    protected void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition )
    {
        List<AnalyticsTableColumn> columns = partition.getMasterTable().getDimensionColumns();
        List<AnalyticsTableColumn> values = partition.getMasterTable().getValueColumns();

        validateDimensionColumns( columns );

        StringBuilder sql = new StringBuilder( "insert into " + partition.getTempTableName() + " (" );

        for ( AnalyticsTableColumn col : ListUtils.union( columns, values ) )
        {
            if ( col.isVirtual() )
            {
                continue;
            }

            sql.append( col.getName() + "," );
        }

        removeLastComma( sql ).append( ") select " );

        for ( AnalyticsTableColumn col : columns )
        {
            if ( col.isVirtual() )
            {
                continue;
            }

            sql.append( col.getAlias() + "," );
        }

        TrackedEntityType trackedEntityType = partition.getMasterTable().getTrackedEntityType();

        removeLastComma( sql )
            .append( " from trackedentityinstance tei" )
            .append( " left join organisationunit ou on tei.organisationunitid = ou.organisationunitid" )
            .append( " left join _orgunitstructure ous on ous.organisationunitid = ou.organisationunitid" );

        ((List<TrackedEntityAttribute>) params.getExtraParam( trackedEntityType.getUid(), ALL_TET_ATTRIBUTES ))
            .forEach(
                tea -> sql.append( " left join trackedentityattributevalue \"" + tea.getUid() + "\"" +
                    " on \"" + tea.getUid() + "\".trackedentityinstanceid = tei.trackedentityinstanceid" +
                    " and \"" + tea.getUid() + "\".trackedentityattributeid = " + tea.getId() ) );

        sql.append( " where tei.trackedentitytypeid = " + trackedEntityType.getId() )
            .append( " and tei.lastupdated < '" + getLongDateString( params.getStartTime() ) + "'" )
            .append(
                " and exists ( select 1 from programinstance pi" +
                    " where pi.trackedentityinstanceid = tei.trackedentityinstanceid" +
                    " and exists ( select 1 from event psi" +
                    " where psi.programinstanceid = pi.programinstanceid" +
                    " and psi.status in (" + join( ",", EXPORTABLE_EVENT_STATUSES ) + ")" +
                    " and psi.deleted is false  ) )" )
            .append( " and tei.created is not null " )
            .append( " and tei.deleted is false" );

        invokeTimeAndLog( sql.toString(), partition.getTempTableName() );
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
