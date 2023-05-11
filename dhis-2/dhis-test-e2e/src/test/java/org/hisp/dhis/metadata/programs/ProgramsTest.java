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
package org.hisp.dhis.metadata.programs;

import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.JsonObject;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ProgramsTest
    extends AbstractOrgUnitAssociationTestSupport
{
    public static final String PROGRAM_UID = "Zd2rkv8FsWq";

    private LoginActions loginActions;

    private ProgramActions programActions;

    @BeforeAll
    public void beforeAll()
    {
        loginActions = new LoginActions();
        programActions = new ProgramActions();
    }

    @BeforeEach
    public void before()
    {
        loginActions.loginAsSuperUser();
    }

    @ParameterizedTest( name = "withType[{0}]" )
    @ValueSource( strings = { "WITH_REGISTRATION", "WITHOUT_REGISTRATION" } )
    public void shouldCreateProgram( String programType )
    {
        JsonObject object = programActions.buildProgram();
        object.addProperty( "programType", programType );

        ApiResponse response = programActions.post( object );

        ResponseValidationHelper.validateObjectCreation( response );
    }

    @Test
    public void testProgramOrgUnitsConnections()
    {
        super.testOrgUnitsConnections( programActions::getOrgUnitsAssociations, PROGRAM_UID );
    }
}
