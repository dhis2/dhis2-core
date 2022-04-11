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
package org.hisp.dhis.webapi.controller.linelist.trackedentity;

import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.webapi.controller.linelist.LineListingMappingContext;
import org.hisp.dhis.webapi.controller.linelist.LineListingRequestMapper;
import org.springframework.stereotype.Service;

@Service
class TrackedEntityLineListingRequestMapper
    extends LineListingRequestMapper<TrackedEntityLineListingRequest, TrackedEntityLineListingParams>
{
    private final TrackedEntityTypeService trackedEntityTypeService;

    private final ProgramService programService;

    public TrackedEntityLineListingRequestMapper(
        DataQueryService dataQueryService,
        TrackedEntityTypeService trackedEntityTypeService,
        I18nManager i18nManager,
        ProgramService programService )
    {
        super( dataQueryService, i18nManager );
        this.trackedEntityTypeService = trackedEntityTypeService;
        this.programService = programService;
    }

    @Override
    protected LineListingMappingContext<TrackedEntityLineListingParams> getContext( String type,
        TrackedEntityLineListingRequest request, DhisApiVersion apiVersion )
    {
        return LineListingMappingContext.<TrackedEntityLineListingParams> builder()
            .params( TrackedEntityLineListingParams.builder()
                .programs( programService.getPrograms( request.getPrograms() ) )
                .trackedEntityType( trackedEntityTypeService.getTrackedEntityType( type ) )
                .build() )
            .build();
    }

}
