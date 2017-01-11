package org.hisp.dhis.dataentryform;

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

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;

/**
 * Upgrades the format of the input field identifiers from the legacy
 * "value[12].value:value[34].value" to the new "12-34-val"
 */
public class DataEntryFormUpgrader
    extends TransactionContextStartupRoutine
{
    private static final Log log = LogFactory.getLog( DataEntryFormUpgrader.class );

    private final static String ID_EXPRESSION = "id=\"value\\[(\\d+)\\]\\.value:value\\[(\\d+)\\]\\.value\"";

    private final static Pattern ID_PATTERN = Pattern.compile( ID_EXPRESSION );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataEntryFormService dataEntryFormService;

    public void setDataEntryFormService( DataEntryFormService dataEntryFormService )
    {
        this.dataEntryFormService = dataEntryFormService;
    }

    // -------------------------------------------------------------------------
    // Implementation method
    // -------------------------------------------------------------------------

    @Override
    public void executeInTransaction()
    {
        Collection<DataEntryForm> dataEntryForms = dataEntryFormService.getAllDataEntryForms();

        for ( DataEntryForm form : dataEntryForms )
        {
            try
            {
                String htmlCode = form.getHtmlCode();
                
                if ( htmlCode == null || htmlCode.isEmpty() )
                {
                    log.warn( "No html content for form: " + form );
                    continue;
                }
                
                String customForm = upgradeDataEntryForm( htmlCode );
    
                if ( customForm != null && !customForm.equals( htmlCode ) )
                {
                    form.setHtmlCode( customForm );
                    dataEntryFormService.updateDataEntryForm( form );
                }
            }
            catch ( Exception ex )
            {
                log.error( "Upgrading data entry form failed: " + form.getName() );
                log.error( ex ); // Log and continue
            }
        }
        
        log.info( "Upgraded custom case entry form identifiers" );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String upgradeDataEntryForm( String htmlCode )
    {
        Matcher matcher = ID_PATTERN.matcher( htmlCode );

        StringBuffer out = new StringBuffer();

        while ( matcher.find() )
        {
            String upgradedId = "id=\"" + matcher.group( 1 ) + "-" + matcher.group( 2 ) + "-val\"";

            matcher.appendReplacement( out, upgradedId );
        }

        matcher.appendTail( out );

        return out.toString().replaceAll( "view=\"@@deshortname@@\"", "" );
    }
}