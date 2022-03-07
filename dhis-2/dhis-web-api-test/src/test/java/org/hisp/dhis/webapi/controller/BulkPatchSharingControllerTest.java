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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author viet@dhis2.org
 */
class BulkPatchSharingControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private ObjectMapper jsonMapper;

    private String userAId = "NOOF56dveaZ";

    private String userBId = "Kh68cDMwZsg";

    private String userCId;

    private String deAId = "fbfJHSPpUQD";

    private String deBId = "cYeuwXTCPkU";

    private String dsIdA = "em8Bg4LCr5k";

    @Test
    void testApplyPatchOk()
        throws IOException
    {
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "usera", userAId ) ) );
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userb", userBId ) ) );
        userCId = assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userc" ) ) );
        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements", jsonMapper.writeValueAsString( createDataElement( 'A', deAId, userCId ) ) ) );
        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements", jsonMapper.writeValueAsString( createDataElement( 'B', deBId, userCId ) ) ) );
        assertStatus( HttpStatus.OK, PATCH( "/dataElements/sharing", "patch/bulk_sharing_patch.json" ) );
        JsonIdentifiableObject saveDeA = GET( "/dataElements/{uid}", deAId ).content( HttpStatus.OK )
            .as( JsonIdentifiableObject.class );
        JsonIdentifiableObject saveDeB = GET( "/dataElements/{uid}", deBId ).content( HttpStatus.OK )
            .as( JsonIdentifiableObject.class );
        assertEquals( 2, saveDeA.getSharing().getUsers().size() );
        assertEquals( 2, saveDeB.getSharing().getUsers().size() );
    }

    /**
     * Payload contains two DataElements but only one exists in database.
     *
     * @throws IOException
     */
    @Test
    void testApplyPatchWithInvalidUid()
        throws IOException
    {
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "usera", userAId ) ) );
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userb", userBId ) ) );
        userCId = assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userc" ) ) );
        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements", toJsonString( createDataElement( 'B', deBId, userCId ) ) ) );
        HttpResponse response = PATCH( "/dataElements/sharing", "patch/bulk_sharing_patch.json" );
        assertEquals( HttpStatus.CONFLICT, response.status() );
        assertEquals( "Invalid UID `" + deAId + "` for property `dataElement`", getFirstErrorMessage( response ) );
        JsonIdentifiableObject savedDeB = GET( "/dataElements/{uid}", deBId ).content( HttpStatus.OK )
            .as( JsonIdentifiableObject.class );
        assertEquals( 2, savedDeB.getSharing().getUsers().size() );
        assertNotNull( savedDeB.getSharing().getUsers().get( userAId ) );
        assertNotNull( savedDeB.getSharing().getUsers().get( userBId ) );
    }

    /**
     * Payload contains two DataElements but only one exists in database. Atomic
     * = true so no sharing settings will be saved.
     *
     * @throws IOException
     */
    @Test
    void testApplyPatchWithInvalidUidAtomic()
        throws IOException
    {
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "usera", userAId ) ) );
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userb", userBId ) ) );
        userCId = assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userc" ) ) );
        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements", jsonMapper.writeValueAsString( createDataElement( 'B', deBId, userCId ) ) ) );
        HttpResponse response = PATCH( "/dataElements/sharing?atomic=true", "patch/bulk_sharing_patch.json" );
        assertEquals( HttpStatus.CONFLICT, response.status() );
        assertEquals( "Invalid UID `" + deAId + "` for property `dataElement`", getFirstErrorMessage( response ) );
        JsonIdentifiableObject savedDeB = GET( "/dataElements/{uid}", deBId ).content( HttpStatus.OK )
            .as( JsonIdentifiableObject.class );
        assertEquals( 0, savedDeB.getSharing().getUsers().size() );
    }

    @Test
    void testApplyPatches()
        throws IOException
    {
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "usera", userAId ) ) );
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userb", userBId ) ) );
        userCId = assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userc" ) ) );
        assertStatus( HttpStatus.CREATED,
            POST( "/dataSets", jsonMapper.writeValueAsString( createDataSet( 'A', dsIdA, userCId ) ) ) );
        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements", jsonMapper.writeValueAsString( createDataElement( 'A', deAId, userCId ) ) ) );
        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements", jsonMapper.writeValueAsString( createDataElement( 'B', deBId, userCId ) ) ) );
        assertStatus( HttpStatus.OK, PATCH( "/metadata/sharing", "patch/bulk_sharing_patches.json" ) );
        JsonIdentifiableObject savedDeA = GET( "/dataElements/{uid}", deAId ).content( HttpStatus.OK )
            .as( JsonIdentifiableObject.class );
        JsonIdentifiableObject savedDeB = GET( "/dataElements/{uid}", deBId ).content( HttpStatus.OK )
            .as( JsonIdentifiableObject.class );
        JsonIdentifiableObject savedDsA = GET( "/dataSets/{uid}", dsIdA ).content( HttpStatus.OK )
            .as( JsonIdentifiableObject.class );
        assertEquals( 2, savedDeA.getSharing().getUsers().size() );
        assertEquals( 2, savedDeB.getSharing().getUsers().size() );
        assertEquals( 2, savedDsA.getSharing().getUsers().size() );
        assertNotNull( savedDeA.getSharing().getUsers().get( userAId ) );
        assertNotNull( savedDeB.getSharing().getUsers().get( userAId ) );
        assertNotNull( savedDsA.getSharing().getUsers().get( userAId ) );
        assertNotNull( savedDeA.getSharing().getUsers().get( userBId ) );
        assertNotNull( savedDeB.getSharing().getUsers().get( userBId ) );
        assertNotNull( savedDsA.getSharing().getUsers().get( userBId ) );
    }

    @Test
    void testApplyPatchesInvalidClass()
        throws IOException
    {
        HttpResponse response = PATCH( "/metadata/sharing", "patch/bulk_sharing_patches_invalid_class.json" );
        assertEquals( HttpStatus.CONFLICT, response.status() );
        assertEquals( "Sharing is not enabled for this object `organisationUnit`", getFirstErrorMessage( response ) );
    }

    private String toJsonString( Object object )
        throws JsonProcessingException
    {
        return jsonMapper.writeValueAsString( object );
    }

    private String getFirstErrorMessage( HttpResponse response )
    {
        return response.error().getString( "response.typeReports[0].objectReports[0].errorReports[0].message" )
            .string();
    }

    private DataSet createDataSet( char uniqueChar, String uid, String owner )
    {
        DataSet dataSet = createDataSet( uniqueChar );
        dataSet.setUid( uid );
        dataSet.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).owner( owner ).build() );
        return dataSet;
    }

    private DataElement createDataElement( char uniqueChar, String uid, String owner )
    {
        DataElement de = createDataElement( uniqueChar );
        de.setUid( uid );
        de.setSharing( Sharing.builder().owner( owner ).publicAccess( AccessStringHelper.DEFAULT ).build() );
        return de;
    }

    private String createUser( String name, String uid )
    {
        return "{'name': '" + name + "', 'id': '" + uid + "', 'firstName':'" + name + "', 'surname': '" + name
            + "', 'username':'" + name + "', 'userRoles': [{'id': 'yrB6vc5Ip3r'}]}";
    }

    private String createUser( String name )
    {
        return "{'name': '" + name + "', 'firstName':'" + name + "', 'surname': '" + name
            + "', 'username':'" + name + "', 'userRoles': [{'id': 'yrB6vc5Ip3r'}]}";
    }
}
