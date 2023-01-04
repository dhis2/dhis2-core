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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;

import java.util.Set;

import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Tests the metadata check for categories with the same category options.
 *
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/categories/categories_same_category_options.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityCategoriesDuplicatedControllerTest extends AbstractDataIntegrityIntegrationTest
{
    private final String check = "categories_same_category_options";

    private String categoryWithOptionsA;

    private String categoryWithOptionsB;

    private String categoryOptionRed;

    @Test
    void testCategoriesDuplicated()
    {

        categoryOptionRed = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Red', 'shortName': 'Red' }" ) );

        categoryWithOptionsA = assertStatus( HttpStatus.CREATED,
            POST( "/categories",
                "{ 'name': 'Color', 'shortName': 'Color', 'dataDimensionType': 'DISAGGREGATION' ," +
                    "'categoryOptions' : [{'id' : '" + categoryOptionRed + "'} ] }" ) );

        categoryWithOptionsB = assertStatus( HttpStatus.CREATED,
            POST( "/categories",
                "{ 'name': 'Colour', 'shortName': 'Colour', 'dataDimensionType': 'DISAGGREGATION' ," +
                    "'categoryOptions' : [{'id' : '" + categoryOptionRed + "'} ] }" ) );

        assertNamedMetadataObjectExists( "categories", "default" );
        assertNamedMetadataObjectExists( "categories", "Color" );
        assertNamedMetadataObjectExists( "categories", "Colour" );
        /*
         * This percentage may seem strange, but is based on the number of
         * duplicated category options
         */
        checkDataIntegritySummary( check, 1, 33, true );

        Set<String> expectedCategories = Set.of( categoryWithOptionsA, categoryWithOptionsB );
        Set<String> expectedMessages = Set.of( "(1) Colour", "(1) Color" );
        checkDataIntegrityDetailsIssues( check, expectedCategories, expectedMessages, Set.of(), "categories" );

    }

    @Test
    void testCategoriesNotDuplicated()
    {

        categoryOptionRed = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Red', 'shortName': 'Red' }" ) );

        String categoryOptionBlue = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Blue', 'shortName': 'Blue' }" ) );

        categoryWithOptionsA = assertStatus( HttpStatus.CREATED,
            POST( "/categories",
                "{ 'name': 'Color', 'shortName': 'Color', 'dataDimensionType': 'DISAGGREGATION' ," +
                    "'categoryOptions' : [{'id' : '" + categoryOptionRed + "'} ] }" ) );

        categoryWithOptionsB = assertStatus( HttpStatus.CREATED,
            POST( "/categories",
                "{ 'name': 'Colour', 'shortName': 'Colour', 'dataDimensionType': 'DISAGGREGATION' ," +
                    "'categoryOptions' : [{'id' : '" + categoryOptionBlue + "'} ] }" ) );

        assertHasNoDataIntegrityIssues( "categories", check, true );

    }

    @Test
    void testInvalidCategoriesDivideByZero()
    {

        // Expect a percentage here, since there should always be the default category
        assertHasNoDataIntegrityIssues( "categories", check, true );

    }

}