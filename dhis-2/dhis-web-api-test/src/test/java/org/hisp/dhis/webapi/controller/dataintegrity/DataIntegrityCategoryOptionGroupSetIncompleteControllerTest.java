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

import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Test for metadata check for category option group sets which may not contain
 * all category options of related categories.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/categories/category_option_groups_sets_incomplete.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityCategoryOptionGroupSetIncompleteControllerTest
    extends AbstractDataIntegrityIntegrationTest
{
    private final String check = "category_option_group_sets_incomplete";

    private final String detailsIdType = "categoryOptionGroupSets";

    private String categoryOptionBlue;

    private String categoryOptionRed;

    private String categoryOptionYellow;

    @Test
    void testCategoryOptionGroupSetIncomplete()
    {

        setUpTest();

        String warmGroup = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroups",
                "{ 'name': 'Warm', 'shortName': 'Warm', 'categoryOptions' : [{'id' : '" +
                    categoryOptionRed + "'}]}" ) );

        String coldGroup = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroups",
                "{ 'name': 'Cold', 'shortName': 'Cold', 'categoryOptions' : [{'id' : '" +
                    categoryOptionBlue + "'}]}" ) );

        String testCatOptionGroupSet = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroupSets",
                "{ 'name': 'Color set', 'shortName': 'Color set', 'categoryOptionGroups' : [{'id': '" +
                    warmGroup + "'}, {'id' : '" + coldGroup + "'}]}" ) );

        assertHasDataIntegrityIssues( detailsIdType, check, 50, testCatOptionGroupSet, "Color set", "Yellow",
            true );

    }

    @Test
    void testCategoryOptionGroupComplete()
    {
        setUpTest();

        String warmGroup = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroups",
                "{ 'name': 'Warm', 'shortName': 'Warm', 'categoryOptions' : [{'id' : '" +
                    categoryOptionRed + "'}, {'id' : '" + categoryOptionYellow + "'}]}" ) );

        String coldGroup = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroups",
                "{ 'name': 'Cold', 'shortName': 'Cold', 'categoryOptions' : [{'id' : '" +
                    categoryOptionBlue + "'}]}" ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroupSets",
                "{ 'name': 'Color set', 'shortName': 'Color set', 'categoryOptionGroups' : [{'id': '" +
                    warmGroup + "'}, {'id' : '" + coldGroup + "'}]}" ) );

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );

    }

    @Test
    void testCategoryOptionsInGroupRuns()
    {

        assertHasNoDataIntegrityIssues( detailsIdType, check, false );

    }

    void setUpTest()
    {
        categoryOptionRed = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Red', 'shortName': 'Red' }" ) );

        categoryOptionBlue = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Blue', 'shortName': 'Blue' }" ) );

        categoryOptionYellow = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Yellow', 'shortName': 'Yellow' }" ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/categories",
                "{ 'name': 'Colors', 'shortName': 'Colors', 'dataDimensionType': 'DISAGGREGATION', 'categoryOptions' : [{'id' : '"
                    +
                    categoryOptionRed + "'}, {'id': '" + categoryOptionBlue + "'}, {'id' : '" +
                    categoryOptionYellow + "'}]}" ) );

    }
}