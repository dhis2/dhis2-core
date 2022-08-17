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
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonImportConflict;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void testAddDataValue_MultiText()
    {
        assertDoesNotThrow( () -> addDataValue( "2021-01", "A,B", "", false, multiTextDataElementId, orgUnitId ) );
        JsonArray values = getDataValues( multiTextDataElementId, "2021-01", orgUnitId );
        assertEquals( 1, values.size() );
        assertEquals( "A,B", values.getString( 0 ).string() );
    }

    @Test
    void testAddDataValue_MultiText_NoSuchOption()
    {
        assertWebMessage( "Conflict", 409, "ERROR",
            format( "Data value is not a valid option of the data element option set: `%s`", multiTextDataElementId ),
            postNewDataValue( "2021-01", "A,D", "", false, multiTextDataElementId, orgUnitId )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testAddDataElement_MultiText_RequiresOptionSet()
    {
        String optionSetId = addOptionSet( "MultiSelectSet2", ValueType.TEXT );
        assertWebMessage( "Conflict", 409, "ERROR",
            "Data element value type must match option set value type: `TEXT`",
            postNewDataElement( "MultiSelectDE2", "MSDE2", ValueType.MULTI_TEXT, optionSetId )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testAddDataElement_MultiText_OptionSetValueTypeMismatch()
    {
        assertWebMessage( "Conflict", 409, "ERROR",
            "Data element of value type multi-text must have an option set: `null`",
            postNewDataElement( "MultiSelectDE2", "MSDE2", ValueType.MULTI_TEXT, null )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testPostJsonDataValueSet_MultiText_NoSuchOption()
    {
        String body = format( "{'dataValues':[{"
            + "'period':'201201',"
            + "'orgUnit':'%s',"
            + "'dataElement':'%s',"
            + "'categoryOptionCombo':'%s',"
            + "'value':'A,D'"
            + "}]}", orgUnitId, multiTextDataElementId, categoryOptionComboId );
        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One more conflicts encountered, please check import summary.",
            POST( "/38/dataValueSets/", body ).content( HttpStatus.CONFLICT ) );
        JsonImportConflict conflict = message.find( JsonImportConflict.class,
            c -> c.getErrorCode() == ErrorCode.E7621 );
        assertTrue( conflict.isObject() );
        assertEquals(
            format( "Data value is not a valid option of the data element option set: `%s`", multiTextDataElementId ),
            conflict.getValue() );
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
