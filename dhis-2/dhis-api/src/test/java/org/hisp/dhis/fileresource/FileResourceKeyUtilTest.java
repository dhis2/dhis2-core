package org.hisp.dhis.fileresource;

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.*;

public class FileResourceKeyUtilTest {

    @Test
    public void verify_buildKey() {
        String key = FileResourceKeyUtil.makeKey(FileResourceDomain.DOCUMENT, Optional.empty());
        assertThat(key, startsWith("document/"));
        assertTrue(key.substring("document/".length()).length() == 36);

        key = FileResourceKeyUtil.makeKey(FileResourceDomain.DOCUMENT, Optional.of("myKey"));
        assertThat(key, is("document/myKey"));

    }

}