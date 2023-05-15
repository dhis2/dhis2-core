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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonErrorReport;
import org.junit.jupiter.api.Test;

/**
 * Tests the
 * {@link org.hisp.dhis.webapi.controller.event.ProgramRuleActionController}
 * using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class ProgramRuleActionControllerTest extends DhisControllerConvenienceTest
{

    @Test
    void testGetDataExpressionDescription()
    {
        String pId = assertStatus( HttpStatus.CREATED,
            POST( "/programs/", "{'name':'P1', 'shortName':'P1', 'programType':'WITHOUT_REGISTRATION'}" ) );
        assertWebMessage( "OK", 200, "OK", "Valid",
            POST( "/programRuleActions/data/expression/description?programId=" + pId, "70" ).content( HttpStatus.OK ) );
    }

    @Test
    void testGetDataExpressionDescription_NoSuchProgram()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Expression is not valid",
            POST( "/programRuleActions/data/expression/description?programId=xyz", "70" )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testGetDataExpressionDescription_WithInvalidExpression()
    {
        String pId = assertStatus( HttpStatus.CREATED,
            POST( "/programs/", "{'name':'P1', 'shortName':'P1', 'programType':'WITHOUT_REGISTRATION'}" ) );
        assertWebMessage( "Conflict", 409, "ERROR", "Expression is not valid",
            POST( "/programRuleActions/data/expression/description?programId=" + pId, "1 + " )
                .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testSaveActionWithIrrelevantReferenceObjects()
    {
        Program program = createProgram( 'A' );
        manager.save( program );
        DataElement dataElement = createDataElement( 'A' );
        manager.save( dataElement );
        ProgramStage programStage = createProgramStage( 'A', program );
        programStage.addDataElement( dataElement, 0 );
        programStage.setProgram( program );
        manager.save( programStage );
        ProgramRule programRule = createProgramRule( 'A', program );
        manager.save( programRule );

        String programRuleAction = "{ 'programRule':{'id':'" + programRule.getUid() + "'}, " +
            "'programRuleActionType': 'HIDEPROGRAMSTAGE', " +
            "'dataElement':{'id':'" + dataElement.getUid() + "'}, " +
            "'programStage': {'id':'" + programStage.getUid() + "'} }";

        JsonErrorReport error = POST( "/programRuleActions", programRuleAction ).content( HttpStatus.CONFLICT ).find(
            JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E4058 );
        assertNotNull( error );
        assertEquals(
            "Program Rule `ProgramRuleA` with Action Type `HIDEPROGRAMSTAGE` has irrelevant reference objects",
            error.getMessage() );
    }
}
