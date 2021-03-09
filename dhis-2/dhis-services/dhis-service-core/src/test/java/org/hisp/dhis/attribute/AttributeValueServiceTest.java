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
package org.hisp.dhis.attribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.TransactionalIntegrationTestBase;
import org.hisp.dhis.attribute.exception.NonUniqueAttributeValueException;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserInfo;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Category( IntegrationTest.class )
public class AttributeValueServiceTest
    extends TransactionalIntegrationTestBase
{
    @Autowired
    private AttributeService attributeService;

    @Autowired
    private DataElementStore dataElementStore;

    @Autowired
    private UserService _userService;

    @Autowired
    private CategoryService _categoryService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private CurrentUserService currentUserService;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private Attribute attribute1;

    private Attribute attribute2;

    private Attribute attribute3;

    private UserInfo currentUserInfo;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    protected void setUpTest()
    {
        userService = _userService;
        categoryService = _categoryService;
        createAndInjectAdminUser();

        currentUserInfo = currentUserService.getCurrentUserInfo();

        attribute1 = new Attribute( "attribute 1", ValueType.TEXT );
        attribute1.setDataElementAttribute( true );
        attribute2 = new Attribute( "attribute 2", ValueType.TEXT );
        attribute2.setDataElementAttribute( true );
        attribute3 = new Attribute( "attribute 3", ValueType.TEXT );
        attribute3.setDataElementAttribute( true );

        attributeService.addAttribute( attribute1 );
        attributeService.addAttribute( attribute2 );
        attributeService.addAttribute( attribute3 );

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );

        manager.save( dataElementA );
        manager.save( dataElementB );
        manager.save( dataElementC );
    }

    @Test
    public void testAddAttributeValue()
    {
        AttributeValue avA = new AttributeValue( "valueA", attribute1 );
        AttributeValue avB = new AttributeValue( "valueB", attribute2 );

        attributeService.addAttributeValue( dataElementA, avA );
        attributeService.addAttributeValue( dataElementB, avB );

        assertEquals( 1, dataElementA.getAttributeValues().size() );
        assertNotNull( dataElementA.getAttributeValue( attribute1 ) );
        assertEquals( 1, dataElementB.getAttributeValues().size() );
        assertNotNull( dataElementB.getAttributeValue( attribute2 ) );
    }

    @Test
    public void testDeleteAttributeValue()
    {
        AttributeValue avA = new AttributeValue( "valueA", attribute1 );

        attributeService.addAttributeValue( dataElementA, avA );

        attributeService.deleteAttributeValue( dataElementA, avA );

        assertEquals( 0, dataElementA.getAttributeValues().size() );
    }

    @Test
    public void testGetAttributeValue()
    {
        AttributeValue avA = new AttributeValue( "valueA", attribute1 );
        AttributeValue avB = new AttributeValue( "valueB", attribute2 );

        attributeService.addAttributeValue( dataElementA, avA );
        attributeService.addAttributeValue( dataElementB, avB );

        avA = dataElementA.getAttributeValue( attribute1 );
        assertNotNull( avA );

        List<AttributeValue> attributeValues = dataElementStore
            .getAllValuesByAttributes( Lists.newArrayList( attribute2 ) );

        assertNotNull( attributeValues );
        assertEquals( 1, attributeValues.size() );
        assertEquals( avB.getValue(), attributeValues.get( 0 ).getValue() );

        List<DataElement> list = dataElementStore.getByAttributeAndValue( attribute1, "valueA" );
        assertNotNull( list );
        assertEquals( 1, list.size() );
        assertEquals( dataElementA, list.get( 0 ) );
    }

    @Test
    public void testGetAllByAttribute()
    {
        AttributeValue avA = new AttributeValue( "valueA", attribute1 );
        AttributeValue avB = new AttributeValue( "valueB", attribute2 );

        attributeService.addAttributeValue( dataElementA, avA );
        attributeService.addAttributeValue( dataElementA, avB );
        attributeService.addAttributeValue( dataElementB, avB );

        manager.update( dataElementA );
        manager.update( dataElementB );

        List<DataElement> result = manager
            .getAllByAttributes( DataElement.class, Lists.newArrayList( attribute1, attribute2 ) );

        assertEquals( 2, result.size() );
    }

    @Test
    public void testAddNonUniqueAttributeValue()
        throws NonUniqueAttributeValueException
    {
        Attribute attribute = new Attribute( "ID", ValueType.TEXT );
        attribute.setUnique( true );
        attribute.setDataElementAttribute( true );

        attributeService.addAttribute( attribute );

        AttributeValue attributeValueA = new AttributeValue( "A", attribute );
        attributeService.addAttributeValue( dataElementA, attributeValueA );

        AttributeValue attributeValueB = new AttributeValue( "B", attribute );
        attributeService.addAttributeValue( dataElementB, attributeValueB );
    }

    @Test( expected = NonUniqueAttributeValueException.class )
    public void testAddUniqueAttributeValue()
        throws NonUniqueAttributeValueException
    {
        Attribute attribute = new Attribute( "ID", ValueType.TEXT );
        attribute.setUnique( true );
        attribute.setDataElementAttribute( true );

        attributeService.addAttribute( attribute );

        AttributeValue attributeValueA = new AttributeValue( "A", attribute );
        attributeService.addAttributeValue( dataElementA, attributeValueA );

        AttributeValue attributeValueB = new AttributeValue( "A", attribute );
        attributeService.addAttributeValue( dataElementB, attributeValueB );
    }

    @Test
    public void testAttributeValueFromAttribute()
        throws NonUniqueAttributeValueException
    {
        Attribute attribute = new Attribute( "test", ValueType.TEXT );
        attribute.setDataElementAttribute( true );
        attributeService.addAttribute( attribute );

        AttributeValue attributeValueA = new AttributeValue( "SOME VALUE", attribute );
        AttributeValue attributeValueB = new AttributeValue( "SOME VALUE", attribute );

        attributeService.addAttributeValue( dataElementA, attributeValueA );
        attributeService.addAttributeValue( dataElementB, attributeValueB );

        List<DataElement> dataElements = dataElementStore.getByAttribute( attribute );
        assertEquals( 2, dataElements.size() );

        List<AttributeValue> values = dataElementStore.getAttributeValueByAttribute( attribute );
        assertEquals( 2, values.size() );
    }

    @Test
    public void testAttributeValueFromAttributeAndValue()
        throws NonUniqueAttributeValueException
    {
        Attribute attribute = new Attribute( "test", ValueType.TEXT );
        attribute.setDataElementAttribute( true );
        attributeService.addAttribute( attribute );

        Attribute attribute1 = new Attribute( "test1", ValueType.TEXT );
        attribute1.setDataElementAttribute( true );
        attributeService.addAttribute( attribute1 );

        AttributeValue attributeValueA = new AttributeValue( "SOME VALUE", attribute );
        AttributeValue attributeValueB = new AttributeValue( "SOME VALUE", attribute );
        AttributeValue attributeValueC = new AttributeValue( "ANOTHER VALUE", attribute );

        attributeService.addAttributeValue( dataElementA, attributeValueA );
        attributeService.addAttributeValue( dataElementB, attributeValueB );
        attributeService.addAttributeValue( dataElementC, attributeValueC );

        assertEquals( 1, dataElementA.getAttributeValues().size() );

        List<DataElement> dataElements = dataElementStore.getByAttributeAndValue( attribute, "SOME VALUE" );
        assertEquals( 2, dataElements.size() );

        dataElements = dataElementStore.getByAttributeAndValue( attribute, "ANOTHER VALUE" );
        assertEquals( 1, dataElements.size() );

        List<AttributeValue> values = dataElementStore.getAttributeValueByAttributeAndValue( attribute, "SOME VALUE" );
        assertEquals( 2, values.size() );

        values = dataElementStore
            .getAttributeValueByAttributeAndValue( attribute, "ANOTHER VALUE" );
        assertEquals( 1, values.size() );
    }

    @Test
    public void testDataElementByUniqueAttributeValue()
        throws NonUniqueAttributeValueException
    {
        Attribute attribute = new Attribute( "cid", ValueType.TEXT );
        attribute.setDataElementAttribute( true );
        attribute.setUnique( true );
        attributeService.addAttribute( attribute );

        AttributeValue attributeValueA = new AttributeValue( "CID1", attribute );
        AttributeValue attributeValueB = new AttributeValue( "CID2", attribute );
        AttributeValue attributeValueC = new AttributeValue( "CID3", attribute );

        attributeService.addAttributeValue( dataElementA, attributeValueA );
        attributeService.addAttributeValue( dataElementB, attributeValueB );
        attributeService.addAttributeValue( dataElementC, attributeValueC );

        DataElement deA = dataElementStore.getByUniqueAttributeValue( attribute, "CID1", currentUserInfo );
        DataElement deB = dataElementStore.getByUniqueAttributeValue( attribute, "CID2", currentUserInfo );
        DataElement deC = dataElementStore.getByUniqueAttributeValue( attribute, "CID3", currentUserInfo );

        assertNotNull( deA );
        assertNotNull( deB );
        assertNotNull( deC );
        assertNull( dataElementStore.getByUniqueAttributeValue( attribute, "CID4", currentUserInfo ) );
        assertNull( dataElementStore.getByUniqueAttributeValue( attribute, "CID5", currentUserInfo ) );

        assertEquals( "DataElementA", deA.getName() );
        assertEquals( "DataElementB", deB.getName() );
        assertEquals( "DataElementC", deC.getName() );
    }

    @Test
    public void testUniqueAttributesWithSameValues()
    {
        Attribute attributeA = new Attribute( "ATTRIBUTEA", ValueType.TEXT );
        attributeA.setDataElementAttribute( true );
        attributeA.setUnique( true );
        attributeService.addAttribute( attributeA );

        Attribute attributeB = new Attribute( "ATTRIBUTEB", ValueType.TEXT );
        attributeB.setDataElementAttribute( true );
        attributeB.setUnique( true );
        attributeService.addAttribute( attributeB );

        Attribute attributeC = new Attribute( "ATTRIBUTEC", ValueType.TEXT );
        attributeC.setDataElementAttribute( true );
        attributeC.setUnique( true );
        attributeService.addAttribute( attributeC );

        AttributeValue attributeValueA = new AttributeValue( "VALUE", attributeA );
        AttributeValue attributeValueB = new AttributeValue( "VALUE", attributeB );
        AttributeValue attributeValueC = new AttributeValue( "VALUE", attributeC );

        attributeService.addAttributeValue( dataElementA, attributeValueA );
        attributeService.addAttributeValue( dataElementB, attributeValueB );
        attributeService.addAttributeValue( dataElementC, attributeValueC );

        assertEquals( 1, dataElementA.getAttributeValues().size() );
        assertEquals( 1, dataElementB.getAttributeValues().size() );
        assertEquals( 1, dataElementC.getAttributeValues().size() );

        DataElement de1 = dataElementStore.getByUniqueAttributeValue( attributeA, "VALUE", currentUserInfo );
        DataElement de2 = dataElementStore.getByUniqueAttributeValue( attributeB, "VALUE", currentUserInfo );
        DataElement de3 = dataElementStore.getByUniqueAttributeValue( attributeC, "VALUE", currentUserInfo );

        assertNotNull( de1 );
        assertNotNull( de2 );
        assertNotNull( de3 );

        assertEquals( "DataElementA", de1.getName() );
        assertEquals( "DataElementB", de2.getName() );
        assertEquals( "DataElementC", de3.getName() );
    }

    @Test
    public void testGetAllValuesByAttributes()
    {
        AttributeValue avA = new AttributeValue( "valueA", attribute1 );
        AttributeValue avB = new AttributeValue( "valueB", attribute2 );

        attributeService.addAttributeValue( dataElementA, avA );
        attributeService.addAttributeValue( dataElementB, avB );

        manager.update( dataElementA );
        manager.update( dataElementB );

        List<DataElement> result = manager
            .getAllByAttributes( DataElement.class, Lists.newArrayList( attribute1, attribute2 ) );

        assertEquals( 2, result.size() );

        List<AttributeValue> values = manager.getAllValuesByAttributes( DataElement.class,
            Lists.newArrayList( attribute1, attribute2 ) );

        assertEquals( 2, values.size() );
        assertTrue( values.stream().anyMatch( av -> av.getValue().equals( "valueA" ) ) );
        assertTrue( values.stream().anyMatch( av -> av.getValue().equals( "valueB" ) ) );
    }

    @Test( expected = DeleteNotAllowedException.class )
    public void testDeleteAttributeWithReferences()
    {
        AttributeValue avA = new AttributeValue( "valueA", attribute1 );

        attributeService.addAttributeValue( dataElementA, avA );

        assertEquals( 1, manager.countAllValuesByAttributes( DataElement.class, Lists.newArrayList( attribute1 ) ) );

        attributeService.deleteAttribute( attribute1 );
    }
}
