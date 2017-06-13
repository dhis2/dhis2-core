package org.hisp.dhis.util;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public abstract class StreamActionSupport
    implements Action
{
    private static final Log log = LogFactory.getLog( StreamActionSupport.class );

    // -------------------------------------------------------------------------
    // ActionSupport implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        OutputStream out = null;
        
        HttpServletResponse response = ServletActionContext.getResponse();
        
        String contentType = getContentType();
        boolean disallowCache = disallowCache();
        String filename = getFilename();
        boolean attachment = attachment();
        
        ContextUtils.configureResponse( response, contentType, disallowCache, filename, attachment );
        
        log.debug( "Content type: " + contentType + ", disallow cache: " + 
            disallowCache + ", filename: " + filename + ", attachment: " + attachment );
        
        try
        {
            out = response.getOutputStream();
            
            return execute( response, out );
        }
        finally
        {
            IOUtils.closeQuietly( out );
        }
    }

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    protected abstract String execute( HttpServletResponse response, OutputStream out )
        throws Exception;
    
    protected abstract String getContentType();
    
    protected abstract String getFilename();
    
    protected boolean disallowCache()
    {
        return true;
    }
    
    protected boolean attachment()
    {
        return true;
    }
}
