/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.pdfform;

import java.io.IOException;

import lombok.Getter;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

/**
 * @author viet@dhis2.org
 */
@Getter
public class PdfTable
{
    private boolean hasBorder = false;

    private Rectangle textBoxSize = new Rectangle( 220.0f, PdfDataEntryFormUtil.CONTENT_HEIGHT_DEFAULT );

    private int numOfCells;

    private int horizontalAlignment;

    private int[] widths;

    private float width;

    private PdfPTable internalTable;

    public PdfTable( int numOfCells, int horizontalAlignment )
    {
        this.numOfCells = numOfCells;
        this.horizontalAlignment = horizontalAlignment;

        internalTable = new PdfPTable( numOfCells );
        internalTable.setWidthPercentage( 100.0f );
        internalTable.setHorizontalAlignment( horizontalAlignment );
    }

    public PdfTable( int numOfCells, int horizontalAlignment, float width )
        throws DocumentException
    {
        this.numOfCells = numOfCells;
        this.horizontalAlignment = horizontalAlignment;
        this.width = width;
        initTable();
    }

    public PdfTable( int numOfCells, int horizontalAlignment, int[] widths )
        throws DocumentException
    {
        this.numOfCells = numOfCells;
        this.horizontalAlignment = horizontalAlignment;
        this.widths = widths;
        initTable();
    }

    public void initTable()
        throws DocumentException
    {
        internalTable = new PdfPTable( numOfCells );
        internalTable.setWidths( widths );
        internalTable.setHorizontalAlignment( horizontalAlignment );
    }

    public PdfTable addCell( PdfPCell cell )
    {
        internalTable.addCell( cell );
        return this;
    }

    public PdfTable addToTable( PdfTable table )
    {
        PdfPCell cell = new PdfPCell( internalTable );
        cell.setBorder( Rectangle.NO_BORDER );
        cell.setHorizontalAlignment( Element.ALIGN_LEFT );
        table.addCell( cell );
        return this;
    }

    public PdfTable addInputField( PdfInputField inputField )
        throws IOException,
        DocumentException
    {
        internalTable.addCell( inputField.getLabelCell() );
        internalTable.addCell( inputField.getInputCell() );
        return this;
    }

    public void addToDocument( Document document )
        throws DocumentException
    {
        document.add( internalTable );
    }
}
