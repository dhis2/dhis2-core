package org.hisp.dhis.analytics.table;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.common.ValueType;

public abstract class AbstractEventJdbcTableManager
    extends AbstractJdbcTableManager
{
    @Override
    public Future<?> applyAggregationLevels( ConcurrentLinkedQueue<AnalyticsTable> tables,
        Collection<String> dataElements, int aggregationLevel )
    {
        return null; // Not relevant
    }

    @Override
    public Future<?> vacuumTablesAsync( ConcurrentLinkedQueue<AnalyticsTable> tables )
    {
        return null; // Not relevant
    }
    
    @Override
    public String validState()
    {
        boolean hasData = jdbcTemplate.queryForRowSet( "select dataelementid from trackedentitydatavalue limit 1" ).next();
        
        if ( !hasData )
        {
            return "No events exist, not updating event analytics tables";
        }
        
        return null;
    }
    
    /**
     * Returns the database column type based on the given value type. For boolean
     * values, 1 means true, 0 means false and null means no value.
     */
    protected String getColumnType( ValueType valueType )
    {
        if ( Double.class.equals( valueType.getJavaClass() ) || Integer.class.equals( valueType.getJavaClass() ) )
        {
            return statementBuilder.getDoubleColumnType();
        }
        else if ( Boolean.class.equals( valueType.getJavaClass() ) )
        {
            return "integer";
        }
        else if ( Date.class.equals( valueType.getJavaClass() ) )
        {
            return "timestamp";
        }
        else if ( ValueType.COORDINATE == valueType && databaseInfo.isSpatialSupport() )
        {
            return "geometry(Point, 4326)";
        }
        else
        {
            return "text";
        }
    }
    
    /**
     * Returns the select clause, potentially with a cast statement, based on the
     * given value type.
     */
    protected String getSelectClause( ValueType valueType )
    {
        if ( Double.class.equals( valueType.getJavaClass() ) || Integer.class.equals( valueType.getJavaClass() ) )
        {
            return "cast(value as " + statementBuilder.getDoubleColumnType() + ")";
        }
        else if ( Boolean.class.equals( valueType.getJavaClass() ) )
        {
            return "case when value = 'true' then 1 when value = 'false' then 0 else null end";
        }
        else if ( Date.class.equals( valueType.getJavaClass() ) )
        {
            return "cast(value as timestamp)";
        }
        else if ( ValueType.COORDINATE == valueType && databaseInfo.isSpatialSupport() )
        {
            return "ST_GeomFromGeoJSON('{\"type\":\"Point\", \"coordinates\":' || value || ', \"crs\":{\"type\":\"name\", \"properties\":{\"name\":\"EPSG:4326\"}}}')";
        }
        else
        {
            return "value";
        }
    }
}
