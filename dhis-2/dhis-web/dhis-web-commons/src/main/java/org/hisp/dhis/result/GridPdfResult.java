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

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.hisp.dhis.system.util.CodecUtils.filenameEncode;

import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.util.ContextUtils;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.Result;

/**
 * Creates a PDF representation of the given Grid or list of Grids and writes
 * it to the servlet outputstream. One of the grid or grids arguments must be set.
 * 
 * @author Lars Helge Overland
 */
public class GridPdfResult
    implements Result
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 6613101138470779866L;

    private static final String DEFAULT_NAME = "Grid";

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Grid grid;

    public void setGrid( Grid grid )
    {
        this.grid = grid;
    }

    private List<Grid> grids;

    public void setGrids( List<Grid> grids )
    {
        this.grids = grids;
    }
   
    private boolean attachment = true;
    
    protected boolean isAttachment()
    {
        return attachment;
    }

    // -------------------------------------------------------------------------
    // Result implementation
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings( "unchecked" )
    public void execute( ActionInvocation invocation )
        throws Exception
    {
        // ---------------------------------------------------------------------
        // Get grid
        // ---------------------------------------------------------------------

        Grid _grid = (Grid) invocation.getStack().findValue( "grid" );

        grid = _grid != null ? _grid : grid;

        List<Grid> _grids = (List<Grid>) invocation.getStack().findValue( "grids" );
        
        grids = _grids != null ? _grids : grids;
        
        // ---------------------------------------------------------------------
        // Configure response
        // ---------------------------------------------------------------------

        HttpServletResponse response = ServletActionContext.getResponse();

        OutputStream out = response.getOutputStream();

        String filename = filenameEncode( defaultIfEmpty( grid != null ? grid.getTitle() : grids.iterator().next().getTitle(), DEFAULT_NAME ) ) + ".pdf";
        
        ContextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PDF, true, filename, isAttachment() );

        // ---------------------------------------------------------------------
        // Write PDF to output stream
        // ---------------------------------------------------------------------

        if ( grid != null )
        {
            GridUtils.toPdf( grid, out );
        }
        else
        {
            GridUtils.toPdf( grids, out );
        }
    }
}
