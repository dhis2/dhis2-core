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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link DataValueService} API methods that are related to data
 * integrity checks.
 */
class DataValueServiceIntegrityTest extends SingleSetupIntegrationTestBase
{
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    private DataElement deA;

    private Period peA;

    private OrganisationUnit ouA;

    private CategoryCombo categoryComboAB;

    private CategoryCombo categoryComboAC;

    private Category categoryA;

    private Category categoryB;

    private CategoryOptionCombo categoryOptionComboAB;

    private CategoryOptionCombo categoryOptionComboAC;

    @Override
    protected void setUpTest()
    {
        deA = createDataElement( 'A' );
        dataElementService.addDataElement( deA );

        peA = createPeriod( "2022-07" );

        ouA = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( ouA );

        CategoryOption categoryOptionAB1 = new CategoryOption( "OptionA1" );
        categoryService.addCategoryOption( categoryOptionAB1 );
        CategoryOption categoryOptionAB2 = new CategoryOption( "OptionA2" );
        categoryService.addCategoryOption( categoryOptionAB2 );

        CategoryOption categoryOptionAC1 = new CategoryOption( "OptionB1" );
        categoryService.addCategoryOption( categoryOptionAC1 );
        CategoryOption categoryOptionAC2 = new CategoryOption( "OptionB2" );
        categoryService.addCategoryOption( categoryOptionAC2 );

        categoryA = createCategory( 'A', categoryOptionAB1, categoryOptionAB2 );
        categoryB = createCategory( 'B', categoryOptionAC1, categoryOptionAC2 );
        categoryService.addCategory( categoryA );
        categoryService.addCategory( categoryB );

        categoryComboAB = new CategoryCombo( "CategoryComboAB", DataDimensionType.DISAGGREGATION,
            List.of( categoryA, categoryB ) );
        categoryService.addCategoryCombo( categoryComboAB );

        categoryOptionComboAB = createCategoryOptionCombo( 'A' );
        categoryOptionComboAB.setCategoryCombo( categoryComboAB );
        categoryOptionComboAB.setCategoryOptions( Set.of( categoryOptionAB1, categoryOptionAB2 ) );
        categoryService.addCategoryOptionCombo( categoryOptionComboAB );

        categoryComboAC = new CategoryCombo( "CategoryComboAC", DataDimensionType.DISAGGREGATION,
            List.of( categoryA, categoryB ) );
        categoryService.addCategoryCombo( categoryComboAC );

        categoryOptionComboAC = createCategoryOptionCombo( 'B' );
        categoryOptionComboAC.setCategoryCombo( categoryComboAC );
        categoryOptionComboAC.setCategoryOptions( Set.of( categoryOptionAC1, categoryOptionAC2 ) );
        categoryService.addCategoryOptionCombo( categoryOptionComboAC );

        DataValue dataValueA = new DataValue( deA, peA, ouA, categoryOptionComboAB, null, "1" );
        dataValueService.addDataValue( dataValueA );
    }

    @Test
    void testExistsAnyValue()
    {
        assertTrue( dataValueService.existsAnyValue( categoryComboAB ) );
        assertFalse( dataValueService.existsAnyValue( categoryComboAC ) );
    }
}
