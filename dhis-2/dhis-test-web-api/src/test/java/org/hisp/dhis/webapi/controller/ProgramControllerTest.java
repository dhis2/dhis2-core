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

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramSection;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonProgram;
import org.hisp.dhis.webapi.json.domain.JsonProgramSection;
import org.hisp.dhis.webapi.json.domain.JsonProgramStage;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author David Mackessy
 */
class ProgramControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private ObjectMapper jsonMapper;

    public static final String PS1_UID = "ps1abcdes9i";

    public static final String PS2_UID = "ps2abcdes9i";

    @BeforeEach
    public void testSetup()
        throws JsonProcessingException
    {

        OrganisationUnit orgUnit = createOrganisationUnit( "Org 1 test" );
        POST( "/organisationUnits", jsonMapper.writeValueAsString( orgUnit ) );

        Program orignalProgram = createProgram( 'c', null, orgUnit );
        POST( "/programs", jsonMapper.writeValueAsString( orignalProgram ) );

        CategoryOption option = createCategoryOption( 'y' );
        POST( "/categoryOption", jsonMapper.writeValueAsString( option ) );

        Category category = createCategory( 'x', option );
        POST( "/category", jsonMapper.writeValueAsString( category ) );

        ProgramStage stage1 = createProgramStage( 'a', orignalProgram );
        ProgramStage stage2 = createProgramStage( 'b', orignalProgram );

        POST( "/programStages", jsonMapper.writeValueAsString( stage1 ) );
        POST( "/programStages", jsonMapper.writeValueAsString( stage2 ) );

        ProgramStageSection pss1 = createProgramStageSection( 'l', 3 );
        pss1.setProgramStage( stage1 );
        ProgramStageSection pss2 = createProgramStageSection( 'm', 4 );
        pss2.setProgramStage( stage2 );
        POST( "/programStageSections", jsonMapper.writeValueAsString( pss1 ) );
        POST( "/programStageSections", jsonMapper.writeValueAsString( pss2 ) );

        CategoryCombo categoryCombo = createCategoryCombo( 'z', category );
        POST( "/categoryCombos", jsonMapper.writeValueAsString( categoryCombo ) );

        ProgramSection ps1 = createProgramSection( 'k', orignalProgram );
        ps1.setUid( PS1_UID );
        ProgramSection ps2 = createProgramSection( 'l', orignalProgram );
        ps2.setUid( PS2_UID );
        POST( "/programSections", jsonMapper.writeValueAsString( ps1 ) );
        POST( "/programSections", jsonMapper.writeValueAsString( ps2 ) );
    }

    @Test
    void testCopyProgram()
    {
        JsonWebMessage response = POST( "/programs/%s/copy".formatted( BASE_PR_UID + 'c' ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        String copiedProgramUid = getMatchingGroupFromPattern( response.getMessage(), "'(.*?)'", 1 );
        assertTrue( response.getMessage().contains( "Program created" ) );

        JsonProgram copiedProgram = GET( "/programs/{id}", copiedProgramUid ).content( HttpStatus.OK )
            .as( JsonProgram.class );
        JsonList<JsonProgramStage> copiedStages = copiedProgram.getProgramStages();

        assertEquals( copiedProgramUid, copiedProgram.getUid() );
        assertEquals( "ProgramShortc", copiedProgram.getShortName() );
        assertEquals( "Copy of Programc", copiedProgram.getName() );
        assertEquals( "Descriptionc", copiedProgram.getDescription() );
        assertEquals( 2, copiedStages.size() );
        assertEquals( 2, copiedProgram.getProgramSections().size() );

        // ensure that copied stages have new uids
        Set<String> copiedStageUids = copiedStages.stream().map( JsonProgramStage::getUid )
            .collect( Collectors.toSet() );
        copiedStageUids.removeAll( Set.of( BASE_PG_UID + 'a', BASE_PG_UID + 'b' ) );
        assertEquals( 2, copiedStageUids.size() );

        //check for new Program Stage Sections
        JsonProgramStage copiedStage = copiedStages.get( 0 );
        JsonProgramStage jsonCopiedProgramStage = GET( "/programStages/" + copiedStage.getUid() )
            .content( HttpStatus.OK )
            .as( JsonProgramStage.class );
        assertEquals( 1, jsonCopiedProgramStage.getProgramStageSections().size() );
    }

    @Test
    void testCopyProgramWith2ProgramSections()
    {
        JsonWebMessage response = POST( "/programs/%s/copy".formatted( BASE_PR_UID + 'c' ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        String copiedProgramUid = getMatchingGroupFromPattern( response.getMessage(), "'(.*?)'", 1 );
        assertTrue( response.getMessage().contains( "Program created" ) );

        JsonProgram copiedProgram = GET( "/programs/{id}", copiedProgramUid ).content( HttpStatus.OK )
            .as( JsonProgram.class );
        JsonList<JsonProgramSection> copiedSections = copiedProgram.getProgramSections();

        assertEquals( copiedProgramUid, copiedProgram.getUid() );
        assertEquals( 2, copiedProgram.getProgramSections().size() );

        // ensure that copied program sections have new uids
        Set<String> copiedSectionUids = copiedSections.stream().map( JsonProgramSection::getUid )
            .collect( Collectors.toSet() );
        copiedSectionUids.removeAll( Set.of( PS1_UID, PS2_UID ) );
        assertEquals( 2, copiedSectionUids.size() );
    }

    @Test
    void testCopyProgramWithPrefixCopyOption()
    {
        String prefixCopyOption = "add prefix ";
        JsonWebMessage response = POST(
            "/programs/%s/copy?prefix=%s".formatted( BASE_PR_UID + 'c', prefixCopyOption ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        String newUid = getMatchingGroupFromPattern( response.getMessage(), "'(.*?)'", 1 );
        assertTrue( response.getMessage().contains( "Program created" ) );

        JsonProgram newProgram = GET( "/programs/{id}", newUid ).content( HttpStatus.OK ).as( JsonProgram.class );

        assertEquals( newUid, newProgram.getUid() );
        assertEquals( "ProgramShortc", newProgram.getShortName() );
        assertEquals( "add prefix Programc", newProgram.getName() );
        assertEquals( "Descriptionc", newProgram.getDescription() );
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
