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

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.scriptlibrary.AppScriptLibrary;
import org.hisp.dhis.scriptlibrary.ExecutionContext;
import org.hisp.dhis.scriptlibrary.Engine;
import org.hisp.dhis.scriptlibrary.EngineBuilder;
import org.hisp.dhis.scriptlibrary.ScriptLibrary;
import org.hisp.dhis.scriptlibrary.ScriptAccessException;
import org.hisp.dhis.scriptlibrary.ScriptNotFoundException;
import org.hisp.dhis.webapi.controller.EngineController;
import org.hisp.dhis.webapi.scriptlibrary.IExecutionContextHttp;
import org.hisp.dhis.webapi.scriptlibrary.ExecutionContextHttp;
import org.hisp.dhis.webapi.scriptlibrary.ExecutionContextHttpSE;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


/**
* @author Carl Leitner <litlfred@gmail.com>
 */
@Controller
@RequestMapping (
    value =  "/{app}"
)
public class EngineControllerAction  extends EngineController
{
    public static final String PATH = "/ssa";

    public class ExecutionContextActionSE extends ExecutionContextHttpSE
    {
        protected String requestRemainder = null;
        public String getRequestRemainder()
        {
            return requestRemainder;
        }
        public void setRequestRemainder ( String rr )
        {
            requestRemainder = rr;
        }
        public String toString()
        {
            return super.toString() +   "\n\trequestRemainder=" + requestRemainder + "\n";
        }
    }
    public class ExecutionContextAction extends ExecutionContextHttp
    {
        protected String requestRemainder = null;
        public String getRequestRemainder()
        {
            return requestRemainder;
        }
        public void setRequestRemainder ( String rr )
        {
            requestRemainder = rr;
        }
        public String toString()
        {
            return super.toString() +   "\n\trequestRemainder=" + requestRemainder + "\n";
        }
    }



    @RequestMapping (
        value =   {"/{app}/index.js"}
    )
    public void execScript ( HttpServletResponse httpResponse, HttpServletRequest httpRequest,
                             @PathVariable ( "app" ) String appName )
    {
        try
        {
            ExecutionContextHttpSE execContext = new ExecutionContextHttpSE();
            initExecutionContext ( execContext, appName,  httpRequest, httpResponse );
            Engine engine = runEngine ( execContext, "index.js" );

            if ( engine == null )
            {
                throw new ScriptAccessException ( "Could not run engine for index.js" );
            }

            else if ( engine.runException != null )
            {
                log.error ( "Script index.js in " + appName + " had a run time exception:\n" + engine.runException.toString()  , engine.runException );
                throw engine.runException;
            }

            execContext.getOut().flush();
            execContext.getOut().close();

        }

        catch ( Exception e )
        {
            sendError ( httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "internal script processing error\n" + e.toString()  );
        }

    }


    @RequestMapping (
        value =   { "/{app}" + PATH + "/{script}", "/{app}" + PATH + "/{script}/*" , "/{app}" + PATH + "/{script}/**/*"}
    )
    public void execScript ( HttpServletResponse httpResponse, HttpServletRequest httpRequest,
                             @PathVariable ( "app" ) String appName, @PathVariable ( "script" ) String script )
    {
        try
        {
            log.info ( "Received request to run  " + script + " in app " + appName );
            String contextPath =  ContextUtils.getContextPath ( httpRequest );
            App app = appManager.getApp ( appName, contextPath );

            if ( app == null )
            {
                throw new ScriptAccessException ( "App " + appName + " not registered" );
            }

            ScriptLibrary sl = getScriptLibrary ( app );
            //loop through the manifest info to make sure this script is registered
            JsonArray info = ( JsonArray ) sl.retrieveManifestInfo ( ( "script-library/bindings" + PATH ).split ( "/" ) );
            boolean matched = false;

            for ( JsonValue value : info )
            {
                try
                {
                    matched  = ( ( JsonObject ) value ).getString ( "script" ).compareTo ( script ) == 0;
                }

                catch ( Exception e ) {}

                if ( matched )
                {
                    break;
                }
            }

            if ( ! matched )
            {
                throw new ScriptNotFoundException ( "Script " + script + " is not registered for app " + appName );
            }

            //create and initialize the http execution context to send to the script
            IExecutionContextHttp execContext = getExecutionContext ( httpRequest, httpResponse, appName, script );

            //instantiate an Engine and run the script against the contexxt
            log.info ( "Running " + script + " in app " + appName + " with context=" + execContext.toString() );
            Engine engine = runEngine ( execContext, script );

            if ( engine == null )
            {
                throw new ScriptAccessException ( "Could not run engine for " + script );
            }

            else if ( engine.runException != null )
            {
                log.error ( "Script " + script + " in " + appName + " had a run time exception:\n" + engine.runException.toString()  , engine.runException );
                throw engine.runException;
            }

            execContext.getOut().flush();
            execContext.getOut().close();

        }

        catch ( ScriptAccessException ea )
        {
            sendError ( httpResponse, HttpServletResponse.SC_NOT_IMPLEMENTED, ea.toString() );
        }

        catch ( ScriptNotFoundException enf )
        {
            sendError ( httpResponse, HttpServletResponse.SC_NOT_IMPLEMENTED, enf.toString() );
        }

        catch ( ScriptException e )
        {
            sendError ( httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.toString() );
        }

        catch ( Exception e1 )
        {
            log.error ( "Received request to run  " + script + " in app " + appName , e1 );
            sendError ( httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "internal script processing error\n" + e1.toString()  );
        }

        return;
    }


    protected IExecutionContextHttp getExecutionContext ( HttpServletRequest httpRequest, HttpServletResponse httpResponse, String appName, String script )
    {
        String ext = FilenameUtils.getExtension ( script );
        IExecutionContextHttp execContext;

        if ( ext.compareToIgnoreCase ( "xslt" ) == 0
                || ext.compareToIgnoreCase ( "xsl" ) == 0
                || ext.compareToIgnoreCase ( "xq" ) == 0
           )
        {
            ExecutionContextAction execContextAction = new ExecutionContextAction();
            execContextAction.requestRemainder = ContextUtils.getContextPath ( httpRequest );
            execContext = execContextAction;
        }

        else
        {
            ExecutionContextActionSE execContextSE = new ExecutionContextActionSE();
            execContextSE.requestRemainder = ContextUtils.getContextPath ( httpRequest );
            execContext = execContextSE;
        }

        initExecutionContext ( execContext, appName,  httpRequest, httpResponse );
        return execContext;
    }

}