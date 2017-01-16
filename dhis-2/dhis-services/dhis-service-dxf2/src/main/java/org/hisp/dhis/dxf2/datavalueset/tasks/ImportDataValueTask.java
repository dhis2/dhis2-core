package org.hisp.dhis.dxf2.datavalueset.tasks;

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

import org.hisp.dhis.security.SecurityContextRunnable;
import org.hibernate.SessionFactory;
import org.hisp.dhis.dbms.DbmsUtils;
import org.hisp.dhis.dxf2.adx.AdxDataService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.scheduling.TaskId;

import java.io.InputStream;

/**
 * @author Lars Helge Overland
 */
public class ImportDataValueTask
    extends SecurityContextRunnable
{
    public static final String FORMAT_XML = "xml";
    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_CSV = "csv";
    public static final String FORMAT_PDF = "pdf";
    public static final String FORMAT_ADX = "adx";

    private DataValueSetService dataValueSetService;

    private AdxDataService adxDataService;
    
    private SessionFactory sessionFactory;
    
    private InputStream inputStream;

    private final ImportOptions importOptions;

    private final TaskId taskId;

    private final String format;

    // TODO: Re-factor as bean to avoid injecting session factory / dependencies
    
    public ImportDataValueTask( DataValueSetService dataValueSetService, AdxDataService adxDataService, SessionFactory sessionFactory,
        InputStream inputStream, ImportOptions importOptions, TaskId taskId, String format )
    {
        this.dataValueSetService = dataValueSetService;
        this.adxDataService = adxDataService;
        this.sessionFactory = sessionFactory;
        this.inputStream = inputStream;
        this.importOptions = importOptions;
        this.taskId = taskId;
        this.format = format;
    }

    @Override
    public void call()
    {
        if ( FORMAT_JSON.equals( format ) )
        {
            dataValueSetService.saveDataValueSetJson( inputStream, importOptions, taskId );
        }
        else if ( FORMAT_CSV.equals( format ) )
        {
            dataValueSetService.saveDataValueSetCsv( inputStream, importOptions, taskId );
        }
        else if ( FORMAT_PDF.equals( format ) )
        {
            dataValueSetService.saveDataValueSetPdf( inputStream, importOptions, taskId );
        }
        else if ( FORMAT_ADX.equals( format ) )
        {
            adxDataService.saveDataValueSet( inputStream, importOptions, taskId );
        }
        else // FORMAT_XML
        {
            dataValueSetService.saveDataValueSet( inputStream, importOptions, taskId );
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
}
