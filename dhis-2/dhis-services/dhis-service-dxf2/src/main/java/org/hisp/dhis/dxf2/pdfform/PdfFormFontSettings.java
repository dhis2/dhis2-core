package org.hisp.dhis.dxf2.pdfform;

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

import com.lowagie.text.Font;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author James Chang
 */

public class PdfFormFontSettings
{
    public static final int FONTTYPE_BODY = 0;

    public static final int FONTTYPE_TITLE = 1;

    public static final int FONTTYPE_DESCRIPTION = 2;

    public static final int FONTTYPE_SECTIONHEADER = 3;

    public static final int FONTTYPE_FOOTER = 4;

    private static final float FONTSIZE_BODY = 10;

    private static final float FONTSIZE_TITLE = 16;

    private static final float FONTSIZE_DESCRIPTION = 11;

    private static final float FONTSIZE_SECTIONHEADER = 14;

    private static final float FONTSIZE_FOOTER = 8;

    private static final String FONTFAMILY = "HELVETICA";

    private final Map<Integer, Font> fontTypeMap = new HashMap<>();

    public PdfFormFontSettings()
    {
        fontTypeMap.put( FONTTYPE_BODY, createFont( FONTTYPE_BODY ) );
        fontTypeMap.put( FONTTYPE_TITLE, createFont( FONTTYPE_TITLE ) );
        fontTypeMap.put( FONTTYPE_DESCRIPTION, createFont( FONTTYPE_DESCRIPTION ) );
        fontTypeMap.put( FONTTYPE_SECTIONHEADER, createFont( FONTTYPE_SECTIONHEADER ) );
        fontTypeMap.put( FONTTYPE_FOOTER, createFont( FONTTYPE_FOOTER ) );
    }

    public void setFont( int fontType, Font font )
    {
        fontTypeMap.put( fontType, font );
    }

    public Font getFont( int fontType )
    {
        return fontTypeMap.get( fontType );
    }

    private Font createFont( int fontType )
    {
        Font font = new Font();
        font.setFamily( FONTFAMILY );

        switch ( fontType )
        {
        case FONTTYPE_BODY:
            font.setSize( FONTSIZE_BODY );
            font.setColor( Color.BLACK );
            break;
        case FONTTYPE_TITLE:
            font.setSize( FONTSIZE_TITLE );
            font.setStyle( java.awt.Font.BOLD );
            font.setColor( new Color( 0, 0, 128 ) ); // Navy Color
            break;
        case FONTTYPE_DESCRIPTION:
            font.setSize( FONTSIZE_DESCRIPTION );
            font.setColor( Color.DARK_GRAY );
            break;
        case FONTTYPE_SECTIONHEADER:
            font.setSize( FONTSIZE_SECTIONHEADER );
            font.setStyle( java.awt.Font.BOLD );
            font.setColor( new Color( 70, 130, 180 ) ); // Steel Blue Color
            break;
        case FONTTYPE_FOOTER:
            font.setSize( FONTSIZE_FOOTER );
            break;
        default:
            font.setSize( FONTSIZE_BODY );
            break;
        }

        return font;
    }
}
