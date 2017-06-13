package org.hisp.dhis.result;

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

/**
 * This file is modified and included in DHIS because the original version
 * wont allow dynamic setting of the chart height and width.
 */

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.Result;

import org.hisp.dhis.util.ContextUtils;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

/**
 * A custom Result type for chart data. Built on top of <a
 * href="http://www.jfree.org/jfreechart/" target="_blank">JFreeChart</a>. When
 * executed this Result will write the given chart as a PNG to the servlet
 * output stream.
 * 
 * @author Bernard Choi
 * @author Lars Helge Overland
 */
public class ChartResult
    implements Result
{
    private static final Log log = LogFactory.getLog( ChartResult.class );
    
    private static final String DEFAULT_FILENAME = "chart.png";
    private static final int DEFAULT_WIDTH = 700;
    private static final int DEFAULT_HEIGHT = 500;
    
    private JFreeChart chart = null;

    private Integer height;

    private Integer width;
    
    private String filename;
    
    /**
     * Sets the JFreeChart to use.
     * 
     * @param chart a JFreeChart object.
     */
    public void setChart( JFreeChart chart )
    {
        this.chart = chart;
    }

    /**
     * Sets the chart height.
     * 
     * @param height the height of the chart in pixels.
     */
    public void setHeight( Integer height )
    {
        this.height = height;
    }

    /**
     * Sets the chart width.
     * 
     * @param width the width of the chart in pixels.
     */
    public void setWidth( Integer width )
    {
        this.width = width;
    }
    
    /**
     * Sets the filename.
     * 
     * @param filename the filename.
     */
    public void setFilename( String filename )
    {
        this.filename = filename;
    }

    /**
     * Executes the result. Writes the given chart as a PNG to the servlet
     * output stream.
     * 
     * @param invocation an encapsulation of the action execution state.
     * @throws Exception if an error occurs when creating or writing the chart
     *         to the servlet output stream.
     */
    @Override
    public void execute( ActionInvocation invocation )
        throws Exception
    {
        JFreeChart stackChart = (JFreeChart) invocation.getStack().findValue( "chart" );
        
        chart = stackChart != null ? stackChart : chart;
        
        Integer stackHeight = (Integer) invocation.getStack().findValue( "height" );
        
        height = stackHeight != null && stackHeight > 0 ? stackHeight : height != null ? height: DEFAULT_HEIGHT;
        
        Integer stackWidth = (Integer) invocation.getStack().findValue( "width" );
        
        width = stackWidth != null && stackWidth > 0 ? stackWidth : width != null ? width : DEFAULT_WIDTH;
        
        String stackFilename = (String) invocation.getStack().findValue( "filename" );
        
        filename = StringUtils.defaultIfEmpty( stackFilename, DEFAULT_FILENAME );
        
        if ( chart == null )
        {
            log.warn( "No chart found" );
            return;
        }
                
        HttpServletResponse response = ServletActionContext.getResponse();
        ContextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG, true, filename, false );
        
        OutputStream os = response.getOutputStream();
        ChartUtilities.writeChartAsPNG( os, chart, width, height );
        os.flush();
    }
}
