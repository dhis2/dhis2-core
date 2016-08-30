package org.hisp.dhis.query;

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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.query.operators.EqualOperator;
import org.hisp.dhis.query.operators.NullOperator;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class QueryParserTest
    extends DhisSpringTest
{
    @Autowired
    private QueryParser queryParser;

    @Test( expected = QueryParserException.class )
    public void failedFilters() throws QueryParserException
    {
        queryParser.parse( DataElement.class, Arrays.asList( "id", "name" ) );
    }

    @Test
    public void eqOperator() throws QueryParserException
    {
        Query query = queryParser.parse( DataElement.class, Arrays.asList( "id:eq:1", "id:eq:2" ) );
        assertEquals( 1, query.getCriterions().size() );
        assertTrue( Criteria.class.isInstance( query.getCriterions().get( 0 ) ) );
        Criteria criteria = (Criteria) query.getCriterions().get( 0 );
        assertEquals( 2, criteria.getCriterions().size() );

        Restriction restriction = (Restriction) criteria.getCriterions().get( 0 );
        assertEquals( "id", restriction.getPath() );
        assertEquals( "1", restriction.getOperator().getArgs().get( 0 ) );
        assertTrue( restriction.getOperator() instanceof EqualOperator );

        restriction = (Restriction) criteria.getCriterions().get( 1 );
        assertEquals( "id", restriction.getPath() );
        assertEquals( "2", restriction.getOperator().getArgs().get( 0 ) );
        assertTrue( restriction.getOperator() instanceof EqualOperator );
    }

    @Test
    public void eqOperatorDeepPath1() throws QueryParserException
    {
        Query query = queryParser.parse( DataElement.class, Arrays.asList( "dataElementGroups.id:eq:1", "dataElementGroups.id:eq:2" ) );
        assertEquals( 1, query.getCriterions().size() );
        assertTrue( Criteria.class.isInstance( query.getCriterions().get( 0 ) ) );
        Criteria criteria = (Criteria) query.getCriterions().get( 0 );
        assertEquals( 2, criteria.getCriterions().size() );

        Restriction restriction = (Restriction) criteria.getCriterions().get( 0 );
        assertEquals( "dataElementGroups.id", restriction.getPath() );
        assertEquals( "1", restriction.getOperator().getArgs().get( 0 ) );
        assertTrue( restriction.getOperator() instanceof EqualOperator );

        restriction = (Restriction) criteria.getCriterions().get( 1 );
        assertEquals( "dataElementGroups.id", restriction.getPath() );
        assertEquals( "2", restriction.getOperator().getArgs().get( 0 ) );
        assertTrue( restriction.getOperator() instanceof EqualOperator );
    }

    @Test( expected = QueryParserException.class )
    public void eqOperatorDeepPathFail() throws QueryParserException
    {
        queryParser.parse( DataElement.class, Arrays.asList( "dataElementGroups.id.name:eq:1", "dataElementGroups.id.abc:eq:2" ) );
    }

    @Test
    public void nullOperator() throws QueryParserException
    {
        Query query = queryParser.parse( DataElement.class, Arrays.asList( "id:null", "name:null" ) );
        assertEquals( 1, query.getCriterions().size() );
        assertTrue( Criteria.class.isInstance( query.getCriterions().get( 0 ) ) );
        Criteria criteria = (Criteria) query.getCriterions().get( 0 );
        assertEquals( 2, criteria.getCriterions().size() );

        Restriction restriction = (Restriction) criteria.getCriterions().get( 0 );
        assertEquals( "id", restriction.getPath() );
        assertTrue( restriction.getOperator() instanceof NullOperator );

        restriction = (Restriction) criteria.getCriterions().get( 1 );
        assertEquals( "name", restriction.getPath() );
        assertTrue( restriction.getOperator() instanceof NullOperator );
    }
}