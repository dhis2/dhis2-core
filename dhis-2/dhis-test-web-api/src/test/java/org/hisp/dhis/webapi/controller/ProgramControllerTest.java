/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.feedback.ErrorCode.E1005;
import static org.hisp.dhis.webapi.utils.TestUtils.getMatchingGroupFromPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonProgram;
import org.hisp.dhis.webapi.json.domain.JsonProgramStage;
import org.hisp.dhis.webapi.json.domain.JsonProgramStageDataElement;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;

/**
 * @author David Mackessy
 */
class ProgramControllerTest extends DhisControllerConvenienceTest
{

    @Test
    void testCopyProgramWith2ProgramStages()
    {
        String originalProgramUid = "VoZMWi7rBga";
        String originalStageUid1 = "VoZMWi7rBgb";
        String originalStageUid2 = "VoZMWi7rBgc";
        POST( "/metadata/",
            "{'programs':[{'name':'test program name', 'id':'%s', 'description':'program description', 'shortName':'test program short name','programType':'WITH_REGISTRATION','programStages':[{'id':'%s'},{'id':'%s'}] }],'programStages':[{'id':'%s','name':'test programStage1'},{'id':'%s','name':'test programStage2'}]}"
                .formatted( originalProgramUid, originalStageUid1, originalStageUid2, originalStageUid1,
                    originalStageUid2 ) )
            .content( HttpStatus.OK );
        JsonWebMessage response = POST( "/programs/%s/copy".formatted( originalProgramUid ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        String newUid = getMatchingGroupFromPattern( response.getMessage(), "'(.*?)'", 1 );
        assertTrue( response.getMessage().contains( "Program created" ) );

        JsonProgram newProgram = GET( "/programs/{id}", newUid ).content( HttpStatus.OK ).as( JsonProgram.class );
        JsonList<JsonProgramStage> stages = newProgram.getProgramStages();

        assertEquals( newUid, newProgram.getUid() );
        assertEquals( "test program short name", newProgram.getShortName() );
        assertEquals( "Copy of test program name", newProgram.getName() );
        assertEquals( "program description", newProgram.getDescription() );
        assertEquals( 2, newProgram.getProgramStages().size() );

        // ensure that copied stages have new uids
        Set<String> stageUids = stages.stream().map( JsonProgramStage::getUid ).collect( Collectors.toSet() );
        stageUids.removeAll( Set.of( originalStageUid1, originalStageUid2 ) );
        assertEquals( 2, stageUids.size() );
    }

    @Test
    void testCopyProgramWithProgramStageDataElements()
    {
        String originalProgramUid = "abcMWi7rBga";
        String originalStageUid1 = "VoZMWi7rBgb";
        String psde1 = "uoZMWi7rBd1";
        String psde2 = "uoZMWi7rBd2";
        String dataEl1 = "poZMWi7rBd1";
        String dataEl2 = "poZMWi7rBd2";

        //create data elements to use later
        String dataElementCreate = """
            {"dataElements":[{"id":"%s","name":"test dataElement1","aggregationType":"NONE","domainType":"AGGREGATE","valueType":"TEXT","shortName":"datael1"},{"id":"%s","name":"test dataElement12","aggregationType":"SUM","domainType":"TRACKER","valueType":"INTEGER","shortName":"datael2"}]}
            """
            .formatted( dataEl1, dataEl2 );
        POST( "/metadata", dataElementCreate ).content( HttpStatus.OK );

        //create program with stage
        String program = """
            {"programs":[{"name":"test program azz","id":"%s","shortName":"local test program","programType": "WITH_REGISTRATION","programStages":[{"id":"%s"}]}],"programStages":[{"id":"%s","name":"test programStage azz"}]}
            """
            .formatted( originalProgramUid, originalStageUid1, originalStageUid1 );
        POST( "/metadata", program ).content( HttpStatus.OK );

        //create program stage data elements
        String programStageDataElements = """
            {"programStages":[{"id":"%s","name":"test program stage","program":{"id":"%s"},"programStageDataElements":[{"id":"%s","dataElement":{"id":"%s","displayName":"Age in years","valueType":"INTEGER"},"programStage":{"id":"%s"},"sortOrder":1},{"id":"%s","dataElement":{"id":"%s","displayName":"Year born","valueType":"INTEGER"},"programStage":{"id":"%s"},"sortOrder":2}]}]}
            """
            .formatted( originalStageUid1, originalProgramUid, psde1, dataEl1, originalStageUid1, psde2, dataEl2,
                originalStageUid1 );
        POST( "/metadata", programStageDataElements ).content( HttpStatus.OK );

        //copy program
        JsonWebMessage response = POST( "/programs/%s/copy".formatted( originalProgramUid ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        String newUid = getMatchingGroupFromPattern( response.getMessage(), "'(.*?)'", 1 );
        JsonProgram newProgram = GET( "/programs/{id}", newUid ).content( HttpStatus.OK ).as( JsonProgram.class );
        JsonProgramStage stage = newProgram.getProgramStages().get( 0 );

        JsonProgramStage jsonProgramStage = GET( "/programStages/" + stage.getUid() ).content( HttpStatus.OK )
            .as( JsonProgramStage.class );
        assertEquals( 2, jsonProgramStage.getProgramStageDataElements().size() );

        // ensure that copied stage data elements have new uids
        Set<String> stageDataElUids = jsonProgramStage.getProgramStageDataElements().stream()
            .map( JsonProgramStageDataElement::getUid )
            .collect( Collectors.toSet() );
        stageDataElUids.removeAll( Set.of( psde1, psde2 ) );
        assertEquals( 2, stageDataElUids.size() );
    }

    @Test
    void testCopyProgramWithInvalidUid()
    {
        String invalidProgramUid = "invalid";
        JsonWebMessage response = POST( "/programs/%s/copy".formatted( invalidProgramUid ) )
            .content( HttpStatus.NOT_FOUND )
            .as( JsonWebMessage.class );
        assertEquals( "Not Found", response.getHttpStatus() );
        assertEquals( 404, response.getHttpStatusCode() );
        assertEquals( "ERROR", response.getStatus() );
        assertEquals( "Program with id invalid could not be found.", response.getMessage() );
        assertEquals( E1005, response.getErrorCode() );
    }
}
