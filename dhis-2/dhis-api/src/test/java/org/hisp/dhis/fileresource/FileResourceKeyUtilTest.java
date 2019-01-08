package org.hisp.dhis.fileresource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.junit.Test;

public class FileResourceKeyUtilTest
{

    @Test
    public void verify_buildKey()
    {
        String key = FileResourceKeyUtil.makeKey( FileResourceDomain.DOCUMENT, Optional.empty() );
        assertThat( key, startsWith( "document/" ) );
        assertEquals( 36, key.substring( "document/".length() ).length() );

        key = FileResourceKeyUtil.makeKey( FileResourceDomain.DOCUMENT, Optional.of( "myKey" ) );
        assertThat( key, is( "document/myKey" ) );

    }

}