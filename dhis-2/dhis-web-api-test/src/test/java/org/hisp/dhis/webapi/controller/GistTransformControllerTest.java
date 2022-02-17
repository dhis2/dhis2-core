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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonObject;
import org.junit.jupiter.api.Test;

/**
 * Tests the field {@link org.hisp.dhis.schema.annotation.Gist.Transform}
 * related features of the Gist API.
 *
 * @author Jan Bernitt
 */
class GistTransformControllerTest extends AbstractGistControllerTest
{

    @Test
    void testTransform_Rename()
    {
        JsonObject user = GET(
            "/users/{uid}/gist?fields=surname~rename(name),username~rename(alias)",
            getSuperuserUid() ).content();
        assertEquals( "admin", user.getString( "name" ).string() );
        assertEquals( 2, user.size() );
    }

    @Test
    void testTransform_Ids()
    {
        JsonObject gist = GET( "/dataSets/{id}/organisationUnits/gist?fields=*,dataSets::ids", dataSetId ).content();
        assertHasPager( gist, 1, 50 );
        JsonObject orgUnit = gist.getArray( "organisationUnits" ).getObject( 0 );
        assertEquals( dataSetId, orgUnit.getArray( "dataSets" ).getString( 0 ).string() );
    }

    @Test
    void testTransform_Pluck()
    {
        JsonObject gist = GET( "/users/{uid}/userGroups/gist?fields=name,users::pluck(surname)", getSuperuserUid() )
            .content();
        assertHasPager( gist, 1, 50 );
        assertEquals( "admin",
            gist.getArray( "userGroups" ).getObject( 0 ).getArray( "users" ).getString( 0 ).string() );
    }

    @Test
    void testTransform_Pluck_NoArgument()
    {
        JsonObject gist = GET( "/users/{uid}/userGroups/gist?fields=name,users::pluck", getSuperuserUid() ).content();
        assertHasPager( gist, 1, 50 );
        assertEquals( getSuperuserUid(),
            gist.getArray( "userGroups" ).getObject( 0 ).getArray( "users" ).getString( 0 ).string() );
    }

    @Test
    void testTransform_Member()
    {
        String url = "/users/{uid}/userGroups/gist?fields=name,users::member({uid})";
        // member(id) with a user that is a member
        JsonObject gist = GET( url, getSuperuserUid(), getSuperuserUid() ).content();
        assertTrue( gist.getArray( "userGroups" ).getObject( 0 ).getBoolean( "users" ).booleanValue() );
        // member(id) with a user that is not a member
        gist = GET( url, getSuperuserUid(), "non-existing-user-uid" ).content();
        assertFalse( gist.getArray( "userGroups" ).getObject( 0 ).getBoolean( "users" ).booleanValue() );
    }

    @Test
    void testTransform_NotMember()
    {
        String url = "/users/{uid}/userGroups/gist?fields=name,users::not-member({uid})";
        // not-member(id) with a user that is a member
        JsonObject gist = GET( url, getSuperuserUid(), getSuperuserUid() ).content();
        assertFalse( gist.getArray( "userGroups" ).getObject( 0 ).getBoolean( "users" ).booleanValue() );
        // not-member(id) with a user that is not a member
        gist = GET( url, getSuperuserUid(), "non-existing-user-uid" ).content();
        assertTrue( gist.getArray( "userGroups" ).getObject( 0 ).getBoolean( "users" ).booleanValue() );
    }

    @Test
    void testTransform_Auto_MediumUsesSize()
    {
        JsonObject gist = GET( "/users/{uid}/userGroups/gist?fields=name,users&auto=M", getSuperuserUid() ).content();
        assertHasPager( gist, 1, 50 );
        assertEquals( 1, gist.getArray( "userGroups" ).getObject( 0 ).getNumber( "users" ).intValue() );
    }

    @Test
    void testTransform_Auto_LargeUsesIds()
    {
        JsonObject gist = GET( "/users/{uid}/userGroups/gist?fields=name,users&auto=L", getSuperuserUid() ).content();
        assertHasPager( gist, 1, 50 );
        assertEquals( getSuperuserUid(),
            gist.getArray( "userGroups" ).getObject( 0 ).getArray( "users" ).getString( 0 ).string() );
    }
}
