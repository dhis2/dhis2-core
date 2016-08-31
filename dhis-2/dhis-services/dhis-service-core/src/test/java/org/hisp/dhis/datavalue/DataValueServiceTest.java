package org.hisp.dhis.datavalue;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kristian Nordal
 * @version $Id: DataValueServiceTest.java 5715 2008-09-17 14:05:28Z larshelg $
 */
public class DataValueServiceTest
    extends DhisSpringTest
{
    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private OrganisationUnitService organisationUnitService;
    
    // -------------------------------------------------------------------------
    // Supporting data
    // -------------------------------------------------------------------------

    private DataElement dataElementA;
    private DataElement dataElementB;
    private DataElement dataElementC;
    private DataElement dataElementD;

    private DataElementCategoryOptionCombo optionCombo;
    
    private Period periodA;
    private Period periodB;
    private Period periodC;
    private Period periodD;

    private OrganisationUnit sourceA;
    private OrganisationUnit sourceB;
    private OrganisationUnit sourceC;
    private OrganisationUnit sourceD;

    // -------------------------------------------------------------------------
    // Set up/tear down
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    { 
        // ---------------------------------------------------------------------
        // Add supporting data
        // ---------------------------------------------------------------------

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementD = createDataElement( 'D' );
        
        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );

        periodA = createPeriod( getDay( 5 ), getDay( 6 ) );
        periodB = createPeriod( getDay( 6 ), getDay( 7 ) );
        periodC = createPeriod( getDay( 7 ), getDay( 8 ) );
        periodD = createPeriod( getDay( 8 ), getDay( 9 ) );
        
        sourceA = createOrganisationUnit( 'A' );
        sourceB = createOrganisationUnit( 'B' );
        sourceC = createOrganisationUnit( 'C' );
        sourceD = createOrganisationUnit( 'D' );

        organisationUnitService.addOrganisationUnit( sourceA );
        organisationUnitService.addOrganisationUnit( sourceB );
        organisationUnitService.addOrganisationUnit( sourceC );
        organisationUnitService.addOrganisationUnit( sourceD );

        optionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();
    }
    
    // -------------------------------------------------------------------------
    // Basic DataValue
    // -------------------------------------------------------------------------

    @Test
    public void testAddDataValue()
    {
        DataValue dataValueA = new DataValue( dataElementA, periodA, sourceA, optionCombo, optionCombo );
        dataValueA.setValue( "1" );
        DataValue dataValueB = new DataValue( dataElementB, periodA, sourceA, optionCombo, optionCombo );
        dataValueB.setValue( "2" );
        DataValue dataValueC = new DataValue( dataElementC, periodC, sourceA, optionCombo, optionCombo );
        dataValueC.setValue( "3" );

        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );

        dataValueA = dataValueService.getDataValue( dataElementA, periodA, sourceA, optionCombo );
        assertNotNull( dataValueA );
        assertNotNull( dataValueA.getCreated() );
        assertEquals( sourceA.getId(), dataValueA.getSource().getId() );
        assertEquals( dataElementA, dataValueA.getDataElement() );
        assertEquals( periodA, dataValueA.getPeriod() );
        assertEquals( "1", dataValueA.getValue() );

        dataValueB = dataValueService.getDataValue( dataElementB, periodA, sourceA, optionCombo );
        assertNotNull( dataValueB );
        assertNotNull( dataValueB.getCreated() );
        assertEquals( sourceA.getId(), dataValueB.getSource().getId() );
        assertEquals( dataElementB, dataValueB.getDataElement() );
        assertEquals( periodA, dataValueB.getPeriod() );
        assertEquals( "2", dataValueB.getValue() );

        dataValueC = dataValueService.getDataValue( dataElementC, periodC, sourceA, optionCombo );
        assertNotNull( dataValueC );
        assertNotNull( dataValueC.getCreated() );
        assertEquals( sourceA.getId(), dataValueC.getSource().getId() );
        assertEquals( dataElementC, dataValueC.getDataElement() );
        assertEquals( periodC, dataValueC.getPeriod() );
        assertEquals( "3", dataValueC.getValue() );
    }

    @Test
    public void testUpdataDataValue()
    {
        DataValue dataValueA = new DataValue( dataElementA, periodA, sourceA, optionCombo, optionCombo );
        dataValueA.setValue( "1" );
        DataValue dataValueB = new DataValue( dataElementB, periodA, sourceB, optionCombo, optionCombo );
        dataValueB.setValue( "2" );

        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );

        assertNotNull( dataValueService.getDataValue( dataElementA, periodA, sourceA, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( dataElementB, periodA, sourceB, optionCombo ) );

        dataValueA.setValue( "5" );
        dataValueService.updateDataValue( dataValueA );

        dataValueA = dataValueService.getDataValue( dataElementA, periodA, sourceA, optionCombo );
        assertNotNull( dataValueA );
        assertEquals( "5", dataValueA.getValue() );

        dataValueB = dataValueService.getDataValue( dataElementB, periodA, sourceB, optionCombo );
        assertNotNull( dataValueB );
        assertEquals( "2", dataValueB.getValue() );
    }

    @Test
    public void testDeleteAndGetDataValue()
    {
        DataValue dataValueA = new DataValue( dataElementA, periodA, sourceA, optionCombo, optionCombo );
        dataValueA.setValue( "1" );
        DataValue dataValueB = new DataValue( dataElementB, periodA, sourceA, optionCombo, optionCombo );
        dataValueB.setValue( "2" );
        DataValue dataValueC = new DataValue( dataElementC, periodC, sourceD, optionCombo, optionCombo );
        dataValueC.setValue( "3" );
        DataValue dataValueD = new DataValue( dataElementD, periodC, sourceB, optionCombo, optionCombo );
        dataValueD.setValue( "4" );

        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );

        assertNotNull( dataValueService.getDataValue( dataElementA, periodA, sourceA, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( dataElementB, periodA, sourceA, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( dataElementC, periodC, sourceD, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( dataElementD, periodC, sourceB, optionCombo ) );

        dataValueService.deleteDataValue( dataValueA );
        assertNull( dataValueService.getDataValue( dataElementA, periodA, sourceA, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( dataElementB, periodA, sourceA, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( dataElementC, periodC, sourceD, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( dataElementD, periodC, sourceB, optionCombo ) );

        dataValueService.deleteDataValue( dataValueB );
        assertNull( dataValueService.getDataValue( dataElementA, periodA, sourceA, optionCombo ) );
        assertNull( dataValueService.getDataValue( dataElementB, periodA, sourceA, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( dataElementC, periodC, sourceD, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( dataElementD, periodC, sourceB, optionCombo ) );

        dataValueService.deleteDataValue( dataValueC );
        assertNull( dataValueService.getDataValue( dataElementA, periodA, sourceA, optionCombo ) );
        assertNull( dataValueService.getDataValue( dataElementB, periodA, sourceA, optionCombo ) );
        assertNull( dataValueService.getDataValue( dataElementC, periodC, sourceD, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( dataElementD, periodC, sourceB, optionCombo ) );

        dataValueService.deleteDataValue( dataValueD );
        assertNull( dataValueService.getDataValue( dataElementA, periodA, sourceA, optionCombo ) );
        assertNull( dataValueService.getDataValue( dataElementB, periodA, sourceA, optionCombo ) );
        assertNull( dataValueService.getDataValue( dataElementC, periodC, sourceD, optionCombo ) );
        assertNull( dataValueService.getDataValue( dataElementD, periodC, sourceB, optionCombo ) );
    }

    // -------------------------------------------------------------------------
    // Collections of DataValues
    // -------------------------------------------------------------------------

    @Test
    public void testGetAllDataValues()
    {
        DataValue dataValueA = new DataValue( dataElementA, periodA, sourceA, optionCombo, optionCombo );
        dataValueA.setValue( "1" );
        DataValue dataValueB = new DataValue( dataElementB, periodA, sourceA, optionCombo, optionCombo );
        dataValueB.setValue( "2" );
        DataValue dataValueC = new DataValue( dataElementC, periodC, sourceD, optionCombo, optionCombo );
        dataValueC.setValue( "3" );
        DataValue dataValueD = new DataValue( dataElementD, periodC, sourceB, optionCombo, optionCombo );
        dataValueD.setValue( "4" );
    
        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );
        
        List<DataValue> dataValues = dataValueService.getAllDataValues();
        assertNotNull( dataValues );
        assertEquals( 4, dataValues.size() );
    }   

    @Test
    public void testGetDataValuesSourcePeriod()
    {
        DataValue dataValueA = new DataValue( dataElementA, periodA, sourceA, optionCombo, optionCombo );
        dataValueA.setValue( "1" );
        DataValue dataValueB = new DataValue( dataElementB, periodA, sourceA, optionCombo, optionCombo );
        dataValueB.setValue( "2" );
        DataValue dataValueC = new DataValue( dataElementC, periodC, sourceD, optionCombo, optionCombo );
        dataValueC.setValue( "3" );
        DataValue dataValueD = new DataValue( dataElementD, periodC, sourceB, optionCombo, optionCombo );
        dataValueD.setValue( "4" );

        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );

        List<DataValue> dataValues = dataValueService.getDataValues( sourceA, periodA );
        assertNotNull( dataValues );
        assertEquals( 2, dataValues.size() );

        dataValues = dataValueService.getDataValues( sourceB, periodC );
        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() );

        dataValues = dataValueService.getDataValues( sourceB, periodD );
        assertNotNull( dataValues );
        assertEquals( 0, dataValues.size() );
    }

    @Test
    public void testGetDataValuesSourceDataElement()
    {
        DataValue dataValueA = new DataValue( dataElementA, periodA, sourceA, optionCombo, optionCombo );
        dataValueA.setValue( "1" );
        DataValue dataValueB = new DataValue( dataElementB, periodA, sourceA, optionCombo, optionCombo );
        dataValueB.setValue( "2" );
        DataValue dataValueC = new DataValue( dataElementC, periodC, sourceD, optionCombo, optionCombo );
        dataValueC.setValue( "3" );
        DataValue dataValueD = new DataValue( dataElementD, periodC, sourceB, optionCombo, optionCombo );
        dataValueD.setValue( "4" );

        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );

        List<DataValue> dataValues = dataValueService.getDataValues( sourceA, dataElementA );
        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() );

        dataValues = dataValueService.getDataValues( sourceA, dataElementB );
        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() );

        dataValues = dataValueService.getDataValues( sourceA, dataElementC );
        assertNotNull( dataValues );
        assertEquals( 0, dataValues.size() );
    }

    @Test
    public void testGetDataValuesSourcesDataElement()
    {
        DataValue dataValueA = new DataValue( dataElementA, periodA, sourceA, optionCombo, optionCombo );
        dataValueA.setValue( "1" );
        DataValue dataValueB = new DataValue( dataElementB, periodA, sourceA, optionCombo, optionCombo );
        dataValueB.setValue( "2" );
        DataValue dataValueC = new DataValue( dataElementC, periodC, sourceD, optionCombo, optionCombo );
        dataValueC.setValue( "3" );
        DataValue dataValueD = new DataValue( dataElementA, periodC, sourceB, optionCombo, optionCombo );
        dataValueD.setValue( "4" );

        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );

        List<OrganisationUnit> sources = new ArrayList<>();
        sources.add( sourceA );
        sources.add( sourceB );

        List<DataValue> dataValues = dataValueService.getDataValues( sources, dataElementA );
        assertNotNull( dataValues );
        assertEquals( 2, dataValues.size() );

        dataValues = dataValueService.getDataValues( sources, dataElementB );
        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() );

        dataValues = dataValueService.getDataValues( sources, dataElementC );
        assertNotNull( dataValues );
        assertEquals( 0, dataValues.size() );
    }

    @Test
    public void testGetDataValuesSourcePeriodDataElements()
    {
        DataValue dataValueA = new DataValue( dataElementA, periodA, sourceA, optionCombo, optionCombo );
        dataValueA.setValue( "1" );
        DataValue dataValueB = new DataValue( dataElementB, periodA, sourceA, optionCombo, optionCombo );
        dataValueB.setValue( "2" );
        DataValue dataValueC = new DataValue( dataElementC, periodC, sourceD, optionCombo, optionCombo );
        dataValueC.setValue( "3" );
        DataValue dataValueD = new DataValue( dataElementA, periodC, sourceB, optionCombo, optionCombo );
        dataValueD.setValue( "4" );

        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );

        List<DataElement> dataElements = new ArrayList<>();
        dataElements.add( dataElementA );
        dataElements.add( dataElementB );

        List<DataValue> dataValues = dataValueService.getDataValues( sourceA, periodA, dataElements );
        assertNotNull( dataValues );
        assertEquals( 2, dataValues.size() );

        dataValues = dataValueService.getDataValues( sourceB, periodC, dataElements );
        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() );

        dataValues = dataValueService.getDataValues( sourceD, periodC, dataElements );
        assertNotNull( dataValues );
        assertEquals( 0, dataValues.size() );
    }    

    @Test
    public void testGetDataValuesDataElementPeriodsSources()
    {
        DataValue dataValueA = new DataValue( dataElementA, periodA, sourceB, optionCombo, optionCombo );
        dataValueA.setValue( "1" );
        DataValue dataValueB = new DataValue( dataElementA, periodB, sourceA, optionCombo, optionCombo );
        dataValueB.setValue( "2" );
        DataValue dataValueC = new DataValue( dataElementA, periodA, sourceC, optionCombo, optionCombo );
        dataValueC.setValue( "3" );
        DataValue dataValueD = new DataValue( dataElementB, periodB, sourceD, optionCombo, optionCombo );
        dataValueD.setValue( "4" );
        
        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );
        
        List<Period> periods = new ArrayList<>();
        periods.add( periodA );
        periods.add( periodB );

        List<OrganisationUnit> sources = new ArrayList<>();
        sources.add( sourceA );
        sources.add( sourceB );
        
        List<DataValue> dataValues = dataValueService.getDataValues( dataElementA, periods, sources );
        
        assertEquals( dataValues.size(), 2 );
        assertTrue( dataValues.contains( dataValueA ) );
        assertTrue( dataValues.contains( dataValueB ) );
    }

    @Test
    public void testGetDataValuesOptionComboDataElementPeriodsSources()
    {
        DataValue dataValueA = new DataValue( dataElementA, periodA, sourceB, optionCombo, optionCombo );
        dataValueA.setValue( "1" );
        DataValue dataValueB = new DataValue( dataElementA, periodB, sourceA, optionCombo, optionCombo );
        dataValueB.setValue( "2" );
        DataValue dataValueC = new DataValue( dataElementA, periodA, sourceC, optionCombo, optionCombo );
        dataValueC.setValue( "3" );
        DataValue dataValueD = new DataValue( dataElementB, periodB, sourceD, optionCombo, optionCombo );
        dataValueD.setValue( "4" );
        
        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );
        
        List<Period> periods = new ArrayList<>();
        periods.add( periodA );
        periods.add( periodB );

        List<OrganisationUnit> sources = new ArrayList<>();
        sources.add( sourceA );
        sources.add( sourceB );
        
        Collection<DataValue> dataValues = dataValueService.getDataValues( dataElementA, optionCombo, periods, sources );
        
        assertEquals( dataValues.size(), 2 );
        assertTrue( dataValues.contains( dataValueA ) );
        assertTrue( dataValues.contains( dataValueB ) );
    }
}
