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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.cache.CacheInfo;
import org.hisp.dhis.cache.CacheInfo.CacheCapInfo;
import org.hisp.dhis.cache.CacheInfo.CacheGroupInfo;
import org.hisp.dhis.cache.CappedLocalCache;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.feedback.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Gives insights into the {@link CappedLocalCache} state and allows to
 * invalidate entries as well as configure the cap settings.
 *
 * @author Jan Bernitt
 */
@OpenApi.Tags( "system" )
@OpenApi.Ignore
@Controller
@RequestMapping( value = "/caches" )
@RequiredArgsConstructor
@PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
public class CacheController
{

    private final CappedLocalCache cache;

    @GetMapping( produces = APPLICATION_JSON_VALUE )
    public @ResponseBody CacheInfo getInfo( @RequestParam( value = "condensed", required = false ) Boolean condensed )
    {
        CacheInfo info = getCacheInfo();
        if ( condensed == null || !condensed )
        {
            return info;
        }
        return new CacheInfo( info.getCap(), info.getBurden(), info.getTotal(),
            info.getRegions().stream()
                .filter( r -> r.getEntries() > 0 )
                .sorted( ( a, b ) -> Long.compare( b.getSize(), a.getSize() ) )
                .collect( Collectors.toList() ) );
    }

    @GetMapping( value = "/regions", produces = APPLICATION_JSON_VALUE )
    public @ResponseBody Set<String> getRegions()
    {
        // sort alphabetically
        return new TreeSet<>( cache.getRegions() );
    }

    @GetMapping( value = "/regions/{region}", produces = APPLICATION_JSON_VALUE )
    public @ResponseBody CacheGroupInfo getRegionInfo( @PathVariable( "region" ) String region )
        throws NotFoundException
    {
        CacheInfo info = getCacheInfo();
        for ( CacheGroupInfo groupInfo : info.getRegions() )
        {
            if ( groupInfo.getName().equals( region ) )
            {
                return groupInfo;
            }
        }
        throw new NotFoundException( region );
    }

    @GetMapping( value = "/cap", produces = APPLICATION_JSON_VALUE )
    public @ResponseBody CacheCapInfo getCapInfo()
    {
        return getCacheInfo().getCap();
    }

    @PutMapping( "/cap" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void updateCap( @RequestParam( value = "heap", required = false ) Integer heap,
        @RequestParam( value = "hard", required = false ) Integer hard,
        @RequestParam( value = "soft", required = false ) Integer soft )
    {
        if ( heap != null )
        {
            cache.setCapPercent( heap );
        }
        if ( hard != null )
        {
            cache.setHardCapPercentage( hard );
        }
        if ( soft != null )
        {
            cache.setSoftCapPercentage( soft );
        }
    }

    @PostMapping( "/invalidate" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void invalidate()
    {
        cache.invalidate();
    }

    @PostMapping( "/regions/{region}/invalidate" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void invalidateRegion( @PathVariable( "region" ) String region )
    {
        cache.invalidateRegion( region );
    }

    private CacheInfo getCacheInfo()
    {
        CacheInfo info = cache.getInfo();
        if ( info == null )
        {
            throw new IllegalStateException( "Capped local cache is not used." );
        }
        return info;
    }
}
