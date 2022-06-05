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
package org.hisp.dhis.predictor;

import static java.util.Collections.emptySet;
import static org.hisp.dhis.predictor.CategoryOptionComboMapGenerator.getCcMap;
import static org.hisp.dhis.utils.Assertions.assertMapEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.MapMap;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Tests {@see CategoryOptionComboMapGenerator}.
 *
 * @author Jim Grace
 */
class CategoryOptionComboMapGeneratorTest
    extends DhisConvenienceTest
{
    @Test
    void testGetCcMap()
    {
        CategoryOption coZeroToSeven = createCategoryOption( "Zero - Seven", "ZeroToSeven" );
        CategoryOption coSevenTwelve = createCategoryOption( "Seven Twelve", "SevenTwelve" );
        CategoryOption coUnderTwelve = createCategoryOption( "Under Twelve", "UnderTwelve" );
        CategoryOption coTwelveForty = createCategoryOption( "Twelve Forty", "TwelveForty" );

        CategoryOption coFemaleGendr = createCategoryOption( "Female Gendr", "FemaleGendr" );
        CategoryOption coIsMaleGendr = createCategoryOption( "IsMale Gendr", "IsMaleGendr" );
        CategoryOption coUnknowGendr = createCategoryOption( "Unknow Gendr", "UnknowGendr" );

        Category outputAge = createCategory( 'A', coZeroToSeven, coSevenTwelve, coTwelveForty );
        Category subsetAge = createCategory( 'B', coZeroToSeven, coSevenTwelve );
        Category tooBigAge = createCategory( 'C', coZeroToSeven, coSevenTwelve, coUnderTwelve,
            coTwelveForty );

        Category gender = createCategory( 'D', coFemaleGendr, coIsMaleGendr, coUnknowGendr );

        CategoryCombo outputCombo = createCategoryCombo( "A", "OutputCombo", outputAge, gender );
        CategoryCombo sameAsCombo = createCategoryCombo( "B", "SameAsCombo", outputAge, gender );
        CategoryCombo singleCombo = createCategoryCombo( "C", "SingleCombo", gender );
        CategoryCombo subsetCombo = createCategoryCombo( "D", "SubsetCombo", gender, subsetAge );
        CategoryCombo tooBigCombo = createCategoryCombo( "E", "TooBigCombo", tooBigAge, gender );

        generateOptionCombos( outputCombo );
        generateOptionCombos( sameAsCombo );
        generateOptionCombos( singleCombo );
        generateOptionCombos( subsetCombo );
        generateOptionCombos( tooBigCombo );

        Map<String, String> expectedOutputMap = Map.of(
            "AZeroTFemal", "AZeroTFemal",
            "AZeroTIsMal", "AZeroTIsMal",
            "AZeroTUnkno", "AZeroTUnkno",
            "ASevenFemal", "ASevenFemal",
            "ASevenIsMal", "ASevenIsMal",
            "ASevenUnkno", "ASevenUnkno",
            "ATwelvFemal", "ATwelvFemal",
            "ATwelvIsMal", "ATwelvIsMal",
            "ATwelvUnkno", "ATwelvUnkno" );

        Map<String, String> expectedSameAsMap = Map.of(
            "BZeroTFemal", "AZeroTFemal",
            "BZeroTIsMal", "AZeroTIsMal",
            "BZeroTUnkno", "AZeroTUnkno",
            "BSevenFemal", "ASevenFemal",
            "BSevenIsMal", "ASevenIsMal",
            "BSevenUnkno", "ASevenUnkno",
            "BTwelvFemal", "ATwelvFemal",
            "BTwelvIsMal", "ATwelvIsMal",
            "BTwelvUnkno", "ATwelvUnkno" );

        Map<String, String> expectedSubsetMap = Map.of(
            "DFemalZeroT", "AZeroTFemal",
            "DIsMalZeroT", "AZeroTIsMal",
            "DUnknoZeroT", "AZeroTUnkno",
            "DFemalSeven", "ASevenFemal",
            "DIsMalSeven", "ASevenIsMal",
            "DUnknoSeven", "ASevenUnkno" );

        Set<CategoryCombo> inputCombos = Set.of( outputCombo, sameAsCombo, singleCombo, subsetCombo, tooBigCombo );

        MapMap<String, String, String> ccMap = getCcMap( outputCombo, inputCombos );

        assertNotNull( ccMap.get( "OutputCombo" ) );
        assertNotNull( ccMap.get( "SameAsCombo" ) );
        assertNotNull( ccMap.get( "SubsetCombo" ) );

        assertMapEquals( expectedOutputMap, ccMap.get( "OutputCombo" ) );
        assertMapEquals( expectedSameAsMap, ccMap.get( "SameAsCombo" ) );
        assertMapEquals( expectedSubsetMap, ccMap.get( "SubsetCombo" ) );

        assertEquals( 3, ccMap.size() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Creates all the category option combos needed for each category combo in
     * this unit test. Each cat option combo name and uid starts with the
     * CategoryCombo name (must be 1 character).
     */
    private void generateOptionCombos( CategoryCombo cc )
    {
        assertEquals( 1, cc.getName().length() );

        buildOptionCombos( cc, cc.getName(), cc.getName(), emptySet(), cc.getCategories(), 0 );
    }

    /**
     * Builds recursively all the category option combos for one category combo.
     * This is a quick-and-dirty method that is good enough for this test but is
     * not general-purpose. The cat option combo names are built by
     * concatenating the option names. The cat option combo uid is built by
     * concatenating the first part of the UID for each option. This will work
     * for one or two categories in the category combo (or five or ten) but not
     * for other numbers of categories in a combo.
     */
    private void buildOptionCombos( CategoryCombo cc, String name, String uid, Set<CategoryOption> cos,
        List<Category> cats, int i )
    {
        if ( i < cats.size() )
        {
            for ( CategoryOption co : cats.get( i ).getCategoryOptions() )
            {
                String buildName = name + " - " + co.getName();
                String buildUid = uid + co.getUid().substring( 0, 10 / cats.size() );
                Set<CategoryOption> buildCos = ImmutableSet.<CategoryOption> builder().addAll( cos ).add( co ).build();

                buildOptionCombos( cc, buildName, buildUid, buildCos, cats, i + 1 );
            }
        }
        else
        {
            assertEquals( 11, uid.length() );

            CategoryOptionCombo coc = new CategoryOptionCombo();
            coc.setName( name );
            coc.setUid( uid );
            coc.setCategoryOptions( cos );

            cc.getOptionCombos().add( coc );
        }
    }
}
