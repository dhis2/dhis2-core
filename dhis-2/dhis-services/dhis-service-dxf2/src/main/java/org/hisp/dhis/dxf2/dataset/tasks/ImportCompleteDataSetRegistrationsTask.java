package org.hisp.dhis.dxf2.dataset.tasks;

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

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistrationExchangeService;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.security.SecurityContextRunnable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.InputStream;

/**
 * @author Halvdan Hoem Grelland
 */
public class ImportCompleteDataSetRegistrationsTask
    extends SecurityContextRunnable
{
    public static final String CONTENT_TYPE_JSON = "application/json";

    public static final String CONTENT_TYPE_XML = "application/xml";

    private InputStream input;

    private String tmpFilename;

    private ImportOptions importOptions;

    private String contentType;

    private TaskId taskId;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private CompleteDataSetRegistrationExchangeService registrationService;
    public ImportCompleteDataSetRegistrationsTask(
        InputStream input, String tmpFilename, ImportOptions importOptions, String contentType, TaskId taskId )
    {
        this.input = input;
        this.tmpFilename = tmpFilename;
        this.importOptions = importOptions;
        this.contentType = contentType;
        this.taskId = taskId;
    }

    // -------------------------------------------------------------------------
    // SecurityContextRunnable implementation
    // -------------------------------------------------------------------------

    @Override
    public void call()
    {
        try
        {
            MimeType mimeType = MimeType.valueOf( contentType );

            if ( MimeTypeUtils.APPLICATION_XML.includes( mimeType ) )
            {
                registrationService.saveCompleteDataSetRegistrationsXml( input, importOptions, taskId );
            }
            else if ( MimeTypeUtils.APPLICATION_JSON.includes( mimeType ) )
            {
                registrationService.saveCompleteDataSetRegistrationsJson( input, importOptions, taskId );
            }
            else
            {
                // TODO XML default?
            }
        }
        finally
        {
            cleanUpTmpFile( tmpFilename );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void cleanUpTmpFile( String tmpFilename )
    {
        // TODO Delete tmpfile after we're done
    }
}
