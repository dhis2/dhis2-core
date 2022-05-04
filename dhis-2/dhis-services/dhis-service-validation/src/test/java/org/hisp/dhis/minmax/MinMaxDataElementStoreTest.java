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
package org.hisp.dhis.minmax;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * @author Kristian Nordal
 */
class MinMaxDataElementStoreTest extends DhisSpringTest
{
    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private MinMaxDataElementStore minMaxDataElementStore;

    @Test
    void testBasic()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        DataElement deC = createDataElement( 'C' );
        DataElement deD = createDataElement( 'D' );
        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        CategoryOptionCombo optionCombo = categoryService.getDefaultCategoryOptionCombo();
        MinMaxDataElement valueA = new MinMaxDataElement(
            deA, ouA, optionCombo, 0, 100, false );
        MinMaxDataElement valueB = new MinMaxDataElement(
            deB, ouB, optionCombo, 0, 100, false );
        MinMaxDataElement valueC = new MinMaxDataElement(
            deC, ouB, optionCombo, 0, 100, false );
        MinMaxDataElement valueD = new MinMaxDataElement(
            deD, ouB, optionCombo, 0, 100, false );
        minMaxDataElementStore.save( valueA );
        long idA = valueA.getId();
        minMaxDataElementStore.save( valueB );
        minMaxDataElementStore.save( valueC );
        minMaxDataElementStore.save( valueD );

        assertNotNull( minMaxDataElementStore.get( idA ) );
        assertTrue( minMaxDataElementStore.get( idA ).getMax() == 100 );
        List<DataElement> dataElements1 = new ArrayList<>();
        dataElements1.add( deA );
        List<DataElement> dataElements2 = new ArrayList<>();
        dataElements2.add( deB );
        dataElements2.add( deC );
        dataElements2.add( deD );
        assertNotNull( minMaxDataElementStore.get( ouA, deA, optionCombo ) );
        assertNull( minMaxDataElementStore.get( ouB, deA, optionCombo ) );
        assertEquals( 1, minMaxDataElementStore.get( ouA, dataElements1 ).size() );
        assertEquals( 3, minMaxDataElementStore.get( ouB, dataElements2 ).size() );
        minMaxDataElementStore.delete( valueA );
        assertNull( minMaxDataElementStore.get( idA ) );
    }

    @Test
    void testGetBySourceDataElements()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        DataElement deC = createDataElement( 'C' );
        DataElement deD = createDataElement( 'D' );
        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        CategoryOptionCombo optionCombo = categoryService.getDefaultCategoryOptionCombo();
        MinMaxDataElement valueA = new MinMaxDataElement(
            deA, ouA, optionCombo, 0, 100, false );
        MinMaxDataElement valueB = new MinMaxDataElement(
            deB, ouA, optionCombo, 0, 100, false );
        MinMaxDataElement valueC = new MinMaxDataElement(
            deC, ouA, optionCombo, 0, 100, false );
        MinMaxDataElement valueD = new MinMaxDataElement(
            deD, ouB, optionCombo, 0, 100, false );
        minMaxDataElementStore.save( valueA );
        minMaxDataElementStore.save( valueB );
        minMaxDataElementStore.save( valueC );
        minMaxDataElementStore.save( valueD );

        List<MinMaxDataElement> values = minMaxDataElementStore.get( ouA, List.of( deA, deB ) );

        assertContainsOnly( values, valueA, valueB );
    }

    @Test
    void testQuery()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        OrganisationUnit ouB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        DataElement deC = createDataElement( 'C' );
        DataElement deD = createDataElement( 'D' );
        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        CategoryOptionCombo optionCombo = categoryService.getDefaultCategoryOptionCombo();
        MinMaxDataElement valueA = new MinMaxDataElement(
            deA, ouA, optionCombo, 0, 100, false );
        MinMaxDataElement valueB = new MinMaxDataElement(
            deB, ouB, optionCombo, 0, 100, false );
        MinMaxDataElement valueC = new MinMaxDataElement(
            deC, ouB, optionCombo, 0, 100, false );
        MinMaxDataElement valueD = new MinMaxDataElement(
            deD, ouB, optionCombo, 0, 100, false );
        minMaxDataElementStore.save( valueA );
        minMaxDataElementStore.save( valueB );
        minMaxDataElementStore.save( valueC );
        minMaxDataElementStore.save( valueD );
        MinMaxDataElementQueryParams params = new MinMaxDataElementQueryParams();
        List<String> filters = Lists.newArrayList();
        filters.add( "dataElement.id:eq:" + deA.getUid() );
        params.setFilters( filters );
        List<MinMaxDataElement> result = minMaxDataElementStore.query( params );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        params = new MinMaxDataElementQueryParams();
        filters.clear();
        filters.add( "min:eq:0" );
        params.setFilters( filters );
        result = minMaxDataElementStore.query( params );
        assertNotNull( result );
        assertEquals( 4, result.size() );
        filters.clear();
        filters.add( "dataElement.id:in:[" + deA.getUid() + "," + deB.getUid() + "]" );
        params.setFilters( filters );
        result = minMaxDataElementStore.query( params );
        assertNotNull( result );
        assertEquals( 2, result.size() );
    }
}
