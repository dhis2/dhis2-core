package org.hisp.dhis.logging;

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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Log
{
    protected final long timestamp = Instant.now().toEpochMilli();

    protected LogLevel logLevel = LogLevel.INFO;

    protected String message;

    protected Class<?> source;

    protected String username;

    protected Map<String, Object> metadata = new HashMap<>();

    protected LogData data;

    public Log()
    {
    }

    public Log( String message )
    {
        this.message = message;
    }

    @JsonProperty
    public long getTimestamp()
    {
        return timestamp;
    }

    @JsonProperty
    public LogLevel getLogLevel()
    {
        return logLevel;
    }

    public Log setLogLevel( LogLevel logLevel )
    {
        this.logLevel = logLevel;
        return this;
    }

    @JsonProperty
    public Class<?> getSource()
    {
        return source;
    }

    public Log setSource( Class<?> source )
    {
        this.source = source;
        return this;
    }

    @JsonProperty
    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    @JsonProperty
    public String getUsername()
    {
        return username;
    }

    public Log setUsername( String username )
    {
        this.username = username;
        return this;
    }

    @JsonProperty
    public Map<String, Object> getMetadata()
    {
        return metadata;
    }

    public void setMetadata( Map<String, Object> metadata )
    {
        this.metadata = metadata;
    }

    @JsonProperty
    public LogData getData()
    {
        return data;
    }

    public void setData( LogData data )
    {
        this.data = data;
    }

    // ~ Utility methods
    // ========================================================================================================

    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        builder.append( "* " ).append( logLevel ).append( " " );
        builder.append( Instant.ofEpochMilli( timestamp ).toString() ).append( " " );
        builder.append( message );

        if ( source != null )
        {
            builder.append( " (" ).append( source.getSimpleName() ).append( ".java)" );
        }

        if ( username != null )
        {
            builder.append( " [" ).append( username ).append( "]" );
        }

        return builder.toString();
    }

    public boolean isFatal()
    {
        return LogLevel.FATAL == logLevel;
    }

    public boolean isError()
    {
        return LogLevel.ERROR == logLevel;
    }

    public boolean isWarn()
    {
        return LogLevel.WARN == logLevel;
    }

    public boolean isInfo()
    {
        return LogLevel.INFO == logLevel;
    }

    public boolean isDebug()
    {
        return LogLevel.DEBUG == logLevel;
    }

    public boolean isTrace()
    {
        return LogLevel.TRACE == logLevel;
    }

    public boolean isEnabled( LogLevel logLevel )
    {
        return logLevel.isEnabled( logLevel );
    }
}
