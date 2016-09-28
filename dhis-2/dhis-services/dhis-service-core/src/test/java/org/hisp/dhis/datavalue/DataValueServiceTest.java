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

import com.google.common.collect.Sets;

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
    public void testGetDataValuesDataElementsPeriodsOrgUnits()
    {
        DataValue dataValueA = new DataValue( dataElementA, periodA, sourceA, optionCombo, optionCombo );
        dataValueA.setValue( "1" );
        DataValue dataValueB = new DataValue( dataElementA, periodA, sourceB, optionCombo, optionCombo );
        dataValueB.setValue( "2" );
        DataValue dataValueC = new DataValue( dataElementA, periodB, sourceA, optionCombo, optionCombo );
        dataValueC.setValue( "3" );
        DataValue dataValueD = new DataValue( dataElementA, periodB, sourceB, optionCombo, optionCombo );
        dataValueD.setValue( "4" );
        DataValue dataValueE = new DataValue( dataElementB, periodA, sourceA, optionCombo, optionCombo );
        dataValueE.setValue( "5" );
        DataValue dataValueF = new DataValue( dataElementB, periodA, sourceB, optionCombo, optionCombo );
        dataValueF.setValue( "6" );
        DataValue dataValueG = new DataValue( dataElementB, periodB, sourceA, optionCombo, optionCombo );
        dataValueG.setValue( "7" );
        DataValue dataValueH = new DataValue( dataElementB, periodB, sourceB, optionCombo, optionCombo );
        dataValueH.setValue( "8" );
        DataValue dataValueI = new DataValue( dataElementA, periodC, sourceA, optionCombo, optionCombo );
        dataValueI.setValue( "9" );
        DataValue dataValueJ = new DataValue( dataElementA, periodC, sourceB, optionCombo, optionCombo );
        dataValueJ.setValue( "1" );

        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );
        dataValueService.addDataValue( dataValueE );
        dataValueService.addDataValue( dataValueF );
        dataValueService.addDataValue( dataValueG );
        dataValueService.addDataValue( dataValueH );
        dataValueService.addDataValue( dataValueI );
        dataValueService.addDataValue( dataValueJ );

        assertEquals( 6, dataValueService.getDataValues( Sets.newHashSet( dataElementA ), Sets.newHashSet(), Sets.newHashSet() ).size() );
        assertEquals( 4, dataValueService.getDataValues( Sets.newHashSet( dataElementB ), Sets.newHashSet(), Sets.newHashSet() ).size() );
        assertEquals( 4, dataValueService.getDataValues( Sets.newHashSet(), Sets.newHashSet( periodA ), Sets.newHashSet() ).size() );
        assertEquals( 5, dataValueService.getDataValues( Sets.newHashSet(), Sets.newHashSet(), Sets.newHashSet( sourceA ) ).size() );
        
        assertEquals( 4, dataValueService.getDataValues( Sets.newHashSet( dataElementA, dataElementB ), Sets.newHashSet( periodB ), Sets.newHashSet() ).size() );
        assertEquals( 2, dataValueService.getDataValues( Sets.newHashSet( dataElementA, dataElementB ), Sets.newHashSet( periodA ), Sets.newHashSet( sourceB ) ).size() );
        
        assertEquals( 2, dataValueService.getDataValues( Sets.newHashSet( dataElementA ), Sets.newHashSet( periodC ), Sets.newHashSet() ).size() );
        
        assertEquals( 4, dataValueService.getDataValues( Sets.newHashSet( dataElementA ), Sets.newHashSet( periodA, periodC ), Sets.newHashSet() ).size() );
        assertEquals( 4, dataValueService.getDataValues( Sets.newHashSet( dataElementB ), Sets.newHashSet(), Sets.newHashSet( sourceA, sourceB ) ).size() );

        assertEquals( 1, dataValueService.getDataValues( Sets.newHashSet( dataElementB ), Sets.newHashSet( periodB ), Sets.newHashSet( sourceA ) ).size() );
        assertEquals( dataValueG, dataValueService.getDataValues( Sets.newHashSet( dataElementB ), Sets.newHashSet( periodB ), Sets.newHashSet( sourceA ) ).iterator().next() );

        assertEquals( 1, dataValueService.getDataValues( Sets.newHashSet( dataElementA ), Sets.newHashSet( periodA ), Sets.newHashSet( sourceB ) ).size() );
        assertEquals( dataValueB, dataValueService.getDataValues( Sets.newHashSet( dataElementA ), Sets.newHashSet( periodA ), Sets.newHashSet( sourceB ) ).iterator().next() );        
    }
}
