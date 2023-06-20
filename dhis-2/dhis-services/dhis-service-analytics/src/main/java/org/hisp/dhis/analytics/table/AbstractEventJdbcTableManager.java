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

import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.getClosingParentheses;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;
import static org.hisp.dhis.system.util.MathUtils.NUMERIC_LENIENT_REGEXP;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.analytics.AnalyticsExportSettings;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.ColumnDataType;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Markus Bekken
 */
public abstract class AbstractEventJdbcTableManager
    extends AbstractJdbcTableManager
{
    public AbstractEventJdbcTableManager( IdentifiableObjectManager idObjectManager,
        OrganisationUnitService organisationUnitService, CategoryService categoryService,
        SystemSettingManager systemSettingManager, DataApprovalLevelService dataApprovalLevelService,
        ResourceTableService resourceTableService, AnalyticsTableHookService tableHookService,
        StatementBuilder statementBuilder, PartitionManager partitionManager, DatabaseInfo databaseInfo,
        JdbcTemplate jdbcTemplate, AnalyticsExportSettings analyticsExportSettings,
        PeriodDataProvider periodDataProvider )
    {
        super( idObjectManager, organisationUnitService, categoryService, systemSettingManager,
            dataApprovalLevelService, resourceTableService, tableHookService, statementBuilder, partitionManager,
            databaseInfo, jdbcTemplate, analyticsExportSettings, periodDataProvider );
    }

    protected final String getNumericClause()
    {
        return " and value " + statementBuilder.getRegexpMatch() + " '" + NUMERIC_LENIENT_REGEXP + "'";
    }

    protected final String getDateClause()
    {
        return " and value " + statementBuilder.getRegexpMatch() + " '" + DATE_REGEXP + "'";
    }

    protected final boolean skipIndex( ValueType valueType, boolean hasOptionSet )
    {
        return NO_INDEX_VAL_TYPES.contains( valueType ) && !hasOptionSet;
    }

    /**
     * Returns the select clause, potentially with a cast statement, based on
     * the given value type.
     *
     * @param valueType the value type to represent as database column type.
     */
    protected String getSelectClause( ValueType valueType, String columnName )
    {
        if ( valueType.isDecimal() )
        {
            return "cast(" + columnName + " as " + statementBuilder.getDoubleColumnType() + ")";
        }
        else if ( valueType.isInteger() )
        {
            return "cast(" + columnName + " as bigint)";
        }
        else if ( valueType.isBoolean() )
        {
            return "case when " + columnName + " = 'true' then 1 when " +
                columnName + " = 'false' then 0 else null end";
        }
        else if ( valueType.isDate() )
        {
            return "cast(" + columnName + " as timestamp)";
        }
        else if ( valueType.isGeo() && databaseInfo.isSpatialSupport() )
        {
            return "ST_GeomFromGeoJSON('{\"type\":\"Point\", \"coordinates\":' || (" +
                columnName + ") || ', \"crs\":{\"type\":\"name\", \"properties\":{\"name\":\"EPSG:4326\"}}}')";
        }
        else if ( valueType.isOrganisationUnit() )
        {
            return "ou.uid from organisationunit ou where ou.uid = (select " + columnName;
        }
        else
        {
            return columnName;
        }
    }

    @Override
    public String validState()
    {
        // Data values might be '{}' / empty object if data values existed
        // and were removed later

        String sql = "select programstageinstanceid " +
            "from event " +
            "where eventdatavalues != '{}' limit 1;";

        boolean hasData = jdbcTemplate.queryForRowSet( sql ).next();

        if ( !hasData )
        {
            return "No events exist, not updating event analytics tables";
        }

        return null;
    }

    @Override
    protected boolean hasUpdatedLatestData( Date startDate, Date endDate )
    {
        throw new IllegalStateException( "This method should never be invoked" );
    }

    /**
     * Populates the given analytics table partition using the given columns and
     * join statement.
     *
     * @param partition the {@link AnalyticsTablePartition}.
     * @param columns the list of {@link AnalyticsTableColumn}.
     * @param fromClause the SQL from clause.
     */
    protected void populateTableInternal( AnalyticsTablePartition partition, List<AnalyticsTableColumn> columns,
        String fromClause )
    {
        String tableName = partition.getTempTableName();

        String sql = "insert into " + partition.getTempTableName() + " (";

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

        sql += fromClause;

        invokeTimeAndLog( sql, String.format( "Populate %s", tableName ) );
    }

    protected List<AnalyticsTableColumn> addTrackedEntityAttributes( Program program )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        for ( TrackedEntityAttribute attribute : program.getNonConfidentialTrackedEntityAttributes() )
        {
            ColumnDataType dataType = getColumnType( attribute.getValueType(), databaseInfo.isSpatialSupport() );
            String dataClause = attribute.isNumericType() ? getNumericClause()
                : attribute.isDateType() ? getDateClause() : "";
            String select = getSelectClause( attribute.getValueType(), "value" );
            boolean skipIndex = skipIndex( attribute.getValueType(), attribute.hasOptionSet() );

            String sql = "(select " + select + " " +
                "from trackedentityattributevalue where trackedentityinstanceid=pi.trackedentityinstanceid " +
                "and trackedentityattributeid=" + attribute.getId() + dataClause + ")" +
                getClosingParentheses( select ) + " as " + quote( attribute.getUid() );

            columns.add( new AnalyticsTableColumn( quote( attribute.getUid() ), dataType, sql )
                .withSkipIndex( skipIndex ) );
        }

        return columns;
    }
}
