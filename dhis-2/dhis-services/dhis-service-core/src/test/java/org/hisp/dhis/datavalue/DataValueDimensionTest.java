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
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class DataValueDimensionTest
    extends DhisSpringTest
{
    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OrganisationUnitService organisationUnitService;
    
    private DataElementCategoryOption male;
    private DataElementCategoryOption female;
    private DataElementCategoryOption under15;    
    private DataElementCategoryOption over15;
    
    private DataElementCategory gender;
    private DataElementCategory ageGroup;
    
    private DataElementCategoryCombo genderAndAgeGroup;
    
    private DataElementCategoryOptionCombo defaultOptionCombo;
    
    private DataElement dataElementA;
    
    private Period periodA;
    
    private OrganisationUnit sourceA;
        
    @Override
    public void setUpTest()
    {
        male = new DataElementCategoryOption( "Male" );
        female = new DataElementCategoryOption( "Female" );
        under15 = new DataElementCategoryOption( "<15" );
        over15 = new DataElementCategoryOption( ">15" );
        
        categoryService.addDataElementCategoryOption( male );
        categoryService.addDataElementCategoryOption( female );
        categoryService.addDataElementCategoryOption( under15 );
        categoryService.addDataElementCategoryOption( over15 );
        
        gender = new DataElementCategory( "Gender", DataDimensionType.DISAGGREGATION );
        gender.getCategoryOptions().add( male );
        gender.getCategoryOptions().add( female );
        
        ageGroup = new DataElementCategory( "Agegroup", DataDimensionType.DISAGGREGATION );
        ageGroup.getCategoryOptions().add( under15 );
        ageGroup.getCategoryOptions().add( over15 );
        
        categoryService.addDataElementCategory( gender );
        categoryService.addDataElementCategory( ageGroup );        
        
        genderAndAgeGroup = new DataElementCategoryCombo( "Gender and Agegroup", DataDimensionType.DISAGGREGATION );
        genderAndAgeGroup.getCategories().add( gender );
        genderAndAgeGroup.getCategories().add( ageGroup );
                
        categoryService.addDataElementCategoryCombo( genderAndAgeGroup );
        
        categoryService.generateOptionCombos( genderAndAgeGroup );

        dataElementA = createDataElement( 'A', genderAndAgeGroup );
        
        dataElementService.addDataElement( dataElementA );
        
        periodA = createPeriod( getDate( 2000, 1, 1 ), getDate( 2000, 2, 1 ) );
        
        periodService.addPeriod( periodA );
        
        sourceA = createOrganisationUnit( 'A' );
        
        organisationUnitService.addOrganisationUnit( sourceA );
        
        defaultOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();
        
        for ( DataElementCategoryOptionCombo categoryOptionCombo : genderAndAgeGroup.getOptionCombos() )
        {
            dataValueService.addDataValue( createDataValue( dataElementA, periodA, sourceA, "10", categoryOptionCombo, defaultOptionCombo ) );
        }
    }
        
    @Test
    public void testGetDimensions()
    {
        List<DataElementCategoryOption> categoryOptions = new ArrayList<>();
        categoryOptions.add( male );
        categoryOptions.add( under15 );
        
        DataElementCategoryOptionCombo categoryOptionCombo = categoryService.getDataElementCategoryOptionCombo( categoryOptions );
        
        DataValue dataValue = dataValueService.getDataValue( dataElementA, periodA, sourceA, categoryOptionCombo );
        
        assertNotNull( dataValue );
    }
    
    @Test
    public void testGetByCategoryOptionCombos()
    {
        List<DataElementCategoryOption> categoryOptions = new ArrayList<>();
        categoryOptions.add( male );
        categoryOptions.add( under15 );
        
        DataElementCategoryOptionCombo categoryOptionCombo = categoryService.getDataElementCategoryOptionCombo( categoryOptions );
        
        assertNotNull( categoryOptionCombo );
        assertEquals( genderAndAgeGroup, categoryOptionCombo.getCategoryCombo() );
        assertTrue( CollectionUtils.isEqualCollection( categoryOptions, categoryOptionCombo.getCategoryOptions() ) );
        
        categoryOptions.clear();
        categoryOptions.add( female );
        categoryOptions.add( over15 );
        
        categoryOptionCombo = categoryService.getDataElementCategoryOptionCombo( categoryOptions );
        
        assertNotNull( categoryOptionCombo );
        assertEquals( genderAndAgeGroup, categoryOptionCombo.getCategoryCombo() );
        assertTrue( CollectionUtils.isEqualCollection( categoryOptions, categoryOptionCombo.getCategoryOptions() ) );
        
        categoryOptions.clear();
        categoryOptions.add( male );
        categoryOptions.add( female );
        
        categoryOptionCombo = categoryService.getDataElementCategoryOptionCombo( categoryOptions );
        
        assertNull( categoryOptionCombo );
    }
}
