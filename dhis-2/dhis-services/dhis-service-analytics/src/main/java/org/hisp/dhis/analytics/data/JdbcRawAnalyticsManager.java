package org.hisp.dhis.analytics.data;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.RawAnalyticsManager;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.google.common.collect.Lists;

public class JdbcRawAnalyticsManager
    implements RawAnalyticsManager
{
    private static final Log log = LogFactory.getLog( JdbcRawAnalyticsManager.class );
    
    @Resource( name = "readOnlyJdbcTemplate" )
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StatementBuilder statementBuilder;

    @Override
    public Grid getRawDataValues( DataQueryParams params, Grid grid )
    {        
        List<String> dimensionColumns = getDimensionColumns( params );
        
        log.info( "Dimension columns: " + dimensionColumns );
                
        String sql = getStatement( params, dimensionColumns );
        
        log.info( "Get raw data SQL: " + sql );
        
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );
        
        while ( rowSet.next() )
        {
            grid.addRow();
            
            for ( String col : dimensionColumns )
            {
                grid.addValue( rowSet.getString( col ) );
            }
            
            grid.addValue( rowSet.getDouble( "value" ) )
                .addValue( rowSet.getString( "textvalue" ) );
        }
        
        return grid;
    }
    
    private String getStatement( DataQueryParams params, List<String> dimensionColumns )
    {
        dimensionColumns = dimensionColumns.stream().map( c -> statementBuilder.columnQuote( c ) ).collect( Collectors.toList() );
        
        String sql = "select " + StringUtils.join( dimensionColumns, ", " ) + ", value, textvalue " +
            "from " + params.getPartitions().getSinglePartition();
        
        return sql;
    }
    
    //TODO pe, path
    
    private List<String> getDimensionColumns( DataQueryParams params )
    {   
        List<String> columns = Lists.newArrayList( "dx", "ou", "level" );
        
        columns.addAll( params.getDimensions().stream().map( d -> d.getDimension() ).collect( Collectors.toList() ) );
                
        return columns;
    }    
}
