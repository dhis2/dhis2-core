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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hisp.dhis.commons.collection.CollectionUtils.isEmpty;
import static org.hisp.dhis.config.HibernateEncryptionConfig.AES_128_STRING_ENCRYPTOR;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dataexchange.aggregate.AggregateDataExchange;
import org.hisp.dhis.dataexchange.aggregate.Api;
import org.hisp.dhis.dataexchange.aggregate.SourceRequest;
import org.hisp.dhis.dataexchange.aggregate.TargetType;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Component
public class AggregateDataExchangeObjectBundleHook
    extends AbstractObjectBundleHook<AggregateDataExchange>
{
    private static final int SOURCE_REQUEST_NAME_MAX_LENGTH = 50;

    private final PooledPBEStringEncryptor encryptor;

    public AggregateDataExchangeObjectBundleHook(
        @Qualifier( AES_128_STRING_ENCRYPTOR ) PooledPBEStringEncryptor encryptor )
    {
        this.encryptor = encryptor;
    }

    @Override
    public void validate( AggregateDataExchange exchange, ObjectBundle bundle,
        Consumer<ErrorReport> addReports )
    {
        if ( isEmpty( exchange.getSource().getRequests() ) )
        {
            addReports.accept( new ErrorReport( AggregateDataExchange.class, ErrorCode.E6302, exchange.getUid() ) );
        }

        for ( SourceRequest request : exchange.getSource().getRequests() )
        {
            if ( isEmpty( request.getName() ) )
            {
                addReports.accept( new ErrorReport( AggregateDataExchange.class, ErrorCode.E4000, "source.name" ) );
            }

            if ( request.getName().length() > SOURCE_REQUEST_NAME_MAX_LENGTH )
            {
                addReports.accept( new ErrorReport( AggregateDataExchange.class, ErrorCode.E4001,
                    "source.name", SOURCE_REQUEST_NAME_MAX_LENGTH, request.getName().length() ) );
            }

            if ( isEmpty( request.getDx() ) || isEmpty( request.getPe() ) || isEmpty( request.getOu() ) )
            {
                addReports.accept( new ErrorReport( AggregateDataExchange.class, ErrorCode.E6303 ) );
            }
        }

        if ( exchange.getTarget().getType() == null )
        {
            addReports.accept( new ErrorReport( AggregateDataExchange.class, ErrorCode.E4000, "target.type" ) );
        }

        if ( exchange.getTarget().getType() == TargetType.EXTERNAL && exchange.getTarget().getApi() == null )
        {
            addReports.accept( new ErrorReport( AggregateDataExchange.class, ErrorCode.E6304 ) );
        }

        Api api = exchange.getTarget().getApi();

        if ( api != null && isEmpty( api.getUrl() ) )
        {
            addReports.accept( new ErrorReport( AggregateDataExchange.class, ErrorCode.E4000, "target.api.url" ) );
        }

        if ( api != null && !(api.isAccessTokenAuth() || api.isBasicAuth()) )
        {
            addReports.accept( new ErrorReport( AggregateDataExchange.class, ErrorCode.E6305 ) );
        }
    }

    @Override
    public void preCreate( AggregateDataExchange exchange, ObjectBundle bundle )
    {
        encryptApiSecrets( exchange );
    }

    @Override
    public void preUpdate( AggregateDataExchange exchange, AggregateDataExchange persistedExchange,
        ObjectBundle bundle )
    {
        encryptApiSecrets( exchange );
    }

    /**
     * Encrypts target API secrets.
     *
     * @param exchange the {@link AggregateDataExchange}.
     */
    private void encryptApiSecrets( AggregateDataExchange exchange )
    {
        if ( exchange.getTarget().getApi() != null )
        {
            Api api = exchange.getTarget().getApi();

            if ( api != null && StringUtils.isNotBlank( api.getPassword() ) )
            {
                api.setPassword( encryptor.encrypt( api.getPassword() ) );
            }

            if ( api != null && StringUtils.isNotBlank( api.getAccessToken() ) )
            {
                api.setAccessToken( encryptor.encrypt( api.getAccessToken() ) );
            }
        }
    }
}
