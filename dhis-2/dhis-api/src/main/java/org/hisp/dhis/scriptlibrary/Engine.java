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

import javax.script.ScriptException;
import org.hisp.dhis.user.User;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.scriptlibrary.IExecutionContext ;
import org.hisp.dhis.scriptlibrary.ScriptNotFoundException;
import org.hisp.dhis.scriptlibrary.ScriptAccessException;

abstract public class Engine implements Runnable
{
    protected abstract Object eval (  IExecutionContext execContext )
    throws ScriptException, ScriptNotFoundException, ScriptAccessException;

    protected App app;
    protected ScriptLibrary sl;

    public Engine ( App app, ScriptLibrary sl )
    {
        this.app = app;
        this.sl = sl;
    }

    public IExecutionContext execContext = null;
    public IExecutionContext getScriptContext()
    {
        return execContext;
    }
    public void setExecutionContext ( IExecutionContext execContext )
    {
        this.execContext = execContext;
    }
    public Exception runException ;
    public Object runResult ;

    @Override
    public void run()
    {
        System.out.println ( "Run Engine: 0 run" );
        runException = null;

        if ( execContext.getUser() == null )
        {
            //sanity check.
            runException = new ScriptAccessException ( "No script execution on null user allowed" );
            return;
        }

        if ( execContext.getScriptName() == null )
        {
            //sanity check.
            runException = new ScriptNotFoundException ( "No script defined" );
            return;
        }

        runResult = null;

        try
        {
            System.out.println ( "Run Engine: eval" );
            System.out.println ( "Run Engine: scriptName " + execContext.getScriptName() );
            runResult = eval ( execContext );
            System.out.println ( "Run Engine: eval done" );
        }

        catch ( Exception e )
        {
            runException = new ScriptExecutionException ( "Could not evaluate script " + e.toString() );
            return;
        }

        return;
    }





}