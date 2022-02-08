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
import java.util.Objects;

import org.hisp.dhis.common.IdentifiableObject;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;

/**
 * @author viet@dhis2.org
 */
public abstract class PdfDocument<T extends IdentifiableObject>
{
    protected PdfDataEntrySettings settings;

    protected Document document;

    protected PdfTable mainTable;

    public PdfDocument( Document document, PdfDataEntrySettings settings )
    {
        Objects.requireNonNull( settings );
        Objects.requireNonNull( document );
        this.settings = settings;
        this.document = document;

        init();
    }

    protected void init()
    {
        setHeaderFooterOnDocument();
        document.setPageSize( settings.getDefaultPageSize() );
        mainTable = createMainTable();
    }

    abstract protected void write( PdfWriter writer, T object )
        throws DocumentException,
        IOException;

    // --------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------

    protected PdfTable createMainTable()
    {
        return new PdfTable( 1, Element.ALIGN_LEFT );
    }

    protected void addTextRow( String text )
        throws DocumentException
    {
        PdfTextRow.newInstance().setText( text ).addToTable( mainTable );
    }

    protected void addEmptyRow()
        throws DocumentException
    {
        PdfTextRow.newInstance().addToTable( mainTable );
    }

    protected void setTitle( String displayName, String displayDescription )
        throws DocumentException
    {
        document
            .add( new Paragraph( displayName, settings.getFont( PdfFormFontSettings.FONTTYPE_TITLE ) ) );

        document.add( new Paragraph( displayDescription,
            settings.getFont( PdfFormFontSettings.FONTTYPE_DESCRIPTION ) ) );
    }

    private void setHeaderFooterOnDocument()
    {
        HeaderFooter header = new HeaderFooter( new Phrase( settings.getHeader(),
            settings.getFont( PdfFormFontSettings.FONTTYPE_FOOTER ) ), false );
        header.setBorder( Rectangle.NO_BORDER );
        header.setAlignment( Element.ALIGN_LEFT );
        document.setHeader( header );

        HeaderFooter footer = new HeaderFooter( new Phrase( "",
            settings.getFont( PdfFormFontSettings.FONTTYPE_FOOTER ) ), true );
        footer.setBorder( Rectangle.NO_BORDER );
        footer.setAlignment( Element.ALIGN_RIGHT );
        document.setFooter( footer );
    }
}
