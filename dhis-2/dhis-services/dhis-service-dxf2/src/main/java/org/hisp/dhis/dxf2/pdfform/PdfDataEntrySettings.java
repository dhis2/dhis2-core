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

import lombok.Builder;

import org.hisp.dhis.i18n.I18nFormat;

import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;

/**
 * @author viet@dhis2.org
 */
@Builder
public class PdfDataEntrySettings
{
    public static String FOOTERTEXT_DEFAULT = "PDF Template generated from DHIS %s - %s";

    public static String HEADER_TEXT_KEY = "pdf_data_entry_form_header_text";

    public static final int PERIODRANGE_PREVYEARS = 1;

    public static final int PERIODRANGE_FUTUREYEARS = 2;

    public static final int PERIODRANGE_PREVYEARS_YEARLY = 5;

    public static final int PERIODRANGE_FUTUREYEARS_YEARLY = 6;

    public static final int TEXTBOXWIDTH = 160;

    private String headerText;

    private String serverName;

    private I18nFormat format;

    private PdfFormFontSettings fontSettings = new PdfFormFontSettings();

    // ----------------------------------------------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------------------------------------------

    public Font getFont( int fontType )
    {
        return fontSettings.getFont( fontType );
    }

    public String getHeader()
    {
        return String.format( FOOTERTEXT_DEFAULT, serverName, headerText );
    }

    public void setServerName( String serverName )
    {
        this.serverName = serverName;
    }

    public void setHeaderText( String headerText )
    {
        this.headerText = headerText;
    }

    public Rectangle getDefaultPageSize()
    {
        return PageSize.A4;
    }

    public void setI18nFormat( I18nFormat format )
    {
        this.format = format;
    }

    public I18nFormat getFormat()
    {
        return this.format;
    }
}
