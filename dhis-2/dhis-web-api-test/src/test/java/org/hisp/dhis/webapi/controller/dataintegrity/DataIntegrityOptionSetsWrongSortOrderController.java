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

import java.util.HashSet;
import java.util.stream.Collectors;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonOption;
import org.hisp.dhis.webapi.json.domain.JsonOptionSet;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataIntegrityOptionSetsWrongSortOrderController extends AbstractDataIntegrityIntegrationTest
{

    @Autowired
    private OptionService myOptionService;

    private Option optionA;

    private Option optionB;

    private Option optionC;

    private OptionSet optionSetA;

    private String goodOptionSet;

    private final static String check = "option_sets_wrong_sort_order";

    /* Skipping this test for now, until we can set the sort order directly */
    @Ignore
    void testOptionSetWrongSortOrder()
    {
        doInTransaction( () -> {

            optionA = new Option( "Sweet", "SWEET", 1 );
            optionB = new Option( "Sour", "SOUR", 2 );
            optionC = new Option( "Salty", "SALTY", 3 );

            optionSetA = new OptionSet( "Taste", ValueType.TEXT );
            optionSetA.addOption( optionA );
            optionSetA.addOption( optionB );
            optionSetA.addOption( optionC );
            myOptionService.saveOptionSet( optionSetA );

            optionSetA.removeOption( optionB );
            myOptionService.saveOptionSet( optionSetA );

            dbmsManager.clearSession();
        } );

        goodOptionSet = optionSetA.getUid();

        JsonObject content = GET( "/optionSets/" + goodOptionSet + "?fields=id,name,options[id,name,sortOrder" )
            .content();
        JsonOptionSet myOptionSet = content.asObject( JsonOptionSet.class );
        assertEquals( myOptionSet.getId(), goodOptionSet );
        JsonList<JsonOption> optionSetOptions = content.getList( "options", JsonOption.class );

        var sortOrders = myOptionSet.getOptions().stream().map( e -> e.getSortOrder() ).collect( Collectors.toSet() );
        var expectedSortOrders = new HashSet<>();
        expectedSortOrders.add( 1 );
        expectedSortOrders.add( 4 );
        assertEquals( expectedSortOrders, sortOrders );

        assertHasDataIntegrityIssues( "option_sets", check, 100, goodOptionSet, "Taste", "4 != 2", true );
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
        JsonList<JsonOption> optionSetOptions = content.getList( "options", JsonOption.class );

        var sortOrders = myOptionSet.getOptions().stream().map( e -> e.getSortOrder() ).collect( Collectors.toSet() );
        var expectedSortOrders = new HashSet<>();
        expectedSortOrders.add( 1 );
        expectedSortOrders.add( 2 );
        assertEquals( expectedSortOrders, sortOrders );

        assertHasNoDataIntegrityIssues( "option_sets", check, true );
    }

    @Test
    void testInvalidCategoriesDivideByZero()
    {

        assertHasNoDataIntegrityIssues( "option_sets", check, false );

    }

    private void tearDown()
    {

        deleteMetadataObject( "optionSets", goodOptionSet );

    }

    @AfterEach
    @BeforeEach
    void setUp()
    {
        tearDown();

    }

}
