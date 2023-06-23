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
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.webapi.utils.TestUtils.getMatchingGroupFromPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonProgram;
import org.hisp.dhis.webapi.json.domain.JsonProgramIndicator;
import org.hisp.dhis.webapi.json.domain.JsonProgramRuleVariable;
import org.hisp.dhis.webapi.json.domain.JsonProgramSection;
import org.hisp.dhis.webapi.json.domain.JsonProgramStage;
import org.hisp.dhis.webapi.json.domain.JsonProgramTrackedEntityAttribute;
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

    public static final String PROGRAM_UID = "PrZMWi7rBga";

    @BeforeEach
    public void testSetup()
        throws JsonProcessingException
    {
        DataElement dataElement1 = createDataElement( 'a' );
        DataElement dataElement2 = createDataElement( 'b' );
        dataElement1.setUid( "deabcdefgha" );
        dataElement2.setUid( "deabcdefghb" );
        TrackedEntityAttribute tea1 = createTrackedEntityAttribute( 'a' );
        TrackedEntityAttribute tea2 = createTrackedEntityAttribute( 'b' );
        tea1.setUid( "TEA1nnnnnaa" );
        tea2.setUid( "TEA1nnnnnab" );
        POST( "/dataElements", jsonMapper.writeValueAsString( dataElement1 ) )
            .content( HttpStatus.CREATED );
        POST( "/dataElements", jsonMapper.writeValueAsString( dataElement2 ) )
            .content( HttpStatus.CREATED );
        POST( "/trackedEntityAttributes", jsonMapper.writeValueAsString( tea1 ) )
            .content( HttpStatus.CREATED );
        POST( "/trackedEntityAttributes", jsonMapper.writeValueAsString( tea2 ) )
            .content( HttpStatus.CREATED );

        POST( "/metadata", WebClient.Body( "program/create_program.json" ) )
            .content( HttpStatus.OK ).as( JsonWebMessage.class );
    }

    @Test
    void testCopyProgram()
    {
        JsonWebMessage response = POST( "/programs/%s/copy".formatted( PROGRAM_UID ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        String copiedProgramUid = getMatchingGroupFromPattern( response.getMessage(), "'(.*?)'", 1 );

        assertTrue( response.getMessage().contains( "Program created" ) );

        JsonProgram copiedProgram = GET( "/programs/{id}", copiedProgramUid ).content( HttpStatus.OK )
            .as( JsonProgram.class );
        JsonList<JsonProgramStage> copiedStages = copiedProgram.getProgramStages();

        assertEquals( copiedProgramUid, copiedProgram.getId() );
        assertEquals( "test program", copiedProgram.getShortName() );
        assertEquals( "Copy of test program", copiedProgram.getName() );
        assertEquals( "program description", copiedProgram.getDescription() );
        assertEquals( 1, copiedStages.size() );
        assertEquals( 2, copiedProgram.getProgramSections().size() );

        // ensure that copied stages have new uids
        Set<String> copiedStageUids = copiedStages.stream().map( JsonProgramStage::getId )
            .collect( Collectors.toSet() );
        copiedStageUids.removeAll( Set.of( "PSzMWi7rBga" ) );
        assertEquals( 1, copiedStageUids.size() );

        //check for new Program Stage Sections & Data Elements
        JsonProgramStage copiedStage = copiedStages.get( 0 );
        JsonProgramStage jsonCopiedProgramStage = GET( "/programStages/" + copiedStage.getId() )
            .content( HttpStatus.OK )
            .as( JsonProgramStage.class );

        assertEquals( 2, jsonCopiedProgramStage.getProgramStageSections().size() );
        assertEquals( 2, jsonCopiedProgramStage.getProgramStageDataElements().size() );
    }

    @Test
    void testCopyProgramWithProgramSectionsVariablesIndicatorsAttributes()
    {
        JsonWebMessage response = POST( "/programs/%s/copy".formatted( PROGRAM_UID ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        String copiedProgramUid = getMatchingGroupFromPattern( response.getMessage(), "'(.*?)'", 1 );
        assertTrue( response.getMessage().contains( "Program created" ) );

        JsonProgram copiedProgram = GET( "/programs/{id}", copiedProgramUid ).content( HttpStatus.OK )
            .as( JsonProgram.class );
        JsonList<JsonProgramSection> copiedSections = copiedProgram.getProgramSections();
        JsonList<JsonProgramIndicator> copiedIndicators = copiedProgram.getProgramIndicators();
        JsonList<JsonProgramRuleVariable> copiedRuleVariables = copiedProgram.getProgramRuleVariables();
        JsonList<JsonProgramTrackedEntityAttribute> copiedTrackedAttributes = copiedProgram
            .getProgramTrackedEntityAttributes();

        assertEquals( copiedProgramUid, copiedProgram.getId() );
        assertEquals( 2, copiedSections.size() );
        assertEquals( 2, copiedIndicators.size() );
        assertEquals( 2, copiedRuleVariables.size() );
        assertEquals( 2, copiedTrackedAttributes.size() );

        // ensure that all copied program properties have new uids
        Set<String> copiedSectionUids = copiedSections.stream().map( JsonProgramSection::getId )
            .collect( Collectors.toSet() );
        copiedSectionUids.removeAll( Set.of( "PSSzMWi7rBa", "PSSzMWi7rBb" ) );
        assertEquals( 2, copiedSectionUids.size() );

        Set<String> copiedIndicatorsUids = copiedIndicators.stream().map( JsonProgramIndicator::getId )
            .collect( Collectors.toSet() );
        copiedIndicatorsUids.removeAll( Set.of( "PInmWi7rBga", "PInmWi7rBgb" ) );
        assertEquals( 2, copiedIndicatorsUids.size() );

        Set<String> copiedRuleVariablesUids = copiedRuleVariables.stream().map( JsonProgramRuleVariable::getId )
            .collect( Collectors.toSet() );
        copiedRuleVariablesUids.removeAll( Set.of( "PRVmWi7rBga", "PRVmWi7rBgb" ) );
        assertEquals( 2, copiedRuleVariablesUids.size() );

        Set<String> copiedTrackedEntityAttributesUids = copiedTrackedAttributes.stream()
            .map( JsonProgramTrackedEntityAttribute::getId )
            .collect( Collectors.toSet() );
        copiedTrackedEntityAttributesUids.removeAll( Set.of( "PTEAmWi7rBa", "PTEAmWi7rBb" ) );
        assertEquals( 2, copiedTrackedEntityAttributesUids.size() );
    }

    @Test
    void testCopyProgramIndicatorDbConstraintsWithNoCopyOptions()
    {
        //1st copy with no copy option request param succeeds
        POST( "/programs/%s/copy".formatted( PROGRAM_UID ) )
            .content( HttpStatus.CREATED );

        //2nd copy with no copy option request param should fail
        JsonWebMessage response2 = POST( "/programs/%s/copy".formatted( PROGRAM_UID ) )
            .content( HttpStatus.INTERNAL_SERVER_ERROR )
            .as( JsonWebMessage.class );

        assertTrue( response2.getMessage().contains( "Unique index or primary key violation" ) );
        assertTrue( response2.getMessage()
            .contains( "uk_7udjng39j4ddafjn57r58v7oq_INDEX_4 ON public.programindicator" ) );
    }

    @Test
    void testCopyProgramIndicatorDbConstraintsWithCopyOptions()
    {
        //1st copy with no copy option request param succeeds
        POST( "/programs/%s/copy".formatted( PROGRAM_UID ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        //2nd copy with copy option request param won't fail as the ProgramIndicator will have new unique name & shortName
        String prefix = "zzz";
        JsonWebMessage response2 = POST( "/programs/%s/copy?prefix=%s".formatted( PROGRAM_UID, prefix ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        String secondCopiedProgramUid = getMatchingGroupFromPattern( response2.getMessage(), "'(.*?)'", 1 );
        JsonProgram copiedProgram = GET( "/programs/{id}", secondCopiedProgramUid ).content( HttpStatus.OK )
            .as( JsonProgram.class );

        String copiedIndicatorUid = copiedProgram.getProgramIndicators().get( 0 ).getId();
        JsonProgramIndicator copiedIndicator = GET( "/programIndicators/{id}", copiedIndicatorUid )
            .content( HttpStatus.OK )
            .as( JsonProgramIndicator.class );

        assertTrue( copiedIndicator.getName().startsWith( prefix ) );
        assertTrue( copiedIndicator.getShortName().startsWith( prefix ) );
    }

    @Test
    void testCopyProgramWithPrefixCopyOption()
    {
        String prefixCopyOption = "add prefix ";
        JsonWebMessage response = POST(
            "/programs/%s/copy?prefix=%s".formatted( PROGRAM_UID, prefixCopyOption ) )
            .content( HttpStatus.CREATED )
            .as( JsonWebMessage.class );

        String newUid = getMatchingGroupFromPattern( response.getMessage(), "'(.*?)'", 1 );
        assertTrue( response.getMessage().contains( "Program created" ) );

        JsonProgram newProgram = GET( "/programs/{id}", newUid ).content( HttpStatus.OK ).as( JsonProgram.class );

        assertEquals( newUid, newProgram.getId() );
        assertEquals( "test program", newProgram.getShortName() );
        assertEquals( "add prefix test program", newProgram.getName() );
        assertEquals( "program description", newProgram.getDescription() );
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

    @Test
    void testCopyProgramWithUserWithNoAuthorities()
    {
        User userWithNoAuthorities = switchToNewUser( "test1" );
        Set<String> authorities = userWithNoAuthorities.getAllAuthorities();
        assertEquals( 0, authorities.size() );

        JsonWebMessage response = POST( "/programs/%s/copy".formatted( PROGRAM_UID ) )
            .content( HttpStatus.FORBIDDEN )
            .as( JsonWebMessage.class );
        assertEquals( "You don't have write permissions for Program PrZMWi7rBga", response.getMessage() );
    }

    @Test
    void testCopyProgramWithUserWithProgramPrivateAddAuthority()
    {
        User userWithInsufficientAuthorities = switchToNewUser( "test1", "F_PROGRAM_PRIVATE_ADD" );
        Set<String> authorities = userWithInsufficientAuthorities.getAllAuthorities();
        assertEquals( 1, authorities.size() );

        JsonWebMessage response = POST( "/programs/%s/copy".formatted( PROGRAM_UID ) )
            .content( HttpStatus.FORBIDDEN )
            .as( JsonWebMessage.class );
        assertEquals( "You don't have write permissions for Program PrZMWi7rBga", response.getMessage() );
    }

    @Test
    void testCopyProgramWithUserWithProgramAuthorityOnly()
    {
        User userWithInsufficientAuthorities = switchToNewUser( "test1", "F_PROGRAM_PUBLIC_ADD" );
        Set<String> authorities = userWithInsufficientAuthorities.getAllAuthorities();
        assertEquals( 1, authorities.size() );

        assertStatus( HttpStatus.FORBIDDEN, POST( "/programs/%s/copy".formatted( PROGRAM_UID ) ) );
    }

    @Test
    void testCopyProgramWithUserWithProgramAndIndicatorAuthority()
    {
        User userWithRequiredAuthorities = switchToNewUser( "test1", "F_PROGRAM_PUBLIC_ADD",
            "F_PROGRAM_INDICATOR_PUBLIC_ADD" );
        Set<String> authorities = userWithRequiredAuthorities.getAllAuthorities();
        assertEquals( 2, authorities.size() );

        assertStatus( HttpStatus.CREATED, POST( "/programs/%s/copy".formatted( PROGRAM_UID ) ) );
    }

    @Test
    void testCopyProgramWithNoPublicSharing()
    {
        PUT( "/programs/" + PROGRAM_UID, "{\n" +
            "    'id': '" + PROGRAM_UID + "',\n" +
            "    'name': 'test program',\n" +
            "    'shortName': 'test program',\n" +
            "    'programType': 'WITH_REGISTRATION',\n" +
            "    'sharing': {\n" +
            "        'public': '--------'\n" +
            "    }\n" +
            "}" ).content( HttpStatus.OK );

        switchToNewUser( "test1", "F_PROGRAM_PUBLIC_ADD",
            "F_PROGRAM_INDICATOR_PUBLIC_ADD" );

        assertStatus( HttpStatus.NOT_FOUND, POST( "/programs/%s/copy".formatted( PROGRAM_UID ) ) );
    }
}
