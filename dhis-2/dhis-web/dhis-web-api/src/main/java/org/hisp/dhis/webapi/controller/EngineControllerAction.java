package org.hisp.dhis.webapi.controller;
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


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.commons.io.FilenameUtils;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.scriptlibrary.*;
import org.hisp.dhis.webapi.scriptlibrary.ExecutionContextHttpInterface;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.scriptlibrary.ExecutionContextHttp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;


/**
* @author Carl Leitner <litlfred@gmail.com>
 */
@Controller
@RequestMapping (
    value =  "/{app}"
)
public class EngineControllerAction  extends EngineController
{
    public static final String PATH = "ssa";

    @Autowired
    protected AppManager appManager;



    @RequestMapping (
        value =   {
                "/{app}/" + PATH + "/{script:.+}",
	    }
    )
    public void execScript ( HttpServletResponse httpResponse, HttpServletRequest httpRequest,
                             @PathVariable ( "app" ) String appKey, 
			     @PathVariable ( "script" ) String script )
    {
        try
        {
            //http://stackoverflow.com/questions/25382620/get-only-wildcard-part-of-requestmapping

            log.info ( "Received request to run  " + script + " in app " + appKey  )  ;

            script = PATH + "/" + script; //only do scripts under ssa directory.

            //create and initialize the http execution context to send to the script
            ExecutionContextHttpInterface execContext = getExecutionContext ( httpRequest, httpResponse, appKey, script );

            //instantiate an Engine and run the script against the contexxt
            log.info ( "Running " + script + " in app " + appKey + " with context=" + execContext.toString() );
            Object result =  engineService.eval(execContext);  //we are not doing anything with the result
            execContext.getOut().flush();
            execContext.getOut().close();

        }

        catch ( ScriptAccessException e )
        {
            sendError ( httpResponse, HttpServletResponse.SC_NOT_IMPLEMENTED, e.toString() );
        }

        catch ( ScriptNotFoundException e )
        {
            sendError ( httpResponse, HttpServletResponse.SC_NOT_IMPLEMENTED, e.toString() );
        }

        catch ( ScriptException e )
        {
            sendError ( httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString() );
        }

        catch ( Exception e )
        {
            log.error ( "Received request to run  " + script + " in app " + appKey , e);
            sendError ( httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "internal script processing error\n" + e.toString()  );
        }

        return;
    }


    protected ExecutionContextHttpInterface getExecutionContext ( HttpServletRequest httpRequest, HttpServletResponse httpResponse, String appKey, String script )
    {
        String ext = FilenameUtils.getExtension ( script );

        ExecutionContextHttp execContext = new ExecutionContextHttp();
        execContext.setScriptName(script);
        initExecutionContext ( execContext, appKey,  httpRequest, httpResponse );
        return execContext;
    }

}