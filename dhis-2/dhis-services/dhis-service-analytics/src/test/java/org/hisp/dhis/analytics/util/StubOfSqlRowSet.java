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

package org.hisp.dhis.analytics.util;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

public class StubOfSqlRowSet
    implements
    SqlRowSet
{

    @Override
    public SqlRowSetMetaData getMetaData()
    {
        return null;
    }

    @Override
    public int findColumn( String s )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public BigDecimal getBigDecimal( int i )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public BigDecimal getBigDecimal( String s )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public boolean getBoolean( int i )
        throws InvalidResultSetAccessException
    {
        return false;
    }

    @Override
    public boolean getBoolean( String s )
        throws InvalidResultSetAccessException
    {
        return false;
    }

    @Override
    public byte getByte( int i )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public byte getByte( String s )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public Date getDate( int i )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Date getDate( String s )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Date getDate( int i, Calendar calendar )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Date getDate( String s, Calendar calendar )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public double getDouble( int i )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public double getDouble( String s )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public float getFloat( int i )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public float getFloat( String s )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public int getInt( int i )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public int getInt( String s )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public long getLong( int i )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public long getLong( String s )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public String getNString( int i )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public String getNString( String s )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Object getObject( int i )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Object getObject( String s )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Object getObject( int i, Map<String, Class<?>> map )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Object getObject( String s, Map<String, Class<?>> map )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public <T> T getObject( int i, Class<T> aClass )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public <T> T getObject( String s, Class<T> aClass )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public short getShort( int i )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public short getShort( String s )
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public String getString( int i )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public String getString( String s )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Time getTime( int i )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Time getTime( String s )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Time getTime( int i, Calendar calendar )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Time getTime( String s, Calendar calendar )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Timestamp getTimestamp( int i )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Timestamp getTimestamp( String s )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Timestamp getTimestamp( int i, Calendar calendar )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public Timestamp getTimestamp( String s, Calendar calendar )
        throws InvalidResultSetAccessException
    {
        return null;
    }

    @Override
    public boolean absolute( int i )
        throws InvalidResultSetAccessException
    {
        return false;
    }

    @Override
    public void afterLast()
        throws InvalidResultSetAccessException
    {

    }

    @Override
    public void beforeFirst()
        throws InvalidResultSetAccessException
    {

    }

    @Override
    public boolean first()
        throws InvalidResultSetAccessException
    {
        return false;
    }

    @Override
    public int getRow()
        throws InvalidResultSetAccessException
    {
        return 0;
    }

    @Override
    public boolean isAfterLast()
        throws InvalidResultSetAccessException
    {
        return false;
    }

    @Override
    public boolean isBeforeFirst()
        throws InvalidResultSetAccessException
    {
        return false;
    }

    @Override
    public boolean isFirst()
        throws InvalidResultSetAccessException
    {
        return false;
    }

    @Override
    public boolean isLast()
        throws InvalidResultSetAccessException
    {
        return false;
    }

    @Override
    public boolean last()
        throws InvalidResultSetAccessException
    {
        return false;
    }

    @Override
    public boolean next()
        throws InvalidResultSetAccessException
    {
        return false;
    }

    @Override
    public boolean previous()
        throws InvalidResultSetAccessException
    {
        return false;
    }

    @Override
    public boolean relative( int i )
        throws InvalidResultSetAccessException
    {
        return false;
    }

    @Override
    public boolean wasNull()
        throws InvalidResultSetAccessException
    {
        return false;
    }
}