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
package org.hisp.dhis.dataanalysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.common.collect.Lists;

class StdDevOutlierAnalysisServiceTest extends SingleSetupIntegrationTestBase
{
    @Autowired
    @Qualifier( "org.hisp.dhis.dataanalysis.StdDevOutlierAnalysisService" )
    private DataAnalysisService stdDevOutlierAnalysisService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataValueService dataValueService;

    private DataElement dataElementSingleQuoteName;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private DataValue dataValueA;

    private DataValue dataValueB;

    private Set<DataElement> dataElementsA = new HashSet<>();

    private CategoryCombo categoryCombo;

    private CategoryOptionCombo categoryOptionCombo;

    private Period periodA;

    private Period periodB;

    private Period periodC;

    private Period periodD;

    private Period periodE;

    private Period periodF;

    private Period periodG;

    private Period periodH;

    private Period periodI;

    private Period periodJ;

    private Date from = getDate( 1998, 1, 1 );

    private OrganisationUnit organisationUnitA;

    // ----------------------------------------------------------------------
    // Fixture
    // ----------------------------------------------------------------------
    @Override
    public void setUpTest()
    {
        categoryCombo = categoryService.getDefaultCategoryCombo();
        dataElementSingleQuoteName = createDataElement( 'A', categoryCombo );
        dataElementSingleQuoteName.setName( "DataElementA'" );
        dataElementB = createDataElement( 'B', categoryCombo );
        dataElementC = createDataElement( 'C', categoryCombo );
        dataElementD = createDataElement( 'D', categoryCombo );
        dataElementService.addDataElement( dataElementSingleQuoteName );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        dataElementsA.add( dataElementSingleQuoteName );
        dataElementsA.add( dataElementB );
        categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        periodA = createPeriod( new MonthlyPeriodType(), getDate( 2000, 3, 1 ), getDate( 2000, 3, 31 ) );
        periodB = createPeriod( new MonthlyPeriodType(), getDate( 2000, 4, 1 ), getDate( 2000, 4, 30 ) );
        periodC = createPeriod( new MonthlyPeriodType(), getDate( 2000, 5, 1 ), getDate( 2000, 5, 30 ) );
        periodD = createPeriod( new MonthlyPeriodType(), getDate( 2000, 6, 1 ), getDate( 2000, 6, 30 ) );
        periodE = createPeriod( new MonthlyPeriodType(), getDate( 2000, 7, 1 ), getDate( 2000, 7, 30 ) );
        periodF = createPeriod( new MonthlyPeriodType(), getDate( 2000, 8, 1 ), getDate( 2000, 8, 30 ) );
        periodG = createPeriod( new MonthlyPeriodType(), getDate( 2000, 9, 1 ), getDate( 2000, 9, 30 ) );
        periodH = createPeriod( new MonthlyPeriodType(), getDate( 2000, 10, 1 ), getDate( 2000, 10, 30 ) );
        periodI = createPeriod( new MonthlyPeriodType(), getDate( 2000, 11, 1 ), getDate( 2000, 11, 30 ) );
        periodJ = createPeriod( new MonthlyPeriodType(), getDate( 2000, 12, 1 ), getDate( 2000, 12, 30 ) );
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnitA );
    }

    // ----------------------------------------------------------------------
    // Business logic tests
    // ----------------------------------------------------------------------
    @Test
    void testGetFindOutliers()
    {
        dataValueA = createDataValue( dataElementSingleQuoteName, periodI, organisationUnitA, "71",
            categoryOptionCombo );
        dataValueB = createDataValue( dataElementSingleQuoteName, periodJ, organisationUnitA, "-71",
            categoryOptionCombo );
        dataValueService.addDataValue(
            createDataValue( dataElementSingleQuoteName, periodA, organisationUnitA, "5", categoryOptionCombo ) );
        dataValueService.addDataValue(
            createDataValue( dataElementSingleQuoteName, periodB, organisationUnitA, "-5", categoryOptionCombo ) );
        dataValueService.addDataValue(
            createDataValue( dataElementSingleQuoteName, periodC, organisationUnitA, "5", categoryOptionCombo ) );
        dataValueService.addDataValue(
            createDataValue( dataElementSingleQuoteName, periodD, organisationUnitA, "-5", categoryOptionCombo ) );
        dataValueService.addDataValue(
            createDataValue( dataElementSingleQuoteName, periodE, organisationUnitA, "10", categoryOptionCombo ) );
        dataValueService.addDataValue(
            createDataValue( dataElementSingleQuoteName, periodF, organisationUnitA, "-10", categoryOptionCombo ) );
        dataValueService.addDataValue(
            createDataValue( dataElementSingleQuoteName, periodG, organisationUnitA, "13", categoryOptionCombo ) );
        dataValueService.addDataValue(
            createDataValue( dataElementSingleQuoteName, periodH, organisationUnitA, "-13", categoryOptionCombo ) );
        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        double stdDevFactor = 2.0;
        List<Period> periods = new ArrayList<>();
        periods.add( periodI );
        periods.add( periodJ );
        periods.add( periodA );
        periods.add( periodE );
        List<DeflatedDataValue> values = stdDevOutlierAnalysisService.analyse( Lists.newArrayList( organisationUnitA ),
            dataElementsA, periods, stdDevFactor, from );
        DeflatedDataValue valueA = new DeflatedDataValue( dataValueA );
        DeflatedDataValue valueB = new DeflatedDataValue( dataValueB );
        assertEquals( 1, values.size() );
        assertTrue( values.contains( valueA ) );
        assertFalse( values.contains( valueB ) );
    }
}
