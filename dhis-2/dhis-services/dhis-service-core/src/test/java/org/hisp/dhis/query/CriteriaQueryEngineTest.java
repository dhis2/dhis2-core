/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.*;

import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.jfree.data.time.Year;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class CriteriaQueryEngineTest extends TransactionalIntegrationTest
{
    @Autowired
    private SchemaService schemaService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private JpaCriteriaQueryEngine<? extends IdentifiableObject> queryEngine;

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    @Autowired
    private UserService _userService;

    @Before
    public void createDataElements()
    {
        userService = _userService;
        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setValueType( ValueType.NUMBER );
        dataElementA.setName( "dataElementA" );

        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setValueType( ValueType.BOOLEAN );
        dataElementB.setName( "dataElementB" );

        DataElement dataElementC = createDataElement( 'C' );
        dataElementC.setValueType( ValueType.INTEGER );
        dataElementC.setName( "dataElementC" );

        DataElement dataElementD = createDataElement( 'D' );
        dataElementD.setValueType( ValueType.NUMBER );
        dataElementD.setName( "dataElementD" );

        DataElement dataElementE = createDataElement( 'E' );
        dataElementE.setValueType( ValueType.BOOLEAN );
        dataElementE.setName( "dataElementE" );

        DataElement dataElementF = createDataElement( 'F' );
        dataElementF.setValueType( ValueType.INTEGER );
        dataElementF.setName( "dataElementF" );

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
        dataElementGroupA.addDataElement( dataElementA );
        dataElementGroupA.addDataElement( dataElementB );
        dataElementGroupA.addDataElement( dataElementC );
        dataElementGroupA.addDataElement( dataElementD );

        DataElementGroup dataElementGroupB = createDataElementGroup( 'B' );
        dataElementGroupB.addDataElement( dataElementE );
        dataElementGroupB.addDataElement( dataElementF );

        identifiableObjectManager.save( dataElementGroupA );
        identifiableObjectManager.save( dataElementGroupB );
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

    @Test
    public void getAllQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        assertEquals( 6, queryEngine.query( query ).size() );
    }

    @Test
    public void getMinMaxQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.setFirstResult( 2 );
        query.setMaxResults( 10 );

        assertEquals( 4, queryEngine.query( query ).size() );

        query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.setFirstResult( 2 );
        query.setMaxResults( 2 );

        assertEquals( 2, queryEngine.query( query ).size() );
    }

    @Test
    public void getEqQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.eq( "id", "deabcdefghA" ) );
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 1, objects.size() );
        assertEquals( "deabcdefghA", objects.get( 0 ).getUid() );
    }

    @Test
    public void getNeQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.ne( "id", "deabcdefghA" ) );
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

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
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 1, objects.size() );
        assertEquals( "deabcdefghF", objects.get( 0 ).getUid() );
    }

    @Test
    public void getGtQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.gt( "created", Year.parseYear( "2003" ).getStart() ) );
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

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
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 2, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghA" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
    }

    @Test
    public void getGeQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.ge( "created", Year.parseYear( "2003" ).getStart() ) );
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

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
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 3, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghA" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
    }

    @Test
    public void getBetweenQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.between( "created", Year.parseYear( "2003" ).getStart(),
            Year.parseYear( "2005" ).getStart() ) );
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 3, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghE" ) );
    }

    @Test
    public void testDateRange()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );

        query.add( Restrictions.ge( "created", Year.parseYear( "2002" ).getStart() ) );
        query.add( Restrictions.le( "created", Year.parseYear( "2004" ).getStart() ) );

        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 3, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
    }

    @Test
    public void getInQuery()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.in( "id", Lists.newArrayList( "deabcdefghD", "deabcdefghF" ) ) );
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 2, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    public void sortNameDesc()
    {
        Schema schema = schemaService.getDynamicSchema( DataElement.class );

        Query query = Query.from( schema );
        query.addOrder( new Order( schema.getProperty( "name" ), Direction.DESCENDING ) );
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

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
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

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
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

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
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

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

        List<? extends IdentifiableObject> objects = queryEngine.query( query );

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

        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 2, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    public void testDateRangeWithConjunction()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );

        Conjunction conjunction = query.conjunction();
        conjunction.add( Restrictions.ge( "created", Year.parseYear( "2002" ).getStart() ) );
        conjunction.add( Restrictions.le( "created", Year.parseYear( "2004" ).getStart() ) );
        query.add( conjunction );

        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 3, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
    }

    @Test
    public void testIsNull()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.isNull( "categoryCombo" ) );

        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 0, objects.size() );
    }

    @Test
    public void testIsNotNull()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.isNotNull( "categoryCombo" ) );

        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 6, objects.size() );

        assertTrue( collectionContainsUid( objects, "deabcdefghA" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghB" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghC" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghD" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghE" ) );
        assertTrue( collectionContainsUid( objects, "deabcdefghF" ) );
    }

    @Test
    @Ignore
    public void testCollectionEqSize4()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ) );
        query.add( Restrictions.eq( "dataElements", 4 ) );
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 1, objects.size() );
        assertEquals( "abcdefghijA", objects.get( 0 ).getUid() );
    }

    @Test
    @Ignore
    public void testCollectionEqSize2()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ) );
        query.add( Restrictions.eq( "dataElements", 2 ) );
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 1, objects.size() );
        assertEquals( "abcdefghijB", objects.get( 0 ).getUid() );
    }

    @Test
    public void testIdentifiableSearch1()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ), Junction.Type.OR );
        query.add( Restrictions.eq( "name", "DataElementGroupA" ) );
        query.add( Restrictions.eq( "name", "DataElementGroupB" ) );

        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 2, objects.size() );
    }

    @Test
    public void testIdentifiableSearch2()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ), Junction.Type.OR );

        Junction disjunction = new Disjunction( schemaService.getDynamicSchema( DataElementGroup.class ) );
        disjunction.add( Restrictions.eq( "name", "DataElementGroupA" ) );
        disjunction.add( Restrictions.eq( "name", "DataElementGroupB" ) );
        query.add( disjunction );

        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 2, objects.size() );
    }

    @Test
    public void testIdentifiableSearch3()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ) );

        Junction disjunction = new Disjunction( schemaService.getDynamicSchema( DataElementGroup.class ) );
        disjunction.add( Restrictions.like( "name", "GroupA", MatchMode.ANYWHERE ) );
        query.add( disjunction );

        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 1, objects.size() );
    }

    @Test
    public void testIdentifiableSearch4()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ), Junction.Type.OR );

        Junction disjunction = new Disjunction( schemaService.getDynamicSchema( DataElementGroup.class ) );
        disjunction.add( Restrictions.like( "name", "GroupA", MatchMode.ANYWHERE ) );
        disjunction.add( Restrictions.like( "name", "GroupA", MatchMode.ANYWHERE ) );
        query.add( disjunction );

        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 1, objects.size() );
    }

    @Test
    public void testIdentifiableSearch5()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ), Junction.Type.OR );

        Junction disjunction = new Disjunction( schemaService.getDynamicSchema( DataElementGroup.class ) );
        disjunction.add( Restrictions.like( "name", "GroupA", MatchMode.ANYWHERE ) );
        disjunction.add( Restrictions.like( "name", "GroupA", MatchMode.ANYWHERE ) );
        disjunction.add( Restrictions.like( "name", "GroupB", MatchMode.ANYWHERE ) );
        query.add( disjunction );

        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 2, objects.size() );
    }

    @Test
    public void testIdentifiableSearch6()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ), Junction.Type.OR );

        Restriction nameRestriction = Restrictions.like( "name", "deF", MatchMode.ANYWHERE );
        Restriction uidRestriction = Restrictions.like( "id", "deF", MatchMode.ANYWHERE );
        Restriction codeRestriction = Restrictions.like( "code", "deF", MatchMode.ANYWHERE );

        Junction identifiableJunction = new Disjunction( schemaService.getDynamicSchema( DataElement.class ) );
        identifiableJunction.add( nameRestriction );
        identifiableJunction.add( uidRestriction );
        identifiableJunction.add( codeRestriction );

        query.add( identifiableJunction );

        List<? extends IdentifiableObject> objects = queryEngine.query( query );
        assertEquals( 1, objects.size() );
    }

    @Test
    @Ignore
    public void testIdentifiableSearch7()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ), Junction.Type.OR );

        Restriction nameRestriction = Restrictions.like( "name", "dataElement", MatchMode.ANYWHERE );
        Restriction uidRestriction = Restrictions.like( "id", "dataElement", MatchMode.ANYWHERE );
        Restriction codeRestriction = Restrictions.like( "code", "dataElement", MatchMode.ANYWHERE );

        Junction identifiableJunction = new Disjunction( schemaService.getDynamicSchema( DataElement.class ) );
        identifiableJunction.add( nameRestriction );
        identifiableJunction.add( uidRestriction );
        identifiableJunction.add( codeRestriction );

        query.add( identifiableJunction );

        List<? extends IdentifiableObject> objects = queryService.query( query );
        assertEquals( 6, objects.size() );
    }

    @Test
    @Ignore
    public void testIdentifiableSearch8()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ), Junction.Type.OR );

        Restriction displayNameRestriction = Restrictions.like( "displayName", "dataElement", MatchMode.ANYWHERE );
        Restriction uidRestriction = Restrictions.like( "id", "dataElement", MatchMode.ANYWHERE );
        Restriction codeRestriction = Restrictions.like( "code", "dataElement", MatchMode.ANYWHERE );

        Junction identifiableJunction = new Disjunction( schemaService.getDynamicSchema( DataElement.class ) );
        identifiableJunction.add( displayNameRestriction );
        identifiableJunction.add( uidRestriction );
        identifiableJunction.add( codeRestriction );

        query.add( identifiableJunction );

        List<? extends IdentifiableObject> objects = queryService.query( query );
        assertEquals( 6, objects.size() );
    }

    @Test
    public void testQueryWithNoAccessPermission()
    {
        User userA = createUser( 'A' );
        userService.addUser( userA );
        User userB = createUser( 'B' );
        userService.addUser( userB );
        DataElement de = identifiableObjectManager.get( DataElement.class, "deabcdefghA" );
        de.setCreatedBy( userB );
        identifiableObjectManager.save( de, false );

        de = identifiableObjectManager.get( DataElement.class, "deabcdefghA" );
        assertEquals( AccessStringHelper.DEFAULT, de.getSharing().getPublicAccess() );
        assertEquals( userB.getUid(), de.getSharing().getOwner() );

        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.eq( "id", de.getUid() ) );
        query.setUser( userA );
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        assertEquals( 0, objects.size() );
    }

    @Test
    public void testEmptyQueryWithNoAccessPermission()
    {
        User userA = createUser( 'A' );
        userService.addUser( userA );
        User userB = createUser( 'B' );
        userService.addUser( userB );
        DataElement de = identifiableObjectManager.get( DataElement.class, "deabcdefghA" );
        de.setCreatedBy( userB );
        identifiableObjectManager.save( de, false );

        de = identifiableObjectManager.get( DataElement.class, "deabcdefghA" );
        assertEquals( AccessStringHelper.DEFAULT, de.getSharing().getPublicAccess() );
        assertEquals( userB.getUid(), de.getSharing().getOwner() );

        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.setUser( userB );
        List<? extends IdentifiableObject> objects = queryEngine.query( query );

        // UserB is the owner so DEA is in the result list
        Optional<? extends IdentifiableObject> notPublicDe = objects.stream()
            .filter( d -> d.getUid().equalsIgnoreCase( "deabcdefghA" ) ).findFirst();

        assertTrue( notPublicDe.isPresent() );

        query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.setUser( userA );
        objects = queryEngine.query( query );

        // UserA isn't the owner and DEA is not public so it doesn't present in
        // result list
        notPublicDe = objects.stream()
            .filter( d -> d.getUid().equalsIgnoreCase( "deabcdefghA" ) ).findFirst();

        assertTrue( !notPublicDe.isPresent() );
    }

    @Test
    public void testCountAndPaging()
    {
        Query query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        assertEquals( 6, queryEngine.count( query ) );
        assertEquals( 6, queryEngine.query( query ).size() );
        query.setMaxResults( 2 );
        query.setFirstResult( 1 );
        assertEquals( 2, queryEngine.query( query ).size() );

        query = Query.from( schemaService.getDynamicSchema( DataElement.class ) );
        query.add( Restrictions.eq( "id", "deabcdefghA" ) );
        assertEquals( 1, queryEngine.count( query ) );
        assertEquals( 1, queryEngine.query( query ).size() );

        query.add( Restrictions.eq( "name", "not exist" ) );
        assertEquals( 0, queryEngine.count( query ) );
        assertEquals( 0, queryEngine.query( query ).size() );

    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }
}
