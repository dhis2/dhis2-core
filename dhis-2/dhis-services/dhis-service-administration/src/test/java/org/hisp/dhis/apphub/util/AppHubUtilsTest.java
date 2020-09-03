package org.hisp.dhis.apphub.util;

import org.hisp.dhis.apphub.AppHubUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppHubUtilsTest
{
    @Test( expected = IllegalQueryException.class )
    public void testValidateQueryA()
    {
        AppHubUtils.validateQuery( "apps/../../evil/endpoint" );
    }

    @Test
    public void testValidateQueryB()
    {
        AppHubUtils.validateQuery( "apps" );
    }

    @Test
    public void testSanitizeQuery()
    {
        assertEquals( "apps", AppHubUtils.sanitizeQuery( "apps" ) );
        assertEquals( "apps", AppHubUtils.sanitizeQuery( "/apps" ) );
        assertEquals( "apps", AppHubUtils.sanitizeQuery( "//apps" ) );
    }
}
