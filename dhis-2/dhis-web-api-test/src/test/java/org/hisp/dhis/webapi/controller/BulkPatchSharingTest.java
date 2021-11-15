/*
 * Copyright (c) 2004-2004-2021, University of Oslo
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author viet@dhis2.org
 */
public class BulkPatchSharingTest extends DhisControllerConvenienceTest
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
    public void testApplyPatchOk()
        throws IOException
    {
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userA", userAId ) ) );
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userB", userBId ) ) );
        userCId = assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userC" ) ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements", jsonMapper.writeValueAsString( createDataElement( 'A', deAId, userCId ) ) ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements", jsonMapper.writeValueAsString( createDataElement( 'B', deBId, userCId ) ) ) );

        String payload = IOUtils.toString( new ClassPathResource( "patch/bulk_sharing_patch.json" ).getInputStream(),
            StandardCharsets.UTF_8 );
        assertStatus( HttpStatus.OK, PATCH( "/dataElements/sharing", payload ) );

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
    public void testApplyPatchWithInvalidUid()
        throws IOException
    {
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userA", userAId ) ) );
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userB", userBId ) ) );
        userCId = assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userC" ) ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements", jsonMapper.writeValueAsString( createDataElement( 'B', deBId, userCId ) ) ) );

        String payload = IOUtils.toString( new ClassPathResource( "patch/bulk_sharing_patch.json" ).getInputStream(),
            StandardCharsets.UTF_8 );
        HttpResponse response = PATCH( "/dataElements/sharing", payload );
        assertEquals( HttpStatus.CONFLICT, response.status() );
        assertEquals( "Invalid UID `" + deAId + "` for property `DataElement`", getFirstErrorMessage( response ) );

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
    public void testApplyPatchWithInvalidUidAtomic()
        throws IOException
    {
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userA", userAId ) ) );
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userB", userBId ) ) );
        userCId = assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userC" ) ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements", jsonMapper.writeValueAsString( createDataElement( 'B', deBId, userCId ) ) ) );

        String payload = IOUtils.toString( new ClassPathResource( "patch/bulk_sharing_patch.json" ).getInputStream(),
            StandardCharsets.UTF_8 );
        HttpResponse response = PATCH( "/dataElements/sharing?atomic=true", payload );
        assertEquals( HttpStatus.CONFLICT, response.status() );
        assertEquals( "Invalid UID `" + deAId + "` for property `DataElement`", getFirstErrorMessage( response ) );

        JsonIdentifiableObject savedDeB = GET( "/dataElements/{uid}", deBId ).content( HttpStatus.OK )
            .as( JsonIdentifiableObject.class );
        assertEquals( 0, savedDeB.getSharing().getUsers().size() );
    }

    @Test
    public void testApplyPatches()
        throws IOException
    {
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userA", userAId ) ) );
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userB", userBId ) ) );
        userCId = assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userC" ) ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/dataSets", jsonMapper.writeValueAsString( createDataSet( 'A', dsIdA, userCId ) ) ) );
        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements", jsonMapper.writeValueAsString( createDataElement( 'A', deAId, userCId ) ) ) );
        assertStatus( HttpStatus.CREATED,
            POST( "/dataElements", jsonMapper.writeValueAsString( createDataElement( 'B', deBId, userCId ) ) ) );

        String payload = IOUtils.toString( new ClassPathResource( "patch/bulk_sharing_patches.json" ).getInputStream(),
            StandardCharsets.UTF_8 );
        assertStatus( HttpStatus.OK, PATCH( "/metadata/sharing", payload ) );

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
    public void testApplyPatchesInvalidClass()
        throws IOException
    {
        String payload = IOUtils.toString(
            new ClassPathResource( "patch/bulk_sharing_patches_invalid_class.json" ).getInputStream(),
            StandardCharsets.UTF_8 );

        HttpResponse response = PATCH( "/metadata/sharing", payload );
        assertEquals( HttpStatus.CONFLICT, response.status() );
        assertEquals( "Sharing is not enabled for this object `organisationUnit`", getFirstErrorMessage( response ) );
    }

    private String getFirstErrorMessage( HttpResponse response )
    {
        return response.error().get( "response.typeReports[0].objectReports[0].errorReports[0].message" )
            .node().value().toString();
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
            + "', 'userCredentials':{'username':'" + name + "'}}";
    }

    private String createUser( String name )
    {
        return "{'name': '" + name + "', 'firstName':'" + name + "', 'surname': '" + name
            + "', 'userCredentials':{'username':'" + name + "'}}";
    }

}
