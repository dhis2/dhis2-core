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

import lombok.Builder;
import lombok.Getter;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfBorderDictionary;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.TextField;

/**
 * @author viet@dhis2.org
 */
@Getter
public class PdfSelectField extends PdfInputField
{
    private String[] optionsLabels;

    private String[] optionsValues;

    @Builder
    public PdfSelectField( String fieldName, String label, PdfCellType cellType, String[] optionLabels,
        String[] optionValues, PdfWriter writer, Font font )
    {
        super( label, fieldName, cellType, writer, font );
        this.optionsLabels = optionLabels;
        this.optionsValues = optionValues;
    }

    public PdfPCell getInputCell()
        throws IOException,
        DocumentException
    {
        PdfPCell cell = PdfDataEntryFormUtil.getPdfPCell( hasBorder );
        TextField textList = new TextField( writer, getTextBoxSize(), fieldName );

        textList.setChoices( optionsLabels );
        textList.setChoiceExports( optionsValues );

        textList.setBorderWidth( 1 );
        textList.setBorderColor( Color.BLACK );
        textList.setBorderStyle( PdfBorderDictionary.STYLE_SOLID );
        textList.setBackgroundColor( COLOR_BACKGROUDTEXTBOX );

        PdfFormField dropDown = textList.getComboField();

        cell.setCellEvent(
            new PdfFieldCell( dropDown, getTextBoxSize().getWidth(), getTextBoxSize().getHeight(), writer ) );

        return cell;
    }
}
