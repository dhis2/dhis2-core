package org.hisp.dhis.query.operators;

/*
 * Copyright (c) 2004-2016, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.query.Type;
import org.hisp.dhis.query.Typed;
import org.hisp.dhis.schema.Property;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class LikeOperator extends Operator
{
    private final boolean caseSensitive;

    private final MatchMode matchMode;

    public LikeOperator( Object arg, boolean caseSensitive, org.hisp.dhis.query.operators.MatchMode matchMode )
    {
        super( Typed.from( String.class ), arg );
        this.caseSensitive = caseSensitive;
        this.matchMode = getMatchMode( matchMode );
    }

    @Override
    public Criterion getHibernateCriterion( Property property )
    {
        if ( caseSensitive )
        {
            return Restrictions.like( property.getFieldName(), String.valueOf( args.get( 0 ) ), matchMode );
        }
        else
        {
            return Restrictions.ilike( property.getFieldName(), String.valueOf( args.get( 0 ) ), matchMode );
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

            switch ( matchMode )
            {
                case EXACT:
                    return s2.equals( s1 );
                case START:
                    return s2.startsWith( s1 );
                case END:
                    return s2.endsWith( s1 );
                case ANYWHERE:
                    return s2.contains( s1 );
            }
        }

        return false;
    }

    private MatchMode getMatchMode( org.hisp.dhis.query.operators.MatchMode matchMode )
    {
        switch ( matchMode )
        {
            case EXACT:
                return MatchMode.EXACT;
            case START:
                return MatchMode.START;
            case END:
                return MatchMode.END;
            case ANYWHERE:
                return MatchMode.ANYWHERE;
        }

        return null;
    }
}
