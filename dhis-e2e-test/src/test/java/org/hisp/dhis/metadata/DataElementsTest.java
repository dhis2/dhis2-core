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
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class DataElementsTest
    extends ApiTest
{
    private RestApiActions dataElementActions;

    private RestApiActions categoryComboActions;

    private LoginActions loginActions;

    private Stream<Arguments> getDataElementCombinations()
    {
        return Stream.of( new Arguments[] {
            Arguments.of( "AGGREGATE", "NUMBER", "SUM", false, null ),
            Arguments.of( "TRACKER", "TEXT", "CUSTOM", true, "DISAGGREGATION" ),
            Arguments.of( "TRACKER", "AGE", "NONE", true, "ATTRIBUTE" )
        } );
    }

    @BeforeAll
    public void beforeAll()
    {
        dataElementActions = new RestApiActions( "/dataElements" );
        categoryComboActions = new RestApiActions( "/categoryCombos" );
        loginActions = new LoginActions();

        loginActions.loginAsDefaultUser();
    }

    @ParameterizedTest
    @MethodSource( "getDataElementCombinations" )
    public void dataElement_create( String domainType, String valueType, String aggregationType, boolean withCategoryCombo,
        String categoryComboDimensionType )
    {
        JsonObject body = generateBaseBody();
        body.addProperty( "domainType", domainType );
        body.addProperty( "valueType", valueType );
        body.addProperty( "aggregationType", aggregationType );

        if ( withCategoryCombo )
        {
            String categoryComboId = createCategoryCombo( categoryComboDimensionType );

            JsonObject categoryCombo = new JsonObject();
            categoryCombo.addProperty( "id", categoryComboId );

            body.add( "categoryCombo", categoryCombo );
        }

        ApiResponse response = dataElementActions.post( body );

        ResponseValidationHelper.validateObjectCreation( response );
    }

    private JsonObject generateBaseBody()
    {
        JsonObject object = new JsonObject();
        object.addProperty( "name", DataGenerator.randomEntityName() );
        object.addProperty( "shortName", DataGenerator.randomEntityName() );

        return object;
    }

    public String createCategoryCombo( String dimensionType )
    {
        JsonObject body = new JsonObject();
        body.addProperty( "name", DataGenerator.randomEntityName() );
        body.addProperty( "dataDimensionType", dimensionType );

        return categoryComboActions.create( body );
    }
}
