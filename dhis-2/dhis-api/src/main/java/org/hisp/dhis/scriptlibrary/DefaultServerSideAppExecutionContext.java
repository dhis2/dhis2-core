package org.hisp.dhis.scriptlibrary;
/*
 * Copyright (c) 2016, IntraHealth International
 * All rights reserved.
 * Apache 2.0
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

import java.io.Reader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.user.User;
import org.springframework.context.ApplicationContext;


public class DefaultServerSideAppExecutionContext implements ServerSideAppExecutionContext
{

    protected static final Log log = LogFactory.getLog( DefaultServerSideAppExecutionContext.class );
    protected ApplicationContext applicationContext;
    protected Reader in;
    protected Writer out;
    protected Writer error;
    protected User user;
    protected String scriptName;
    protected String appKey;

    public DefaultServerSideAppExecutionContext()
    {
        user = null;
        in = new StringReader( "" );
        out = new StringWriter();
        error = new StringWriter();
    }

    /*
     * IExecutionContext Interface  methods
     */

    public String toString()
    {
        return "DefaultServerSideAppExecutionContext for:\n\tappKey=" + getAppKey()
            + "\n\tfor script name=" + getScriptName()
            + "\n\tfor user " + user.toString()
            ;
    }

    public String getAppKey()
    {
        log.debug( "Getting appKey" + appKey );
        return appKey;
    }

    public String getScriptName()
    {
        return scriptName;
    }

    public User getUser()
    {
        return user;
    }

    public Reader getIn()
    {
        return in;
    }

    public Writer getOut()
    {
        return out;
    }

    public Writer getError()
    {
        return error;
    }

    public ApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    public void setApplicationContext( ApplicationContext applicationContext )
    {
        this.applicationContext = applicationContext;
    }

    public void setAppKey( String appKey )
    {
        this.appKey = appKey;
    }

    public void setScriptName( String scriptName )
    {
        this.scriptName = scriptName;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public void setIn( Reader in )
    {
        this.in = in;
    }

    public void setOut( Writer out )
    {
        this.out = out;
    }

    public void setError( Writer error )
    {
        this.error = error;
    }

    public void logInfo( Object o )
    {
        log.info( "[" + appKey + ":" + scriptName + "] " + o );
    }

    public void logDebug( Object o )
    {
        log.debug( "[" + appKey + ":" + scriptName + "] " + o );
    }

    public void logError( Object o )
    {
        log.error( "[" + appKey + ":" + scriptName + "] " + o );
    }

    public void logFatal( Object o )
    {
        log.fatal( "[" + appKey + ":" + scriptName + "] " + o );
    }


}