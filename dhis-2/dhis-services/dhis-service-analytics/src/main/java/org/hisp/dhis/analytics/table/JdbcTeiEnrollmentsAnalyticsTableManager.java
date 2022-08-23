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
import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE_ENROLLMENTS;
import static org.hisp.dhis.analytics.ColumnDataType.BOOLEAN;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.DOUBLE;
import static org.hisp.dhis.analytics.ColumnDataType.GEOMETRY;
import static org.hisp.dhis.analytics.ColumnDataType.INTEGER;
import static org.hisp.dhis.analytics.ColumnDataType.JSONB;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.ColumnDataType.TIMESTAMP;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_255;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_50;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NULL;
import static org.hisp.dhis.analytics.IndexType.GIN;
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
import java.util.stream.Collectors;

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
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component( "org.hisp.dhis.analytics.TeiEnrollmentsAnalyticsTableManager" )
public class JdbcTeiEnrollmentsAnalyticsTableManager extends AbstractJdbcTableManager
{
    private final TrackedEntityTypeService trackedEntityTypeService;

    public JdbcTeiEnrollmentsAnalyticsTableManager( IdentifiableObjectManager idObjectManager,
        OrganisationUnitService organisationUnitService, CategoryService categoryService,
        SystemSettingManager systemSettingManager, DataApprovalLevelService dataApprovalLevelService,
        ResourceTableService resourceTableService, AnalyticsTableHookService tableHookService,
        StatementBuilder statementBuilder, PartitionManager partitionManager, DatabaseInfo databaseInfo,
        JdbcTemplate jdbcTemplate, TrackedEntityTypeService trackedEntityTypeService )
    {
        super( idObjectManager, organisationUnitService, categoryService, systemSettingManager,
            dataApprovalLevelService, resourceTableService, tableHookService, statementBuilder, partitionManager,
            databaseInfo, jdbcTemplate );

        notNull( trackedEntityTypeService, "trackedEntityTypeService cannot be null" );
        this.trackedEntityTypeService = trackedEntityTypeService;
    }

    private static final List<AnalyticsTableColumn> GROUP_BY_COLS = List.of(
        new AnalyticsTableColumn( quote( "trackedentityinstanceuid" ), CHARACTER_11, NOT_NULL, "tei.uid" ),
        new AnalyticsTableColumn( quote( "trackedentitytypeuid" ), CHARACTER_11, NOT_NULL, "tet.uid" ),
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
        new AnalyticsTableColumn( quote( "programuid" ), CHARACTER_11, NULL, "p.uid" ),
        new AnalyticsTableColumn( quote( "programinstanceuid" ), CHARACTER_11, NULL, "pi.uid" ),
        new AnalyticsTableColumn( quote( "enrollmentdate" ), TIMESTAMP, "pi.enrollmentdate" ),
        new AnalyticsTableColumn( quote( "enddate" ), TIMESTAMP, "pi.enddate" ),
        new AnalyticsTableColumn( quote( "incidentdate" ), TIMESTAMP, "pi.incidentdate" ),
        new AnalyticsTableColumn( quote( "enrollmentstatus" ), VARCHAR_50, "pi.status" ),
        new AnalyticsTableColumn( quote( "pigeometry" ), GEOMETRY, "pi.geometry" ).withIndexType( GIST ),
        new AnalyticsTableColumn( quote( "pilongitude" ), DOUBLE,
            "case when 'POINT' = GeometryType(pi.geometry) then ST_X(pi.geometry) else null end" ),
        new AnalyticsTableColumn( quote( "pilatitude" ), DOUBLE,
            "case when 'POINT' = GeometryType(pi.geometry) then ST_Y(pi.geometry) else null end" ),
        new AnalyticsTableColumn( quote( "uidlevel1" ), CHARACTER_11, NULL, "ous.uidlevel1" ),
        new AnalyticsTableColumn( quote( "uidlevel2" ), CHARACTER_11, NULL, "ous.uidlevel2" ),
        new AnalyticsTableColumn( quote( "uidlevel3" ), CHARACTER_11, NULL, "ous.uidlevel3" ),
        new AnalyticsTableColumn( quote( "uidlevel4" ), CHARACTER_11, NULL, "ous.uidlevel4" ),
        new AnalyticsTableColumn( quote( "ou" ), CHARACTER_11, NULL, "ou.uid" ),
        new AnalyticsTableColumn( quote( "ouname" ), VARCHAR_255, NULL, "ou.name" ),
        new AnalyticsTableColumn( quote( "oucode" ), CHARACTER_11, NULL, "ou.code" ),
        new AnalyticsTableColumn( quote( "oulevel" ), INTEGER, NULL, "ous.level" ) );

    private static final List<AnalyticsTableColumn> ADDITIONAL_GROUP_BY_COLS = List.of(
        new AnalyticsTableColumn( quote( "createdbyusername" ), JSONB, "tei.createdbyuserinfo" ),
        new AnalyticsTableColumn( quote( "lastupdatedbyuserinfo" ), JSONB, "tei.lastupdatedbyuserinfo" ) );

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
        return TRACKED_ENTITY_INSTANCE_ENROLLMENTS;
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
        return trackedEntityTypeService.getAllTrackedEntityType()
            .stream()
            .map( tet -> new AnalyticsTable( getAnalyticsTableType(), getTableColumns(), emptyList(), tet ) )
            .collect( Collectors.toList() );
    }

    private List<AnalyticsTableColumn> getTableColumns()
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>( getFixedColumns() );

        columns.addAll( addPeriodTypeColumns( "dps" ) );

        columns.add( new AnalyticsTableColumn( quote( "events" ), JSONB, NULL,
            " JSON_AGG( JSON_BUILD_OBJECT('programStage', ps.uid,'programStageInstanceUid', psi.uid,"
                + " 'executionDate', psi.executiondate, 'dueDate', psi.duedate,"
                + " 'eventDataValues', eventdatavalues))" ).withIndexType( GIN ) );

        return columns;
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

        removeLastComma( sql )
            .append( " from trackedentityinstance tei " )
            .append( " left join trackedentitytype tet on tet.trackedentitytypeid = tei.trackedentitytypeid" )
            .append( " left join programinstance pi on pi.trackedentityinstanceid = tei.trackedentityinstanceid" )
            .append( " and tei.deleted is false" )
            .append( " left join program p on p.programid = pi.programid and pi.deleted is false" )
            .append( " left join programstageinstance psi on psi.programinstanceid = pi.programinstanceid" )
            .append( " left join programstage ps on ps.programstageid = psi.programstageid" )
            .append( " left join organisationunit ou on pi.organisationunitid = ou.organisationunitid" )
            .append( " left join _orgunitstructure ous on ous.organisationunitid = ou.organisationunitid" )
            .append(
                " left join _organisationunitgroupsetstructure ougs on psi.organisationunitid=ougs.organisationunitid " )
            .append( " and (cast(date_trunc('month', pi.incidentdate) as date)" )
            .append( "=ougs.startdate or ougs.startdate is null) " )
            .append(
                " inner join _dateperiodstructure dps on cast(pi.incidentdate as date)=dps.dateperiod " )
            .append( " where tei.trackedentitytypeid = " + partition.getMasterTable().getTrackedEntityType().getId() )
            .append( " and tei.lastupdated < '" + getLongDateString( params.getStartTime() ) + "'" )
            .append( " and psi.status in (" + join( ",", EXPORTABLE_EVENT_STATUSES ) + ")" )
            .append( " and psi.deleted is false " )
            .append( " and pi.incidentdate is not null " )
            .append( " group by " )
            .append(
                addPeriodTypeColumns( "dps" ).stream().map( AnalyticsTableColumn::getAlias ).collect( joining( "," ) ) )
            .append( "," )
            .append( getGroupByCols().stream().map( AnalyticsTableColumn::getAlias ).collect( joining( "," ) ) );

        invokeTimeAndLog( sql.toString(), partition.getTempTableName() );
    }

    private List<AnalyticsTableColumn> getGroupByCols()
    {
        List<AnalyticsTableColumn> allGroupByCols = new ArrayList<>( GROUP_BY_COLS );
        allGroupByCols.addAll( ADDITIONAL_GROUP_BY_COLS );

        return allGroupByCols;
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
