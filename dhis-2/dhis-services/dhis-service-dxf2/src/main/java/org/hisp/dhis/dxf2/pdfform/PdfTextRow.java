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

import org.apache.commons.lang3.StringUtils;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;

/**
 * @author viet@dhis2.org
 */
public class PdfTextRow
{
    private String text = StringUtils.EMPTY;

    private int horizontalAlignment = Element.ALIGN_RIGHT;

    private Font font = PdfFormFontSettings.getFont( PdfFormFontSettings.FONTTYPE_BODY );

    private boolean hasBorder = false;

    private PdfTable self;

    public PdfTextRow()
        throws DocumentException
    {
        init();
    }

    public PdfTextRow( String text )
        throws DocumentException
    {
        this.text = text;
        init();
    }

    public PdfTextRow( String text, int horizontalAlignment, Font font, boolean hasBorder )
        throws DocumentException
    {
        this.text = text;
        this.horizontalAlignment = horizontalAlignment;
        this.font = font;
        this.hasBorder = hasBorder;
        init();
    }

    public PdfTextRow setText( String text )
    {
        this.text = text;
        return this;
    }

    public PdfTextRow setHorizontalAlignment( int horizontalAlignment )
    {
        this.horizontalAlignment = horizontalAlignment;
        return this;
    }

    public PdfTextRow setFont( Font font )
    {
        this.font = font;
        return this;
    }

    public PdfTextRow setHasBorder( boolean hasBorder )
    {
        this.hasBorder = hasBorder;
        return this;
    }

    public static PdfTextRow newInstance()
        throws DocumentException
    {
        return new PdfTextRow();
    }

    public void addToTable( PdfTable table )
    {
        self.addToTable( table );
    }

    private void init()
        throws DocumentException
    {
        PdfPCell cell = PdfDataEntryFormUtil.getPdfPCell( hasBorder );
        cell.setHorizontalAlignment( horizontalAlignment );

        cell.setPhrase( new Phrase( text, font ) );

        self = new PdfTable( 1, Element.ALIGN_LEFT ).addCell( cell );
    }

}
