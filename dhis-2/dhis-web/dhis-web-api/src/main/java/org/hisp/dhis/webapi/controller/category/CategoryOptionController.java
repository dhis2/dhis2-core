package org.hisp.dhis.webapi.controller.category;

import org.apache.commons.lang3.StringUtils;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.schema.descriptors.CategoryOptionSchemaDescriptor;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@Controller
@Slf4j
@RequestMapping( value = CategoryOptionSchemaDescriptor.API_ENDPOINT )
public class CategoryOptionController
    extends AbstractCrudController<CategoryOption>
{
    @Autowired
    private CurrentUserService currentUserService;
    
    @Autowired
    private OrganisationUnitService organisationUnitService;
    
    @Override
    protected void preProcessRequestParameters( WebMetadata metadata, WebOptions options, List<String> filters, List<Order> orders )
    {
        if ( "true".equalsIgnoreCase( options.get( "includeCaptureScopeOnly" ) ) )
        {
            if ( filters.isEmpty() )
            {
                User user = currentUserService.getCurrentUser();

                if ( user != null )
                {
                    List<String> orgUnitUids = user.getOrganisationUnits().stream().map( orgUnit -> orgUnit.getUid() ).collect( Collectors.toList() );
                    filters.add( "organisationUnits.id:in:[" + StringUtils.join( orgUnitUids, "," ) + "]" );
                    filters.add( "organisationUnits:empty" );
                    options.getOptions().put( WebOptions.ROOT_JUNCTION, "OR" );
                }
            }
            else
            {
                Log.error( "Filters and includeCaptureScopeOnly cannot be specified at the same time" );
                throw new IllegalArgumentException( "Filters and includeCaptureScopeOnly cannot be specified at the same time." );
            }
        }
    }
    
}
