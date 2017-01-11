package org.hisp.dhis.dxf2.dataset.tasks;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hisp.dhis.dbms.DbmsUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistrationExchangeService;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.security.SecurityContextRunnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Halvdan Hoem Grelland
 */
public class ImportCompleteDataSetRegistrationsTask
    extends SecurityContextRunnable
{
    Log log = LogFactory.getLog( ImportCompleteDataSetRegistrationsTask.class );

    public static final String FORMAT_JSON = "json", FORMAT_XML = "xml";

    private String format;

    private InputStream input;

    private Path tmpFile;

    private ImportOptions importOptions;

    private TaskId taskId;

    private SessionFactory sessionFactory;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CompleteDataSetRegistrationExchangeService registrationService;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ImportCompleteDataSetRegistrationsTask( CompleteDataSetRegistrationExchangeService registrationService,
        SessionFactory sessionFactory, InputStream input, Path tmpFile, ImportOptions importOptions, String format, TaskId taskId )
    {
        this.registrationService = registrationService;
        this.sessionFactory = sessionFactory;
        this.input = input;
        this.tmpFile = tmpFile;
        this.importOptions = importOptions;
        this.format = format;
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
            if ( FORMAT_XML.equals( format ) )
            {
                registrationService.saveCompleteDataSetRegistrationsXml( input, importOptions, taskId );
            }
            else if ( FORMAT_JSON.equals( format ) )
            {
                registrationService.saveCompleteDataSetRegistrationsJson( input, importOptions, taskId );
            }
        }
        finally
        {
            cleanUpTmpFile( tmpFile );
        }
    }

    @Override
    public void before()
    {
        DbmsUtils.bindSessionToThread( sessionFactory );
    }

    @Override
    public void after()
    {
        DbmsUtils.unbindSessionFromThread( sessionFactory );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void cleanUpTmpFile( Path tmpFile )
    {
        if ( tmpFile == null )
        {
            return;
        }

        try
        {
            Files.deleteIfExists( tmpFile );
        }
        catch ( IOException ignored )
        {
            // Intentionally ignored
            log.warn( "Deleting temporary file failed: " + tmpFile, ignored );
        }
    }
}
