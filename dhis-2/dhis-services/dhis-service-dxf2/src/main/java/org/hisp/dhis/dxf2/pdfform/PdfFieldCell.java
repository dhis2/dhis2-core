package org.hisp.dhis.dxf2.pdfform;

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

import java.io.IOException;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.GrayColor;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfBorderDictionary;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PushbuttonField;
import com.lowagie.text.pdf.RadioCheckField;

/**
 * @author James Chang
 */
public class PdfFieldCell
    implements PdfPCellEvent
{
    public static final int TYPE_DEFAULT = 0;

    public static final int TYPE_BUTTON = 1;

    public static final int TYPE_TEXT_ORGUNIT = 2;

    public static final int TYPE_TEXT_NUMBER = 3;

    public static final int TYPE_CHECKBOX = 4;

    public static final int TYPE_RADIOBUTTON = 5;

    public static final int TPYE_LABEL = 6;

    public static final String TPYEDEFINE_NAME = "T";    
    
    private static final float RADIOBUTTON_WIDTH = 10.0f;

    private static final float RADIOBUTTON_TEXTOFFSET = 3.0f;

    private static final float OFFSET_TOP = 0.5f;

    private static final float OFFSET_LEFT = 3.0f;
    
    private PdfFormField parent;

    private PdfFormField formField;

    private PdfWriter writer;

    private float width;
    
    private float height;

    private int type;

    private String jsAction;

    private String[] values;

    private String[] texts;

    private String checkValue;

    private String text;

    private String name;
    
    
    // Constructors
    public PdfFieldCell( PdfFormField formField, float width, float height, PdfWriter writer )
    {
        this.formField = formField;
        this.width = width;
        this.height = height;
        this.writer = writer;
        this.type = TYPE_DEFAULT;
    }

    public PdfFieldCell( PdfFormField formField, float width, float height, int type, PdfWriter writer )
    {
        this.formField = formField;
        this.width = width;
        this.height = height;
        this.writer = writer;
        this.type = type;
    }

    public PdfFieldCell( PdfFormField formField, String jsAction, String name, String text, int type, PdfWriter writer )
    {
        this.formField = formField;
        this.writer = writer;
        this.type = type;
        this.name = name;
        this.text = text;
        this.jsAction = jsAction;
    }

    public PdfFieldCell( PdfFormField parent, String[] texts, String[] values, String checkValue, float width, float height,
        int type, PdfWriter writer )
    {
        this.writer = writer;
        this.type = type;
        this.parent = parent;
        this.texts = texts;
        this.values = values;
        this.checkValue = checkValue;
        this.width = width;
        this.height = height;
    }

    @Override
    public void cellLayout( PdfPCell cell, Rectangle rect, PdfContentByte[] canvases )
    {
        try
        {
            PdfContentByte canvasText = canvases[PdfPTable.TEXTCANVAS];
            
            if ( type == TYPE_RADIOBUTTON )
            {
                if ( parent != null )
                {
                    float leftLoc = rect.getLeft();
                    float rightLoc = rect.getLeft() + RADIOBUTTON_WIDTH;

                    String text;
                    String value;
                    
                    for ( int i = 0; i < texts.length; i++ )
                    {

                        text = texts[i];
                        value = values[i];

                        Rectangle radioRec = new Rectangle( leftLoc, rect.getTop() - height, rightLoc, rect.getTop() );

                        RadioCheckField rf = new RadioCheckField( writer, radioRec, "RDBtn_" + text, value );

                        if ( value != null && value.equals( checkValue ) )
                        {
                            rf.setChecked( true );
                        }

                        rf.setBorderColor( GrayColor.GRAYBLACK );
                        rf.setBackgroundColor( GrayColor.GRAYWHITE );
                        rf.setCheckType( RadioCheckField.TYPE_CIRCLE );

                        parent.addKid( rf.getRadioField() );

                        leftLoc = rightLoc;
                        rightLoc += width;

                        ColumnText.showTextAligned( canvasText, Element.ALIGN_LEFT, new Phrase( text ), leftLoc
                            + RADIOBUTTON_TEXTOFFSET, height, 0 );

                        leftLoc = rightLoc;
                        rightLoc += RADIOBUTTON_WIDTH;
                    }

                    writer.addAnnotation( parent );
                }
            }
            else if ( type == TYPE_BUTTON )
            {
                // Add the push button
                PushbuttonField button = new PushbuttonField( writer, rect, name );
                button.setBackgroundColor( new GrayColor( 0.75f ) );
                button.setBorderColor( GrayColor.GRAYBLACK );
                button.setBorderWidth( 1 );
                button.setBorderStyle( PdfBorderDictionary.STYLE_BEVELED );
                button.setTextColor( GrayColor.GRAYBLACK );
                button.setFontSize( PdfDataEntryFormUtil.UNITSIZE_DEFAULT );
                button.setText( text );
                button.setLayout( PushbuttonField.LAYOUT_ICON_LEFT_LABEL_RIGHT );
                button.setScaleIcon( PushbuttonField.SCALE_ICON_ALWAYS );
                button.setProportionalIcon( true );
                button.setIconHorizontalAdjustment( 0 );

                formField = button.getField();
                formField.setAction( PdfAction.javaScript( jsAction, writer ) );
            }
            else if ( type == TYPE_CHECKBOX )
            {
                float extraCheckBoxOffset_Left = 2.0f;
                float extraCheckBoxOffset_Top = 1.5f;
                
                formField.setWidget(
                    new Rectangle( rect.getLeft() + OFFSET_LEFT + extraCheckBoxOffset_Left
                        , rect.getTop() - height - OFFSET_TOP - extraCheckBoxOffset_Top
                        , rect.getLeft() + width + OFFSET_LEFT + extraCheckBoxOffset_Left
                        , rect.getTop() - OFFSET_TOP - extraCheckBoxOffset_Top ),
                    PdfAnnotation.HIGHLIGHT_NONE );
            }            
            else
            {

                if ( type == TYPE_TEXT_ORGUNIT )
                {
                    formField.setAdditionalActions( PdfName.BL, PdfAction.javaScript(
                        "if(event.value == '') app.alert('Please enter org unit identifier');", writer ) );
                }

                // TYPE_TEXT_NUMBER and TYPE_CHECKBOX cases included as well here
                
                formField.setWidget(
                    new Rectangle( rect.getLeft() + OFFSET_LEFT, rect.getTop() - height - OFFSET_TOP, rect.getLeft() + width + OFFSET_LEFT, rect.getTop() - OFFSET_TOP ),
                    PdfAnnotation.HIGHLIGHT_NONE );

            }

            writer.addAnnotation( formField );

        }
        catch ( DocumentException ex )
        {
            throw new RuntimeException( ex.getMessage() );
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( ex.getMessage() );
        }
    }
}