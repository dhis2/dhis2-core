/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dataelement;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kristian Nordal
 */
class DataElementServiceTest extends TransactionalIntegrationTest
{
    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private IdentifiableObjectStore<OptionSet> optionStore;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------
    @Test
    void testAddDataElement()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        long idA = dataElementService.addDataElement( dataElementA );
        long idB = dataElementService.addDataElement( dataElementB );
        long idC = dataElementService.addDataElement( dataElementC );
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
    void testAddDataElement_WithOptionSet()
    {
        DataElement de = createDataElementWithOptionSet( ValueType.MULTI_TEXT, ValueType.MULTI_TEXT );
        assertDoesNotThrow( () -> dataElementService.addDataElement( de ) );
    }

    @Test
    void testAddDataElement_WithOptionSetValueTypeMismatch()
    {
        DataElement de = createDataElementWithOptionSet( ValueType.TEXT, ValueType.MULTI_TEXT );

        IllegalQueryException ex = assertThrows( IllegalQueryException.class,
            () -> dataElementService.addDataElement( de ) );
        assertEquals( ErrorCode.E1115, ex.getErrorCode() );
        assertEquals( "Data element value type must match option set value type: `MULTI_TEXT`", ex.getMessage() );
    }

    @Test
    void testAddDataElement_WithOptionSetHavingCodeWithSeparator()
    {
        DataElement de = createDataElementWithOptionSet( ValueType.MULTI_TEXT,
            ValueType.MULTI_TEXT, "A", "B", "C,D" );

        IllegalQueryException ex = assertThrows( IllegalQueryException.class,
            () -> dataElementService.addDataElement( de ) );
        assertEquals( ErrorCode.E1117, ex.getErrorCode() );
        assertEquals( format(
            "Data element `%s` of value type multi-text cannot reference an option set `%s` with the separator character in one of its codes: `C,D`",
            de.getUid(), de.getOptionSet().getUid() ), ex.getMessage() );
    }

    @Test
    void testUpdateDataElement()
    {
        DataElement dataElementA = createDataElement( 'A' );
        long idA = dataElementService.addDataElement( dataElementA );
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
    void testUpdateDataElement_WithOptionSet()
    {
        DataElement de = createDataElementWithOptionSet( ValueType.MULTI_TEXT, ValueType.MULTI_TEXT );
        dataElementService.addDataElement( de );
        de.setName( "new-name" );
        assertDoesNotThrow( () -> dataElementService.updateDataElement( de ) );
    }

    @Test
    void testUpdateDataElement_WithOptionSetValueTypeMismatch()
    {
        DataElement de = createDataElementWithOptionSet( ValueType.MULTI_TEXT, ValueType.MULTI_TEXT );
        dataElementService.addDataElement( de );
        de.setValueType( ValueType.TEXT );

        IllegalQueryException ex = assertThrows( IllegalQueryException.class,
            () -> dataElementService.updateDataElement( de ) );
        assertEquals( ErrorCode.E1115, ex.getErrorCode() );
        assertEquals( "Data element value type must match option set value type: `MULTI_TEXT`", ex.getMessage() );
    }

    @Test
    void testUpdateDataElement_WithOptionSetHavingCodeWithSeparator()
    {
        DataElement de = createDataElement( 'A' );
        dataElementService.addDataElement( de );
        de.setValueType( ValueType.MULTI_TEXT );
        de.setOptionSet( addOptionSet( ValueType.MULTI_TEXT, "A", "B", "C,D" ) );

        IllegalQueryException ex = assertThrows( IllegalQueryException.class,
            () -> dataElementService.updateDataElement( de ) );
        assertEquals( ErrorCode.E1117, ex.getErrorCode() );
        assertEquals( format(
            "Data element `%s` of value type multi-text cannot reference an option set `%s` with the separator character in one of its codes: `C,D`",
            de.getUid(), de.getOptionSet().getUid() ), ex.getMessage() );
    }

    @Test
    void testDeleteAndGetDataElement()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );
        long idA = dataElementService.addDataElement( dataElementA );
        long idB = dataElementService.addDataElement( dataElementB );
        long idC = dataElementService.addDataElement( dataElementC );
        long idD = dataElementService.addDataElement( dataElementD );
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
    void testGetDataElementByCode()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        dataElementA.setCode( "codeA" );
        dataElementB.setCode( "codeB" );
        dataElementC.setCode( "codeC" );
        long idA = dataElementService.addDataElement( dataElementA );
        long idB = dataElementService.addDataElement( dataElementB );
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
    void testGetAllDataElements()
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
    void testGetAllDataElementsByType()
    {
        assertEquals( 0, dataElementService.getAllDataElements().size() );
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );
        dataElementA.setValueType( ValueType.FILE_RESOURCE );
        dataElementB.setValueType( ValueType.EMAIL );
        dataElementC.setValueType( ValueType.BOOLEAN );
        dataElementD.setValueType( ValueType.FILE_RESOURCE );
        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        List<DataElement> dataElementsRef = new ArrayList<>();
        dataElementsRef.add( dataElementA );
        dataElementsRef.add( dataElementD );
        List<DataElement> dataElements = dataElementService.getAllDataElementsByValueType( ValueType.FILE_RESOURCE );
        assertNotNull( dataElements );
        assertEquals( dataElementsRef.size(), dataElements.size() );
        assertTrue( dataElements.containsAll( dataElementsRef ) );
    }

    // -------------------------------------------------------------------------
    // DataElementGroup
    // -------------------------------------------------------------------------
    @Test
    void testAddDataElementGroup()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        DataElementGroup dataElementGroupC = new DataElementGroup( "DataElementGroupC" );
        long idA = dataElementService.addDataElementGroup( dataElementGroupA );
        long idB = dataElementService.addDataElementGroup( dataElementGroupB );
        long idC = dataElementService.addDataElementGroup( dataElementGroupC );
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
    void testUpdateDataElementGroup()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        DataElementGroup dataElementGroupC = new DataElementGroup( "DataElementGroupC" );
        long idA = dataElementService.addDataElementGroup( dataElementGroupA );
        long idB = dataElementService.addDataElementGroup( dataElementGroupB );
        long idC = dataElementService.addDataElementGroup( dataElementGroupC );
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
    void testDeleteAndGetDataElementGroup()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        DataElementGroup dataElementGroupC = new DataElementGroup( "DataElementGroupC" );
        DataElementGroup dataElementGroupD = new DataElementGroup( "DataElementGroupD" );
        long idA = dataElementService.addDataElementGroup( dataElementGroupA );
        long idB = dataElementService.addDataElementGroup( dataElementGroupB );
        long idC = dataElementService.addDataElementGroup( dataElementGroupC );
        long idD = dataElementService.addDataElementGroup( dataElementGroupD );
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
    void testGetDataElementGroupByName()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        long idA = dataElementService.addDataElementGroup( dataElementGroupA );
        long idB = dataElementService.addDataElementGroup( dataElementGroupB );
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
    void testAndAndGetDataElementGroupSet()
    {
        DataElementGroup degA = createDataElementGroup( 'A' );
        DataElementGroup degB = createDataElementGroup( 'B' );
        DataElementGroup degC = createDataElementGroup( 'C' );
        dataElementService.addDataElementGroup( degA );
        dataElementService.addDataElementGroup( degB );
        dataElementService.addDataElementGroup( degC );
        DataElementGroupSet degsA = createDataElementGroupSet( 'A' );
        degsA.addDataElementGroup( degA );
        degsA.addDataElementGroup( degB );
        DataElementGroupSet degsB = createDataElementGroupSet( 'B' );
        degsB.addDataElementGroup( degB );
        degsB.addDataElementGroup( degC );
        dataElementService.addDataElementGroupSet( degsA );
        dataElementService.addDataElementGroupSet( degsB );
        assertTrue( degsA.getMembers().contains( degA ) );
        assertTrue( degsA.getMembers().contains( degB ) );
        assertTrue( degA.getGroupSets().contains( degsA ) );
        assertTrue( degB.getGroupSets().contains( degsA ) );
        assertTrue( degsB.getMembers().contains( degB ) );
        assertTrue( degsB.getMembers().contains( degC ) );
        assertTrue( degB.getGroupSets().contains( degsB ) );
        assertTrue( degC.getGroupSets().contains( degsB ) );
    }

    @Test
    void testDataElementUrl()
    {
        DataElement de = createDataElement( 'A', ValueType.URL, AggregationType.SUM );
        long id = dataElementService.addDataElement( de );
        assertNotNull( dataElementService.getDataElement( id ) );
    }

    private DataElement createDataElementWithOptionSet( ValueType deValueType, ValueType osValueType )
    {
        return createDataElementWithOptionSet( deValueType, osValueType, "A" );
    }

    private DataElement createDataElementWithOptionSet( ValueType deValueType, ValueType osValueType, String... codes )
    {
        DataElement de = createDataElement( 'A' );
        de.setOptionSet( addOptionSet( osValueType, codes ) );
        de.setValueType( deValueType );
        return de;
    }

    private OptionSet addOptionSet( ValueType osValueType, String... codes )
    {
        OptionSet options = createOptionSet( 'A',
            stream( codes ).map( code -> new Option( code, code ) ).toArray( Option[]::new ) );
        options.setValueType( osValueType );
        // we use the store to even be able to save a set with separator
        // character in codes
        optionStore.save( options );
        return options;
    }
}
