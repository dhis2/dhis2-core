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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Volker Schmidt
 */
class DatabaseInfoTest
{
    @Test
    void cloneDatabaseInfo()
        throws Exception
    {
        DatabaseInfo databaseInfo = getDataBaseInfo();
        DatabaseInfo cloned = databaseInfo.instance();
        BeanUtils.copyProperties( cloned, databaseInfo );
        assertNotSame( databaseInfo, cloned );
        assertEquals( databaseInfo.getName(), cloned.getName() );
        assertEquals( databaseInfo.getUser(), cloned.getUser() );
        assertEquals( databaseInfo.getUrl(), cloned.getUrl() );
        assertEquals( databaseInfo.getPassword(), cloned.getPassword() );
        assertEquals( databaseInfo.getDatabaseVersion(), cloned.getDatabaseVersion() );
        assertEquals( databaseInfo.isSpatialSupport(), cloned.isSpatialSupport() );
    }

    @Test
    void clearSensitiveInfo()
    {
        DatabaseInfo databaseInfo = getDataBaseInfo();
        databaseInfo.clearSensitiveInfo();
        assertNull( databaseInfo.getName() );
        assertNull( databaseInfo.getUser() );
        assertNull( databaseInfo.getUrl() );
        assertNull( databaseInfo.getPassword() );
        assertNull( databaseInfo.getDatabaseVersion() );
    }

    private DatabaseInfo getDataBaseInfo()
    {
        DatabaseInfo databaseInfo = new DatabaseInfo();
        databaseInfo.setName( "testDatabase" );
        databaseInfo.setUser( "testUser" );
        databaseInfo.setUrl( "theUrl" );
        databaseInfo.setPassword( "myPassword" );
        databaseInfo.setDatabaseVersion( "xzy 10.7" );
        databaseInfo.setSpatialSupport( true );
        return databaseInfo;
    }
}
