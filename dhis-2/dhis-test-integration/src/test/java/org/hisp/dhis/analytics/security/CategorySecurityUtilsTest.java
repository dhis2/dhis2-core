/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.security;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.security.CategorySecurityUtils.getCategoriesWithoutRestrictions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.Test;

/**
 * A test for {@link CategorySecurityUtils} This class is a test for the method
 * {@link CategorySecurityUtils#getCategoriesWithoutRestrictions(DataQueryParams)}
 * which is used to get the categories that are not restricted by the user.
 */
class CategorySecurityUtilsTest
{

    @Test
    void testCategoryOptionsConstraints()
    {
        runTest(
            // categories in Program -> categoryCombo -> categories
            List.of( "cat1", "cat2", "cat3" ),
            // Dimensions with flag if they have items
            List.of(
                // has items
                Pair.of( "cat1", true ),
                // has no items
                Pair.of( "cat2", false ) ),
            // only categories not specified or without items should be returned
            List.of( "cat2", "cat3" ) );

    }

    @Test
    void testCategoryOptionsNoConstraints()
    {
        List<String> allCategories = List.of( "cat1", "cat2", "cat3" );
        runTest(
            // categories in Program -> categoryCombo -> categories
            allCategories,
            // when no dimensions/filters
            Collections.emptyList(),
            // all categories should be returned
            allCategories );
    }

    /**
     * The actual test implementation. It mocks the objects needed for the test
     * and runs the test.
     *
     * @param categoryUids the categories in the Program -> categoryCombo ->
     *        categories
     * @param dimensionsWithFlag the dimensions with flag if they have items
     * @param expected the expected result
     */
    private void runTest( List<String> categoryUids, List<Pair<String, Boolean>> dimensionsWithFlag,
        List<String> expected )
    {
        List<Category> categories = categoryUids.stream()
            .map( this::mockCategory )
            .collect( Collectors.toList() );

        CategoryCombo categoryCombo = mock( CategoryCombo.class );
        when( categoryCombo.getCategories() ).thenReturn( categories );

        Program program = mock( Program.class );
        when( program.getCategoryCombo() ).thenReturn( categoryCombo );
        when( program.hasNonDefaultCategoryCombo() ).thenReturn( true );

        EventQueryParams.Builder paramBuilder = new EventQueryParams.Builder().withProgram( program );

        dimensionsWithFlag.stream()
            .map( dimWithFlag -> mockDimension(
                dimWithFlag.getLeft(),
                dimWithFlag.getRight() ) )
            .forEach( paramBuilder::addDimension );

        EventQueryParams params = paramBuilder.build();

        // the actual tested method
        List<String> actual = getCategoriesWithoutRestrictions( params )
            .stream()
            .map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toList() );

        assertThat( expected, containsInAnyOrder( actual.toArray() ) );
        assertThat( actual, hasSize( expected.size() ) );
    }

    private DimensionalObject mockDimension( String dim, boolean withItem )
    {
        DimensionalObject dimension = mock( DimensionalObject.class );
        when( dimension.getDimensionType() ).thenReturn( DimensionType.CATEGORY );
        when( dimension.getUid() ).thenReturn( dim );
        when( dimension.getDimension() ).thenReturn( dim );
        if ( withItem )
        {
            when( dimension.getItems() )
                .thenReturn(
                    singletonList( mockDimensionalItemObject() ) );
        }
        return dimension;
    }

    private DimensionalItemObject mockDimensionalItemObject()
    {
        return mock( DimensionalItemObject.class );
    }

    private Category mockCategory( String uid )
    {
        Category category = mock( Category.class );
        when( category.getUid() ).thenReturn( uid );
        return category;
    }
}
