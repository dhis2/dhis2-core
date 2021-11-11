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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
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

    @Autowired
    private AclService aclService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Test
    public void testApplyPatchOk()
        throws IOException
    {
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userA", "NOOF56dveaZ" ) ) );
        assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userB", "Kh68cDMwZsg" ) ) );
        String userCId = assertStatus( HttpStatus.CREATED, POST( "/users", createUser( "userC" ) ) );

        DataElement deA = createDataElement( 'A', "fbfJHSPpUQD", userCId );
        assertStatus( HttpStatus.CREATED, POST( "/dataElements", jsonMapper.writeValueAsString( deA ) ) );

        DataElement deB = createDataElement( 'B', "cYeuwXTCPkU", userCId );
        assertStatus( HttpStatus.CREATED, POST( "/dataElements", jsonMapper.writeValueAsString( deB ) ) );

        String payload = IOUtils.toString( new ClassPathResource( "patch/bulk_sharing_patch.json" ).getInputStream(),
            StandardCharsets.UTF_8 );
        assertStatus( HttpStatus.OK, PATCH( "/dataElements/sharing", payload ) );

        JsonIdentifiableObject saveDeA = GET( "/dataElements/{uid}", "fbfJHSPpUQD" ).content( HttpStatus.OK )
            .as( JsonIdentifiableObject.class );
        JsonIdentifiableObject saveDeB = GET( "/dataElements/{uid}", "cYeuwXTCPkU" ).content( HttpStatus.OK )
            .as( JsonIdentifiableObject.class );
        assertEquals( 2, saveDeA.getSharing().getUsers().size() );
        assertEquals( 2, saveDeB.getSharing().getUsers().size() );
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
