package org.hisp.dhis.webapi.scriptlibrary;
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
import org.hisp.dhis.scriptlibrary.ExecutionContext;
import org.hisp.dhis.webapi.scriptlibrary.IExecutionContextHttp;

public class ExecutionContextHttp extends ExecutionContext implements IExecutionContextHttp
{
    /*
     *
     *    any library dependencies are loaded.  the context has the following public
     *    variables set:
     *      * httpRequest    - the HttpServletRequest
     *      * httpRsponse   - the HttpServletResponse
     *      * user            - DHIS2 User object
     *      * (streams)       - IO Streams for script execution are in SteamReader in, StreamWriter error & out
     */


    /*
     * Begin public class variables.  These are exposed to the script
     */

    public HttpServletResponse httpResponse = null;
    public HttpServletRequest httpRequest = null;
    /*
     * end public class variables.  These are exposed to the script
     */
    public HttpServletResponse getHttpServletResponse()
    {
        return  httpResponse ;
    }
    public HttpServletRequest getHttpServletRequest()
    {
        return httpRequest;
    }

    public void setHttpServletResponse ( HttpServletResponse httpResponse )
    {
        this.httpResponse = httpResponse;
    }
    public void setHttpServletRequest ( HttpServletRequest httpRequest )
    {
        this.httpRequest = httpRequest;
    }

}