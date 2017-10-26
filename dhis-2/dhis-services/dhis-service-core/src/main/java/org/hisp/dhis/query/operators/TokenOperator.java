package org.hisp.dhis.query.operators;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.query.Type;
import org.hisp.dhis.query.Typed;
import org.hisp.dhis.query.planner.QueryPath;

import java.util.Arrays;
import java.util.List;

/**
 * Created by henninghakonsen on 24/10/2017.
 * Project: dhis-2.
 */
public class TokenOperator extends Operator{
    private final boolean caseSensitive;

    private final org.hibernate.criterion.MatchMode matchMode;

    public TokenOperator( Object arg, boolean caseSensitive, org.hisp.dhis.query.operators.MatchMode matchMode )
    {
        super( "token", Typed.from( String.class ), arg );
        this.caseSensitive = caseSensitive;
        this.matchMode = getMatchMode( matchMode );
    }

    public TokenOperator( String name, Object arg, boolean caseSensitive, org.hisp.dhis.query.operators.MatchMode matchMode )
    {
        super( name, Typed.from( String.class ), arg );
        this.caseSensitive = caseSensitive;
        this.matchMode = getMatchMode( matchMode );
    }

    @Override
    public Criterion getHibernateCriterion( QueryPath queryPath )
    {
        if ( caseSensitive )
        {
            return Restrictions.like( queryPath.getPath(), String.valueOf( args.get( 0 ) ).replace( "%", "\\%" ), matchMode );
        }
        else
        {
            return Restrictions.ilike( queryPath.getPath(), String.valueOf( args.get( 0 ) ).replace( "%", "\\%" ), matchMode );
        }
    }

    @Override
    public boolean test( Object value )
    {
        if ( args.isEmpty() || value == null )
        {
            return false;
        }

        Type type = new Type( value );

        if ( type.isString() )
        {
            String s1 = caseSensitive ? getValue( String.class ) : getValue( String.class ).toLowerCase();
            String s2 = caseSensitive ? (String) value : ((String) value).toLowerCase();

            List<String> s1_tokens = Arrays.asList( s1.replaceAll("[^a-zA-Z0-9]", " ").split("[\\s@&.?$+-]+") );
            List<String> s2_tokens = Arrays.asList( s2.replaceAll("[^a-zA-Z0-9]", " ").split("[\\s@&.?$+-]+") );

            if ( s1_tokens.size() == 1 )
            {
                return s2.contains( s1 );
            } else {
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
                        if ( found ) break;
                    }
                    if ( !found ) return false;
                }
            }
        }

        return true;
    }

    private org.hibernate.criterion.MatchMode getMatchMode(org.hisp.dhis.query.operators.MatchMode matchMode )
    {
        switch ( matchMode )
        {
            case EXACT:
                return org.hibernate.criterion.MatchMode.EXACT;
            case START:
                return org.hibernate.criterion.MatchMode.START;
            case END:
                return org.hibernate.criterion.MatchMode.END;
            case ANYWHERE:
                return org.hibernate.criterion.MatchMode.ANYWHERE;
        }

        return null;
    }
}
