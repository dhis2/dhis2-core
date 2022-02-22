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
package org.hisp.dhis.webapi.controller.dataview;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackerdataview.TrackerDataView;
import org.hisp.dhis.trackerdataview.TrackerDataViewService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Zubair Asghar
 */

@Controller
@RequestMapping( value = TrackerDataViewController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class TrackerDataViewController
{
    public static final String RESOURCE_PATH = "/dataViews";

    private final TrackerDataViewService trackerDataViewService;

    private final RenderService renderService;

    // TODO correct userRole need to be provided here
    @PreAuthorize( "hasRole('ALL')" )
    @GetMapping( produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    @ResponseStatus( HttpStatus.OK )
    public TrackerDataView getDataView( @RequestParam String dataViewId )
    {
        return trackerDataViewService.getDataView( dataViewId );
    }

    // TODO correct userRole need to be provided here
    @PreAuthorize( "hasRole('ALL')" )
    @PostMapping
    @ResponseStatus( HttpStatus.CREATED )
    public void saveDataView( HttpServletRequest request )
        throws IOException
    {
        TrackerDataView trackerDataView = renderService.fromJson( request.getInputStream(), TrackerDataView.class );
        trackerDataViewService.saveDataView( trackerDataView );
    }

    // TODO correct userRole need to be provided here
    @PreAuthorize( "hasRole('ALL')" )
    @PutMapping
    @ResponseStatus( value = HttpStatus.OK )
    public void updateDataView( HttpServletRequest request )
        throws IOException
    {
        TrackerDataView trackerDataView = renderService.fromJson( request.getInputStream(), TrackerDataView.class );
        trackerDataViewService.updateDataView( trackerDataView );
    }

    @PreAuthorize( "hasRole('ALL')" )
    @DeleteMapping
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteDataView( @RequestParam String dataViewId )
    {
        trackerDataViewService.removeDataView( dataViewId );
    }
}
