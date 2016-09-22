package org.hisp.dhis.web.ohie.fhir.webapi;
/*
 * Copyright (c) 2016, IntraHealth International
 * All rights reserved.
 * Apache 2.0
 * Carl Leitner <litlfred@gmail.com>
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

import ca.uhn.fhir.model.api.Bundle;
import ca.uhn.fhir.model.api.IResource;
import java.io.Reader;
import java.io.InputStreamReader;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.TreeNode;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.datavalue.DefaultDataValueService;
import org.hisp.dhis.scriptlibrary.*;
import org.hisp.dhis.webapi.controller.EngineController;
import org.hisp.dhis.webapi.scriptlibrary.ExecutionContextHttp;
import org.hisp.dhis.webapi.scriptlibrary.HttpException;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.hisp.dhis.web.ohie.fhir.service.DSTU2Processor;


/**
* @author Carl Leitner <litlfred@gmail.com>
 */
@Controller
@ComponentScan ( basePackages = {"org.hisp.dhis.webapi.service"} )
@RequestMapping (  value =  EngineControllerDSTU2.RESOURCE_PATH  + "/{app}" )
public class EngineControllerDSTU2 extends EngineController
{

    @Autowired
    AppManager appManager;

    public static final String RESOURCE_PATH = "/fhir/dstu2";

    protected static final Log log = LogFactory.getLog ( DefaultDataValueService.class );

    public class ExecutionContextFHIR extends ExecutionContextHttp
    {
        protected Object response = null;
        public Object getResponse()
        {
            return response;
        }
        public void setResponse ( Object response )
        {
            this.response = response;
        }
        protected String outputType = null;
        public String getOutputType()
        {
            return outputType;
        }
        public void setOutputType ( String outputType )
        {
            this.outputType = outputType;
        }
        protected Boolean isXml = false;
        public Boolean getIsXml()
        {
            return isXml;
        }
        public void setIsXml ( Boolean isXml )
        {
            this.isXml = isXml;
        }
        protected int errorCode = 200 ;
        public int getErrorCode()
        {
            return errorCode;
        }
        public void setErrorCode ( int errorCode )
        {
            this.errorCode = errorCode;
        }
        protected String id;
        public String getId()
        {
            return id;
        }
        public void setId ( String id )
        {
            this.id = id;
        }
        protected String operation;
        public String getOperation()
        {
            return operation;
        }
        public void setOperation ( String operation )
        {
            this.operation = operation;
        }
        protected String resourceName;
        public String getResourceName()
        {
            return this.resourceName;
        }
        public void setResourceName ( String resourceName )
        {
            this.resourceName = resourceName;
        }
        protected String errorMessage = "";
        public String getErrorMessage()
        {
            return errorMessage;
        }
        public void setErrorMessage ( String errorMessage )
        {
            this.errorMessage = errorMessage;
        }
        protected Object bundle;
        public Object getBundle()
        {
            return bundle;
        }
        public void setBundle ( Object bundle )
        {
            this.bundle = bundle;
        }
        protected Object resource;
        public Object getResource()
        {
            return resource;
        }
        public void setResource ( Object resource )
        {
            this.resource  = resource;
        }

        public String toString()
        {
            return super.toString()
                   + "\n\tid=" + id + "\n"
                   + "\terrorCode=" + errorCode + "\n"
                   + "\terrorMessage=" + errorMessage + "\n"
                   + "\toperation=" + operation + "\n"
                   + "\tresponse=" + response + "\n"
                   ;
        }

    }


    protected DSTU2Processor fhirProcessor  = new DSTU2Processor();

    @RequestMapping (
        value =  "/{resource}/{id}",
        method = RequestMethod.GET
    )
    public void operationRead ( HttpServletResponse httpResponse, HttpServletRequest httpRequest,
                                @PathVariable ( "app" ) String app,
                                @PathVariable ( "id" ) String id  , @PathVariable ( "resource" ) String resource )
    {
        log.info ( "Received read request for " + resource + " on id [" + id + "]" );
        doOperation ( httpRequest, httpResponse, app, resource, "read", id );
    }

    @RequestMapping (
        value =  "/{resource}/{id}/{extended}",
        method = RequestMethod.GET
    )
    public void operationExtended ( HttpServletResponse httpResponse, HttpServletRequest httpRequest,
                                    @PathVariable ( "app" ) String app, @PathVariable ( "extended" ) String extOp,
                                    @PathVariable ( "id" ) String id  , @PathVariable ( "resource" ) String resource )
    {
        log.info ( "Received extended operation request [" + extOp + "] for " + resource + " on id [" + id + "]" );
        doOperation ( httpRequest, httpResponse, app, resource, extOp, id );
    }

    @RequestMapping (
        value =  "/{resource}",
        method = RequestMethod.GET
    )
    public void operationSearch ( HttpServletResponse httpResponse, HttpServletRequest httpRequest,
                                  @PathVariable ( "app" ) String app, @PathVariable ( "resource" ) String resource )
    {
        doOperation ( httpRequest, httpResponse, app, resource, "search" );
    }

    @RequestMapping (
        value =  "/{resource}",
        method = RequestMethod.POST
    )
    public void operationCreate ( HttpServletResponse httpResponse, HttpServletRequest httpRequest,
                                  @PathVariable ( "app" ) String app, @PathVariable ( "resource" ) String resource )
    {
        doOperation ( httpRequest, httpResponse, app, resource, "create" );
    }

    @RequestMapping (
        method = RequestMethod.POST
    )
    public void operationBatch ( HttpServletResponse httpResponse, HttpServletRequest httpRequest,
                                 @PathVariable ( "app" ) String app )
    {
        doOperation ( httpRequest, httpResponse, app, null, "batch" );
    }

    protected void doOperation ( HttpServletRequest httpRequest, HttpServletResponse httpResponse, String app, String resource, String operation )
    {
        doOperation ( httpRequest, httpResponse, app, resource, operation, null );
    }



    protected void doOperation ( HttpServletRequest httpRequest, HttpServletResponse httpResponse, String appName, String resource, String operation, String id )
    {
        log.info ( "Attempting " + operation + " on " + resource + " in " + appName );
        String contextPath =  ContextUtils.getContextPath ( httpRequest );
        TreeNode info;

        try
        {

            info =  appManager.retrieveManifestInfo ( appName, ( "script-library/bindings" + RESOURCE_PATH ).split ( "/" ) );

            if ( info == null || ! info.isArray())
            {
                log.info ( "D" );
                throw new NullPointerException ( "No binding info for " + appName );
            }
        }

        catch ( Exception e )
        {
            log.info ( "Could not retrieve binding info for " + appName + ":"  + e.toString() );
            e.printStackTrace ( System.out );
            return;
        }


        for ( int i=0; i < info.size(); i++ )
        {
            if (! info.get(i).isObject()) {
                continue;
            }
            TreeNode node = info.get(i);
            String script = null;
            String op = null;

            try
            {
                op = node.get ( "operation" ).toString();
                script = node.get ( "script" ).toString();

                if ( !op.equals ( operation )
                        ||  script == null
                   )
                {
                    log.info ( "Skipping: " + script + "/" + op +  " against " + operation + "/" + resource );
                    continue;
                }

                TreeNode rsrc = node.get ( "resource" );

                if ( rsrc.isValueNode() )
                {
                    String r =  rsrc.toString();

                    if (  ( ( resource != null ) && ( ! resource.equals ( r ) ) ) )
                    {
                        log.info ( "Skipping: " + script + "/" + op + "/" + r + " against " + operation + "/" + resource );
                        continue;
                    }
                }

                else if ( rsrc.isArray())
                {
                    for ( int j=0; j < rsrc.size(); j++)
                    {
                        String r = null;

                        try
                        {
                            r = rsrc.get(i).toString();

                            if (   ( ( resource != null ) && ( ! resource.equals ( r ) ) ) )
                            {
                                log.info ( "Skipping: " + script + "/" + op + "/" + r + " against " + operation + "/" + resource );
                                continue;
                            }

                            break;  //we have a match
                        }

                        catch ( Exception e )
                        {
                            log.info ( "Skipping: " + script + "/" + op + "/" + r + " against " + operation + "/" + resource + ":\n" + e.toString() );
                            continue;
                        }
                    }
                }
            }

            catch ( Exception e )
            {
                log.info ( "Skipping: " + e.toString() );
                continue;
            }

            try
            {
                ExecutionContextFHIR execContext = getExecutionContext ( script, httpRequest, httpResponse, appName, resource, operation, id );
                Object result =  engineService.eval(execContext);
                processOperationResult ( result ,execContext);
            }

            catch ( HttpException he )
            {
                sendError ( httpResponse, he.getStatusCode(), he.getMessage() );
                return ;
            }

            catch ( ScriptAccessException ea )
            {
                sendError ( httpResponse, HttpServletResponse.SC_NOT_IMPLEMENTED, ea.toString() );
                return ;
            }

            catch ( ScriptNotFoundException enf )
            {
                sendError ( httpResponse, HttpServletResponse.SC_NOT_IMPLEMENTED, enf.toString() );
                return ;
            }

            catch ( Exception e )
            {
                sendError ( httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "internal script processing error for " + script + " on " + resource + ":\n" + e.toString() );
                return ;
            }

            return; //we break out of the loop
        }

        log.info ( "No binding information for " + operation + " on " + resource + " in " + appName );
        sendError ( httpResponse, HttpServletResponse.SC_NOT_IMPLEMENTED, "Operation " + operation + " not found on resource " + resource );
    }

    protected void processOperationResult ( Object response, ExecutionContextFHIR execContext )
    throws HttpException
    {
        log.info ( "Processing result" );
        log.info ( "retrieved script context" );
        HttpServletResponse httpResponse = execContext.getHttpServletResponse();
        log.info ( "retrieved script http response" );

        if ( execContext.getErrorCode() >= 400 )
        {
            throw new HttpException ( execContext.getErrorMessage(), execContext.getErrorCode() );
        }

        log.info ( "no error reported by script" );


        if ( response == null )
        {
            log.info ( "Got null response" );
        }

        else
        {
            log.info ( "Got response:" + response.toString() );
        }

        String json = null;


        if ( response instanceof ScriptObjectMirror && response != null )
        {
            log.info ( "response is ScriptObjectMirror" );
            ScriptObjectMirror responseSom  = ( ScriptObjectMirror ) response;
            log.info ( "creating JSON representation of response" );
            json = response.toString();

        }

        else if ( response instanceof Map && response != null )
        {
            log.info ( "response is Map" );
            json =  JSONObject.toJSONString ( ( Map ) response );
        }

        else
        {
            log.info ( "response is null" );
        }

        //make sure we can validate as  valid
        String out;

        if ( json == null )
        {
            log.info ( "json response is null" );
            out = "";
        }

        else
        {
            log.info ( "post processing json response:" + execContext.getOutputType() );

            switch ( execContext.getOutputType() )
            {
            case "resource":
                log.info ( "post processing json response - resource" );
                IResource r = fhirProcessor.resourceFromJSON ( json );
                log.info ( "post processing json response - created resource" );

                if ( execContext.getIsXml() )
                {
                    log.info ( "post processing json response - output XML" );
                    out = fhirProcessor.resourceToXML ( r );
                }

                else
                {
                    log.info ( "post processing json response - output JSON" );
                    out = fhirProcessor.resourceToJSON ( r );
                }

                break;

            case "bundle":
                log.info ( "post processing json response - bundle" );
                Bundle b = fhirProcessor.bundleFromJSON ( json );

                if ( execContext.getIsXml() )
                {
                    log.info ( "post processing json response - output XML" );
                    out = fhirProcessor.bundleToXML ( b );
                }

                else
                {
                    log.info ( "post processing json response - output JSON" );
                    out = fhirProcessor.bundleToJSON ( b );
                }

                break;

            default:
                out = response.toString();
                //do nothing
            }
        }

        log.info ( "output=\n" + out );

        try
        {
            httpResponse.getWriter().write ( out );
            httpResponse.getWriter().flush();
            httpResponse.getWriter().close();
        }

        catch ( Exception e )
        {
            log.info ( "Could not write to http response output stream:" + e.toString() );
        }

        //send the output and handle error codes
    }




    protected ExecutionContextFHIR getExecutionContext ( String script, HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            String appName, String resource, String operation, String id )
    {
        ExecutionContextFHIR execContext = new ExecutionContextFHIR();
        execContext.setScriptName(script);
        initExecutionContext ( execContext, appName,  httpRequest, httpResponse );
        String contentType = httpRequest.getContentType();
        String format = httpRequest.getParameter ( "_format" );
        execContext.setIsXml (  ( contentType == "application/xml" )
                                || ( contentType == "application/xml+fhir" )
                                || ( contentType == "text/xml" )
                             );
        execContext.setResourceName ( resource );
        String inputType;
        execContext.setOperation ( operation );

        if ( operation != null )
        {
            inputType = fhirProcessor.operationsInput.get ( operation );
        }

        else
        {
            inputType = "bundle"; //e.g. for bundle
        }

        execContext.setOutputType ( fhirProcessor.operationsOutput.get ( operation ) );
        String method = httpRequest.getMethod().toUpperCase();
        execContext.setId ( id );

        if ( method == "POST" || method == "PUT" )
        {
            try
            {
                Reader in = new InputStreamReader ( httpRequest.getInputStream() );

                //convert to json object with HAPI
                switch ( inputType )
                {
                case "resource":
                    IResource res;

                    if ( execContext.getIsXml() )
                    {
                        res = fhirProcessor.resourceFromXML ( in );
                    }

                    else
                    {
                        res = fhirProcessor.resourceFromJSON ( in );
                    }

                    execContext.setResource ( res );
                    break;

                case "bundle":
                    Bundle bun;

                    if ( execContext.getIsXml() )
                    {
                        bun = fhirProcessor.bundleFromXML ( in );
                    }

                    else
                    {
                        bun = fhirProcessor.bundleFromJSON ( in );
                    }

                    execContext.setBundle ( bun );
                    break;

                default:
                    log.info ( "unknown input type for " + operation + " on " + resource );
                    sendError ( httpResponse, HttpServletResponse.SC_BAD_REQUEST, "unknown input type for " + operation + " on " + resource );
                    return null;
                }
            }

            catch ( Exception e )
            {
                sendError ( httpResponse, HttpServletResponse.SC_BAD_REQUEST, "could not process input: " + e.toString() );
                return null;
            }
        }

        return execContext;
    }

}