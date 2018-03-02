package org.hisp.dhis.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that matches to true if redis.enabled property is absent in
 * dhis.conf or if it is explicitly set to false
 * 
 * @author Ameen Mohamed
 *
 */
public class RedisDisabledCondition implements Condition
{
    
    private static final Log log = LogFactory.getLog( RedisEnabledCondition.class );
    
    
    @Override
    public boolean matches( ConditionContext context, AnnotatedTypeMetadata metadata )
    {
        DhisConfigurationProvider dhisConfigurationProvider = (DhisConfigurationProvider) context.getBeanFactory()
            .getBean( "dhisConfigurationProvider" );

        boolean redisDisabled = (dhisConfigurationProvider == null
            || !dhisConfigurationProvider.getProperty( ConfigurationKey.REDIS_ENABLED ).equalsIgnoreCase( "true" ));

        if ( redisDisabled )
        {
            log.info( "Redis is disabled. Falling back to default session handling." );
        }
        return redisDisabled;
    }

}
