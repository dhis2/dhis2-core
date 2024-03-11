package org.hisp.dhis.textpattern;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

/**
 * @author Stian Sandvold
 */
public interface MethodType
{

    /**
     * Validates the pattern of the type against the string input
     * @param raw the string to validate
     * @return true if raw matches pattern, false if not
     */
    boolean validatePattern( String raw );

    /**
     * Validates a text against a format. Format will be adjusted based on the MethodType
     * @param format the format to validate for
     * @param text the text to validate
     * @return true if it matches, false if not
     */
    boolean validateText( String format, String text );

    /**
     * Returns the param part of the Method from the raw String
     * @param raw the string to retrieve param from
     * @return the param from the raw String
     */
    String getParam( String raw );

    /**
     * Returns a regex String based on the format
     * @param format the format to transform into regex
     * @return a regex String that matches the format and MethodType
     */
    String getValueRegex( String format );

    /**
     * Returns a String after applying format to the value
     * @param format the format to apply to the value
     * @param value the string to format
     * @return the formatted text
     */
    String getFormattedText( String format, String value );

}
