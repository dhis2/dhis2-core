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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



abstract public class ServerSideAppEngine implements ServerSideAppEngine
{

    private static final Log log = LogFactory.getLog(ServerSideAppEngine.class);

    //@Autowired this is not a bean? so won't work/do anything?
    //protected SessionFactory sessionFactory;


    public ServerSideAppExecutionContext execContext = null;

    public ServerSideAppExecutionContext getExecutionContext() {
        return execContext;
    }

    @Override
    public void setExecutionContext(ServerSideAppExecutionContext execContext) {
        this.execContext = execContext;
    }



    protected Reader scriptReader;

    public void setScriptReader(Reader r) {
        scriptReader = r;
    }
    public Reader getScriptReader() {
        return scriptReader;
    }

    @Override
    public Object call()
            throws ScriptException
    {
        Object res =null;
        log.info("Run ServerSideAppEngine: beginning execution");

        if (execContext.getUser() == null) {
            //sanity check.
            throw new ScriptAccessException("No script execution on null user allowed");
        }

        if ( execContext.getScriptName() == null) {
            //sanity check.
            throw new ScriptNotFoundException("No script defined");
        }
        log.info("Run ServerSideAppEngine: evaluating script " + execContext.getScriptName());
        try {
            res = evaluateScript();
        } catch (ScriptException e) {
            throw e; //don't do anything speciak
        } catch (Exception e) {
            //wrap it in a script execution exception
            log.info("evaluation failed : " + e.toString() + "\n" +
                    ExceptionUtils.getStackTrace(e));
            throw new ScriptExecutionException("evaluation failed : " + e.toString() );
        }
        log.info("Run ServerSideAppEngine: evaluation done");
        return res;

    }


}