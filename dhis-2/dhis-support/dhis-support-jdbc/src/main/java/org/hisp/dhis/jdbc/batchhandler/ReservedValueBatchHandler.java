package org.hisp.dhis.jdbc.batchhandler;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.reservedvalue.ReservedValue;
import org.hisp.quick.JdbcConfiguration;
import org.hisp.quick.batchhandler.AbstractBatchHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

/**
 * @author Stian Sandvold
 */
public class ReservedValueBatchHandler
    extends AbstractBatchHandler<ReservedValue>
{
    public ReservedValueBatchHandler( JdbcConfiguration configuration )
    {
        super( configuration );
    }

    @Override
    public String getTableName()
    {
        return "reservedvalue";
    }

    @Override
    public String getAutoIncrementColumn()
    {
        return "reservedvalueid";
    }

    @Override
    public boolean isInclusiveUniqueColumns()
    {
        return true;
    }

    @Override
    public List<String> getIdentifierColumns()
    {
        return getStringList( "reservedvalueid" );
    }

    @Override
    public List<Object> getIdentifierValues( ReservedValue object )
    {
        return getObjectList( object.getId() );
    }

    @Override
    public List<String> getUniqueColumns()
    {
        return getStringList( "reservedvalueid", "ownerobject", "owneruid", "key", "value" );
    }

    @Override
    public List<Object> getUniqueValues( ReservedValue object )
    {
        return getObjectList(
            object.getId(),
            object.getOwnerObject(),
            object.getOwnerUid(),
            object.getKey(),
            object.getValue()
        );
    }

    @Override
    public List<String> getColumns()
    {
        return getStringList( "ownerobject", "owneruid", "key", "value", "expirydate", "created" );
    }

    @Override
    public List<Object> getValues( ReservedValue object )
    {
        return getObjectList(
            object.getOwnerObject(),
            object.getOwnerUid(),
            object.getKey(),
            object.getValue(),
            object.getExpiryDate(),
            object.getCreated()
        );
    }

    @Override
    public ReservedValue mapRow( ResultSet resultSet )
        throws SQLException
    {
        Calendar expires = Calendar.getInstance();

        ReservedValue rv = new ReservedValue();

        expires.setTime( resultSet.getDate( "expirydate" ) );

        rv.setId( resultSet.getInt( "reservedvalueid" ) );
        rv.setOwnerObject( resultSet.getString( "ownerobject" ) );
        rv.setOwnerUid( resultSet.getString( "ownerUid" ) );
        rv.setKey( resultSet.getString( "key" ) );
        rv.setValue( resultSet.getString( "value" ) );
        rv.setExpiryDate( expires.getTime() );
        rv.setCreated( resultSet.getDate( "created" ) );

        return rv;
    }
}
