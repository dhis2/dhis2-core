package org.hisp.dhis.webapi.mvc;

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

import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hisp.dhis.webapi.mvc.annotation.ApiVersion.Version;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class CustomRequestMappingHandlerMapping
    extends RequestMappingHandlerMapping
{
    @Override
    protected RequestMappingInfo getMappingForMethod( Method method, Class<?> handlerType )
    {
        RequestMappingInfo info = super.getMappingForMethod( method, handlerType );

        if ( info == null )
        {
            return null;
        }

        ApiVersion typeApiVersion = AnnotationUtils.findAnnotation( handlerType, ApiVersion.class );
        ApiVersion methodApiVersion = AnnotationUtils.findAnnotation( method, ApiVersion.class );

        if ( typeApiVersion == null && methodApiVersion == null )
        {
            return info;
        }

        RequestMethodsRequestCondition methodsCondition = info.getMethodsCondition();

        if ( methodsCondition.getMethods().isEmpty() )
        {
            methodsCondition = new RequestMethodsRequestCondition( RequestMethod.GET );
        }

        Set<String> rqmPatterns = info.getPatternsCondition().getPatterns();
        Set<String> patterns = new HashSet<>();

        Set<Version> versions = getVersions( typeApiVersion, methodApiVersion );

        for ( String pattern : rqmPatterns )
        {
            versions.stream()
                .filter( version -> !version.isIgnore() )
                .forEach( version -> {
                    if ( !pattern.startsWith( version.getPath() ) )
                    {
                        if ( pattern.startsWith( "/" ) ) patterns.add( "/" + version.getPath() + pattern );
                        else patterns.add( "/" + version.getPath() + "/" + pattern );
                    }
                    else
                    {
                        patterns.add( pattern );
                    }
                } );
        }

        PatternsRequestCondition patternsRequestCondition = new PatternsRequestCondition(
            patterns.toArray( new String[]{} ), null, null, true, true, null );

        return new RequestMappingInfo(
            null, patternsRequestCondition, methodsCondition, info.getParamsCondition(), info.getHeadersCondition(), info.getConsumesCondition(),
            info.getProducesCondition(), info.getCustomCondition()
        );
    }

    private Set<Version> getVersions( ApiVersion typeApiVersion, ApiVersion methodApiVersion )
    {
        Set<Version> includes = new HashSet<>();
        Set<Version> excludes = new HashSet<>();

        if ( typeApiVersion != null )
        {
            includes.addAll( Arrays.asList( typeApiVersion.include() ) );
            excludes.addAll( Arrays.asList( typeApiVersion.exclude() ) );
        }

        if ( methodApiVersion != null )
        {
            includes.addAll( Arrays.asList( methodApiVersion.include() ) );
            excludes.addAll( Arrays.asList( methodApiVersion.exclude() ) );
        }

        if ( includes.contains( Version.ALL ) )
        {
            boolean includeDefault = includes.contains( Version.DEFAULT );
            boolean includeTest = includes.contains( Version.TEST );
            includes = new HashSet<>( Arrays.asList( Version.values() ) );

            if ( !includeDefault )
            {
                includes.remove( Version.DEFAULT );
            }

            if ( !includeTest )
            {
                includes.remove( Version.TEST );
            }
        }

        includes.removeAll( excludes );

        return includes;
    }
}
