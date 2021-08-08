/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.commons.jsonfiltering.web;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An interface which allow to define Field Filtering for rest endpoints.
 *
 * @param <T>
 */
public interface FieldFilterCustomizer<T>
{
    /**
     * It's usually not needed to override this method. Rather it would be
     * better to override {@link #getSupportedUriPatterns()} and
     * {@link #getApplicableClass()}
     *
     * @param requestUri request to enable this filtering on
     * @param beanClass The class on which the filtering should be enabled on
     * @return true if filtering is applicable, false otherwise
     */
    default boolean isApplicable( String requestUri, Class<?> beanClass )
    {
        if ( isUriSupported( requestUri ) )
        {
            return getApplicableClass().isAssignableFrom( beanClass );
        }
        return false;
    }

    /**
     * It's usually not needed to override this method. Rather it would be
     * better to override {@link #getSupportedUriPatterns()}
     *
     * @param requestUri request URI received by the controller
     * @return true if request should be filed-filtered, false otherwise
     */
    default boolean isUriSupported( String requestUri )
    {
        return Optional.ofNullable( getSupportedUriPatterns() )
            .orElse( Collections.emptyList() )
            .stream()
            .map( pattern -> pattern.matcher( requestUri ) )
            .anyMatch( Matcher::matches );
    }

    /**
     * @return a collection of patterns that match URIs from controller on which
     *         to enable field filtering
     */
    Collection<Pattern> getSupportedUriPatterns();

    /**
     * @return a class on which field filtering is enabled
     */
    Class<T> getApplicableClass();

    /**
     *
     * @param filter filter string from controller request parameters
     * @return a customized filter
     */
    default String customize( String filter )
    {
        return filter;
    }

}
