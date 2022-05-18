/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.apache.commons.io.IOUtils.toBufferedInputStream;
import static org.hisp.dhis.common.IdentifiableProperty.UID;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import lombok.AllArgsConstructor;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.dbms.DbmsUtils;
import org.hisp.dhis.dxf2.geojson.GeoJsonImportParams;
import org.hisp.dhis.dxf2.geojson.GeoJsonImportReport;
import org.hisp.dhis.dxf2.geojson.GeoJsonService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.security.SecurityContextRunnable;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Jan Bernitt
 */
@RequestMapping( "/organisationUnits/geometry" )
@RestController
@AllArgsConstructor
public class GeoJsonImportController
{
    private final GeoJsonService geoJsonService;

    private final Notifier notifier;

    private final TaskExecutor taskExecutor;

    private final SessionFactory sessionFactory;

    @PostMapping( value = "", consumes = { "application/geo+json", "application/json" } )
    public WebMessage postImport(
        @RequestParam( defaultValue = "true" ) boolean geoJsonId,
        @RequestParam( required = false ) String geoJsonProperty,
        @RequestParam( required = false ) String orgUnitProperty,
        @RequestParam( required = false ) String attributeId,
        @RequestParam( required = false ) boolean dryRun,
        @RequestParam( required = false, defaultValue = "false" ) boolean async,
        HttpServletRequest request,
        @CurrentUser User currentUser )
        throws IOException
    {
        GeoJsonImportParams params = GeoJsonImportParams.builder()
            .attributeId( attributeId )
            .dryRun( dryRun )
            .idType( orgUnitProperty == null ? UID : IdentifiableProperty.valueOf( orgUnitProperty.toUpperCase() ) )
            .orgUnitIdProperty( geoJsonId ? "id" : "properties." + geoJsonProperty )
            .user( currentUser )
            .build();

        if ( async )
        {
            JobConfiguration config = new JobConfiguration( "GeoJSON import", JobType.GEOJSON_IMPORT,
                currentUser.getUid(), true );
            taskExecutor.execute(
                new GeoJsonAsyncImporter( params, config, toBufferedInputStream( request.getInputStream() ) ) );
            return jobConfigurationReport( config );
        }

        return toWebMessage( geoJsonService.importGeoData( params, request.getInputStream() ) );
    }

    private WebMessage toWebMessage( GeoJsonImportReport report )
    {
        if ( report.getStatus() == ImportStatus.ERROR )
        {
            return conflict( "Import failed." ).setResponse( report );
        }
        return report.getImportCount().getIgnored() > 0
            ? ok( "Import partially successful." ).setResponse( report )
            : ok( "Import successful." ).setResponse( report );
    }

    @AllArgsConstructor
    private class GeoJsonAsyncImporter extends SecurityContextRunnable
    {

        private final GeoJsonImportParams params;

        private final JobConfiguration config;

        private final InputStream data;

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

        @Override
        public void call()
        {
            notifier.clear( config );
            notifier.notify( config, NotificationLevel.INFO, "GeoJSON import stared", true );
            GeoJsonImportReport report = geoJsonService.importGeoData( params, data );
            notifier.notify( config, NotificationLevel.INFO, "GeoJSON import complete. " + report.getImportCount(),
                true );
            notifier.addJobSummary( config, report, GeoJsonImportReport.class );
        }

        @Override
        public void handleError( Throwable ex )
        {
            notifier.notify( config, NotificationLevel.ERROR, "GeoJSON import failed: " + ex.getMessage(), true );
        }
    }
}
