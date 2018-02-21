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

import com.google.common.collect.Lists;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.jfree.data.time.Year;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class QueryServiceTest
    extends DhisSpringTest
{
    @Autowired
    private SchemaService schemaService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    @Before
    public void createDataElements()
    {
        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setValueType( ValueType.NUMBER );

        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setValueType( ValueType.BOOLEAN );

        DataElement dataElementC = createDataElement( 'C' );
        dataElementC.setValueType( ValueType.INTEGER );

        DataElement dataElementD = createDataElement( 'D' );
        dataElementD.setValueType( ValueType.NUMBER );

        DataElement dataElementE = createDataElement( 'E' );
        dataElementE.setValueType( ValueType.BOOLEAN );

        DataElement dataElementF = createDataElement( 'F' );
        dataElementF.setValueType( ValueType.INTEGER );

        dataElementA.setCreated( Year.parseYear( "2001" ).getStart() );
        dataElementB.setCreated( Year.parseYear( "2002" ).getStart() );
        dataElementC.setCreated( Year.parseYear( "2003" ).getStart() );
        dataElementD.setCreated( Year.parseYear( "2004" ).getStart() );
        dataElementE.setCreated( Year.parseYear( "2005" ).getStart() );
        dataElementF.setCreated( Year.parseYear( "2006" ).getStart() );

        identifiableObjectManager.save( dataElementB );
        identifiableObjectManager.save( dataElementE );
        identifiableObjectManager.save( dataElementA );
        identifiableObjectManager.save( dataElementC );
        identifiableObjectManager.save( dataElementF );
        identifiableObjectManager.save( dataElementD );

        DataElementGroup dataElementGroupA = createDataElementGroup( 'A' );
        dataElementGroupA.getMembers().addAll( Lists.newArrayList( dataElementA, dataElementB, dataElementC ) );

        DataElementGroup dataElementGroupB = createDataElementGroup( 'B' );
        dataElementGroupB.getMembers().addAll( Lists.newArrayList( dataElementD, dataElementE, dataElementF ) );

        identifiableObjectManager.save( dataElementGroupA );
        identifiableObjectManager.save( dataElementGroupB );
    }

    @Test
    public void getAllQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        assertEquals( 6, queryService.query( query ).size() );
    }

    @Test
    public void getAllQueryUrl() throws QueryParserException
    {
        Query query = queryService.getQueryFromUrl( DataElement.class, Lists.<String>newArrayList(), Lists.<Order>newArrayList() );
        assertEquals( 6, queryService.query( query ).size() );
    }

    @Test
    public void getMinMaxQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.setFirstResult( 2 );
        query.setMaxResults( 10 );

        assertEquals( 4, queryService.query( query ).size() );

        query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.setFirstResult( 2 );
        query.setMaxResults( 2 );

        assertEquals( 2, queryService.query( query ).size() );
    }

    @Test
    public void getEqQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.eq( "id", "deabcdefghA" ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 1, objects.size() );
        assertEquals( "deabcdefghA", objects.get( 0 ).getUid() );
    }

    @Test
    public void getEqQueryUrl() throws QueryParserException
    {
        Query query = queryService.getQueryFromUrl( DataElement.class, Lists.newArrayList( "id:eq:deabcdefghA" ), Lists.<Order>newArrayList() );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 1, objects.size() );
        assertEquals( "deabcdefghA", objects.get( 0 ).getUid() );
    }

    @Test
    public void getNeQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.ne( "id", "deabcdefghA" ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 5, objects.size() );

        assertFalse( collectionContainsUid( objects, "deabcdefghA" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghE" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    public void getNeQueryUrl() throws QueryParserException
    {
        Query query = queryService.getQueryFromUrl( DataElement.class, Lists.newArrayList( "id:ne:deabcdefghA" ), Lists.<Order>newArrayList() );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 5, objects.size() );

        assertFalse( collectionContainsUid( objects, "deabcdefghA" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghE" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    public void getLikeQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.like( "name", "F", MatchMode.ANYWHERE ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 1, objects.size() );
        assertEquals( "deabcdefghF", objects.get( 0 ).getUid() );
    }

    @Test
    public void getLikeQueryUrl() throws QueryParserException
    {
        Query query = queryService.getQueryFromUrl( DataElement.class, Lists.newArrayList( "name:like:F" ), Lists.<Order>newArrayList() );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 1, objects.size() );
        assertEquals( "deabcdefghF", objects.get( 0 ).getUid() );
    }

    @Test
    public void getGtQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.gt( "created", Year.parseYear( "2003" ).getStart() ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 3, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghE" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    public void getGtQueryUrl() throws QueryParserException
    {
        Query query = queryService.getQueryFromUrl( DataElement.class, Lists.newArrayList( "created:gt:2003" ), Lists.<Order>newArrayList() );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 3, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghE" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    public void getLtQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.lt( "created", Year.parseYear( "2003" ).getStart() ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 2, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghA" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
    }

    @Test
    public void getLtQueryUrl() throws QueryParserException
    {
        Query query = queryService.getQueryFromUrl( DataElement.class, Lists.newArrayList( "created:lt:2003" ), Lists.<Order>newArrayList() );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 2, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghA" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
    }

    @Test
    public void getGeQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.ge( "created", Year.parseYear( "2003" ).getStart() ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 4, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghE" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    public void getGeQueryUrl() throws QueryParserException
    {
        Query query = queryService.getQueryFromUrl( DataElement.class, Lists.newArrayList( "created:ge:2003" ), Lists.<Order>newArrayList() );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 4, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghE" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    public void getLeQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.le( "created", Year.parseYear( "2003" ).getStart() ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 3, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghA" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
    }

    @Test
    public void getLeQueryUrl() throws QueryParserException
    {
        Query query = queryService.getQueryFromUrl( DataElement.class, Lists.newArrayList( "created:le:2003" ), Lists.<Order>newArrayList() );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 3, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghA" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
    }

    @Test
    public void getBetweenQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.between( "created", Year.parseYear( "2003" ).getStart(), Year.parseYear( "2005" ).getStart() ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 3, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghE" ) );
    }

    @Test
    public void getInQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.in( "id", Lists.newArrayList( "deabcdefghD", "deabcdefghF" ) ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 2, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    @SuppressWarnings( "rawtypes" )
    public void resultTransformerTest()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );

        List<? extends IdentifiableObject> objects = queryService.query( query, result1 -> new ArrayList() );
        assertEquals( 0, objects.size() );

        objects = queryService.query( query, result1 -> result1 );
        assertEquals( 6, objects.size() );
    }

    @Test
    public void sortNameDesc()
    {
        Schema schema = schemaService.getDynamicSchema( DataElement.class );

        Query query = Query.from( schema );
        query.addOrder( new Order( schema.getProperty( "name" ), Direction.DESCENDING ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 6, objects.size() );

        assertEquals( "deabcdefghF", objects.get( 0 ).getUid() );
        assertEquals( "deabcdefghE", objects.get( 1 ).getUid() );
        assertEquals( "deabcdefghD", objects.get( 2 ).getUid() );
        assertEquals( "deabcdefghC", objects.get( 3 ).getUid() );
        assertEquals( "deabcdefghB", objects.get( 4 ).getUid() );
        assertEquals( "deabcdefghA", objects.get( 5 ).getUid() );
    }

    @Test
    public void sortNameAsc()
    {
        Schema schema = schemaService.getDynamicSchema( DataElement.class );

        Query query = Query.from( schema );
        query.addOrder( new Order( schema.getProperty( "name" ), Direction.ASCENDING ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 6, objects.size() );

        assertEquals( "deabcdefghA", objects.get( 0 ).getUid() );
        assertEquals( "deabcdefghB", objects.get( 1 ).getUid() );
        assertEquals( "deabcdefghC", objects.get( 2 ).getUid() );
        assertEquals( "deabcdefghD", objects.get( 3 ).getUid() );
        assertEquals( "deabcdefghE", objects.get( 4 ).getUid() );
        assertEquals( "deabcdefghF", objects.get( 5 ).getUid() );
    }

    @Test
    public void sortCreatedDesc()
    {
        Schema schema = schemaService.getDynamicSchema( DataElement.class );

        Query query = Query.from( schema );
        query.addOrder( new Order( schema.getProperty( "created" ), Direction.DESCENDING ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 6, objects.size() );

        assertEquals( "deabcdefghF", objects.get( 0 ).getUid() );
        assertEquals( "deabcdefghE", objects.get( 1 ).getUid() );
        assertEquals( "deabcdefghD", objects.get( 2 ).getUid() );
        assertEquals( "deabcdefghC", objects.get( 3 ).getUid() );
        assertEquals( "deabcdefghB", objects.get( 4 ).getUid() );
        assertEquals( "deabcdefghA", objects.get( 5 ).getUid() );
    }

    @Test
    public void sortCreatedAsc()
    {
        Schema schema = schemaService.getDynamicSchema( DataElement.class );

        Query query = Query.from( schema );
        query.addOrder( new Order( schema.getProperty( "created" ), Direction.ASCENDING ) );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 6, objects.size() );

        assertEquals( "deabcdefghA", objects.get( 0 ).getUid() );
        assertEquals( "deabcdefghB", objects.get( 1 ).getUid() );
        assertEquals( "deabcdefghC", objects.get( 2 ).getUid() );
        assertEquals( "deabcdefghD", objects.get( 3 ).getUid() );
        assertEquals( "deabcdefghE", objects.get( 4 ).getUid() );
        assertEquals( "deabcdefghF", objects.get( 5 ).getUid() );
    }

    @Test
    public void testDoubleEqConjunction()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );

        Conjunction conjunction = query.conjunction();
        conjunction.add( Restrictions.eq( "id", "deabcdefghD" ) );
        conjunction.add( Restrictions.eq( "id", "deabcdefghF" ) );
        query.add( conjunction );

        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 0, objects.size() );
    }

    @Test
    public void testDoubleEqDisjunction()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );

        Disjunction disjunction = query.disjunction();
        disjunction.add( Restrictions.eq( "id", "deabcdefghD" ) );
        disjunction.add( Restrictions.eq( "id", "deabcdefghF" ) );
        query.add( disjunction );

        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 2, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    public void testDateRange()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );

        query.add( Restrictions.ge( "created", Year.parseYear( "2002" ).getStart() ) );
        query.add( Restrictions.le( "created", Year.parseYear( "2004" ).getStart() ) );

        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 3, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
    }

    @Test
    public void testDateRangeWithConjunction()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );

        Conjunction conjunction = query.conjunction();
        conjunction.add( Restrictions.ge( "created", Year.parseYear( "2002" ).getStart() ) );
        conjunction.add( Restrictions.le( "created", Year.parseYear( "2004" ).getStart() ) );
        query.add( conjunction );

        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 3, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
    }

    @Test
    public void testIsNotNull()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.isNotNull( "categoryCombo" ) );

        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 6, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghA" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghE" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    public void testIsNotNullUrl() throws QueryParserException
    {
        Query query = queryService.getQueryFromUrl( DataElement.class, Lists.newArrayList( "categoryCombo:!null" ), Lists.<Order>newArrayList() );
        List<? extends IdentifiableObject> objects = queryService.query( query );

        assertEquals( 6, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghA" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghE" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    public void testCriteriaAndRootJunctionDE()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ), Junction.Type.AND );
        query.add( Restrictions.eq( "id", "deabcdefghA" ) );
        query.add( Restrictions.eq( "id", "deabcdefghB" ) );
        query.add( Restrictions.eq( "id", "deabcdefghC" ) );

        List<? extends IdentifiableObject> objects = queryService.query( query );
        assertTrue( objects.isEmpty() );
    }

    @Test
    public void testCriteriaOrRootJunctionDE()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ), Junction.Type.OR );
        query.add( Restrictions.eq( "id", "deabcdefghA" ) );
        query.add( Restrictions.eq( "id", "deabcdefghB" ) );
        query.add( Restrictions.eq( "id", "deabcdefghC" ) );

        List<? extends IdentifiableObject> objects = queryService.query( query );
        assertEquals( 3, objects.size() );
    }

    @Test
    public void testCriteriaAndRootJunctionDEG()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ), Junction.Type.AND );
        query.add( Restrictions.eq( "dataElements.id", "deabcdefghA" ) );
        query.add( Restrictions.eq( "dataElements.id", "deabcdefghB" ) );
        query.add( Restrictions.eq( "dataElements.id", "deabcdefghC" ) );
        query.add( Restrictions.eq( "dataElements.id", "deabcdefghD" ) );
        query.add( Restrictions.eq( "dataElements.id", "deabcdefghE" ) );
        query.add( Restrictions.eq( "dataElements.id", "deabcdefghF" ) );

        List<? extends IdentifiableObject> objects = queryService.query( query );
        assertTrue( objects.isEmpty() );
    }

    @Test
    public void testCriteriaOrRootJunctionDEG1()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ), Junction.Type.OR );
        query.add( Restrictions.eq( "dataElements.id", "deabcdefghA" ) );
        query.add( Restrictions.eq( "dataElements.id", "deabcdefghD" ) );

        List<? extends IdentifiableObject> objects = queryService.query( query );
        assertEquals( 2, objects.size() );
    }

    @Test
    public void testCriteriaOrRootJunctionDEG2()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ), Junction.Type.OR );
        query.add( Restrictions.eq( "dataElements.id", "deabcdefghA" ) );

        List<? extends IdentifiableObject> objects = queryService.query( query );
        assertEquals( 1, objects.size() );
    }

    @Test
    public void testMixedQSRootJunction1()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ), Junction.Type.OR );
        query.add( Restrictions.eq( "id", "abcdefghijA" ) );
        query.add( Restrictions.eq( "dataElements.id", "deabcdefghA" ) );
        query.add( Restrictions.eq( "dataElements.id", "deabcdefghD" ) );

        List<? extends IdentifiableObject> objects = queryService.query( query );
        assertEquals( 2, objects.size() );
    }

    @Test
    public void testMixedQSRootJunction2()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ), Junction.Type.OR );
        query.add( Restrictions.eq( "id", "abcdefghijA" ) );
        query.add( Restrictions.eq( "dataElements.id", "does-not-exist" ) );

        List<? extends IdentifiableObject> objects = queryService.query( query );
        assertEquals( 1, objects.size() );
    }

    private boolean collectionContainsUid( Collection<? extends IdentifiableObject> collection, String uid )
    {
        for ( IdentifiableObject identifiableObject : collection )
        {
            if ( identifiableObject.getUid().equals( uid ) )
            {
                return true;
            }
        }

        return false;
    }
}
