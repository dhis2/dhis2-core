package org.hisp.dhis.webapi.controller.event;

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

import org.hisp.dhis.schema.descriptors.TrackedEntityAttributeSchemaDescriptor;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeReservedValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeReservedValueService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = TrackedEntityAttributeSchemaDescriptor.API_ENDPOINT )
public class TrackedEntityAttributeController
    extends AbstractCrudController<TrackedEntityAttribute>
{
    @Autowired
    TrackedEntityAttributeReservedValueService trackedEntityAttributeReservedValueService;

    @Autowired
    TrackedEntityAttributeService trackedEntityAttributeService;

    @RequestMapping( value = "/{id}/generateAndReserve", method = RequestMethod.GET, produces = { ContextUtils.CONTENT_TYPE_JSON, ContextUtils.CONTENT_TYPE_JAVASCRIPT } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_INSTANCE_ADD')" )
    public @ResponseBody List<TrackedEntityAttributeReservedValue> queryTrackedEntityInstancesJson(
        @RequestParam( required = false ) Integer numberToReserve,
        @PathVariable String id,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        if ( numberToReserve == null || numberToReserve < 1 )
        {
            numberToReserve = 1;
        }

        TrackedEntityAttribute attribute = trackedEntityAttributeService.getTrackedEntityAttribute( id );
        if ( attribute == null )
        {
            throw new Exception( "No attribute found with id " + id );
        }

        return trackedEntityAttributeReservedValueService.createTrackedEntityReservedValues(
            attribute, numberToReserve );
    }

    @RequestMapping( value = "/{id}/generate", method = RequestMethod.GET, produces = { ContextUtils.CONTENT_TYPE_JSON, ContextUtils.CONTENT_TYPE_JAVASCRIPT } )
    public @ResponseBody String queryTrackedEntityInstancesJson(
        @PathVariable String id,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        TrackedEntityAttribute attribute = trackedEntityAttributeService.getTrackedEntityAttribute( id );
        if ( attribute == null )
        {
            throw new Exception( "No attribute found with id " + id );
        }

        return trackedEntityAttributeReservedValueService.getGeneratedValue( attribute );
    }
}
