package org.hisp.dhis.datasource;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.AbstractDataSource;

import com.google.common.collect.Iterators;

/**
 * Data source implementation which routes to the configured target data sources
 * in a circular fashion.
 * 
 * @author Lars Helge Overland
 */
public class CircularRoutingDataSource
    extends AbstractDataSource
{
    private Iterator<DataSource> dataSourceIterator;

    public CircularRoutingDataSource()
    {
    }
    
    public CircularRoutingDataSource( List<DataSource> targetDataSources )
    {
        this.dataSourceIterator = Iterators.cycle( Collections.synchronizedList( targetDataSources ) );
    }
        
    // -------------------------------------------------------------------------
    // AbstractDataSource implementation
    // -------------------------------------------------------------------------

    @Override
    public Connection getConnection()
        throws SQLException
    {
        return getDataSource().getConnection();
    }

    @Override
    public Connection getConnection( String username, String password )
        throws SQLException
    {
        return getDataSource().getConnection( username, password );
    }

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    private synchronized DataSource getDataSource()
    {
        return dataSourceIterator.next();
    }
}
