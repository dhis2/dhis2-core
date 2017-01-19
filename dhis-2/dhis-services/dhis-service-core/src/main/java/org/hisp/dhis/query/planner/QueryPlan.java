package org.hisp.dhis.query.planner;

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

import org.hisp.dhis.query.Query;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.User;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class QueryPlan
{
    private final Query persistedQuery;

    private final Query nonPersistedQuery;

    public QueryPlan( Query persistedQuery, Query nonPersistedQuery )
    {
        this.persistedQuery = persistedQuery;
        this.nonPersistedQuery = nonPersistedQuery;
    }

    public Query getPersistedQuery()
    {
        return persistedQuery;
    }

    public Query getNonPersistedQuery()
    {
        return nonPersistedQuery;
    }

    public Schema getSchema()
    {
        if ( persistedQuery != null )
        {
            return persistedQuery.getSchema();
        }
        else if ( nonPersistedQuery != null )
        {
            return nonPersistedQuery.getSchema();
        }

        return null;
    }

    public User getUser()
    {
        if ( persistedQuery != null )
        {
            return persistedQuery.getUser();
        }
        else if ( nonPersistedQuery != null )
        {
            return nonPersistedQuery.getUser();
        }

        return null;
    }

    public void setUser( User user )
    {
        if ( persistedQuery != null )
        {
            persistedQuery.setUser( user );
        }
        else if ( nonPersistedQuery != null )
        {
            nonPersistedQuery.setUser( user );
        }
    }
}
