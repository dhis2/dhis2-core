package org.hisp.dhis.attribute;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.hisp.dhis.*;
import org.hisp.dhis.attribute.exception.NonUniqueAttributeValueException;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Category( IntegrationTest.class )
public class AttributeValueServiceTest
    extends IntegrationTestBase
{
    @Autowired
    private AttributeService attributeService;

    @Autowired
    private DataElementStore dataElementStore;

    @Autowired
    private UserService _userService;

    @Autowired
    private CategoryService _categoryService;

    private AttributeValue avA;
    private AttributeValue avB;
    private AttributeValue avC;
    private DataElement dataElementA;
    private DataElement dataElementB;
    private DataElement dataElementC;
    private Attribute attribute1;
    private Attribute attribute2;
    private Attribute attribute3;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    protected void setUpTest() throws NonUniqueAttributeValueException
    {
        userService = _userService;
        categoryService = _categoryService;
        createAndInjectAdminUser();
        avA = new AttributeValue( "value 1" );
        avB = new AttributeValue( "value 2" );
        avC = new AttributeValue( "value 3" );

        attribute1 = new Attribute( "attribute 1", ValueType.TEXT );
        attribute1.setDataElementAttribute( true );
        attribute2 = new Attribute( "attribute 2", ValueType.TEXT );
        attribute2.setDataElementAttribute( true );
        attribute3 = new Attribute( "attribute 3", ValueType.TEXT );
        attribute3.setDataElementAttribute( true );

        attributeService.addAttribute( attribute1 );
        attributeService.addAttribute( attribute2 );
        attributeService.addAttribute( attribute3 );

        avA.setAttribute( attribute1 );
        avA.setAutoFields();
        avB.setAttribute( attribute2 );
        avC.setAttribute( attribute3 );
        avB.setAutoFields();
        avC.setAutoFields();
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementA.getAttributeValues().add( avA );
        dataElementB.getAttributeValues().add( avB );
        dataElementC.getAttributeValues().add( avC );

        dataElementStore.save( dataElementA );
        dataElementStore.save( dataElementB );
        dataElementStore.save( dataElementC );

        attributeService.addAttributeValue( dataElementA, avA );
        attributeService.addAttributeValue( dataElementB, avB );
        attributeService.addAttributeValue( dataElementC, avC );

    }

    @Test
    public void testAddAttributeValue()
    {
        AttributeValue avA = new AttributeValue( "valueA", attribute1 );
        avA.setAutoFields();
        AttributeValue avB = new AttributeValue( "valueB", attribute2 );
        avB.setAutoFields();

        attributeService.addAttributeValue( dataElementA, avA );
        attributeService.addAttributeValue( dataElementB, avB );

        assertEquals( 2, dataElementA.getAttributeValues().size() );
        assertNotNull( dataElementA.getAttributeValue( attribute1 ) );
        assertEquals( 2, dataElementB.getAttributeValues().size() );
        assertNotNull( dataElementB.getAttributeValue( attribute2 ) );
    }

    @Test
    public void testUpdateAttributeValue() throws NonUniqueAttributeValueException
    {
        avA.setValue( "updated value 1" );
        avB.setValue( "updated value 2" );

        attributeService.updateAttributeValue( dataElementA, avA );
        attributeService.updateAttributeValue( dataElementB, avB );

        avA = dataElementA.getAttributeValue( attribute1 );
        avB = dataElementB.getAttributeValue( attribute2 );

        assertNotNull( avA );
        assertNotNull( avB );

        assertEquals( "updated value 1", avA.getValue() );
        assertEquals( "updated value 2", avB.getValue() );
    }

    @Test
    public void testDeleteAttributeValue()
    {
        attributeService.deleteAttributeValue( dataElementA, avA );

        assertEquals( 0, dataElementA.getAttributeValues().size() );
    }

    @Test
    public void testGetAttributeValue()
    {
        avA = dataElementA.getAttributeValue( attribute1 );
        List<DataElement> objects = dataElementStore.getByAttribute( attribute2 );
        List<AttributeValue> avs = objects.stream().map( o -> o.getAttributeValue( attribute2 ) )
            .collect( Collectors.toList() );

        assertNotNull( avA );
        assertTrue( !avs.isEmpty() );
        assertEquals( avB.getValue(), avs.get( 0 ).getValue() );
    }

    @Test
    public void testAddNonUniqueAttributeValue() throws NonUniqueAttributeValueException
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
    public void testAddUniqueAttributeValue() throws NonUniqueAttributeValueException
    {
        Attribute attribute = new Attribute( "ID", ValueType.TEXT );
        attribute.setUnique( true );
        attribute.setDataElementAttribute( true );

        attributeService.addAttribute( attribute );

        AttributeValue attributeValueA = new AttributeValue( "A", attribute );
        attributeService.addAttributeValue( dataElementA, attributeValueA );

        dataElementA.getAttributeValues().forEach( System.out::println );

        AttributeValue attributeValueB = new AttributeValue( "A", attribute );
        attributeService.addAttributeValue( dataElementB, attributeValueB );
    }

    @Test( expected = NonUniqueAttributeValueException.class )
    public void testUpdateNonUniqueAttributeValue() throws NonUniqueAttributeValueException
    {
        Attribute attribute = new Attribute( "ID", ValueType.TEXT );
        attribute.setUnique( true );
        attribute.setDataElementAttribute( true );

        attributeService.addAttribute( attribute );

        AttributeValue attributeValueA = new AttributeValue( "A", attribute );
        attributeService.addAttributeValue( dataElementA, attributeValueA );

        AttributeValue attributeValueB = new AttributeValue( "B", attribute );
        attributeService.addAttributeValue( dataElementB, attributeValueB );

        attributeValueB.setValue( "A" );
        attributeService.updateAttributeValue( dataElementB, attributeValueB );
    }

    @Test
    public void testGetJsonAttributeValues() throws Exception
    {

        Attribute attribute1 = new Attribute( "attribute1", ValueType.TEXT );
        attribute1.setDataElementAttribute( true );
        attributeService.addAttribute( attribute1 );

        AttributeValue av = new AttributeValue( "value1", attribute1 );
        attributeService.addAttributeValue( dataElementA, av );

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode node = mapper.createObjectNode();
        node.put( "id",attribute1.getId() );
        node.put( "value", "updatedvalue1" );

        List<String> jsonValues  = new ArrayList<>();
        jsonValues.add( node.toString() );

        attributeService.updateAttributeValues( dataElementA, jsonValues );

        assertEquals( "updatedvalue1", dataElementA.getAttributeValue( av.getAttribute() ).getValue() );
    }

    @Test
    public void testAttributeValueFromAttribute() throws NonUniqueAttributeValueException
    {
        Attribute attribute = new Attribute( "test", ValueType.TEXT );
        attribute.setDataElementAttribute( true );
        attributeService.addAttribute( attribute );

        AttributeValue attributeValueA = new AttributeValue( "SOME VALUE", attribute );
        attributeValueA.setAutoFields();
        AttributeValue attributeValueB = new AttributeValue( "SOME VALUE", attribute );
        attributeValueB.setAutoFields();

        attributeService.addAttributeValue( dataElementA, attributeValueA );
        attributeService.addAttributeValue( dataElementB, attributeValueB );

        List<DataElement> dataElements = dataElementStore.getByAttribute( attribute );
        assertEquals( 2, dataElements.size() );
    }

    @Test
    public void testAttributeValueFromAttributeAndValue() throws NonUniqueAttributeValueException
    {
        Attribute attribute = new Attribute( "test", ValueType.TEXT );
        attribute.setDataElementAttribute( true );
        attributeService.addAttribute( attribute );

        Attribute attribute1 = new Attribute( "test1", ValueType.TEXT );
        attribute1.setDataElementAttribute( true );
        attributeService.addAttribute( attribute1 );

        AttributeValue attributeValueA = new AttributeValue( "SOME VALUE", attribute );
        AttributeValue attributeValueA1 = new AttributeValue( "SOME VALUE1", attribute1 );
        AttributeValue attributeValueB = new AttributeValue( "SOME VALUE", attribute );
        AttributeValue attributeValueC = new AttributeValue( "ANOTHER VALUE", attribute );

        attributeService.addAttributeValue( dataElementA, attributeValueA );
        attributeService.addAttributeValue( dataElementA, attributeValueA1 );
        attributeService.addAttributeValue( dataElementB, attributeValueB );
        attributeService.addAttributeValue( dataElementC, attributeValueC );

        assertEquals( 3, dataElementA.getAttributeValues().size() );

        List<DataElement> dataElements = dataElementStore.getByAttributeAndValue( attribute, "SOME VALUE" );
        assertEquals( 2, dataElements.size() );

        dataElements = dataElementStore.getByAttributeAndValue( attribute, "ANOTHER VALUE" );
        assertEquals( 1, dataElements.size() );
    }

    @Test
    public void testDataElementByUniqueAttributeValue() throws NonUniqueAttributeValueException
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

        assertNotNull( dataElementStore.getByUniqueAttributeValue( attribute, "CID1" ) );
        assertNotNull( dataElementStore.getByUniqueAttributeValue( attribute, "CID2" ) );
        assertNotNull( dataElementStore.getByUniqueAttributeValue( attribute, "CID3" ) );
        assertNull( dataElementStore.getByUniqueAttributeValue( attribute, "CID4" ) );
        assertNull( dataElementStore.getByUniqueAttributeValue( attribute, "CID5" ) );

        assertEquals( "DataElementA", dataElementStore.getByUniqueAttributeValue( attribute, "CID1" ).getName() );
        assertEquals( "DataElementB", dataElementStore.getByUniqueAttributeValue( attribute, "CID2" ).getName() );
        assertEquals( "DataElementC", dataElementStore.getByUniqueAttributeValue( attribute, "CID3" ).getName() );
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

        assertEquals( 2, dataElementA.getAttributeValues().size() );
        assertEquals( 2, dataElementB.getAttributeValues().size() );
        assertEquals( 2, dataElementC.getAttributeValues().size() );

        DataElement de1 = dataElementStore.getByUniqueAttributeValue( attributeA, "VALUE" );
        DataElement de2 = dataElementStore.getByUniqueAttributeValue( attributeB, "VALUE" );
        DataElement de3 = dataElementStore.getByUniqueAttributeValue( attributeC, "VALUE" );

        assertNotNull( de1 );
        assertNotNull( de2 );
        assertNotNull( de3 );

        assertEquals( "DataElementA", de1.getName() );
        assertEquals( "DataElementB", de2.getName() );
        assertEquals( "DataElementC", de3.getName() );
    }
}
