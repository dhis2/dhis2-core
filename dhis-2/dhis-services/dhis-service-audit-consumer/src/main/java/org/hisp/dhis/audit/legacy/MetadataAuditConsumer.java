package org.hisp.dhis.audit.legacy;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Objects;

import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.artemis.Topics;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.audit.AuditConsumer;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.audit.MetadataAudit;
import org.hisp.dhis.schema.audit.MetadataAuditService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A MetadataAudit object consumer.
 *
 * @author Luciano Fiandesio
 */
@Component
public class MetadataAuditConsumer implements AuditConsumer
{
    private static final Log log = LogFactory.getLog( MetadataAuditConsumer.class );

    private final ObjectMapper mapper = new ObjectMapper();

    private final MetadataAuditService metadataAuditService;
    private final RenderService renderService;
    private final boolean metadataAuditLog;
    private final boolean metadataAuditPersist;

    public MetadataAuditConsumer(
        MetadataAuditService metadataAuditService,
        RenderService renderService,
        DhisConfigurationProvider dhisConfig )
    {
        this.metadataAuditService = metadataAuditService;
        this.renderService = renderService;

        this.metadataAuditPersist = Objects.equals( dhisConfig.getProperty( ConfigurationKey.METADATA_AUDIT_PERSIST ), "on" );
        this.metadataAuditLog = Objects.equals( dhisConfig.getProperty( ConfigurationKey.METADATA_AUDIT_LOG ), "on" );
    }

    @JmsListener( destination = Topics.METADATA_TOPIC_NAME )
    public void consume( TextMessage message )
    {
        try
        {
            if ( metadataAuditLog || metadataAuditPersist )
            {
                String payload = message.getText();

                Audit auditMessage = renderService.fromJson( payload, Audit.class );
                MetadataAudit audit = toMetadataAudit( auditMessage.getData() );

                if ( metadataAuditLog )
                {
                    log.info( renderService.toJsonAsString( audit ) );
                }

                if ( metadataAuditPersist )
                {
                    metadataAuditService.addMetadataAudit( audit );
                }
            }
        }
        catch ( IOException e )
        {
            log.error(
                "An error occurred de-serializing the message payload. The message can not be de-serialized to an Audit object.",
                e );
        }
        catch ( Exception e )
        {
            log.error( "An error occurred persisting an Audit message of type 'METADATA'", e );
        }
    }

    @SuppressWarnings( "unchecked" )
    private MetadataAudit toMetadataAudit( Object map )
    {
        if ( map instanceof LinkedHashMap && ((LinkedHashMap) map).containsKey( "value" ) )
        {
            ((LinkedHashMap) map).put( "value", renderService.toJsonAsString( ((LinkedHashMap) map).get( "value" ) ) );
        }

        return mapper.convertValue( map, MetadataAudit.class );
    }
}
