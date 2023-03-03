/*
 * Copyright (c) 2004-2023, University of Oslo
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

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;

import org.apache.commons.codec.digest.DigestUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@RequestMapping( QueryController.RESOURCE_PATH )
@Controller
public class QueryController
{
    public static final String RESOURCE_PATH = "/query";

    private final Cache<String> shortenerCache;

    public QueryController( CacheProvider cacheProvider )
    {
        this.shortenerCache = cacheProvider.createQueryShortenerCache();
    }

    @PostMapping( "/shorten" )
    @CrossOrigin
    public RedirectView postQuery( HttpServletRequest request, @RequestParam String targetUrl,
        RedirectAttributes redirectAttributes )
    {
        String hash = DigestUtils.sha1Hex( targetUrl );
        shortenerCache.put( hash, targetUrl );
        return new RedirectView( "/api/query/shortened/" + hash, false, false );
    }

    @GetMapping( "/shortened/{hash}" )
    @CrossOrigin
    public String getQuery( @PathVariable( "hash" ) String hash, HttpServletRequest request )
        throws NotFoundException
    {
        Optional<String> targetUrl = shortenerCache.get( hash );
        if ( targetUrl.isPresent() )
        {
            return "forward:" + targetUrl.get();
        }
        throw new NotFoundException( "No shortened query found with this hash id, it may have expired." );
    }
}
