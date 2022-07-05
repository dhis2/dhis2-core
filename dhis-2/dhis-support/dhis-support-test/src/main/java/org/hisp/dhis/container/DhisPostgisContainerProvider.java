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
package org.hisp.dhis.container;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgisContainerProvider;
import org.testcontainers.utility.DockerImageName;

/**
 * Custom PostgisContainerProvider to create {@link DhisPostgreSQLContainer}
 *
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@SuppressWarnings( "rawtypes" )
public class DhisPostgisContainerProvider
    extends PostgisContainerProvider
{
    public static final String DEFAULT_TAG = "10-2.5-alpine";

    private static final String DEFAULT_IMAGE = "postgis/postgis";

    @Override
    public JdbcDatabaseContainer newInstance()
    {
        return newInstance( DEFAULT_TAG );
    }

    @Override
    public JdbcDatabaseContainer newInstance( String tag )
    {
        DockerImageName postgres = DockerImageName.parse( DEFAULT_IMAGE + ":" + tag )
            .asCompatibleSubstituteFor( "postgres" );
        return new DhisPostgreSQLContainer( postgres );
    }
}
