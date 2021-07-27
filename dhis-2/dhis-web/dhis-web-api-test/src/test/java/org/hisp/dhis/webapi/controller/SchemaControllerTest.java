package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link SchemaController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
public class SchemaControllerTest extends DhisControllerConvenienceTest
{
    @Test
    public void testValidateSchema()
    {
        assertWebMessage( "OK", 200, "OK", null,
            POST( "/schemas/organisationUnit", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" )
                .content( HttpStatus.OK ) );
    }

    @Test
    public void testValidateSchema_NoSuchType()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "404 Type xyz does not exist.",
            POST( "/schemas/xyz", "{}" )
                .content( HttpStatus.NOT_FOUND ) );

    }
}
