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
package org.hisp.dhis.webapi.controller.metadata;

import static org.hisp.dhis.web.WebClient.Body;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.webapi.json.domain.JsonImportSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MetadataImportIntegrationTest extends DhisControllerIntegrationTest
{
    @Test
    @DisplayName( "Should return error when import program with inaccessible programStage" )
    void testImportInaccessibleReference()
    {
        JsonImportSummary response = POST( "/metadata",
            Body( "metadata/test_user.json" ) ).content( HttpStatus.OK ).as( JsonImportSummary.class );
        User user = userService.getUser( "HvbPAQEyXSD" );
        assertNotNull( user );
        switchContextToUser( user );
        response = POST( "/metadata",
            Body( "metadata/program_with_inaccessible_programStage.json" ) ).content( HttpStatus.CONFLICT )
            .as( JsonImportSummary.class );
        JsonErrorReport errorReport = response.find( JsonErrorReport.class,
            error -> error.getErrorCode() == ErrorCode.E5008 );
        assertNotNull( errorReport );
        assertEquals(
            "READ access is required for reference [LloQNgtkrbt] (ProgramStage) on object test [SkV0iNXNJ2S] (Program) for association `programStage`",
            errorReport.getMessage() );
    }

    @Test
    @DisplayName( "Should return error when import program with inaccessible DataElement which is referenced by a ProgramStageDataElement" )
    void testImportInaccessibleEmbeddedReference()
    {
        POST( "/metadata",
            Body( "metadata/test_user.json" ) ).content( HttpStatus.OK ).as( JsonImportSummary.class );
        User user = userService.getUser( "HvbPAQEyXSD" );
        assertNotNull( user );
        switchContextToUser( user );
        JsonImportSummary response = POST( "/metadata",
            Body( "metadata/program_with_inaccessible_dataelement.json" ) ).content( HttpStatus.CONFLICT )
            .as( JsonImportSummary.class );
        JsonErrorReport errorReport = response.find( JsonErrorReport.class,
            error -> error.getErrorCode() == ErrorCode.E5008 );
        assertNotNull( errorReport );
        assertEquals(
            "READ access is required for reference [rFQNCGMYud2] (DataElement) on object [m69ZMmIJctI] (ProgramStageDataElement) for association `dataElement`",
            errorReport.getMessage() );
    }

    @Test
    @DisplayName( "Should return error when update Program with non-writable ProgramStage" )
    void testUpdateInaccessibleReference()
    {
        JsonImportSummary response = POST( "/metadata",
            Body( "metadata/test_user.json" ) ).content( HttpStatus.OK ).as( JsonImportSummary.class );
        User user = userService.getUser( "HvbPAQEyXSD" );
        assertNotNull( user );
        switchContextToUser( user );
        POST( "/metadata",
            Body( "metadata/program_with_readable_programStage.json" ) ).content( HttpStatus.OK )
            .as( JsonImportSummary.class );

        response = POST( "/metadata",
            Body( "metadata/update_program_with_non_writtable_programStage.json" ) ).content( HttpStatus.CONFLICT )
            .as( JsonImportSummary.class );

        JsonErrorReport errorReport = response.find( JsonErrorReport.class,
            error -> error.getErrorCode() == ErrorCode.E3001 );
        assertNotNull( errorReport );
        assertEquals(
            "User `test User testuser [HvbPAQEyXSD] (User)` is not allowed to update object `test [LloQNgtkrbt] (ProgramStage)`",
            errorReport.getMessage() );
    }
}
