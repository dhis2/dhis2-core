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
package org.hisp.dhis.webapi.controller.linelist;

import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;

import com.google.common.collect.ImmutableMap;

@SuppressWarnings( "unchecked" )
public abstract class LineListingRequestMapper<R extends LineListingRequest, P extends LineListingParams>
{

    private final I18nFormat i18nFormat;

    private final DataQueryService dataQueryService;

    public LineListingRequestMapper( DataQueryService dataQueryService, I18nManager i18nManager )
    {
        this.dataQueryService = dataQueryService;
        this.i18nFormat = i18nManager.getI18nFormat();
    }

    @SuppressWarnings( "unchecked" )
    public P map( String type, R request, DhisApiVersion apiVersion )
    {
        LineListingMappingContext<P> context = getContext( type, request, apiVersion );

        context = context.toBuilder()
            .request( request )
            .userOrganisationUnits( dataQueryService.getUserOrgUnits( null, request.getUserOrgUnit() ) )
            .build();

        Map<Type, List<Object>> queryElementsByType = preprocessQueryElements( context );

        return (P) context.getParams().toBuilder()
            .countRequested( request.isTotalPages() )
            .requestPaged( request.isPaging() )
            .page( request.getPage() )
            .pageSize( request.getPageSize() )
            .dimensions( castTo( queryElementsByType.get( Type.DIMENSIONS ), DimensionalObject.class ) )
            .filters( castTo( queryElementsByType.get( Type.FILTERS ), DimensionalObject.class ) )
            .items( castTo( queryElementsByType.get( Type.ITEMS ), QueryItem.class ) )
            .itemFilters( castTo( queryElementsByType.get( Type.ITEM_FILTERS ), QueryItem.class ) )
            .build();
    }

    protected abstract LineListingMappingContext<P> getContext( String type, R request, DhisApiVersion apiVersion );

    private Map<Type, List<Object>> preprocessQueryElements( LineListingMappingContext<P> mappingContext )
    {
        Map<Type, List<Object>> elementsByType = ImmutableMap.<Type, List<Object>> builder()
            .put( Type.DIMENSIONS, new ArrayList<>() )
            .put( Type.FILTERS, new ArrayList<>() )
            .put( Type.ITEMS, new ArrayList<>() )
            .put( Type.ITEM_FILTERS, new ArrayList<>() )
            .build();

        Stream.of( QueryElementPreProcessorDefinition.values() )
            .forEach( definition -> {

                Optional.of( mappingContext )
                    .map( LineListingMappingContext::getRequest )
                    .map( definition.getUidsGetter() )
                    .orElse( Collections.emptySet() )
                    .forEach( uid -> {
                        String dimensionId = getDimensionFromParam( uid );
                        List<String> items = getDimensionItemsFromParam( uid );

                        DimensionalObject dimensionalObject = dataQueryService.getDimension( dimensionId,
                            items,
                            mappingContext.getRequest().getRelativePeriodDate(),
                            mappingContext.getUserOrganisationUnits(),
                            i18nFormat, true, false,
                            IdScheme.UID );

                        if ( dimensionalObject != null )
                        {
                            elementsByType.get( definition.getBaseType() ).add( dimensionalObject );
                        }
                        else
                        {
                            // TODO: retrieve query items
                            elementsByType.get( definition.getQueryItemType() ).add( null );
                        }
                    } );
            } );
        return elementsByType;
    }

    private static <T> List<T> castTo( List<?> elements, Class<T> clazz )
    {
        return elements.stream()
            .map( clazz::cast )
            .collect( Collectors.toList() );
    }

    @Getter
    @RequiredArgsConstructor
    private enum QueryElementPreProcessorDefinition
    {
        DIMENSIONS( Type.DIMENSIONS, Type.ITEMS, LineListingRequest::getDimension ),
        FILTERS( Type.FILTERS, Type.ITEM_FILTERS, LineListingRequest::getFilter );

        private final Type baseType;

        private final Type queryItemType;

        private final Function<LineListingRequest, Collection<String>> uidsGetter;
    }

    private enum Type
    {
        DIMENSIONS,
        FILTERS,
        ITEMS,
        ITEM_FILTERS;
    }

}
