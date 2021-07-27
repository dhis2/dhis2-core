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
package org.hisp.dhis.webapi.filter;

import javax.servlet.Filter;

import org.hisp.dhis.condition.RedisDisabledCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * Configuration registered if {@link RedisDisabledCondition} matches to true.
 * This serves as a fallback to spring-session if redis is disabled. Since
 * web.xml has a "springSessionRepositoryFilter" mapped to all urls, the
 * container will expect a filter bean with that name. Therefore we define a
 * dummy {@link Filter} named springSessionRepositoryFilter. Here we define a
 * {@link CharacterEncodingFilter} without setting any encoding so that requests
 * will simply pass through the filter.
 *
 * @author Ameen Mohamed
 *
 */
@Configuration
@Conditional( RedisDisabledCondition.class )
public class DefaultSessionConfiguration
{
    /**
     * Defines a {@link CharacterEncodingFilter} named
     * springSessionRepositoryFilter
     *
     * @return a {@link CharacterEncodingFilter} without specifying encoding.
     */
    @Bean( "springSessionRepositoryFilter" )
    public Filter springSessionRepositoryFilter()
    {
        return new CharacterEncodingFilter();
    }
}