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
import java.lang.NullPointerException;
import javax.script.ScriptEngine;
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class EngineSE extends Engine
{

    protected static final Log log = LogFactory.getLog ( EngineSE.class );


    public ScriptEngine engine;

    public EngineSE(ScriptEngine engine){
        this.engine=engine;
    }

    @Override
    public Object evaluateScript ()
    throws ScriptException
    {
        Reader scriptReader = getScriptReader();
        log.info ( "EngineSE - eval start" );

        if ( execContext == null )
        {
            log.info ( "EngineSE - bad script context" );
            throw new ScriptExecutionException ( "Null script context" );
        }


        if ( engine == null )
        {
            log.info ( "EngineSE - bad SE" );
            throw new ScriptExecutionException ( "Bad ScriptEngine" );
        }


        try
        {
            log.info ( "EngineSE - putting context" );
            ScriptContext ctx = engine.getContext();
            ctx.setWriter ( execContext.getOut() );
            ctx.setErrorWriter ( execContext.getError() );
            ctx.setReader ( execContext.getIn() );
            log.info ( "EngineSE - setting execution context for "
                                 + execContext.getAppKey() + ":" + execContext.getScriptName() + " to:\n"
                                 + execContext.toString() );
            engine.put ( "dhisScriptContext", execContext );
            Object res  =  engine.eval ( scriptReader );
            log.info ( "EngineSE - eval done" );
            log.info ( "EngineSE - eval execution context=" + execContext.toString() );
            Object o = ctx.getAttribute ( "dhisScriptContext" );

            if ( o != null )
            {
                log.info ( "EngineSE - eval execution context=" + o.toString() );
            }

            return res;
        }

        catch ( Exception e )
        {
            log.info ("Error running script engine: "  + e.toString() + "\n" +
                        ExceptionUtils.getStackTrace(e));
            throw new ScriptExecutionException("Could not execute script:" + e.toString());
        }
    }


}