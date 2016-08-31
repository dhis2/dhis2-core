package org.hisp.dhis.reporting.reportviewer.action;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.util.ContextUtils;
import org.springframework.core.io.ClassPathResource;

import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 */
public class GetReportTemplateAction
    implements Action
{
    private static final String TEMPLATE_JASPER = "jasper-report-template.jrxml";
    private static final String TEMPLATE_HTML = "html-report-template.html";
    
    private static final String TYPE_JASPER = "jasper";
    private static final String TYPE_HTML = "html";
    
    private String type;
        
    public void setType( String type )
    {
        this.type = type;
    }

    @Override
    public String execute()
        throws Exception
    {
        HttpServletResponse response = ServletActionContext.getResponse();

        Map<String, String> typeTemplateMap = new HashMap<>();
        typeTemplateMap.put( TYPE_JASPER, TEMPLATE_JASPER );
        typeTemplateMap.put( TYPE_HTML, TEMPLATE_HTML );
        
        Map<String, String> typeContentTypeMap = new HashMap<>();
        typeContentTypeMap.put( TYPE_JASPER, ContextUtils.CONTENT_TYPE_XML );
        typeContentTypeMap.put( TYPE_HTML, ContextUtils.CONTENT_TYPE_HTML );
        
        if ( type != null & typeTemplateMap.containsKey( type ) )
        {
            String template = typeTemplateMap.get( type );
            String contentType = typeContentTypeMap.get( type );
            
            ContextUtils.configureResponse( response, contentType, false, template, true );
            
            IOUtils.copy( new BufferedInputStream( new ClassPathResource( template ).getInputStream() ), 
                new BufferedOutputStream( response.getOutputStream() ) );
        }
        
        return SUCCESS;
    }
}
