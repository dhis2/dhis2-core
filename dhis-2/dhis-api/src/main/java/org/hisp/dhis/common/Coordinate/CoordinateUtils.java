package org.hisp.dhis.common.Coordinate;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.organisationunit.CoordinatesTuple;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Henning Håkonsen
 */
public class CoordinateUtils
{
    private static final Pattern JSON_POINT_PATTERN = Pattern.compile( "(\\[.*?])" );

    private static final Pattern JSON_COORDINATE_PATTERN = Pattern.compile( "(\\[{3}.*?]{3})" );

    private static final Pattern COORDINATE_PATTERN = Pattern.compile( "([\\-0-9.]+,[\\-0-9.]+)" );

    public static boolean hasDescendantsWithCoordinates( Set<OrganisationUnit> organisationUnits )
    {
        return organisationUnits.stream().anyMatch( OrganisationUnit::hasCoordinates );
    }

    public static boolean isPolygon( FeatureType featureType )
    {
        return featureType != null && featureType.isPolygon();
    }

    public static boolean isPoint( FeatureType featureType )
    {
        return featureType != null && featureType == FeatureType.POINT;
    }

    public static List<CoordinatesTuple> getCoordinatesAsList( String coordinates, FeatureType featureType )
    {
        List<CoordinatesTuple> list = new ArrayList<>();

        if ( coordinates != null && !coordinates.trim().isEmpty() )
        {
            Matcher jsonMatcher = isPoint( featureType ) ?
                JSON_POINT_PATTERN.matcher( coordinates ) : JSON_COORDINATE_PATTERN.matcher( coordinates );

            while ( jsonMatcher.find() )
            {
                CoordinatesTuple tuple = new CoordinatesTuple();

                Matcher matcher = COORDINATE_PATTERN.matcher( jsonMatcher.group() );

                while ( matcher.find() )
                {
                    tuple.addCoordinates( matcher.group() );
                }

                list.add( tuple );
            }
        }

        return list;
    }

    public static String setMultiPolygonCoordinatesFromList( List<CoordinatesTuple> list )
    {
        StringBuilder builder = new StringBuilder();

        if ( CoordinatesTuple.hasCoordinates( list ) )
        {
            builder.append( "[" );

            for ( CoordinatesTuple tuple : list )
            {
                if ( tuple.hasCoordinates() )
                {
                    builder.append( "[[" );

                    for ( String coordinates : tuple.getCoordinatesTuple() )
                    {
                        builder.append( "[" ).append( coordinates ).append( "]," );
                    }

                    builder.deleteCharAt( builder.lastIndexOf( "," ) );
                    builder.append( "]]," );
                }
            }

            builder.deleteCharAt( builder.lastIndexOf( "," ) );
            builder.append( "]" );
        }

        return StringUtils.trimToNull( builder.toString() );
    }

    public static String setPointCoordinatesFromList( List<CoordinatesTuple> list )
    {
        StringBuilder builder = new StringBuilder();

        if ( list != null && list.size() > 0 )
        {
            for ( CoordinatesTuple tuple : list )
            {
                for ( String coordinates : tuple.getCoordinatesTuple() )
                {
                    builder.append( "[" ).append( coordinates ).append( "]" );
                }
            }
        }

        return StringUtils.trimToNull( builder.toString() );
    }
}
