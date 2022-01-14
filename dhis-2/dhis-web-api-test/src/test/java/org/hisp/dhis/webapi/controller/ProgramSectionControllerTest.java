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

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramSection;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author viet@dhis2.org
 */
public class ProgramSectionControllerTest
    extends DhisControllerConvenienceTest
{
    @Autowired
    private ObjectMapper jsonMapper;

    @Test
    public void testCreateWithProgramSection()
        throws JsonProcessingException
    {
        switchToNewUser( "test", "F_PROGRAM_PUBLIC_ADD" );
        Program program = createProgram( 'A' );

        ProgramSection programSection = createProgramSection( 'A', program );

        assertStatus( HttpStatus.CREATED,
            POST( "/programs", jsonMapper.writeValueAsString( program ) ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/programSections", jsonMapper.writeValueAsString( programSection ) ) );
    }

    @Test
    public void testWithoutAuthority()
        throws JsonProcessingException
    {
        Program program = createProgram( 'A' );
        assertStatus( HttpStatus.CREATED,
            POST( "/programs", jsonMapper.writeValueAsString( program ) ) );
        ProgramSection programSection = createProgramSection( 'A', program );

        switchToNewUser( "test" );
        assertStatus( HttpStatus.CREATED,
            POST( "/programSections", jsonMapper.writeValueAsString( programSection ) ) );
    }

    @Test
    public void testDeleteProgramSectionNoAuthority()
        throws JsonProcessingException
    {
        Program program = createProgram( 'A' );
        assertStatus( HttpStatus.CREATED,
            POST( "/programs", jsonMapper.writeValueAsString( program ) ) );
        ProgramSection programSection = createProgramSection( 'A', program );
        String programSectionId = programSection.getUid();

        assertStatus( HttpStatus.CREATED,
            POST( "/programSections", jsonMapper.writeValueAsString( programSection ) ) );

        switchToNewUser( "test" );
        assertStatus( HttpStatus.FORBIDDEN,
            DELETE( "/programSections/" + programSectionId ) );
    }

    @Test
    public void testDeleteProgramSection()
        throws JsonProcessingException
    {
        switchToNewUser( "test", "F_PROGRAM_PUBLIC_ADD", "F_PROGRAM_DELETE" );
        Program program = createProgram( 'A' );
        assertStatus( HttpStatus.CREATED,
            POST( "/programs", jsonMapper.writeValueAsString( program ) ) );
        ProgramSection programSection = createProgramSection( 'A', program );
        String programSectionId = programSection.getUid();

        assertStatus( HttpStatus.CREATED,
            POST( "/programSections", jsonMapper.writeValueAsString( programSection ) ) );

        assertStatus( HttpStatus.OK,
            DELETE( "/programSections/" + programSectionId ) );

    }
}
