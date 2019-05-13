/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.webapi.config.jdbc;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.Nullable;

import javax.sql.DataSource;
import java.util.Collection;

/**
 * @author Jon Schneider
 */
public class DataSourcePoolMetrics
    implements
    MeterBinder
{

    private final DataSource dataSource;

    private final String name;

    private final Iterable<Tag> tags;

    private final DataSourcePoolMetadata poolMetadata;

    public DataSourcePoolMetrics( DataSource dataSource,
        @Nullable Collection<DataSourcePoolMetadataProvider> metadataProviders, String name, Iterable<Tag> tags )
    {
        this.name = name;
        this.tags = tags;
        this.dataSource = dataSource;
        DataSourcePoolMetadataProvider provider = new DataSourcePoolMetadataProviders( metadataProviders );
        this.poolMetadata = provider.getDataSourcePoolMetadata( dataSource );
    }

    @Override
    public void bindTo( MeterRegistry registry )
    {
        if ( poolMetadata != null )
        {
            registry.gauge( name + ".connections.active", tags, dataSource,
                dataSource -> poolMetadata.getActive() != null ? poolMetadata.getActive() : 0 );
            registry.gauge( name + ".connections.max", tags, dataSource, dataSource -> poolMetadata.getMax() );
            registry.gauge( name + ".connections.min", tags, dataSource, dataSource -> poolMetadata.getMin() );
        }
    }
}
