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

import java.awt.*;
import java.io.IOException;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfAppearance;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RadioCheckField;

/**
 * @author viet@dhis2.org
 */
public class PdfCheckBoxField extends PdfInputField
{

    public PdfCheckBoxField( String label, String fieldName, PdfWriter writer, Font font )
    {
        super( label, fieldName, PdfCellType.CHECKBOX, writer, font );
    }

    public PdfCheckBoxField( String label, String fieldName, PdfWriter writer )
    {
        super( label, fieldName, PdfCellType.CHECKBOX, writer,
            PdfFormFontSettings.getFont( PdfFormFontSettings.FONTTYPE_BODY ) );
    }

    @Override
    public PdfPCell getInputCell()
        throws IOException,
        DocumentException
    {
        float sizeDefault = PdfDataEntryFormUtil.UNITSIZE_DEFAULT;

        RadioCheckField checkbox = new RadioCheckField( writer, new Rectangle( sizeDefault, sizeDefault ), "Yes",
            "On" );

        checkbox.setBorderWidth( 1 );
        checkbox.setBorderColor( Color.BLACK );

        PdfFormField checkboxfield = checkbox.getCheckField();
        checkboxfield.setFieldName( fieldName + "_" + PdfFieldCell.TPYEDEFINE_NAME + PdfFieldCell.TYPE_CHECKBOX );

        setCheckboxAppearance( checkboxfield, writer.getDirectContent(), sizeDefault );

        PdfPCell cell = PdfDataEntryFormUtil.getPdfPCell( hasBorder );

        cell.setCellEvent(
            new PdfFieldCell( checkboxfield, sizeDefault, sizeDefault, PdfFieldCell.TYPE_CHECKBOX, writer ) );

        return cell;
    }

    private void setCheckboxAppearance( PdfFormField checkboxfield, PdfContentByte canvas, float width )
    {
        PdfAppearance[] onOff = new PdfAppearance[2];
        onOff[0] = canvas.createAppearance( width + 2, width + 2 );
        onOff[0].rectangle( 1, 1, width, width );
        onOff[0].stroke();
        onOff[1] = canvas.createAppearance( width + 2, width + 2 );
        onOff[1].setRGBColorFill( 255, 128, 128 );
        onOff[1].rectangle( 1, 1, width, width );
        onOff[1].fillStroke();
        onOff[1].moveTo( 1, 1 );
        onOff[1].lineTo( width + 1, width + 1 );
        onOff[1].moveTo( 1, width + 1 );
        onOff[1].lineTo( width + 1, 1 );
        onOff[1].stroke();

        checkboxfield.setAppearance( PdfAnnotation.APPEARANCE_NORMAL, "Off", onOff[0] );
        checkboxfield.setAppearance( PdfAnnotation.APPEARANCE_NORMAL, "On", onOff[1] );
    }
}
