package org.hisp.dhis.dataelement;

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
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.ValueType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Kristian Nordal
 */
public class DataElementServiceTest
    extends DhisSpringTest
{
    @Autowired
    private DataElementService dataElementService;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddDataElement()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );

        int idA = dataElementService.addDataElement( dataElementA );
        int idB = dataElementService.addDataElement( dataElementB );
        int idC = dataElementService.addDataElement( dataElementC );

        assertNotNull( dataElementA.getUid() );
        assertNotNull( dataElementB.getUid() );
        assertNotNull( dataElementC.getUid() );

        assertNotNull( dataElementA.getLastUpdated() );
        assertNotNull( dataElementB.getLastUpdated() );
        assertNotNull( dataElementC.getLastUpdated() );

        dataElementA = dataElementService.getDataElement( idA );
        assertNotNull( dataElementA );
        assertEquals( idA, dataElementA.getId() );
        assertEquals( "DataElementA", dataElementA.getName() );

        dataElementB = dataElementService.getDataElement( idB );
        assertNotNull( dataElementB );
        assertEquals( idB, dataElementB.getId() );
        assertEquals( "DataElementB", dataElementB.getName() );

        dataElementC = dataElementService.getDataElement( idC );
        assertNotNull( dataElementC );
        assertEquals( idC, dataElementC.getId() );
        assertEquals( "DataElementC", dataElementC.getName() );
    }

    @Test
    public void testUpdateDataElement()
    {
        DataElement dataElementA = createDataElement( 'A' );

        int idA = dataElementService.addDataElement( dataElementA );
        assertNotNull( dataElementA.getUid() );
        assertNotNull( dataElementA.getLastUpdated() );

        dataElementA = dataElementService.getDataElement( idA );
        assertEquals( ValueType.INTEGER, dataElementA.getValueType() );

        dataElementA.setValueType( ValueType.BOOLEAN );
        dataElementService.updateDataElement( dataElementA );
        dataElementA = dataElementService.getDataElement( idA );
        assertNotNull( dataElementA.getValueType() );
        assertEquals( ValueType.BOOLEAN, dataElementA.getValueType() );
    }

    @Test
    public void testDeleteAndGetDataElement()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );

        int idA = dataElementService.addDataElement( dataElementA );
        int idB = dataElementService.addDataElement( dataElementB );
        int idC = dataElementService.addDataElement( dataElementC );
        int idD = dataElementService.addDataElement( dataElementD );

        assertNotNull( dataElementService.getDataElement( idA ) );
        assertNotNull( dataElementService.getDataElement( idB ) );
        assertNotNull( dataElementService.getDataElement( idC ) );
        assertNotNull( dataElementService.getDataElement( idD ) );

        dataElementService.deleteDataElement( dataElementB );

        assertNotNull( dataElementService.getDataElement( idA ) );
        assertNull( dataElementService.getDataElement( idB ) );
        assertNotNull( dataElementService.getDataElement( idC ) );
        assertNotNull( dataElementService.getDataElement( idD ) );

        dataElementService.deleteDataElement( dataElementC );

        assertNotNull( dataElementService.getDataElement( idA ) );
        assertNull( dataElementService.getDataElement( idB ) );
        assertNull( dataElementService.getDataElement( idC ) );
        assertNotNull( dataElementService.getDataElement( idD ) );

        dataElementService.deleteDataElement( dataElementD );

        assertNotNull( dataElementService.getDataElement( idA ) );
        assertNull( dataElementService.getDataElement( idB ) );
        assertNull( dataElementService.getDataElement( idC ) );
        assertNull( dataElementService.getDataElement( idD ) );
    }

    @Test
    public void testGetDataElementByCode()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );

        dataElementA.setCode( "codeA" );
        dataElementB.setCode( "codeB" );
        dataElementC.setCode( "codeC" );

        int idA = dataElementService.addDataElement( dataElementA );
        int idB = dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );

        dataElementA = dataElementService.getDataElementByCode( "codeA" );
        assertNotNull( dataElementA );
        assertEquals( idA, dataElementA.getId() );
        assertEquals( "DataElementA", dataElementA.getName() );

        dataElementB = dataElementService.getDataElementByCode( "codeB" );
        assertNotNull( dataElementB );
        assertEquals( idB, dataElementB.getId() );
        assertEquals( "DataElementB", dataElementB.getName() );

        DataElement dataElementE = dataElementService.getDataElementByCode( "codeE" );
        assertNull( dataElementE );
    }

    @Test
    public void testGetDataElementByShortName()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        int idA = dataElementService.addDataElement( dataElementA );
        int idB = dataElementService.addDataElement( dataElementB );

        dataElementA = dataElementService.getDataElementByShortName( "DataElementShortA" );
        assertNotNull( dataElementA );
        assertEquals( idA, dataElementA.getId() );
        assertEquals( "DataElementA", dataElementA.getName() );

        dataElementB = dataElementService.getDataElementByShortName( "DataElementShortB" );
        assertNotNull( dataElementB );
        assertEquals( idB, dataElementB.getId() );
        assertEquals( "DataElementB", dataElementB.getName() );

        DataElement dataElementC = dataElementService.getDataElementByShortName( "DataElementShortC" );
        assertNull( dataElementC );
    }

    @Test
    public void testGetAllDataElements()
    {
        assertEquals( 0, dataElementService.getAllDataElements().size() );

        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );

        List<DataElement> dataElementsRef = new ArrayList<>();
        dataElementsRef.add( dataElementA );
        dataElementsRef.add( dataElementB );
        dataElementsRef.add( dataElementC );
        dataElementsRef.add( dataElementD );

        List<DataElement> dataElements = dataElementService.getAllDataElements();
        assertNotNull( dataElements );
        assertEquals( dataElementsRef.size(), dataElements.size() );
        assertTrue( dataElements.containsAll( dataElementsRef ) );
    }

    @Test
    public void testGetAggregateableDataElements()
    {
        assertEquals( 0, dataElementService.getAggregateableDataElements().size() );

        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );

        dataElementA.setValueType( ValueType.INTEGER );
        dataElementB.setValueType( ValueType.BOOLEAN );
        dataElementC.setValueType( ValueType.TEXT );
        dataElementD.setValueType( ValueType.INTEGER );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );

        List<DataElement> dataElementsRef = new ArrayList<>();
        dataElementsRef.add( dataElementA );
        dataElementsRef.add( dataElementB );
        dataElementsRef.add( dataElementD );

        List<DataElement> dataElements = dataElementService.getAggregateableDataElements();
        assertNotNull( dataElements );
        assertEquals( dataElementsRef.size(), dataElements.size() );
        assertTrue( dataElements.containsAll( dataElementsRef ) );
    }

    @Test
    public void testGetDataElementsByAggregationType()
    {
        assertEquals( 0, dataElementService.getDataElementsByAggregationType( AggregationType.AVERAGE_SUM_ORG_UNIT ).size() );
        assertEquals( 0, dataElementService.getDataElementsByAggregationType( AggregationType.SUM ).size() );

        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setAggregationType( AggregationType.AVERAGE_SUM_ORG_UNIT );
        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setAggregationType( AggregationType.SUM );
        DataElement dataElementC = createDataElement( 'C' );
        dataElementC.setAggregationType( AggregationType.SUM );
        DataElement dataElementD = createDataElement( 'D' );
        dataElementD.setAggregationType( AggregationType.SUM );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );

        assertEquals( 1, dataElementService.getDataElementsByAggregationType( AggregationType.AVERAGE_SUM_ORG_UNIT ).size() );
        assertEquals( 3, dataElementService.getDataElementsByAggregationType( AggregationType.SUM ).size() );
    }

    @Test
    public void testGetDataElementsByValueType()
    {
        DataElement dataElementA = createDataElement( 'A', ValueType.NUMBER, AggregationType.SUM );
        DataElement dataElementB = createDataElement( 'B', ValueType.NUMBER, AggregationType.SUM );
        DataElement dataElementC = createDataElement( 'C', ValueType.BOOLEAN, AggregationType.SUM );
        DataElement dataElementD = createDataElement( 'D', ValueType.TEXT, AggregationType.SUM );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );

        assertEquals( 2, dataElementService.getDataElementsByValueType( ValueType.NUMBER ).size() );
        assertEquals( 1, dataElementService.getDataElementsByValueType( ValueType.BOOLEAN ).size() );
        assertEquals( 1, dataElementService.getDataElementsByValueType( ValueType.TEXT ).size() );
        assertEquals( 0, dataElementService.getDataElementsByValueType( ValueType.LONG_TEXT ).size() );
        assertEquals( 0, dataElementService.getDataElementsByValueType( ValueType.INTEGER ).size() );
        assertEquals( 0, dataElementService.getDataElementsByValueType( ValueType.INTEGER_POSITIVE ).size() );
        assertEquals( 0, dataElementService.getDataElementsByValueType( ValueType.INTEGER_NEGATIVE ).size() );
    }

    @Test
    public void testGetDataElementsByValueTypes()
    {
        DataElement dataElementA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        DataElement dataElementB = createDataElement( 'B', ValueType.INTEGER_POSITIVE, AggregationType.SUM );
        DataElement dataElementC = createDataElement( 'C', ValueType.INTEGER_ZERO_OR_POSITIVE, AggregationType.SUM );
        DataElement dataElementD = createDataElement( 'D', ValueType.NUMBER, AggregationType.SUM );
        DataElement dataElementE = createDataElement( 'E', ValueType.TEXT, AggregationType.SUM );
        DataElement dataElementF = createDataElement( 'F', ValueType.LONG_TEXT, AggregationType.SUM );
        DataElement dataElementG = createDataElement( 'G', ValueType.COORDINATE, AggregationType.SUM );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        dataElementService.addDataElement( dataElementE );
        dataElementService.addDataElement( dataElementF );
        dataElementService.addDataElement( dataElementG );

        assertEquals( 3, dataElementService.getDataElementsByValueTypes( ValueType.INTEGER_TYPES ).size() );
        assertEquals( 4, dataElementService.getDataElementsByValueTypes( ValueType.NUMERIC_TYPES ).size() );
        assertEquals( 3, dataElementService.getDataElementsByValueTypes( ValueType.TEXT_TYPES ).size() );
    }

    // -------------------------------------------------------------------------
    // DataElementGroup
    // -------------------------------------------------------------------------

    @Test
    public void testAddDataElementGroup()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        DataElementGroup dataElementGroupC = new DataElementGroup( "DataElementGroupC" );

        int idA = dataElementService.addDataElementGroup( dataElementGroupA );
        int idB = dataElementService.addDataElementGroup( dataElementGroupB );
        int idC = dataElementService.addDataElementGroup( dataElementGroupC );

        dataElementGroupA = dataElementService.getDataElementGroup( idA );
        assertNotNull( dataElementGroupA );
        assertEquals( idA, dataElementGroupA.getId() );
        assertEquals( "DataElementGroupA", dataElementGroupA.getName() );

        dataElementGroupB = dataElementService.getDataElementGroup( idB );
        assertNotNull( dataElementGroupB );
        assertEquals( idB, dataElementGroupB.getId() );
        assertEquals( "DataElementGroupB", dataElementGroupB.getName() );

        dataElementGroupC = dataElementService.getDataElementGroup( idC );
        assertNotNull( dataElementGroupC );
        assertEquals( idC, dataElementGroupC.getId() );
        assertEquals( "DataElementGroupC", dataElementGroupC.getName() );
    }

    @Test
    public void testUpdateDataElementGroup()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        DataElementGroup dataElementGroupC = new DataElementGroup( "DataElementGroupC" );

        int idA = dataElementService.addDataElementGroup( dataElementGroupA );
        int idB = dataElementService.addDataElementGroup( dataElementGroupB );
        int idC = dataElementService.addDataElementGroup( dataElementGroupC );

        dataElementGroupA = dataElementService.getDataElementGroup( idA );
        assertNotNull( dataElementGroupA );
        assertEquals( idA, dataElementGroupA.getId() );
        assertEquals( "DataElementGroupA", dataElementGroupA.getName() );

        dataElementGroupA.setName( "DataElementGroupAA" );
        dataElementService.updateDataElementGroup( dataElementGroupA );

        dataElementGroupA = dataElementService.getDataElementGroup( idA );
        assertNotNull( dataElementGroupA );
        assertEquals( idA, dataElementGroupA.getId() );
        assertEquals( "DataElementGroupAA", dataElementGroupA.getName() );

        dataElementGroupB = dataElementService.getDataElementGroup( idB );
        assertNotNull( dataElementGroupB );
        assertEquals( idB, dataElementGroupB.getId() );
        assertEquals( "DataElementGroupB", dataElementGroupB.getName() );

        dataElementGroupC = dataElementService.getDataElementGroup( idC );
        assertNotNull( dataElementGroupC );
        assertEquals( idC, dataElementGroupC.getId() );
        assertEquals( "DataElementGroupC", dataElementGroupC.getName() );
    }

    @Test
    public void testDeleteAndGetDataElementGroup()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        DataElementGroup dataElementGroupC = new DataElementGroup( "DataElementGroupC" );
        DataElementGroup dataElementGroupD = new DataElementGroup( "DataElementGroupD" );

        int idA = dataElementService.addDataElementGroup( dataElementGroupA );
        int idB = dataElementService.addDataElementGroup( dataElementGroupB );
        int idC = dataElementService.addDataElementGroup( dataElementGroupC );
        int idD = dataElementService.addDataElementGroup( dataElementGroupD );

        assertNotNull( dataElementService.getDataElementGroup( idA ) );
        assertNotNull( dataElementService.getDataElementGroup( idB ) );
        assertNotNull( dataElementService.getDataElementGroup( idC ) );
        assertNotNull( dataElementService.getDataElementGroup( idD ) );

        dataElementService.deleteDataElementGroup( dataElementGroupA );
        assertNull( dataElementService.getDataElementGroup( idA ) );
        assertNotNull( dataElementService.getDataElementGroup( idB ) );
        assertNotNull( dataElementService.getDataElementGroup( idC ) );
        assertNotNull( dataElementService.getDataElementGroup( idD ) );

        dataElementService.deleteDataElementGroup( dataElementGroupB );
        assertNull( dataElementService.getDataElementGroup( idA ) );
        assertNull( dataElementService.getDataElementGroup( idB ) );
        assertNotNull( dataElementService.getDataElementGroup( idC ) );
        assertNotNull( dataElementService.getDataElementGroup( idD ) );

        dataElementService.deleteDataElementGroup( dataElementGroupC );
        assertNull( dataElementService.getDataElementGroup( idA ) );
        assertNull( dataElementService.getDataElementGroup( idB ) );
        assertNull( dataElementService.getDataElementGroup( idC ) );
        assertNotNull( dataElementService.getDataElementGroup( idD ) );

        dataElementService.deleteDataElementGroup( dataElementGroupD );
        assertNull( dataElementService.getDataElementGroup( idA ) );
        assertNull( dataElementService.getDataElementGroup( idB ) );
        assertNull( dataElementService.getDataElementGroup( idC ) );
        assertNull( dataElementService.getDataElementGroup( idD ) );
    }

    @Test
    public void testGetDataElementGroupByName()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        int idA = dataElementService.addDataElementGroup( dataElementGroupA );
        int idB = dataElementService.addDataElementGroup( dataElementGroupB );

        assertNotNull( dataElementService.getDataElementGroup( idA ) );
        assertNotNull( dataElementService.getDataElementGroup( idB ) );

        dataElementGroupA = dataElementService.getDataElementGroupByName( "DataElementGroupA" );
        assertNotNull( dataElementGroupA );
        assertEquals( idA, dataElementGroupA.getId() );
        assertEquals( "DataElementGroupA", dataElementGroupA.getName() );

        dataElementGroupB = dataElementService.getDataElementGroupByName( "DataElementGroupB" );
        assertNotNull( dataElementGroupB );
        assertEquals( idB, dataElementGroupB.getId() );
        assertEquals( "DataElementGroupB", dataElementGroupB.getName() );

        DataElementGroup dataElementGroupC = dataElementService.getDataElementGroupByName( "DataElementGroupC" );
        assertNull( dataElementGroupC );
    }

    @Test
    public void testGetDataElementsByZeroIsSignificantAndGroup()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );

        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setZeroIsSignificant( true );
        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setZeroIsSignificant( false );
        DataElement dataElementC = createDataElement( 'C' );
        dataElementC.setZeroIsSignificant( true );
        DataElement dataElementD = createDataElement( 'D' );
        dataElementD.setZeroIsSignificant( false );

        dataElementGroupA.addDataElement( dataElementA );
        dataElementGroupA.addDataElement( dataElementB );
        dataElementGroupA.addDataElement( dataElementC );
        dataElementGroupA.addDataElement( dataElementD );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );

        dataElementService.addDataElementGroup( dataElementGroupA );

        Set<DataElement> expected = new HashSet<>();
        expected.add( dataElementA );
        expected.add( dataElementC );

        assertEquals( expected, dataElementService.getDataElementsByZeroIsSignificantAndGroup( true, dataElementGroupA ) );
    }
}
