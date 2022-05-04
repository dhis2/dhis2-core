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
package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Kristian Nordal
 */
class DataValueServiceTest
    extends DhisSpringTest
{
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    // -------------------------------------------------------------------------
    // Supporting data
    // -------------------------------------------------------------------------

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private DataElement deD;

    private CategoryOptionCombo optionCombo;

    private Period peA;

    private Period peB;

    private Period peC;

    private Period peX;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private OrganisationUnit ouD;

    // -------------------------------------------------------------------------
    // Set up/tear down
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );
        deD = createDataElement( 'D' );
        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        peA = createPeriod( getDay( 5 ), getDay( 6 ) );
        peB = createPeriod( getDay( 6 ), getDay( 7 ) );
        peC = createPeriod( getDay( 7 ), getDay( 8 ) );
        peX = createPeriod( getDay( 27 ), getDay( 28 ) );
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );
        ouD = createOrganisationUnit( 'D' );
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        organisationUnitService.addOrganisationUnit( ouD );
        optionCombo = categoryService.getDefaultCategoryOptionCombo();
    }

    // -------------------------------------------------------------------------
    // Basic DataValue
    // -------------------------------------------------------------------------

    @Test
    void testAddDataValue()
    {
        DataValue dataValueA = new DataValue( deA, peA, ouA, optionCombo, optionCombo, "1" );
        DataValue dataValueB = new DataValue( deB, peA, ouA, optionCombo, optionCombo, "2" );
        DataValue dataValueC = new DataValue( deC, peC, ouA, optionCombo, optionCombo, "3" );
        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueA = dataValueService.getDataValue( deA, peA, ouA, optionCombo );
        assertNotNull( dataValueA );
        assertNotNull( dataValueA.getCreated() );
        assertEquals( ouA.getId(), dataValueA.getSource().getId() );
        assertEquals( deA, dataValueA.getDataElement() );
        assertEquals( peA, dataValueA.getPeriod() );
        assertEquals( "1", dataValueA.getValue() );
        dataValueB = dataValueService.getDataValue( deB, peA, ouA, optionCombo );
        assertNotNull( dataValueB );
        assertNotNull( dataValueB.getCreated() );
        assertEquals( ouA.getId(), dataValueB.getSource().getId() );
        assertEquals( deB, dataValueB.getDataElement() );
        assertEquals( peA, dataValueB.getPeriod() );
        assertEquals( "2", dataValueB.getValue() );
        dataValueC = dataValueService.getDataValue( deC, peC, ouA, optionCombo );
        assertNotNull( dataValueC );
        assertNotNull( dataValueC.getCreated() );
        assertEquals( ouA.getId(), dataValueC.getSource().getId() );
        assertEquals( deC, dataValueC.getDataElement() );
        assertEquals( peC, dataValueC.getPeriod() );
        assertEquals( "3", dataValueC.getValue() );
    }

    @Test
    void testUpdataDataValue()
    {
        DataValue dataValueA = new DataValue( deA, peA, ouA, optionCombo, optionCombo, "1" );
        DataValue dataValueB = new DataValue( deB, peA, ouB, optionCombo, optionCombo, "2" );
        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        assertNotNull( dataValueService.getDataValue( deA, peA, ouA, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( deB, peA, ouB, optionCombo ) );
        dataValueA.setValue( "5" );
        dataValueService.updateDataValue( dataValueA );
        dataValueA = dataValueService.getDataValue( deA, peA, ouA, optionCombo );
        assertNotNull( dataValueA );
        assertEquals( "5", dataValueA.getValue() );
        dataValueB = dataValueService.getDataValue( deB, peA, ouB, optionCombo );
        assertNotNull( dataValueB );
        assertEquals( "2", dataValueB.getValue() );
    }

    @Test
    void testDeleteAndGetDataValue()
    {
        DataValue dataValueA = new DataValue( deA, peA, ouA, optionCombo, optionCombo, "1" );
        DataValue dataValueB = new DataValue( deB, peA, ouA, optionCombo, optionCombo, "2" );
        DataValue dataValueC = new DataValue( deC, peC, ouD, optionCombo, optionCombo, "3" );
        DataValue dataValueD = new DataValue( deD, peC, ouB, optionCombo, optionCombo, "4" );
        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );
        assertNotNull( dataValueService.getDataValue( deA, peA, ouA, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( deB, peA, ouA, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( deC, peC, ouD, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( deD, peC, ouB, optionCombo ) );
        dataValueService.deleteDataValue( dataValueA );
        assertNull( dataValueService.getDataValue( deA, peA, ouA, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( deB, peA, ouA, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( deC, peC, ouD, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( deD, peC, ouB, optionCombo ) );
        dataValueService.deleteDataValue( dataValueB );
        assertNull( dataValueService.getDataValue( deA, peA, ouA, optionCombo ) );
        assertNull( dataValueService.getDataValue( deB, peA, ouA, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( deC, peC, ouD, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( deD, peC, ouB, optionCombo ) );
        dataValueService.deleteDataValue( dataValueC );
        assertNull( dataValueService.getDataValue( deA, peA, ouA, optionCombo ) );
        assertNull( dataValueService.getDataValue( deB, peA, ouA, optionCombo ) );
        assertNull( dataValueService.getDataValue( deC, peC, ouD, optionCombo ) );
        assertNotNull( dataValueService.getDataValue( deD, peC, ouB, optionCombo ) );
        dataValueService.deleteDataValue( dataValueD );
        assertNull( dataValueService.getDataValue( deA, peA, ouA, optionCombo ) );
        assertNull( dataValueService.getDataValue( deB, peA, ouA, optionCombo ) );
        assertNull( dataValueService.getDataValue( deC, peC, ouD, optionCombo ) );
        assertNull( dataValueService.getDataValue( deD, peC, ouB, optionCombo ) );
    }

    // -------------------------------------------------------------------------
    // Collections of DataValues
    // -------------------------------------------------------------------------

    @Test
    void testGetDataValuesDataExportParamsA()
    {
        DataValue dataValueA = new DataValue( deA, peA, ouA, optionCombo, optionCombo, "1" );
        DataValue dataValueB = new DataValue( deA, peA, ouB, optionCombo, optionCombo, "2" );
        DataValue dataValueC = new DataValue( deA, peB, ouA, optionCombo, optionCombo, "3" );
        DataValue dataValueD = new DataValue( deA, peB, ouB, optionCombo, optionCombo, "4" );
        DataValue dataValueE = new DataValue( deB, peA, ouA, optionCombo, optionCombo, "5" );
        DataValue dataValueF = new DataValue( deB, peA, ouB, optionCombo, optionCombo, "6" );
        DataValue dataValueG = new DataValue( deB, peB, ouA, optionCombo, optionCombo, "7" );
        DataValue dataValueH = new DataValue( deB, peB, ouB, optionCombo, optionCombo, "8" );
        DataValue dataValueI = new DataValue( deA, peC, ouA, optionCombo, optionCombo, "9" );
        DataValue dataValueJ = new DataValue( deA, peC, ouB, optionCombo, optionCombo, "10" );
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
        DataExportParams params = new DataExportParams()
            .setDataElements( Sets.newHashSet( deA ) )
            .setPeriods( Sets.newHashSet( peA, peB, peC ) )
            .setOrganisationUnits( Sets.newHashSet( ouA ) );
        List<DataValue> values = dataValueService.getDataValues( params );
        assertEquals( 3, values.size() );
        assertTrue( values.contains( dataValueA ) );
        assertTrue( values.contains( dataValueC ) );
        assertTrue( values.contains( dataValueI ) );
        params = new DataExportParams()
            .setDataElements( Sets.newHashSet( deB ) )
            .setPeriods( Sets.newHashSet( peA ) )
            .setOrganisationUnits( Sets.newHashSet( ouA, ouB ) );
        values = dataValueService.getDataValues( params );
        assertEquals( 2, values.size() );
        assertTrue( values.contains( dataValueE ) );
        assertTrue( values.contains( dataValueF ) );
    }

    @Test
    void testGetDataValuesDataExportParamsB()
    {
        DataValue dataValueA = new DataValue( deA, peA, ouA, optionCombo, optionCombo, "1" );
        DataValue dataValueB = new DataValue( deA, peA, ouB, optionCombo, optionCombo, "2" );
        DataValue dataValueC = new DataValue( deA, peB, ouA, optionCombo, optionCombo, "3" );
        DataValue dataValueD = new DataValue( deA, peB, ouB, optionCombo, optionCombo, "4" );
        DataValue dataValueE = new DataValue( deB, peA, ouA, optionCombo, optionCombo, "5" );
        DataValue dataValueF = new DataValue( deB, peA, ouB, optionCombo, optionCombo, "6" );
        DataValue dataValueG = new DataValue( deB, peB, ouA, optionCombo, optionCombo, "7" );
        DataValue dataValueH = new DataValue( deB, peB, ouB, optionCombo, optionCombo, "8" );
        DataValue dataValueI = new DataValue( deA, peC, ouA, optionCombo, optionCombo, "9" );
        DataValue dataValueJ = new DataValue( deA, peC, ouB, optionCombo, optionCombo, "10" );
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

        assertEquals( 4, dataValueService
            .getDataValues( new DataExportParams()
                .setDataElements( Sets.newHashSet( deA, deB ) )
                .setPeriods( Sets.newHashSet( peB ) )
                .setOrganisationUnits( Sets.newHashSet( ouA, ouB ) ) )
            .size() );
        assertEquals( 2,
            dataValueService
                .getDataValues( new DataExportParams()
                    .setDataElements( Sets.newHashSet( deA, deB ) )
                    .setPeriods( Sets.newHashSet( peA ) )
                    .setOrganisationUnits( Sets.newHashSet( ouB ) ) )
                .size() );
        assertEquals( 4,
            dataValueService.getDataValues( new DataExportParams()
                .setDataElements( Sets.newHashSet( deA ) )
                .setPeriods( Sets.newHashSet( peA, peC ) )
                .setOrganisationUnits( Sets.newHashSet( ouA, ouB ) ) ).size() );
        assertEquals( 4,
            dataValueService.getDataValues( new DataExportParams()
                .setDataElements( Sets.newHashSet( deB ) )
                .setPeriods( Sets.newHashSet( peA, peB ) )
                .setOrganisationUnits( Sets.newHashSet( ouA, ouB ) ) ).size() );
        assertEquals( 1,
            dataValueService
                .getDataValues( new DataExportParams()
                    .setDataElements( Sets.newHashSet( deB ) )
                    .setPeriods( Sets.newHashSet( peB ) )
                    .setOrganisationUnits( Sets.newHashSet( ouA ) ) )
                .size() );
        assertEquals( 1,
            dataValueService
                .getDataValues( new DataExportParams()
                    .setDataElements( Sets.newHashSet( deA ) )
                    .setPeriods( Sets.newHashSet( peA ) )
                    .setOrganisationUnits( Sets.newHashSet( ouB ) ) )
                .size() );
        assertEquals( 1,
            dataValueService
                .getDataValues( new DataExportParams()
                    .setDataElements( Sets.newHashSet( deA ) )
                    .setStartDate( peA.getStartDate() ).setEndDate( peA.getEndDate() )
                    .setOrganisationUnits( Sets.newHashSet( ouB ) ) )
                .size() );
    }

    @Test
    void testGetDataValuesExportParamsC()
    {
        DataValue dataValueA = new DataValue( deA, peA, ouA, optionCombo, optionCombo, "1" );
        DataValue dataValueB = new DataValue( deA, peA, ouB, optionCombo, optionCombo, "2" );
        DataValue dataValueC = new DataValue( deB, peA, ouA, optionCombo, optionCombo, "3" );
        DataValue dataValueD = new DataValue( deB, peA, ouB, optionCombo, optionCombo, "4" );
        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );

        assertEquals( 2, dataValueService
            .getDataValues( new DataExportParams()
                .setDataElements( Sets.newHashSet( deA ) )
                .setCategoryOptionCombos( Sets.newHashSet( optionCombo ) )
                .setPeriods( Sets.newHashSet( peA ) )
                .setOrganisationUnits( Sets.newHashSet( ouA, ouB ) ) )
            .size() );
    }

    @Test
    void testGetDataValuesNonExistingPeriodA()
    {

        DataValue dataValueA = new DataValue( deA, peA, ouA, optionCombo, optionCombo, "1" );
        dataValueService.addDataValue( dataValueA );

        assertEquals( 0, dataValueService
            .getDataValues( new DataExportParams()
                .setDataElements( Sets.newHashSet( deA ) )
                .setCategoryOptionCombos( Sets.newHashSet( optionCombo ) )
                .setPeriods( Sets.newHashSet( peX ) )
                .setOrganisationUnits( Sets.newHashSet( ouA ) ) )
            .size() );
    }

    @Test
    void testGetDataValuesNonExistingPeriodB()
    {
        DataValue dataValueA = new DataValue( deA, peA, ouA, optionCombo, optionCombo, "1" );
        DataValue dataValueB = new DataValue( deA, peA, ouB, optionCombo, optionCombo, "2" );
        DataValue dataValueC = new DataValue( deB, peA, ouA, optionCombo, optionCombo, "3" );
        DataValue dataValueD = new DataValue( deB, peA, ouB, optionCombo, optionCombo, "4" );
        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );

        assertEquals( 2, dataValueService
            .getDataValues( new DataExportParams()
                .setDataElements( Sets.newHashSet( deA ) )
                .setCategoryOptionCombos( Sets.newHashSet( optionCombo ) )
                .setPeriods( Sets.newHashSet( peA, peX ) )
                .setOrganisationUnits( Sets.newHashSet( ouA, ouB ) ) )
            .size() );
    }

    @Test
    void testGetAllDataValues()
    {
        DataValue dataValueA = new DataValue( deA, peA, ouA, optionCombo, optionCombo );
        dataValueA.setValue( "1" );
        DataValue dataValueB = new DataValue( deB, peA, ouA, optionCombo, optionCombo );
        dataValueB.setValue( "2" );
        DataValue dataValueC = new DataValue( deC, peC, ouD, optionCombo, optionCombo );
        dataValueC.setValue( "3" );
        DataValue dataValueD = new DataValue( deD, peC, ouB, optionCombo, optionCombo );
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
    void testGetDataValuesDataElementsPeriodsOrgUnits()
    {
        DataValue dataValueA = new DataValue( deA, peA, ouA, optionCombo, optionCombo, "1" );
        DataValue dataValueB = new DataValue( deA, peA, ouB, optionCombo, optionCombo, "2" );
        DataValue dataValueC = new DataValue( deA, peB, ouA, optionCombo, optionCombo, "3" );
        DataValue dataValueD = new DataValue( deA, peB, ouB, optionCombo, optionCombo, "4" );
        DataValue dataValueE = new DataValue( deB, peA, ouA, optionCombo, optionCombo, "5" );
        DataValue dataValueF = new DataValue( deB, peA, ouB, optionCombo, optionCombo, "6" );
        DataValue dataValueG = new DataValue( deB, peB, ouA, optionCombo, optionCombo, "7" );
        DataValue dataValueH = new DataValue( deB, peB, ouB, optionCombo, optionCombo, "8" );
        DataValue dataValueI = new DataValue( deA, peC, ouA, optionCombo, optionCombo, "9" );
        DataValue dataValueJ = new DataValue( deA, peC, ouB, optionCombo, optionCombo, "10" );
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
        assertEquals( 4, dataValueService
            .getDataValues( new DataExportParams().setDataElements( Sets.newHashSet( deA, deB ) )
                .setPeriods( Sets.newHashSet( peB ) ).setOrganisationUnits( Sets.newHashSet( ouA, ouB ) ) )
            .size() );
        assertEquals( 2,
            dataValueService
                .getDataValues( new DataExportParams().setDataElements( Sets.newHashSet( deA, deB ) )
                    .setPeriods( Sets.newHashSet( peA ) ).setOrganisationUnits( Sets.newHashSet( ouB ) ) )
                .size() );
        assertEquals( 2,
            dataValueService.getDataValues( new DataExportParams().setDataElements( Sets.newHashSet( deA ) )
                .setPeriods( Sets.newHashSet( peC ) ).setOrganisationUnits( Sets.newHashSet( ouA, ouB ) ) )
                .size() );
        assertEquals( 4,
            dataValueService.getDataValues( new DataExportParams().setDataElements( Sets.newHashSet( deA ) )
                .setPeriods( Sets.newHashSet( peA, peC ) )
                .setOrganisationUnits( Sets.newHashSet( ouA, ouB ) ) ).size() );
        assertEquals( 4,
            dataValueService.getDataValues( new DataExportParams().setDataElements( Sets.newHashSet( deB ) )
                .setPeriods( Sets.newHashSet( peA, peB ) )
                .setOrganisationUnits( Sets.newHashSet( ouA, ouB ) ) ).size() );
        assertEquals( 1,
            dataValueService
                .getDataValues( new DataExportParams().setDataElements( Sets.newHashSet( deB ) )
                    .setPeriods( Sets.newHashSet( peB ) ).setOrganisationUnits( Sets.newHashSet( ouA ) ) )
                .size() );
        assertEquals( 1,
            dataValueService
                .getDataValues( new DataExportParams().setDataElements( Sets.newHashSet( deB ) )
                    .setPeriods( Sets.newHashSet( peB ) ).setOrganisationUnits( Sets.newHashSet( ouA ) ) )
                .size() );
        assertEquals( 1,
            dataValueService
                .getDataValues( new DataExportParams().setDataElements( Sets.newHashSet( deA ) )
                    .setPeriods( Sets.newHashSet( peA ) ).setOrganisationUnits( Sets.newHashSet( ouB ) ) )
                .size() );
    }

    @Test
    void testGetDataValueCountLastUpdatedBetween()
    {
        DataValue dataValueA = new DataValue( deA, peA, ouA, optionCombo, optionCombo, "1" );
        DataValue dataValueB = new DataValue( deA, peA, ouB, optionCombo, optionCombo, "2" );
        DataValue dataValueC = new DataValue( deB, peA, ouB, optionCombo, optionCombo, "3" );
        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        assertEquals( 3, dataValueService.getDataValueCountLastUpdatedBetween( getDate( 1970, 1, 1 ), null, false ) );
        assertEquals( 3, dataValueService.getDataValueCountLastUpdatedBetween( getDate( 1970, 1, 1 ), null, true ) );
        dataValueService.deleteDataValue( dataValueC );
        assertEquals( 3, dataValueService.getDataValueCountLastUpdatedBetween( getDate( 1970, 1, 1 ), null, true ) );
        assertEquals( 2, dataValueService.getDataValueCountLastUpdatedBetween( getDate( 1970, 1, 1 ), null, false ) );
        dataValueService.deleteDataValue( dataValueB );
        assertEquals( 3, dataValueService.getDataValueCountLastUpdatedBetween( getDate( 1970, 1, 1 ), null, true ) );
        assertEquals( 1, dataValueService.getDataValueCountLastUpdatedBetween( getDate( 1970, 1, 1 ), null, false ) );
    }

    @Test
    void testVAlidateMissingDataElement()
    {
        assertIllegalQueryEx( assertThrows( IllegalQueryException.class,
            () -> dataValueService.validate( new DataExportParams()
                .setPeriods( Sets.newHashSet( peB ) )
                .setOrganisationUnits( Sets.newHashSet( ouA ) ) ) ),
            ErrorCode.E2001 );
    }

    @Test
    void testValidateMissingPeriod()
    {
        assertIllegalQueryEx( assertThrows( IllegalQueryException.class,
            () -> dataValueService.validate( new DataExportParams()
                .setDataElements( Sets.newHashSet( deA, deB ) )
                .setOrganisationUnits( Sets.newHashSet( ouB ) ) ) ),
            ErrorCode.E2002 );
    }

    @Test
    void testValidatePeriodAndStartEndDate()
    {
        assertIllegalQueryEx( assertThrows( IllegalQueryException.class,
            () -> dataValueService.validate( new DataExportParams()
                .setDataElements( Sets.newHashSet( deA, deB ) )
                .setPeriods( Sets.newHashSet( peA ) )
                .setStartDate( getDate( 2022, 1, 1 ) )
                .setEndDate( getDate( 2022, 3, 1 ) )
                .setOrganisationUnits( Sets.newHashSet( ouB ) ) ) ),
            ErrorCode.E2003 );
    }

    @Test
    void testValidateMissingOrgUnit()
    {
        assertIllegalQueryEx( assertThrows( IllegalQueryException.class,
            () -> dataValueService.validate( new DataExportParams()
                .setDataElements( Sets.newHashSet( deA, deB ) )
                .setPeriods( Sets.newHashSet( peB ) ) ) ),
            ErrorCode.E2006 );
    }
}
