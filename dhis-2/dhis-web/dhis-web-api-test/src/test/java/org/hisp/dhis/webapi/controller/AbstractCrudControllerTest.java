/*
 * Copyright (c) 2004-2021, University of Oslo
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
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonList;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.hisp.dhis.webapi.snippets.SomeUserId;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the generic operations offered by the {@link AbstractCrudController}
 * using specific endpoints.
 *
 * @author Jan Bernitt
 */
public class AbstractCrudControllerTest extends DhisControllerConvenienceTest
{
    @Test
    public void testGetObjectList()
    {
        JsonList<JsonUser> users = GET( "/users/" )
            .content( HttpStatus.OK ).getList( "users", JsonUser.class );

        assertEquals( 1, users.size() );
        JsonUser user = users.get( 0 );
        assertEquals( "admin admin", user.getDisplayName() );
    }

    @Test
    public void testGetObject()
    {
        String id = run( SomeUserId::new );
        JsonUser userById = GET( "/users/{id}", id )
            .content( HttpStatus.OK ).as( JsonUser.class );

        assertEquals( id, userById.getId() );
        assertTrue( userById.getUserCredentials().exists() );
    }

    @Test
    public void testGetObjectProperty()
    {
        // response will look like: { "surname": <name> }
        JsonUser userProperty = GET( "/users/{id}/surname", run( SomeUserId::new ) )
            .content( HttpStatus.OK ).as( JsonUser.class );

        assertEquals( "admin", userProperty.getSurname() );
        assertEquals( 1, userProperty.size() );
    }

    @Test
    public void testPartialUpdateObject()
    {
        String id = run( SomeUserId::new );
        assertStatus( HttpStatus.NO_CONTENT, PATCH( "/users/" + id, "{'surname':'Peter'}" ) );

        assertEquals( "Peter", GET( "/users/{id}", id ).content().as( JsonUser.class ).getSurname() );
    }

    @Test
    public void replaceTranslations()
    {

    }

}
