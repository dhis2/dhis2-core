/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.webapi.WebClient.Body;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Test for the
 * {@link org.hisp.dhis.webapi.controller.datavalue.DataValueController}.
 *
 * @author Jan Bernitt
 */
public class DataValueControllerTest extends AbstractDataValueControllerTest
{

    @Autowired
    private DataValueService dataValueService;

    @Test
    public void testSetDataValuesFollowUp_Empty()
    {
        assertEquals( "Follow-up must be specified",
            PUT( "/dataValues/followups", Body( "{}" ) ).error( HttpStatus.CONFLICT ).getMessage() );
        assertEquals( "Follow-up must be specified",
            PUT( "/dataValues/followups", Body( "{'values':null}" ) ).error( HttpStatus.CONFLICT ).getMessage() );
        assertEquals( "Follow-up must be specified",
            PUT( "/dataValues/followups", Body( "{'values':[]}" ) ).error( HttpStatus.CONFLICT ).getMessage() );
    }

    @Test
    public void testSetDataValuesFollowUp_NonExisting()
    {
        addDataValue( "2021-01", "2", null, false );
        assertEquals( "Data value does not exist",
            PUT( "/dataValues/followups", Body( String.format( "{'values':[%s]}",
                dataValueKeyJSON( "2021-02", true ) ) ) ).error( HttpStatus.CONFLICT ).getMessage() );
    }

    @Test
    public void testSetDataValuesFollowUp_Single()
    {
        addDataValue( "2021-01", "2", null, false );

        assertStatus( HttpStatus.OK, PUT( "/dataValues/followups", Body( String.format( "{'values':[%s]}",
            dataValueKeyJSON( "2021-01", true ) ) ) ) );
        assertFollowups( true );

        assertStatus( HttpStatus.OK, PUT( "/dataValues/followups", Body( String.format( "{'values':[%s]}",
            dataValueKeyJSON( "2021-01", false ) ) ) ) );
        assertFollowups( false );
    }

    @Test
    public void testSetDataValuesFollowUp_Multi()
    {
        addDataValue( "2021-01", "2", null, false );
        addDataValue( "2021-02", "3", null, false );
        addDataValue( "2021-03", "4", null, false );

        assertStatus( HttpStatus.OK, PUT( "/dataValues/followups", Body( String.format( "{'values':[%s, %s, %s]}",
            dataValueKeyJSON( "2021-01", true ),
            dataValueKeyJSON( "2021-02", true ),
            dataValueKeyJSON( "2021-03", true ) ) ) ) );

        assertFollowups( true, true, true );

        assertStatus( HttpStatus.OK, PUT( "/dataValues/followups", Body( String.format( "{'values':[%s, %s, %s]}",
            dataValueKeyJSON( "2021-01", false ),
            dataValueKeyJSON( "2021-02", true ),
            dataValueKeyJSON( "2021-03", false ) ) ) ) );

        assertFollowups( false, true, false );
    }

    private void assertFollowups( boolean... expected )
    {
        List<DataValue> values = dataValueService.getAllDataValues();
        assertEquals( expected.length, values.size() );
        int expectedTrue = 0;
        int actualTrue = 0;
        for ( int i = 0; i < expected.length; i++ )
        {
            expectedTrue += expected[i] ? 1 : 0;
            actualTrue += values.get( i ).isFollowup() ? 1 : 0;
        }
        assertEquals( "Number of values marked for followup does not match", expectedTrue, actualTrue );
    }

    private String dataValueKeyJSON( String period, boolean followup )
    {
        return String.format(
            "{'dataElement':'%s', 'period':'%s', 'orgUnit':'%s', 'categoryOptionCombo':'%s', 'followup':%b}",
            dataElementId, period, orgUnitId, categoryOptionId, followup );
    }
}
