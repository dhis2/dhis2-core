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
 * Test for metadata check which category options which are part of two or more
 * category option groups within a category option group set.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/data_elements/category_option_groups_excess_members.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityCategoryOptionGroupSetExcessGroupMembershipControllerTest
    extends AbstractDataIntegrityIntegrationTest
{
    private final String check = "category_options_excess_groupset_membership";

    private String categoryOptionSour;

    private String categoryOptionGroupColor;

    private String categoryOptionSweet;

    private String categoryOptionRed;

    @Test
    void testCategoryOptionInMultipleGroups()
    {

        setUpTest();

        String categoryOptionGroupTaste = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroups",
                "{ 'name': 'Taste', 'shortName': 'Taste' , 'categoryOptions' : [{'id' : '" +
                    categoryOptionSweet + "'}, {'id': '" + categoryOptionSour + "'}, {'id' : '" +
                    categoryOptionRed + "'}]}" ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroupSets",
                "{ 'name': 'Taste and Color', 'shortName': 'Taste and Color', 'categoryOptionGroups' : [{'id': '" +
                    categoryOptionGroupColor + "'}, {'id' : '" + categoryOptionGroupTaste + "'}]}" ) );

        //Total number of category options is 5 with the default
        assertHasDataIntegrityIssues( "categories", check, 20, categoryOptionRed, "Red", null,
            true );

    }

    @Test
    void testCategoryOptionsInOneGroup()
    {
        setUpTest();

        String categoryOptionGroupTaste = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroups",
                "{ 'name': 'Taste', 'shortName': 'Taste' , 'categoryOptions' : [{'id' : '" +
                    categoryOptionSweet + "'}, {'id': '" + categoryOptionSour + "'}]}" ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroupSets",
                "{ 'name': 'Taste and Color', 'shortName': 'Taste and Color', 'categoryOptionGroups' : [{'id': '" +
                    categoryOptionGroupColor + "'}, {'id' : '" + categoryOptionGroupTaste + "'}]}" ) );

        assertHasNoDataIntegrityIssues( "categories", check, true );

    }

    @Test
    void testCategoryOptionsInGroupRuns()
    {

        assertHasNoDataIntegrityIssues( "categories", check, true );

    }

    void setUpTest()
    {
        categoryOptionRed = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Red', 'shortName': 'Red' }" ) );

        String categoryOptionBlue = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Blue', 'shortName': 'Blue' }" ) );

        categoryOptionSweet = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Sweet', 'shortName': 'Sweet' }" ) );

        categoryOptionSour = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Sour', 'shortName': 'Sour' }" ) );

        categoryOptionGroupColor = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroups",
                "{ 'name': 'Color', 'shortName': 'Color', 'categoryOptions' : [{'id' : '" +
                    categoryOptionRed + "'}, {'id': '" + categoryOptionBlue + "'}]}" ) );

    }
}