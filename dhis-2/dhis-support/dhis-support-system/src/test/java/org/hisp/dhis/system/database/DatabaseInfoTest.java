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
package org.hisp.dhis.system.database;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DatabaseInfo}.
 *
 * @author Volker Schmidt
 */
class DatabaseInfoTest
{

    private DatabaseInfo databaseInfo;

    @BeforeEach
    void setUp()
    {
        databaseInfo = new DatabaseInfo();
        databaseInfo.setName( "testDatabase" );
        databaseInfo.setUser( "testUser" );
        databaseInfo.setUrl( "theUrl" );
        databaseInfo.setPassword( "myPassword" );
        databaseInfo.setDatabaseVersion( "xzy 10.7" );
        databaseInfo.setSpatialSupport( Boolean.TRUE );
    }

    @Test
    void cloneDatabaseInfo()
    {
        final DatabaseInfo cloned = databaseInfo.instance();
        Assertions.assertNotSame( databaseInfo, cloned );
        Assertions.assertEquals( databaseInfo.getName(), cloned.getName() );
        Assertions.assertEquals( databaseInfo.getUser(), cloned.getUser() );
        Assertions.assertEquals( databaseInfo.getUrl(), cloned.getUrl() );
        Assertions.assertEquals( databaseInfo.getPassword(), cloned.getPassword() );
        Assertions.assertEquals( databaseInfo.getDatabaseVersion(), cloned.getDatabaseVersion() );
        Assertions.assertEquals( databaseInfo.getSpatialSupport(), cloned.getSpatialSupport() );
    }

    @Test
    void clearSensitiveInfo()
    {
        databaseInfo.clearSensitiveInfo();
        Assertions.assertNull( databaseInfo.getName() );
        Assertions.assertNull( databaseInfo.getUser() );
        Assertions.assertNull( databaseInfo.getUrl() );
        Assertions.assertNull( databaseInfo.getPassword() );
        Assertions.assertNull( databaseInfo.getDatabaseVersion() );
        Assertions.assertNull( databaseInfo.getSpatialSupport() );
    }
}
