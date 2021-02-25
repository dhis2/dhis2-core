package org.hisp.dhis.webapi.controller;

import static org.junit.Assert.assertEquals;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonList;
import org.hisp.dhis.webapi.json.JsonUser;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * @author Jan Bernitt
 */
public class CrudControllerTest extends DhisControllerConvenienceTest
{

    @Test
    public void testGetObjectList()
        throws Exception
    {
        JsonList<JsonUser> users = GET( "/users/" ).when( HttpStatus.OK ).getList( "users", JsonUser.class );
        assertEquals( 1, users.size() );
    }
}
