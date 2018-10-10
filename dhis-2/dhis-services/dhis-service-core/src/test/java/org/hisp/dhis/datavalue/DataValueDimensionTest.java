package org.hisp.dhis.datavalue;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import org.hisp.dhis.period.PeriodService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class DataValueDimensionTest
    extends DhisSpringTest
{
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    private CategoryOption male;
    private CategoryOption female;
    private CategoryOption under15;
    private CategoryOption over15;

    private Category gender;
    private Category ageGroup;

    private CategoryCombo genderAndAgeGroup;

    private CategoryOptionCombo defaultOptionCombo;

    private DataElement dataElementA;

    private Period periodA;

    private OrganisationUnit sourceA;

    @Override
    public void setUpTest()
    {
        male = new CategoryOption( "Male" );
        female = new CategoryOption( "Female" );
        under15 = new CategoryOption( "<15" );
        over15 = new CategoryOption( ">15" );

        categoryService.addCategoryOption( male );
        categoryService.addCategoryOption( female );
        categoryService.addCategoryOption( under15 );
        categoryService.addCategoryOption( over15 );

        gender = new Category( "Gender", DataDimensionType.DISAGGREGATION );
        gender.getCategoryOptions().add( male );
        gender.getCategoryOptions().add( female );

        ageGroup = new Category( "Agegroup", DataDimensionType.DISAGGREGATION );
        ageGroup.getCategoryOptions().add( under15 );
        ageGroup.getCategoryOptions().add( over15 );

        categoryService.addCategory( gender );
        categoryService.addCategory( ageGroup );

        genderAndAgeGroup = new CategoryCombo( "Gender and Agegroup", DataDimensionType.DISAGGREGATION );
        genderAndAgeGroup.getCategories().add( gender );
        genderAndAgeGroup.getCategories().add( ageGroup );

        categoryService.addCategoryCombo( genderAndAgeGroup );

        categoryService.generateOptionCombos( genderAndAgeGroup );

        dataElementA = createDataElement( 'A', genderAndAgeGroup );

        dataElementService.addDataElement( dataElementA );

        periodA = createPeriod( getDate( 2000, 1, 1 ), getDate( 2000, 2, 1 ) );

        periodService.addPeriod( periodA );

        sourceA = createOrganisationUnit( 'A' );

        organisationUnitService.addOrganisationUnit( sourceA );

        defaultOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        for ( CategoryOptionCombo categoryOptionCombo : genderAndAgeGroup.getOptionCombos() )
        {
            dataValueService.addDataValue( createDataValue( dataElementA, periodA, sourceA, "10", categoryOptionCombo, defaultOptionCombo ) );
        }
    }
}
