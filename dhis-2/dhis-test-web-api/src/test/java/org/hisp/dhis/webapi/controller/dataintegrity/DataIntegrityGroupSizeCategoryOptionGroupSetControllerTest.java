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
 * Tests the metadata check for category option group sets which have fewer than
 * two members.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/groups/group_size_category_option_group_sets.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityGroupSizeCategoryOptionGroupSetControllerTest extends AbstractDataIntegrityIntegrationTest
{
    private static final String check = "category_option_group_sets_scarce";

    private static final String detailsIdType = "categoryOptionGroupSets";

    private String categoryOptionGroupTaste;

    @Test
    void testCategoryOptionGroupSetSizeTooLow()
    {

        setUpTest();

        String categoryOptionGroupSetOne = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroupSets",
                "{'name': 'One', 'shortName' : 'One', 'categoryOptionGroups' : [{'id' : '" + categoryOptionGroupTaste
                    + "'}]}" ) );

        String categoryOptionGroupSetNil = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroupSets",
                "{ 'name': 'Nil', 'shortName': 'Nil' }" ) );

        assertHasDataIntegrityIssues( detailsIdType, check, 66,
            Set.of( categoryOptionGroupSetOne, categoryOptionGroupSetNil ), Set.of( "One", "Nil" ), Set.of( "0", "1" ),
            true );

    }

    @Test
    void testCategoryOptionGroupSetSizeOK()
    {
        setUpTest();

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );

    }

    @Test
    void testCategoryOptionGroupSetSizeRuns()
    {

        assertHasNoDataIntegrityIssues( detailsIdType, check, false );

    }

    void setUpTest()
    {

        String categoryOptionBlue = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Blue', 'shortName': 'Blue' }" ) );

        String categoryOptionRed = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Red', 'shortName': 'Red' }" ) );

        String categoryOptionGroupColors = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroups",
                "{ 'name': 'Color', 'shortName': 'Color', 'categoryOptions' : [{'id' : '" +
                    categoryOptionRed + "'}, {'id': '" + categoryOptionBlue + "'}]}" ) );

        String categoryOptionSweet = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptions",
                "{ 'name': 'Sweet', 'shortName': 'Sweet' }" ) );

        categoryOptionGroupTaste = assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroups",
                "{ 'name': 'Taste', 'shortName': 'Taste', 'categoryOptions' : [{'id' : '" +
                    categoryOptionSweet + "'}]}" ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/categoryOptionGroupSets",
                "{'name': 'Two', 'shortName' : 'Two', 'categoryOptionGroups' : [{'id' : '" +
                    categoryOptionGroupTaste + "'},{'id' : '" + categoryOptionGroupColors + "'}]}" ) );

    }
}