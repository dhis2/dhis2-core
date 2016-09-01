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
import java.util.Map;
import java.lang.NullPointerException;
import javax.script.Bindings;
import javax.script.SimpleScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.ScriptContext;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.scriptlibrary.Engine;
import org.hisp.dhis.scriptlibrary.IExecutionContext;
import org.hisp.dhis.scriptlibrary.IExecutionContextSE;
import org.hisp.dhis.scriptlibrary.ScriptNotFoundException;
import org.hisp.dhis.scriptlibrary.ScriptAccessException;
import org.springframework.beans.factory.annotation.Autowired;

public class EngineSE extends Engine
{
    @Autowired
    protected  AppManager appManager;

    public ScriptEngine engine;
    public ScriptContext context;
    public EngineSE ( App app, ScriptLibrary sl, ScriptEngine engine )
    {
        super ( app, sl );
        this.engine = engine;
    }


    @Override
    protected Object eval ( IExecutionContext execContext )
    throws ScriptException, ScriptNotFoundException, ScriptAccessException
    {
        if ( ! ( execContext instanceof IExecutionContextSE ) )
        {
            throw new ScriptException ( "Script execution context must implement IExecutionContextSE" );
        }

        System.out.println ( "EngineSE - eval start" );
        Reader source;

        if ( execContext == null )
        {
            System.out.println ( "EngineSE - bad script context" );
            throw new NullPointerException ( "Null script context" );
        }

        if ( sl == null )
        {
            System.out.println ( "EngineSE - bad lib" );
            throw new NullPointerException ( "Bad script library" );
        }

        if ( engine == null )
        {
            System.out.println ( "EngineSE - bad SE" );
            throw new NullPointerException ( "Bad ScriptEngine" );
        }

        try
        {

            System.out.println ( "EngineSE - trying retrieve" );
            source = sl.retrieveScript ( execContext.getScriptName() );
        }

        catch ( Exception e )
        {
            System.out.println ( "EngineSE - bad retrieve" );
            throw new ScriptNotFoundException ( "Could not retrieve script "  + execContext.getScriptName() + "\n" + e.toString() );
        }


        try
        {
            System.out.println ( "EngineSE - putting context" );
            ScriptContext ctx = engine.getContext();
            ctx.setWriter ( execContext.getOut() );
            ctx.setErrorWriter ( execContext.getError() );
            ctx.setReader ( execContext.getIn() );
            System.out.println ( "EngineSE - setting execution context for "
                                 + execContext.getAppName() + ":" + execContext.getScriptName() + " to:\n"
                                 + execContext.toString() );
            engine.put ( "dhisScriptContext", execContext );
            Object res  =  engine.eval ( source );
            System.out.println ( "EngineSE - eval done" );
            System.out.println ( "EngineSE - eval execution context=" + execContext.toString() );
            Object o = ctx.getAttribute ( "dhisScriptContext" );

            if ( o != null )
            {
                System.out.println ( "EngineSE - eval execution context=" + o.toString() );
            }

            return res;
        }

        catch ( Exception e )
        {
            System.out.println ( e );
            e.printStackTrace ( System.out );
            runException = e;
            return null;
        }
    }


}