package org.hisp.dhis.analytics.data;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.RawAnalyticsManager;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.google.common.collect.Sets;

public class JdbcRawAnalyticsManager
    implements RawAnalyticsManager
{
    private static final Log log = LogFactory.getLog( JdbcRawAnalyticsManager.class );
    
    private static final Set<String> DIMS_IGNORE_CRITERIA = Sets.newHashSet( DimensionalObject.PERIOD_DIM_ID );
    private static final String DIM_NAME_OU = "ou.path";
    
    @Resource( name = "readOnlyJdbcTemplate" )
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StatementBuilder statementBuilder;

    @Override
    public Grid getRawDataValues( DataQueryParams params, Grid grid )
    {        
        List<DimensionalObject> dimensions = params.getDimensions();
        
        String sql = getStatement( params );
        
        log.info( "Get raw data SQL: " + sql );
        
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet( sql );
        
        while ( rowSet.next() )
        {
            grid.addRow();
            
            for ( DimensionalObject dim : dimensions )
            {
                grid.addValue( rowSet.getString( dim.getDimensionName() ) );
            }
            
            grid.addValue( rowSet.getDouble( "value" ) )
                .addValue( rowSet.getString( "textvalue" ) );
        }
        
        return grid;
    }
    
    private String getStatement( DataQueryParams params )
    {
        List<String> dimensionColumns = params.getDimensions()
            .stream().map( d -> statementBuilder.columnQuote( d.getDimensionName() ) )
            .collect( Collectors.toList() );
        
        SqlHelper sqlHelper = new SqlHelper();
        
        String sql = "select " + StringUtils.join( dimensionColumns, ", " ) + ", " + DIM_NAME_OU + ", value, textvalue ";
        
        sql += 
            "from " + params.getPartitions().getSinglePartition() + " ax " +
            "inner join organisationunit ou on ax.ou = ou.uid ";
        
        for ( DimensionalObject dim : params.getDimensions() )
        {
            if ( !dim.getItems().isEmpty() && !dim.isFixed() && !DIMS_IGNORE_CRITERIA.contains( dim.getDimension() ) )
            {
                String col = statementBuilder.columnQuote( dim.getDimensionName() );

                if ( DimensionalObject.ORGUNIT_DIM_ID.equals( dim.getDimension() ) )
                {
                    sql += sqlHelper.whereAnd() + " (";
                    
                    for ( DimensionalItemObject item : dim.getItems() )
                    {
                        OrganisationUnit unit = (OrganisationUnit) item;
                        
                        sql += DIM_NAME_OU + " like '" + unit.getPath() + "%' or ";
                    }
                    
                    sql = TextUtils.removeLastOr( sql ) + ") ";
                }
                else
                {
                    sql += sqlHelper.whereAnd() + " " + col + " in (" + getQuotedCommaDelimitedString( getUids( dim.getItems() ) ) + ") ";
                }                
            }
        }
        
        return sql;
    }
}
