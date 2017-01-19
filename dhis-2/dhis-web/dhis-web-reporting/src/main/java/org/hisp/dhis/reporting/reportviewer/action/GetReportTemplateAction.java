package org.hisp.dhis.reporting.reportviewer.action;

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
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.util.ContextUtils;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.ImmutableMap;
import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 */
public class GetReportTemplateAction
    implements Action
{    
    private Map<String, String> TYPE_TEMPLATE_MAP = ImmutableMap.<String, String>builder()
        .put( "jasper", "jasper-report-template.jrxml" )
        .put( "html", "html-report-template.html" ).build();
    
    private Map<String, String> TYPE_CONTENT_TYPE_MAP = ImmutableMap.<String, String>builder()
        .put( "jasper", ContextUtils.CONTENT_TYPE_XML )
        .put( "html", ContextUtils.CONTENT_TYPE_HTML ).build();
            
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

        if ( type != null & TYPE_TEMPLATE_MAP.containsKey( type ) )
        {
            String template = TYPE_TEMPLATE_MAP.get( type );
            String contentType = TYPE_CONTENT_TYPE_MAP.get( type );
            
            ContextUtils.configureResponse( response, contentType, false, template, true );
            
            String content = IOUtils.toString( new ClassPathResource( template ).getInputStream() );
            
            IOUtils.write( content, response.getWriter() );
        }
        
        return SUCCESS;
    }
}
