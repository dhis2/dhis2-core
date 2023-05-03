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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.webapi.service.ContextService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

class IconControllerTest extends DhisControllerConvenienceTest
{
    private static final String iconKey = "iconKey";

    private static final String description = "description";

    private static final String keywords = "[\"k1\",\"k2\"]";

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ContextService contextService;

    @Test
    void shouldCreateCustomIconWhenFileResourceExist()
    {
        String message = createIcon( createFileResource() );

        assertEquals( String.format( "Icon %s created", iconKey ), message );
    }

    @Test
    void shouldGetIconWhenIconKeyExists()
    {
        String fileResourceId = createFileResource();
        createIcon( fileResourceId );

        JsonObject response = GET( String.format( "/icons/%s", iconKey ) ).content();

        assertEquals( iconKey, response.getString( "key" ).string() );
        assertEquals( description, response.getString( "description" ).string() );
        assertEquals( fileResourceId, response.getString( "fileResourceUid" ).string() );
        assertEquals( keywords, response.getArray( "keywords" ).toString() );
        assertEquals( currentUserService.getCurrentUser().getUid(), response.getString( "userUid" ).string() );
        assertEquals( String.format( contextService.getApiPath() + "/fileResources/%s/data", fileResourceId ),
            response.getString( "href" ).string() );
    }

    @Test
    void shouldUpdateIconWhenKeyExists()
    {
        String updatedDescription = "updatedDescription";
        String updatedKeywords = "['new k1', 'new k2']";
        createIcon( createFileResource() );

        JsonObject response = PUT( "/icons", "{'key':'" + iconKey + "', 'description':'" + updatedDescription
            + "', 'keywords':" + updatedKeywords + "}" ).content();

        assertEquals( String.format( "Icon %s updated", iconKey ), response.getString( "message" ).string() );
    }

    @Test
    void shouldDeleteIconWhenKeyExists()
    {
        createIcon( createFileResource() );

        JsonObject response = DELETE( String.format( "/icons/%s", iconKey ) ).content();

        assertEquals( String.format( "Icon %s deleted", iconKey ), response.getString( "message" ).string() );
    }

    private String createIcon( String fileResourceId )
    {
        JsonWebMessage message = POST( "/icons/",
            "{'key':'" + iconKey + "', 'description':'" + description + "', 'fileResourceUid':'" + fileResourceId
                + "', 'keywords':" + keywords + "}" )
                    .content( HttpStatus.CREATED ).as( JsonWebMessage.class );
        return message.getMessage();
    }

    private String createFileResource()
    {
        MockMultipartFile image = new MockMultipartFile( "file", "OU_profile_image.png", "image/png",
            "<<png data>>".getBytes() );
        HttpResponse response = POST_MULTIPART( "/fileResources?domain=ORG_UNIT", image );
        JsonObject savedObject = response.content( HttpStatus.ACCEPTED ).getObject( "response" )
            .getObject( "fileResource" );

        return savedObject.getString( "id" ).string();
    }
}
