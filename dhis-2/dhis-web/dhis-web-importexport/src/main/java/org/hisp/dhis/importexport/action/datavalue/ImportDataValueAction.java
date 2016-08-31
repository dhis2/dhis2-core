package org.hisp.dhis.importexport.action.datavalue;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hibernate.SessionFactory;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.datavalueset.tasks.ImportDataValueTask;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 */
public class ImportDataValueAction
    implements Action
{
    private static final Log log = LogFactory.getLog( ImportDataValueAction.class );

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private CurrentUserService currentUserService;
    
    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private Notifier notifier;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private File upload;

    public void setUpload( File upload )
    {
        this.upload = upload;
    }

    private boolean dryRun;

    public void setDryRun( boolean dryRun )
    {
        this.dryRun = dryRun;
    }

    private ImportStrategy strategy;

    public void setStrategy( String stgy )
    {
        this.strategy = ImportStrategy.valueOf( stgy );
    }
    
    private IdentifiableProperty idScheme;

    public void setIdScheme( IdentifiableProperty idScheme )
    {
        this.idScheme = idScheme;
    }

    private IdentifiableProperty dataElementIdScheme;

    public void setDataElementIdScheme( IdentifiableProperty dataElementIdScheme )
    {
        this.dataElementIdScheme = dataElementIdScheme;
    }

    private IdentifiableProperty orgUnitIdScheme;

    public void setOrgUnitIdScheme( IdentifiableProperty orgUnitIdScheme )
    {
        this.orgUnitIdScheme = orgUnitIdScheme;
    }

    private boolean skipExistingCheck;

    public void setSkipExistingCheck( boolean skipExistingCheck )
    {
        this.skipExistingCheck = skipExistingCheck;
    }

    private String importFormat;

    public void setImportFormat( String importFormat )
    {
        this.importFormat = importFormat;
    }

    private boolean preheatCache = true;
    
    public void setPreheatCache( boolean preheatCache )
    {
        this.preheatCache = preheatCache;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        strategy = strategy != null ? strategy : ImportStrategy.NEW_AND_UPDATES;

        TaskId taskId = new TaskId( TaskCategory.DATAVALUE_IMPORT, currentUserService.getCurrentUser() );

        notifier.clear( taskId );

        InputStream in = new FileInputStream( upload );

        in = StreamUtils.wrapAndCheckCompressionFormat( in );

        ImportOptions options = new ImportOptions().
            setIdScheme( idScheme ).setDataElementIdScheme( dataElementIdScheme ).setOrgUnitIdScheme( orgUnitIdScheme ).
            setDryRun( dryRun ).setPreheatCache( preheatCache ).setStrategy( strategy ).setSkipExistingCheck( skipExistingCheck );
        
        log.info( options );

        scheduler.executeTask( new ImportDataValueTask( dataValueSetService, sessionFactory, in, options, taskId, importFormat ) );

        return SUCCESS;
    }
}
