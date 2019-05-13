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

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.jboss.C3P0PooledDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * @author Luciano Fiandesio
 */
public class C3p0MetadataProvider
    extends
    AbstractDataSourcePoolMetadata<ComboPooledDataSource>
{

    /**
     * Create an instance with the data source to use.
     *
     * @param dataSource the data source
     */
    public C3p0MetadataProvider( ComboPooledDataSource dataSource )
    {
        super( dataSource );
    }

    @Override
    public Integer getActive()
    {
        try
        {
            return getDataSource().getNumBusyConnections();
        }
        catch ( SQLException e )
        {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public Integer getMax()
    {
        return getDataSource().getMaxPoolSize();
    }

    @Override
    public Integer getMin()
    {
        return getDataSource().getMinPoolSize();
    }

    @Override
    public String getValidationQuery()
    {
        return "";
    }

    @Override
    public Boolean getDefaultAutoCommit()
    {
        return getDataSource().isAutoCommitOnClose();
    }
}
