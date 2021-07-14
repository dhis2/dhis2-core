package org.hisp.dhis.webapi.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.Test;

/**
 * Tests the {@link ExpressionController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
public class ExpressionControllerTest extends DhisControllerConvenienceTest
{
    @Test
    public void testGetExpressionDescription()
    {
        JsonWebMessage response = GET( "/expressions/description?expression=0" )
            .content().as( JsonWebMessage.class );
        assertWebMessage( "OK", 200, "OK", "Valid", response );
        assertEquals( "0", response.getDescription() );
    }

    @Test
    public void testGetExpressionDescription_InvalidExpression()
    {
        JsonWebMessage response = GET( "/expressions/description?expression=invalid" )
            .content().as( JsonWebMessage.class );
        assertWebMessage( "OK", 200, "ERROR", "Expression is not well-formed", response );
        assertNull( response.getDescription() );
    }
}
