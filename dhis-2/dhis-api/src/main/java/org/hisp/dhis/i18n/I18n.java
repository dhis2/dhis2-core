package org.hisp.dhis.i18n;

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

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author Pham Thi Thuy
 * @author Nguyen Dang Quang
 * @author Anders Gjendem
 */
public class I18n
{    
    private ResourceBundle globalResourceBundle;

    private ResourceBundle specificResourceBundle;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public I18n( ResourceBundle globalResourceBundle, ResourceBundle specificResourceBundle )
    {
        this.globalResourceBundle = globalResourceBundle;
        this.specificResourceBundle = specificResourceBundle;
    }

    // -------------------------------------------------------------------------
    // Methods
    // -------------------------------------------------------------------------

    /**
     * Get a translated String for a given key for the currently selected locale
     *
     * @param key the key for a given translation
     * @return a translated String for a given key, or the key if no translation
     *         is found
     */
    public String getString( String key )
    {
        String translation = key;

        if ( specificResourceBundle != null )
        {
            try
            {
                translation = specificResourceBundle.getString( key );
            }
            catch ( MissingResourceException ignored )
            {
            }
        }

        if ( translation.equals( key ) && globalResourceBundle != null )
        {
            try
            {
                translation = globalResourceBundle.getString( key );
            }
            catch ( MissingResourceException ignored )
            {
            }
        }

        return translation;
    }
}
