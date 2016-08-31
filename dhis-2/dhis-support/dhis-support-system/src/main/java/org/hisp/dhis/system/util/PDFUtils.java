package org.hisp.dhis.system.util;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;

import static com.lowagie.text.Element.ALIGN_CENTER;
import static com.lowagie.text.Element.ALIGN_LEFT;

/**
 * @author Lars Helge Overland
 * @author Dang Duy Hieu
 */
public class PDFUtils
{
    private static final String EMPTY = "";

    /**
     * Creates a document.
     *
     * @param outputStream The output stream to write the document content.
     * @return A Document.
     */
    public static Document openDocument( OutputStream outputStream )
    {
        return openDocument( outputStream, PageSize.A4 );
    }

    /**
     * Creates a document.
     *
     * @param outputStream The output stream to write the document content.
     * @param pageSize     the page size.
     * @return A Document.
     */
    public static Document openDocument( OutputStream outputStream, Rectangle pageSize )
    {
        try
        {
            Document document = new Document( pageSize );

            PdfWriter.getInstance( document, outputStream );

            document.open();

            return document;
        }
        catch ( DocumentException ex )
        {
            throw new RuntimeException( "Failed to open PDF document", ex );
        }
    }

    /**
     * Starts a new page in the document.
     *
     * @param document The document to start a new page in.
     */
    public static void startNewPage( Document document )
    {
        document.newPage();
    }

    /**
     * <p>
     * Creates a table. Specify the columns and widths by providing one<br>
     * float per column with a percentage value. For instance
     * </p>
     * <p>
     * <p>
     * getPdfPTable( 0.35f, 0.65f )
     * </p>
     * <p>
     * <p>
     * will give you a table with two columns where the first covers 35 %<br>
     * of the page while the second covers 65 %.
     * </p>
     *
     * @param keepTogether Indicates whether the table could be broken across
     *                     multiple pages or should be kept at one page.
     * @param columnWidths The column widths.
     * @return
     */
    public static PdfPTable getPdfPTable( boolean keepTogether, float... columnWidths )
    {
        PdfPTable table = new PdfPTable( columnWidths );

        table.setWidthPercentage( 100f );
        table.setKeepTogether( keepTogether );

        return table;
    }

    /**
     * Adds a table to a document.
     *
     * @param document The document to add the table to.
     * @param table    The table to add to the document.
     */
    public static void addTableToDocument( Document document, PdfPTable table )
    {
        try
        {
            document.add( table );
        }
        catch ( DocumentException ex )
        {
            throw new RuntimeException( "Failed to add table to document", ex );
        }
    }

    /**
     * Moves the cursor to the next page in the document.
     *
     * @param document The document.
     */
    public static void moveToNewPage( Document document )
    {
        document.newPage();
    }

    /**
     * Closes the document if it is open.
     *
     * @param document The document to close.
     */
    public static void closeDocument( Document document )
    {
        if ( document.isOpen() )
        {
            document.close();
        }
    }

    /**
     * Creates a cell.
     *
     * @param text            The text to include in the cell.
     * @param colspan         The column span of the cell.
     * @param font            The font of the cell text.
     * @param horizontalAlign The vertical alignment of the text in the cell.
     * @return A PdfCell.
     */
    public static PdfPCell getCell( String text, int colspan, Font font, int horizontalAlign )
    {
        Paragraph paragraph = new Paragraph( text, font );

        PdfPCell cell = new PdfPCell( paragraph );

        cell.setColspan( colspan );
        cell.setBorder( 0 );
        cell.setMinimumHeight( 15 );
        cell.setHorizontalAlignment( horizontalAlign );

        return cell;
    }

    public static PdfPCell getTitleCell( String text, int colspan )
    {
        return getCell( text, colspan, getBoldFont( 16 ), ALIGN_CENTER );
    }

    public static PdfPCell getSubtitleCell( String text, int colspan )
    {
        return getCell( text, colspan, getItalicFont( 12 ), ALIGN_CENTER );
    }

    public static PdfPCell getHeaderCell( String text, int colspan )
    {
        return getCell( text, colspan, getFont( 12 ), ALIGN_LEFT );
    }

    public static PdfPCell getTextCell( String text )
    {
        return getCell( text, 1, getFont( 9 ), ALIGN_LEFT );
    }

    public static PdfPCell getTextCell( Object object )
    {
        String text = object != null ? String.valueOf( object ) : EMPTY;

        return getCell( text, 1, getFont( 9 ), ALIGN_LEFT );
    }

    public static PdfPCell getItalicCell( String text )
    {
        return getCell( text, 1, getItalicFont( 9 ), ALIGN_LEFT );
    }

    public static PdfPCell resetPaddings( PdfPCell cell, float top, float bottom, float left, float right )
    {
        cell.setPaddingTop( top );
        cell.setPaddingBottom( bottom );
        cell.setPaddingLeft( left );
        cell.setPaddingRight( right );

        return cell;
    }

    /**
     * Creates an empty cell.
     *
     * @param colspan The column span of the cell.
     * @param height  The height of the column.
     * @return A PdfCell.
     */
    public static PdfPCell getEmptyCell( int colSpan, int height )
    {
        PdfPCell cell = new PdfPCell();

        cell.setColspan( colSpan );
        cell.setBorder( 0 );
        cell.setMinimumHeight( height );

        return cell;
    }

    // -------------------------------------------------------------------------
    // Font methods
    // -------------------------------------------------------------------------

    public static Font getFont( float size )
    {
        return getFont( "ubuntu.ttf", size );
    }

    public static Font getBoldFont( float size )
    {
        return getFont( "ubuntu-bold.ttf", size );
    }

    public static Font getItalicFont( float size )
    {
        return getFont( "ubuntu-italic.ttf", size );
    }

    private static Font getFont( String fontPath, float size )
    {
        try
        {
            BaseFont baseFont = BaseFont.createFont( fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED );
            return new Font( baseFont, size );
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Error while creating base font", ex );
        }
    }
}
