package org.hisp.dhis.analytics.data;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.Lists;

public class JdbcRawDataAnalyticsManager
{
    @Resource( name = "readOnlyJdbcTemplate" )
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StatementBuilder statementBuilder;

    private String getSelectClause( DataQueryParams params )
    {
        return null;
    }
    
    private List<String> getColumns( DataQueryParams params )
    {
        //TODO pe, path
        
        List<String> columns = Lists.newArrayList( "dx", "ou", "level", "value", "textvalue" );
        
        List<String> dimensionCols = params.getDimensions().stream().map( d -> d.getDimension() ).collect( Collectors.toList() );
        
        return columns;
    }
    
}
