package org.hisp.dhis.query.operators;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.query.Typed;
import org.hisp.dhis.query.planner.QueryPath;

/**
 * @author Henning Håkonsen
 */
public class NotTokenOperator
    extends Operator
{
    private final boolean caseSensitive;

    private final org.hibernate.criterion.MatchMode matchMode;

    public NotTokenOperator( Object arg, boolean caseSensitive, org.hisp.dhis.query.operators.MatchMode matchMode )
    {
        super( "!token", Typed.from( String.class ), arg );
        this.caseSensitive = caseSensitive;
        this.matchMode = getMatchMode( matchMode );
    }

    @Override
    public Criterion getHibernateCriterion( QueryPath queryPath )
    {
        String value = caseSensitive ? getValue( String.class ) : getValue( String.class ).toLowerCase();

        StringBuilder regex = new StringBuilder();
        for ( String token : TokenUtils.getTokens( value ) )
        {
            regex.append( "(?=.*" ).append( token ).append( ")" );
        }
        return Restrictions.sqlRestriction( "c_." + queryPath.getPath() + " !~* '" + regex + "' " );
    }

    @Override
    public boolean test( Object value )
    {
        String targetValue = caseSensitive ? getValue( String.class ) : getValue( String.class ).toLowerCase();
        return !TokenUtils.test( args, value, targetValue, caseSensitive, matchMode );
    }
}