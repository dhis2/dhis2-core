/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller.metadata;

import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dbms.DbmsUtils;
import org.hisp.dhis.dxf2.gml.GmlImportService;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.security.SecurityContextRunnable;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Component
@Scope( "prototype" )
@Slf4j
public class GmlAsyncImporter extends SecurityContextRunnable
{
    @Autowired
    private GmlImportService gmlImportService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private Notifier notifier;

    private MetadataImportParams params;

    private InputStream inputStream;

    @Override
    public void call()
    {
        // This is to fix LazyInitializationException
        if ( params.getUser() != null )
        {
            params.setUser( manager.get( User.class, params.getUser().getUid() ) );
        }

        if ( params.getOverrideUser() != null )
        {
            params.setOverrideUser( manager.get( User.class, params.getOverrideUser().getUid() ) );
        }

        gmlImportService.importGml( inputStream, params );
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

    public void setParams( MetadataImportParams params )
    {
        this.params = params;
    }

    public void setInputStream( InputStream inputStream )
    {
        this.inputStream = inputStream;
    }

    @Override
    public void handleError( Throwable ex )
    {
        log.error( DebugUtils.getStackTrace( ex ) );
        notifier.notify( params.getId(), NotificationLevel.ERROR, "Process failed: " + ex.getMessage(), true );
    }
}
