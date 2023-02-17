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

import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonOption;
import org.junit.jupiter.api.Test;

/**
 * Test for option sets with no options.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/option_sets/option_sets_empty.yaml
 * }
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOptionSetNoOptionsControllerTest extends AbstractDataIntegrityIntegrationTest
{
    private static final String check = "options_sets_empty";

    private static final String detailsIdType = "optionSets";

    @Test
    void testOptionSetInvalid()
    {

        String goodOptionSet = assertStatus( HttpStatus.CREATED,
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

        String badOptionSet = assertStatus( HttpStatus.CREATED,
            POST( "/optionSets",
                "{ 'name': 'Color', 'shortName': 'Color', 'valueType' : 'TEXT' }" ) );

        JsonObject content = GET( "/" + detailsIdType + "/" + goodOptionSet ).content();
        JsonList<JsonOption> optionSetOptions = content.getList( "options", JsonOption.class );
        assertEquals( 2, optionSetOptions.size() );

        content = GET( "/" + detailsIdType + "/" + badOptionSet ).content();
        optionSetOptions = content.getList( "options", JsonOption.class );
        assertEquals( 0, optionSetOptions.size() );

        assertHasDataIntegrityIssues( detailsIdType, check, 50, badOptionSet, "Color", null,
            true );

    }

    @Test
    void testOptionSetsValid()
    {

        String goodOptionSet = assertStatus( HttpStatus.CREATED,
            POST( "/optionSets",
                "{ 'name': 'Taste', 'shortName': 'Taste', 'valueType' : 'TEXT' }" ) );

        String badOptionSet = assertStatus( HttpStatus.CREATED,
            POST( "/optionSets",
                "{ 'name': 'Color', 'shortName': 'Color', 'valueType' : 'TEXT' }" ) );

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
                    "  'sortOrder': 1," +
                    "  'name': 'Sour'," +
                    "  'optionSet': { " +
                    "    'id': '" + badOptionSet + "'" +
                    "  }}" ) );

        JsonObject content = GET( "/" + detailsIdType + "/" + goodOptionSet ).content();
        JsonList<JsonOption> optionSetOptions = content.getList( "options", JsonOption.class );
        assertEquals( 1, optionSetOptions.size() );

        content = GET( "/" + detailsIdType + "/" + badOptionSet ).content();
        optionSetOptions = content.getList( "options", JsonOption.class );
        assertEquals( 1, optionSetOptions.size() );

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );

    }

    @Test
    void testInvalidCategoriesDivideByZero()
    {

        assertHasNoDataIntegrityIssues( detailsIdType, check, false );

    }

}