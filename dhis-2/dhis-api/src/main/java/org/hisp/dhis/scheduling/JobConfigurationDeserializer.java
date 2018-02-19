package org.hisp.dhis.scheduling;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

/**
 * Custom deserializer for {@link JobConfiguration} objects. This is due to different objects
 * being stored in the parameter variable.
 *
 * @author Henning Håkonsen
 */
public class JobConfigurationDeserializer
    extends JsonDeserializer<JobConfiguration>
{
    private final String NAME = "name";
    private final String JOB_TYPE = "jobType";
    private final String ENABLED = "enabled";
    private final String CONTINUOUS_EXECUTION = "continuousExecution";
    private final String JOB_PARAMETERS = "jobParameters";
    private final String CRON_EXPRESSION = "cronExpression";

    @Override
    public JobConfiguration deserialize( JsonParser jsonParser,
        DeserializationContext deserializationContext )
        throws IOException
    {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = mapper.readTree( jsonParser );

        if ( !root.has( NAME ) )
        {
            throw new IOException( "name must not be null" );
        }

        if ( !root.has( JOB_TYPE ) )
        {
            throw new IOException( "jobType must not be null" );
        }

        JobType jobType = JobType.valueOf( root.get( JOB_TYPE ).textValue() );

        JobParameters jobParameters = null;
        if ( root.get( JOB_PARAMETERS ) != null && jobType.getJobParameters() != null )
        {
            jobParameters = mapper.convertValue( root.get( JOB_PARAMETERS ), jobType.getJobParameters() );
        }

        boolean enabled = !root.has( ENABLED ) || root.get( ENABLED ).asBoolean();

        boolean continuousExecution = root.has( CONTINUOUS_EXECUTION ) && root.get( CONTINUOUS_EXECUTION ).asBoolean();

        String cronExpression = root.has( CRON_EXPRESSION ) ? root.get( CRON_EXPRESSION ).textValue() : "";

        return new JobConfiguration( root.get( NAME ).textValue(), jobType,
            cronExpression, jobParameters, continuousExecution, enabled );
    }
}
