package org.hisp.dhis.commons.util;

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

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Utility class for encoding.
 * 
 * @author Lars Helge Overland
 */
public class Encoder
{
    /**
     * HTML-escapes the String representation of the given Object.
     * @param object the Object.
     * @return an HTML-escaped String representation.
     */
    public String htmlEncode( Object object )
    {
        return object != null ? StringEscapeUtils.escapeHtml4( String.valueOf( object ) ) : null;
    }

    /**
     * HTML-escapes the given String.
     * @param object the String.
     * @return an HTML-escaped representation.
     */
    public String htmlEncode( String object )
    {
        return StringEscapeUtils.escapeHtml4( object );
    }

    /**
     * XML-escapes the given String.
     * @param object the String.
     * @return an XML-escaped representation.
     */
    public String xmlEncode( String object )
    {
        return StringEscapeUtils.escapeXml11( object );
    }

    /**
     * JavaScript-escaped the given String.
     * @param object the String.
     * @return a JavaScript-escaped representation.
     */
    public String jsEncode( String object )
    {
        return StringEscapeUtils.escapeEcmaScript( object );
    }

    /**
     * Escaped the given JSON content using Java String rules.
     *
     * Assumes " is used as quote char and not used inside values and does
     * not escape '.
     *
     * @param object the String.
     * @return the escaped representation.
     */
    public String jsonEncode( String object )
    {
        return StringEscapeUtils.escapeJava( object );
    }

    /**
     * JavaScript-escaped the given String.
     * 
     * @param object the object.
     * @param quoteChar the quote char.
     * @return the escaped representation.
     * 
     * See {@link #jsEncode(String)}.
     * @deprecated quoteChar is ignored.
     */
    @Deprecated
    public String jsEscape( String object, String quoteChar )
    {
        return jsEncode( object );
    }
}