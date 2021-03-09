package org.hisp.dhis.helpers;



import org.apache.commons.lang3.tuple.MutablePair;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class QueryParamsBuilder
{
    List<MutablePair<String, String>> queryParams;

    public QueryParamsBuilder()
    {
        queryParams = new ArrayList<>();
    }

    /**
     * Adds or updates the query param.
     * Format: key=value
     *
     * @param param
     * @return
     */
    public QueryParamsBuilder add( String param )
    {
        String[] split = param.split( "=" );
        MutablePair pair = getByKey( split[0] );

        if ( pair != null && !pair.getKey().equals( "filter") )
        {
            pair.setRight( split[1] );
            return this;
        }

        queryParams.add( MutablePair.of( split[0], split[1] ) );

        return this;
    }

    public QueryParamsBuilder addAll( String... params )
    {
        for ( String param : params )
        {
            this.add( param );
        }

        return this;
    }

    private MutablePair getByKey( String key )
    {
        return queryParams.stream()
            .filter( p -> p.getLeft().equals( key ) )
            .findFirst()
            .orElse( null );
    }

    public String build()
    {
        if ( queryParams.size() == 0 )
        {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append( "?" );

        for ( int i = 0; i < queryParams.size(); i++ )
        {
            builder.append( String.format( "%s=%s", queryParams.get( i ).getLeft(), queryParams.get( i ).getRight() ) );

            if ( i != queryParams.size() - 1 )
            {
                builder.append( "&" );
            }
        }

        return builder.toString();
    }
}
