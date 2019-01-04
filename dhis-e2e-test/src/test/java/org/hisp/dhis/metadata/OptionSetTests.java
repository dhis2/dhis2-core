/*
 * Copyright (c) 2004-2018, University of Oslo
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

/*
 * Copyright (c) 2004-2018, University of Oslo
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

package org.hisp.dhis.metadata;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.OptionActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OptionSetTests
    extends ApiTest
{
    private OptionActions actions = new OptionActions();

    private LoginActions loginActions = new LoginActions();

    @BeforeEach
    public void beforeEach()
    {
        loginActions.loginAsDefaultUser();
    }

    @Test
    public void optionSet_remove_withAssociatedData()
    {
        String optionSetId = createOptionSet();
        createOption( optionSetId );

        ApiResponse response = actions.optionSetActions.delete( optionSetId );

        ResponseValidationHelper.validateObjectRemoval( response, "Option set was not deleted" );

        response = actions.optionSetActions.get( optionSetId );
        assertEquals( 404, response.statusCode(), "Deleted option set still accessible!" );
    }

    @Test
    public void optionSet_associate_withOption()
    {
        String id = createOptionSet();
        String optionId = createOption( id );

        ApiResponse response = actions.optionSetActions.get( id );

        assertEquals( 200, response.statusCode() );
        assertEquals( optionId, response.extractString( "options.id[0]" ), "Option reference was not found in option set" );
    }

    @Test
    public void optionSet_addOptions()
    {
        String option1 = createOption( null );
        String option2 = createOption( null );

        String optionSetId = createOptionSet( option1, option2 );

        ApiResponse response = actions.optionSetActions.get( optionSetId );

        assertEquals( 200, response.statusCode() );
        assertEquals( option1, response.extractString( "options.id[0]" ) );
        assertEquals( option2, response.extractString( "options.id[1]" ) );
    }

    @Test
    public void optionSet_removeOption()
    {
        String option1 = createOption( null );
        String optionSetId = createOptionSet( option1 );

        ApiResponse response = actions.optionSetActions.get( optionSetId );
        JsonObject object = response.getBody();
        object.remove( "options" );

        response = actions.optionSetActions.update( optionSetId, object );
        assertEquals( 200, response.statusCode() );

        response = actions.optionSetActions.get( optionSetId );
        assertEquals( 200, response.statusCode() );
        assertEquals( 0, response.extractList( "options" ).size(), "Option was not removed" );
    }

    @Test
    public void optionSet_remove()
    {
        String id = createOptionSet();

        ApiResponse response = actions.optionSetActions.delete( id );

        ResponseValidationHelper.validateObjectRemoval( response, "Option set was not deleted" );
    }

    private String createOptionSet( String... optionIds )
    {
        String random = DataGenerator.randomString();
        return actions.createOptionSet( "AutoTest option set " + random, "TEXT", optionIds );
    }

    /**
     * Creates an option associated with option set
     *
     * @param optionSetId UID of option set
     * @return
     */
    private String createOption( String optionSetId )
    {
        return actions.createOption( "Option name auto", "Option code auto", optionSetId );
    }
}
