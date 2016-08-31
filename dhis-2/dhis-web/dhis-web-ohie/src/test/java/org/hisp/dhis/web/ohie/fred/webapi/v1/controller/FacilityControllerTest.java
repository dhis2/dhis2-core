package org.hisp.dhis.web.ohie.fred.webapi.v1.controller;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hamcrest.Matchers;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.web.ohie.fred.FredSpringWebTest;
import org.hisp.dhis.web.ohie.fred.webapi.v1.domain.Facility;
import org.hisp.dhis.web.ohie.fred.webapi.v1.utils.OrganisationUnitToFacilityConverter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Category( IntegrationTest.class )
public class FacilityControllerTest extends FredSpringWebTest
{
    @Autowired
    private IdentifiableObjectManager manager;

    @Test
    public void testRedirectedToV1() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( get( "/ohie" ).session( session ) ).andExpect( redirectedUrl( "/ohie/fred/v1" ) );
        mvc.perform( get( "/ohie/" ).session( session ) ).andExpect( redirectedUrl( "/ohie/fred/v1" ) );
    }

    //---------------------------------------------------------------------------------------------
    // Test GET
    //---------------------------------------------------------------------------------------------

    @Test
    public void testGetFacilitiesWithALL() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( get( "/fred/v1/facilities" ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.facilities" ).isArray() )
            .andExpect( status().isOk() );
    }

    @Test
    public void testGetFacilitiesWithModuleRights() throws Exception
    {
        MockHttpSession session = getSession( "M_dhis-web-api-fred" );

        mvc.perform( get( "/fred/v1/facilities" ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.facilities" ).isArray() )
            .andExpect( status().isOk() );
    }

    @Test
    public void testGetFacilitiesNoAccess() throws Exception
    {
        MockHttpSession session = getSession( "DUMMY" );

        mvc.perform( get( "/fred/v1/facilities" ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isForbidden() );
    }

    @Test
    public void testGetFacilitiesWithContent() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( get( "/fred/v1/facilities" ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.facilities" ).isArray() )
            .andExpect( jsonPath( "$.facilities[0].name" ).value( "OrgUnitA" ) )
            .andExpect( status().isOk() );
    }

    @Test
    public void testGetFacility404() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( get( "/fred/v1/facilities/abc123" ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.code" ).value( HttpStatus.NOT_FOUND.toString() ) )
            .andExpect( status().isNotFound() );
    }

    @Test
    public void testGetFacilityVerifyPresenceOfETag() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( get( "/fred/v1/facilities/" + organisationUnit.getUid() ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( header().string( "ETag", Matchers.notNullValue() ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() );
    }

    @Test
    public void testGetFacilityUid() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( get( "/fred/v1/facilities/" + organisationUnit.getUid() ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.uuid", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.name" ).value( "OrgUnitA" ) )
            .andExpect( jsonPath( "$.active" ).value( true ) )
            .andExpect( jsonPath( "$.createdAt", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.updatedAt", Matchers.notNullValue() ) )
            .andExpect( header().string( "ETag", Matchers.notNullValue() ) )
            .andExpect( status().isOk() );
    }

    @Test
    public void testGetFacilityUuid() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( get( "/fred/v1/facilities/" + organisationUnit.getUuid() ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.uuid", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.name" ).value( "OrgUnitA" ) )
            .andExpect( jsonPath( "$.active" ).value( true ) )
            .andExpect( jsonPath( "$.createdAt", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.updatedAt", Matchers.notNullValue() ) )
            .andExpect( header().string( "ETag", Matchers.notNullValue() ) )
            .andExpect( status().isOk() );
    }

    //---------------------------------------------------------------------------------------------
    // Test PUT
    //---------------------------------------------------------------------------------------------

    @Test
    public void testPutFacility404() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( put( "/fred/v1/facilities/INVALID_IDENTIFIER" ).content( "{}" ).session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.code" ).value( HttpStatus.NOT_FOUND.toString() ) )
            .andExpect( status().isNotFound() );
    }

    @Test
    public void testPutInvalidJsonUid() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( put( "/fred/v1/facilities/" + organisationUnit.getUid() ).content( "INVALID JSON" )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isBadRequest() );
    }

    @Test
    public void testPutInvalidJsonUuid() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( put( "/fred/v1/facilities/" + organisationUnit.getUuid() ).content( "INVALID JSON" )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isBadRequest() );
    }

    @Test
    public void testPutFacilityWithoutRequiredPropertiesUid() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( put( "/fred/v1/facilities/" + organisationUnit.getUid() ).content( "{}" )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isUnprocessableEntity() );
    }

    @Test
    public void testPutFacilityWithoutRequiredPropertiesUuid() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( put( "/fred/v1/facilities/" + organisationUnit.getUuid() ).content( "{}" )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isUnprocessableEntity() );
    }

    @Test
    public void testPutFacilityUid() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        Facility facility = new OrganisationUnitToFacilityConverter().convert( organisationUnit );
        facility.setName( "FacilityB" );
        facility.setActive( false );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( put( "/fred/v1/facilities/" + organisationUnit.getUid() ).content( objectMapper.writeValueAsString( facility ) )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.uuid", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.name" ).value( "FacilityB" ) )
            .andExpect( jsonPath( "$.active" ).value( false ) )
            .andExpect( jsonPath( "$.createdAt", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.updatedAt", Matchers.notNullValue() ) )
            .andExpect( header().string( "ETag", Matchers.notNullValue() ) )
            .andExpect( status().isOk() );
    }

    @Test
    public void testPutFacilityUuid() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        Facility facility = new OrganisationUnitToFacilityConverter().convert( organisationUnit );
        facility.setName( "FacilityB" );
        facility.setActive( false );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( put( "/fred/v1/facilities/" + organisationUnit.getUuid() ).content( objectMapper.writeValueAsString( facility ) )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.uuid", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.name" ).value( "FacilityB" ) )
            .andExpect( jsonPath( "$.active" ).value( false ) )
            .andExpect( jsonPath( "$.createdAt", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.updatedAt", Matchers.notNullValue() ) )
            .andExpect( header().string( "ETag", Matchers.notNullValue() ) )
            .andExpect( status().isOk() );
    }

    @Test
    public void testPutInvalidUuidShouldFail() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        Facility facility = new OrganisationUnitToFacilityConverter().convert( organisationUnit );
        facility.setUuid( "DUMMY_UUID" );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( put( "/fred/v1/facilities/" + organisationUnit.getUuid() ).content( objectMapper.writeValueAsString( facility ) )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isPreconditionFailed() );
    }

    // TODO: this should fail, need to figure out which code to return
    @Test
    public void testPutChangeUuidShouldFail() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnit.setUuid( "ddccbbaa-bbaa-bbaa-bbaa-ffeeddccbbaa" );
        manager.save( organisationUnit );

        Facility facility = new OrganisationUnitToFacilityConverter().convert( organisationUnit );
        facility.setUuid( "aabbccdd-aabb-aabb-aabb-aabbccddeeff" );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( put( "/fred/v1/facilities/" + organisationUnit.getUuid() ).content( objectMapper.writeValueAsString( facility ) )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() );
    }

    //---------------------------------------------------------------------------------------------
    // Test POST
    //---------------------------------------------------------------------------------------------

    @Test
    public void testPostWithoutRequiredProperties() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( post( "/fred/v1/facilities" ).content( "{}" )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isUnprocessableEntity() );
    }

    @Test
    public void testPostInvalidUuidShouldFail() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        Facility facility = new Facility( "FacilityA" );
        facility.setUuid( "DUMMY_UUID" );

        mvc.perform( post( "/fred/v1/facilities" ).content( objectMapper.writeValueAsString( facility ) )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isPreconditionFailed() );
    }

    @Test
    public void testPostName() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        Facility facility = new Facility( "FacilityA" );

        mvc.perform( post( "/fred/v1/facilities" ).content( objectMapper.writeValueAsString( facility ) )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.uuid", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.name" ).value( "FacilityA" ) )
            .andExpect( jsonPath( "$.active" ).value( true ) )
            .andExpect( jsonPath( "$.createdAt", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.updatedAt", Matchers.notNullValue() ) )
            .andExpect( header().string( "ETag", Matchers.notNullValue() ) )
            .andExpect( status().isCreated() );
    }

    @Test
    public void testPostShouldKeepUuid() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        Facility facility = new Facility( "FacilityA" );
        facility.setUuid( "aabbccdd-aabb-aabb-aabb-aabbccddeeff" );

        mvc.perform( post( "/fred/v1/facilities" ).content( objectMapper.writeValueAsString( facility ) )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.uuid" ).value( "aabbccdd-aabb-aabb-aabb-aabbccddeeff" ) )
            .andExpect( jsonPath( "$.name" ).value( "FacilityA" ) )
            .andExpect( jsonPath( "$.active" ).value( true ) )
            .andExpect( jsonPath( "$.createdAt", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.updatedAt", Matchers.notNullValue() ) )
            .andExpect( header().string( "ETag", Matchers.notNullValue() ) )
            .andExpect( status().isCreated() );
    }

    @Test
    public void testPostNameActive() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        Facility facility = new Facility( "FacilityA", false );

        mvc.perform( post( "/fred/v1/facilities" ).content( objectMapper.writeValueAsString( facility ) )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.uuid", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.name" ).value( "FacilityA" ) )
            .andExpect( jsonPath( "$.active" ).value( false ) )
            .andExpect( jsonPath( "$.createdAt", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.updatedAt", Matchers.notNullValue() ) )
            .andExpect( header().string( "ETag", Matchers.notNullValue() ) )
            .andExpect( status().isCreated() );
    }

    @Test
    public void testPostNameDuplicate() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        Facility facility = new Facility( "FacilityA" );

        mvc.perform( post( "/fred/v1/facilities" ).content( objectMapper.writeValueAsString( facility ) )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.uuid", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.name" ).value( "FacilityA" ) )
            .andExpect( jsonPath( "$.active" ).value( true ) )
            .andExpect( jsonPath( "$.createdAt", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.updatedAt", Matchers.notNullValue() ) )
            .andExpect( header().string( "ETag", Matchers.notNullValue() ) )
            .andExpect( status().isCreated() );

        mvc.perform( post( "/fred/v1/facilities" ).content( objectMapper.writeValueAsString( facility ) )
            .session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.uuid", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.name" ).value( "FacilityA" ) )
            .andExpect( jsonPath( "$.active" ).value( true ) )
            .andExpect( jsonPath( "$.createdAt", Matchers.notNullValue() ) )
            .andExpect( jsonPath( "$.updatedAt", Matchers.notNullValue() ) )
            .andExpect( header().string( "ETag", Matchers.notNullValue() ) )
            .andExpect( status().isCreated() );
    }

    @Test
    public void testPostInvalidJson() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( post( "/fred/v1/facilities" ).content( "INVALID JSON" ).session( session ).contentType( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isBadRequest() );
    }

    //---------------------------------------------------------------------------------------------
    // Test DELETE
    //---------------------------------------------------------------------------------------------

    @Test
    public void testDeleteFacility404() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( delete( "/fred/v1/facilities/INVALID_IDENTIFIER" ).session( session ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.code" ).value( HttpStatus.NOT_FOUND.toString() ) )
            .andExpect( status().isNotFound() );
    }

    @Test
    public void testDeleteFacilityUid() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( delete( "/fred/v1/facilities/" + organisationUnit.getUid() ).session( session ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() );
    }

    @Test
    public void testDeleteFacilityUuid() throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        manager.save( organisationUnit );

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( delete( "/fred/v1/facilities/" + organisationUnit.getUuid() ).session( session ) )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() );
    }

    //---------------------------------------------------------------------------------------------
    // Test VERBS
    //---------------------------------------------------------------------------------------------

    @Test
    public void testDeleteFacilities() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( delete( "/fred/v1/facilities" ).session( session ) )
            .andExpect( status().isMethodNotAllowed() );
    }

    @Test
    public void testPutFacilities() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( put( "/fred/v1/facilities" ).session( session ) )
            .andExpect( status().isMethodNotAllowed() );
    }

    @Test
    public void testPostFacilityID() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( post( "/fred/v1/facilities/1" ).session( session ) )
            .andExpect( status().isMethodNotAllowed() );
    }
}
