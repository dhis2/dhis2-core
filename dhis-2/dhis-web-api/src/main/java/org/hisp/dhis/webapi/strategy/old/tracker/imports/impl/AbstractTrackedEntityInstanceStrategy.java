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
package org.hisp.dhis.webapi.strategy.old.tracker.imports.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.webapi.strategy.old.tracker.imports.TrackedEntityInstanceStrategyHandler;
import org.springframework.http.MediaType;

@RequiredArgsConstructor
public abstract class AbstractTrackedEntityInstanceStrategy implements TrackedEntityInstanceStrategyHandler
{
    protected final TrackedEntityInstanceService trackedEntityInstanceService;

    protected final AsyncTaskExecutor taskExecutor;

    protected List<TrackedEntityInstance> getTrackedEntityInstancesListByMediaType( String mediaType,
        InputStream inputStream )
        throws IOException,
        BadRequestException
    {
        if ( MediaType.valueOf( mediaType ).equals( MediaType.APPLICATION_JSON ) )
        {
            return trackedEntityInstanceService.getTrackedEntityInstancesJson( inputStream );
        }
        else if ( mediaType
            .equals( MediaType.APPLICATION_XML_VALUE ) )
        {
            return trackedEntityInstanceService.getTrackedEntityInstancesXml( inputStream );
        }
        else
        {
            throw new BadRequestException( "Value " + mediaType + " not allowed as Media Type " );
        }
    }
}
