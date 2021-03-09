package org.hisp.dhis;



import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TestRunStorage
{
    private static LinkedHashMap<String, String> createdEntities;

    public static void addCreatedEntity( final String resource, final String id )
    {
        if ( createdEntities == null )
        {
            createdEntities = new LinkedHashMap<>();
        }

        createdEntities.put( id, resource );
    }

    public static Map<String, String> getCreatedEntities()
    {
        if ( createdEntities == null )
        {
            return new LinkedHashMap<>();
        }

        return new LinkedHashMap<>( createdEntities );
    }

    public static List<String> getCreatedEntities( String resource )
    {
        if ( createdEntities == null )
        {
            return new ArrayList<>();
        }

        return getCreatedEntities()
            .entrySet().stream()
            .filter( entrySet -> resource.equals( entrySet.getValue() ) )
            .map( entry -> entry.getKey() )
            .collect( toList() );
    }

    public static void removeEntity( final String resource, final String id )
    {
        if ( createdEntities == null )
        {
            return;
        }

        createdEntities.remove( id, resource );
    }

    public static void removeAllEntities()
    {
        if ( createdEntities == null )
        {
            return;
        }

        createdEntities.clear();
    }
}
