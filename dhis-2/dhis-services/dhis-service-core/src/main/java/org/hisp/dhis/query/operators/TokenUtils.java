package org.hisp.dhis.query.operators;

import org.hibernate.criterion.MatchMode;
import org.hisp.dhis.query.Type;

import java.util.Arrays;
import java.util.List;

/**
 * @author Henning Håkonsen
 */
public class TokenUtils
{
    public static List<String> getTokens( String value )
    {
        return Arrays.asList( value.replaceAll( "[^a-zA-Z0-9]", " " ).split( "[\\s@&.?$+-]+" ) );
    }

    public static boolean test( List<Object> args, Object testValue, String targetValue, boolean caseSensitive, MatchMode matchMode )
    {
        if ( args.isEmpty() || testValue == null )
        {
            return false;
        }

        Type type = new Type( testValue );

        if ( type.isString() )
        {
            String s2 = caseSensitive ? (String) testValue : ((String) testValue).toLowerCase();

            List<String> s1_tokens = getTokens( targetValue );
            List<String> s2_tokens = Arrays.asList( s2.replaceAll( "[^a-zA-Z0-9]", " " ).split( "[\\s@&.?$+-]+" ) );

            if ( s1_tokens.size() == 1 )
            {
                return s2.contains( targetValue );
            }
            else
            {
                for ( String s : s1_tokens )
                {
                    boolean found = false;
                    for ( String s3 : s2_tokens )
                    {
                        switch ( matchMode )
                        {
                        case EXACT:
                            found = s3.equals( s );
                            break;
                        case START:
                            found = s3.startsWith( s );
                            break;
                        case END:
                            found = s3.endsWith( s );
                            break;
                        case ANYWHERE:
                            found = s3.contains( s );
                        }
                        if ( found )
                            break;
                    }
                    if ( !found )
                        return false;
                }
            }
        }

        return true;
    }
}
