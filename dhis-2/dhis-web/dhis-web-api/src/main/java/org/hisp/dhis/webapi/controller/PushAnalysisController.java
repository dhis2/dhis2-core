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

import org.apache.commons.io.IOUtils;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.pushanalysis.PushAnalysis;
import org.hisp.dhis.pushanalysis.PushAnalysisService;
import org.hisp.dhis.schema.descriptors.PushAnalysisSchemaDescriptor;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Stian Sandvold
 */
@Controller
@RequestMapping( PushAnalysisSchemaDescriptor.API_ENDPOINT )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class PushAnalysisController extends AbstractCrudController<PushAnalysis>
{

    @Autowired
    private PushAnalysisService pushAnalysisService;

    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    private CurrentUserService currentUserService;

    /**
     * Endpoint that renders the same content as a Push Analysis email would contain, for the logged in user.
     * Used for users to preview the content of Push Analysis
     *
     * @param uid      Push Analysis uid
     * @param response
     * @throws Exception
     */
    @RequestMapping( value = "/{uid}/render", method = RequestMethod.GET )
    public void renderPushAnalytics(
        @PathVariable() String uid,
        HttpServletResponse response
    )
        throws Exception
    {
        PushAnalysis pushAnalysis = pushAnalysisService.getByUid( uid );
        if ( pushAnalysis == null )
        {
            throw new WebMessageException(
                WebMessageUtils.notFound( "Push analysis with uid " + uid + " was not found." ) );
        }

        contextUtils
            .configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING );

        IOUtils.write(
            pushAnalysisService.generatePushAnalysisForUser( currentUserService.getCurrentUser(), pushAnalysis ),
            response.getOutputStream() );
    }

    @RequestMapping( value = "/{uid}/start", method = RequestMethod.GET )
    public void startPushAnalysis(
        @PathVariable String uid,
        HttpServletResponse response
    )
        throws Exception
    {
        PushAnalysis pushAnalysis = pushAnalysisService.getByUid( uid );
        if ( pushAnalysis == null )
        {
            throw new WebMessageException(
                WebMessageUtils.notFound( "Push analysis with uid " + uid + " was not found." ) );
        }

        if ( !pushAnalysisService.startPushAnalysis( pushAnalysis ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict(
                "Could not start push analysis. Push analysis is already running, or no interval has been set for this push analysis." ) );
        }
    }

    @RequestMapping( value = "/{uid}/stop", method = RequestMethod.GET )
    public void stopPushAnalysis(
        @PathVariable String uid,
        HttpServletResponse response
    )
        throws Exception
    {
        PushAnalysis pushAnalysis = pushAnalysisService.getByUid( uid );
        if ( pushAnalysis == null )
        {
            throw new WebMessageException(
                WebMessageUtils.notFound( "Push analysis with uid " + uid + " was not found." ) );
        }

        pushAnalysisService.stopPushAnalysis( pushAnalysis );
    }

    // Temporary
    @RequestMapping( value = "/{uid}/run", method = RequestMethod.GET )
    public void sendPushAnalysis(
        @PathVariable() String uid,
        HttpServletResponse response
    )
        throws Exception
    {

        PushAnalysis pushAnalysis = pushAnalysisService.getByUid( uid );
        if ( pushAnalysis == null )
        {
            throw new WebMessageException(
                WebMessageUtils.notFound( "Push analysis with uid " + uid + " was not found." ) );
        }

        pushAnalysisService.runPushAnalysis( pushAnalysis );
    }

}
