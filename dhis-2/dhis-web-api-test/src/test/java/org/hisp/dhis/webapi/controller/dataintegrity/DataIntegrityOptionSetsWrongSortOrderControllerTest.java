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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonOption;
import org.hisp.dhis.webapi.json.domain.JsonOptionSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for option sets which are not used.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/option_sets/option_sets_wrong_sort_order.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOptionSetsWrongSortOrderControllerTest extends AbstractDataIntegrityIntegrationTest
{

    @Autowired
    private OptionService myOptionService;

    private String goodOptionSet;

    private static final String check = "option_sets_wrong_sort_order";

    private static final String detailsIdType = "optionSets";

    @Test
    @Disabled( "Cannot directly set the sort order of option sets" )
    void testOptionSetWrongSortOrder()
    {

        Option optionA = new Option( "Sweet", "SWEET", 1 );
        Option optionB = new Option( "Sour", "SOUR", 2 );
        Option optionC = new Option( "Salty", "SALTY", 3 );
        OptionSet optionSetA = new OptionSet( "Taste", ValueType.TEXT );
        optionSetA.addOption( optionA );
        optionSetA.addOption( optionB );
        optionSetA.addOption( optionC );
        myOptionService.saveOptionSet( optionSetA );
        optionSetA.removeOption( optionB );
        myOptionService.saveOptionSet( optionSetA );
        dbmsManager.clearSession();

        goodOptionSet = optionSetA.getUid();

        JsonObject content = GET( "/optionSets/" + goodOptionSet + "?fields=id,name,options[id,name,sortOrder" )
            .content();
        JsonOptionSet myOptionSet = content.asObject( JsonOptionSet.class );
        assertEquals( myOptionSet.getId(), goodOptionSet );

        Set<Number> sortOrders = myOptionSet.getOptions().stream().map( JsonOption::getSortOrder )
            .collect( Collectors.toSet() );
        Set<Integer> expectedSortOrders = Set.of( 1, 4 );
        assertEquals( expectedSortOrders, sortOrders );

        assertHasDataIntegrityIssues( detailsIdType, check, 100, goodOptionSet, "Taste", "4 != 2", true );
    }

    @Test
    void testOptionSetRightSortOrder()
    {

        goodOptionSet = assertStatus( HttpStatus.CREATED,
            POST( "/optionSets",
                "{ 'name': 'Taste', 'shortName': 'Taste', 'valueType' : 'TEXT' }" ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/options",
                "{ 'code': 'SWEET'," +
                    "  'sortOrder': 1," +
                    "  'name': 'Sweet'," +
                    "  'optionSet': { " +
                    "    'id': '" + goodOptionSet + "'" +
                    "  }}" ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/options",
                "{ 'code': 'SOUR'," +
                    "  'sortOrder': 2," +
                    "  'name': 'Sour'," +
                    "  'optionSet': { " +
                    "    'id': '" + goodOptionSet + "'" +
                    "  }}" ) );

        JsonObject content = GET( "/optionSets/" + goodOptionSet + "?fields=id,name,options[id,name,sortOrder" )
            .content();
        JsonOptionSet myOptionSet = content.asObject( JsonOptionSet.class );
        assertEquals( myOptionSet.getId(), goodOptionSet );

        Set<Number> sortOrders = myOptionSet.getOptions().stream().map( JsonOption::getSortOrder )
            .collect( Collectors.toSet() );
        Set<Number> expectedSortOrders = Set.of( 1, 2 );
        assertEquals( expectedSortOrders, sortOrders );

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );
    }

    @Test
    void testInvalidCategoriesDivideByZero()
    {

        assertHasNoDataIntegrityIssues( detailsIdType, check, false );

    }

}
