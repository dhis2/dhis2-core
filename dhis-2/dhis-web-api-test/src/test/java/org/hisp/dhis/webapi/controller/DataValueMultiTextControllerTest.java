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
package org.hisp.dhis.webapi.controller;

import static java.lang.String.format;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;

import java.util.stream.Stream;

import org.hisp.dhis.common.ValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests data value validation for
 * {@link org.hisp.dhis.common.ValueType#MULTI_TEXT}.
 *
 * @author Jan Bernitt
 */
class DataValueMultiTextControllerTest extends AbstractDataValueControllerTest
{
    private String multiTextDataElementId;

    @BeforeEach
    @Override
    public void setUp()
    {
        super.setUp();
        String optionSetId = addOptionSet( "MultiSelectSet", ValueType.MULTI_TEXT );
        addOptions( optionSetId, "A", "B", "C" );
        multiTextDataElementId = addDataElement( "MultiSelectDE", "MSDE", ValueType.MULTI_TEXT, optionSetId );
    }

    @Test
    void test()
    {
        addDataValue( "2021-01", "A,B", "", false, multiTextDataElementId, orgUnitId );
    }

    @Test
    void test2()
    {
        addDataValue( "2021-01", "A,D", "", false, multiTextDataElementId, orgUnitId );
    }

    private String addOptionSet( String name, ValueType valueType )
    {
        return assertStatus( HttpStatus.CREATED, POST( "/optionSets/",
            format( "{'name': '%s', 'valueType':'%s'}", name, valueType ) ) );
    }

    private void addOptions( String optionSet, String... codes )
    {
        Stream.of( codes ).forEach( code -> addOption( optionSet, code ) );
    }

    private void addOption( String optionSet, String code )
    {
        assertStatus( HttpStatus.CREATED, POST( "/options/",
            format( "{'name':'%s', 'code':'%s', 'optionSet':{'id':'%s'}}", code, code, optionSet ) ) );
    }
}
