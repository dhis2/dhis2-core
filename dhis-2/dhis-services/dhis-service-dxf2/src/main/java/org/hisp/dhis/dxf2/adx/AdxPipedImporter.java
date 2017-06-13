package org.hisp.dhis.dxf2.adx;

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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.hibernate.SessionFactory;
import org.hisp.dhis.dbms.DbmsUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.scheduling.TaskId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author bobj
 */
public class AdxPipedImporter
    implements Callable<ImportSummary>
{
    public static final int PIPE_BUFFER_SIZE = 4096;

    public static final int TOTAL_MINUTES_TO_WAIT = 5;

    protected PipedInputStream pipeIn;

    private final DataValueSetService dataValueSetService;

    private final ImportOptions importOptions;

    private final TaskId id;
    
    private final SessionFactory sessionFactory;
    
    private final Authentication authentication;

    public AdxPipedImporter( DataValueSetService dataValueSetService, ImportOptions importOptions,
        TaskId id, PipedOutputStream pipeOut, SessionFactory sessionFactory ) throws IOException
    {
        this.dataValueSetService = dataValueSetService;
        this.pipeIn = new PipedInputStream( pipeOut, PIPE_BUFFER_SIZE );
        this.importOptions = importOptions;
        this.id = id;
        this.sessionFactory = sessionFactory;
        this.authentication = SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public ImportSummary call()
    {
        ImportSummary result = null;
        SecurityContextHolder.getContext().setAuthentication( authentication );
        DbmsUtils.bindSessionToThread( sessionFactory );

        try
        {
            result = dataValueSetService.saveDataValueSet( pipeIn, importOptions, id );
        }
        catch ( Exception ex )
        {
            result = new ImportSummary();
            result.setStatus( ImportStatus.ERROR );
            result.setDescription( "Exception: " + ex.getMessage() );
        }
        finally
        {
            IOUtils.closeQuietly( pipeIn );
            DbmsUtils.unbindSessionFromThread( sessionFactory );
        }
                
        return result;
    }
}
