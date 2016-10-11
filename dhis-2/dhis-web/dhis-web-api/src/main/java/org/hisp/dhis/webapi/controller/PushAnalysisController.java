package org.hisp.dhis.webapi.controller;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.pushanalysis.PushAnalysis;
import org.hisp.dhis.pushanalysis.PushAnalysisService;
import org.hisp.dhis.pushanalysis.scheduling.PushAnalysisTask;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.schema.descriptors.PushAnalysisSchemaDescriptor;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Stian Sandvold
 */
@Controller
@RequestMapping( PushAnalysisSchemaDescriptor.API_ENDPOINT )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class PushAnalysisController
    extends AbstractCrudController<PushAnalysis>
{
    private static final Log log = LogFactory.getLog( PushAnalysisController.class );

    @Autowired
    private PushAnalysisService pushAnalysisService;

    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private Scheduler scheduler;

    @RequestMapping( value = "/{uid}/render", method = RequestMethod.GET )
    public void renderPushAnalytics(
        @PathVariable() String uid,
        HttpServletResponse response ) throws WebMessageException, IOException
    {
        PushAnalysis pushAnalysis = pushAnalysisService.getByUid( uid );
        
        if ( pushAnalysis == null )
        {
            throw new WebMessageException(
                WebMessageUtils.notFound( "Push analysis with uid " + uid + " was not found" ) );
        }

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.NO_CACHE );

        log.info( "User '" + currentUserService.getCurrentUser().getUsername() + "' started PushAnalysis for 'rendering'" );

        String result = pushAnalysisService.generateHtmlReport( pushAnalysis, currentUserService.getCurrentUser(), null );
        response.getWriter().write( result );
        response.getWriter().close();
    }

    @ResponseStatus( HttpStatus.NO_CONTENT )
    @RequestMapping( value = "/{uid}/run", method = RequestMethod.POST )
    public void sendPushAnalysis( @PathVariable() String uid ) throws WebMessageException, IOException
    {
        PushAnalysis pushAnalysis = pushAnalysisService.getByUid( uid );
        
        if ( pushAnalysis == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Push analysis with uid " + uid + " was not found" ) );
        }

        scheduler.executeTask( new PushAnalysisTask(
            pushAnalysis.getId(),
            new TaskId( TaskCategory.PUSH_ANALYSIS, currentUserService.getCurrentUser() ),
            pushAnalysisService ) );
    }

    @Override
    protected void preDeleteEntity( PushAnalysis pushAnalysis )
    {
        scheduler.stopTask( pushAnalysis.getSchedulingKey() );
    }

    @Override
    protected void postUpdateEntity( PushAnalysis pushAnalysis )
    {
        pushAnalysisService.refreshPushAnalysisScheduling( pushAnalysis );
    }

    @Override
    protected void postCreateEntity( PushAnalysis pushAnalysis )
    {
        pushAnalysisService.refreshPushAnalysisScheduling( pushAnalysis );
    }

    @Override
    protected void postPatchEntity( PushAnalysis pushAnalysis )
    {
        pushAnalysisService.refreshPushAnalysisScheduling( pushAnalysis );
    }
}
