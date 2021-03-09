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
package org.hisp.dhis.scheduling;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.scheduling.JobType.values;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.beanutils.PropertyUtils;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.schema.NodePropertyIntrospectorService;
import org.hisp.dhis.schema.Property;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;

/**
 * @author Henning Håkonsen
 */
@Slf4j
@Service( "jobConfigurationService" )
public class DefaultJobConfigurationService
    implements JobConfigurationService
{
    private final IdentifiableObjectStore<JobConfiguration> jobConfigurationStore;

    public DefaultJobConfigurationService(
        @Qualifier( "org.hisp.dhis.scheduling.JobConfigurationStore" ) IdentifiableObjectStore<JobConfiguration> jobConfigurationStore )
    {
        checkNotNull( jobConfigurationStore );

        this.jobConfigurationStore = jobConfigurationStore;
    }

    @Override
    @Transactional
    public long addJobConfiguration( JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isInMemoryJob() )
        {
            jobConfigurationStore.save( jobConfiguration );
        }

        return jobConfiguration.getId();
    }

    @Override
    @Transactional
    public void addJobConfigurations( List<JobConfiguration> jobConfigurations )
    {
        jobConfigurations.forEach( jobConfiguration -> jobConfigurationStore.save( jobConfiguration ) );
    }

    @Override
    @Transactional
    public long updateJobConfiguration( JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isInMemoryJob() )
        {
            jobConfigurationStore.update( jobConfiguration );
        }

        return jobConfiguration.getId();
    }

    @Override
    @Transactional
    public void deleteJobConfiguration( JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isInMemoryJob() )
        {
            jobConfigurationStore.delete( jobConfigurationStore.getByUid( jobConfiguration.getUid() ) );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public JobConfiguration getJobConfigurationByUid( String uid )
    {
        return jobConfigurationStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public JobConfiguration getJobConfiguration( long jobId )
    {
        return jobConfigurationStore.get( jobId );
    }

    @Override
    @Transactional( readOnly = true )
    public List<JobConfiguration> getAllJobConfigurations()
    {
        return jobConfigurationStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public Map<String, Map<String, Property>> getJobParametersSchema()
    {
        Map<String, Map<String, Property>> propertyMap = Maps.newHashMap();

        for ( JobType jobType : values() )
        {
            if ( !jobType.isConfigurable() )
            {
                continue;
            }

            Map<String, Property> jobParameters = Maps.uniqueIndex( getJobParameters( jobType ), p -> p.getName() );

            propertyMap.put( jobType.name(), jobParameters );
        }

        return propertyMap;
    }

    @Override
    public List<JobTypeInfo> getJobTypeInfo()
    {
        List<JobTypeInfo> jobTypes = new ArrayList<>();

        for ( JobType jobType : values() )
        {
            if ( !jobType.isConfigurable() )
            {
                continue;
            }

            String name = TextUtils.getPrettyEnumName( jobType );

            List<Property> jobParameters = getJobParameters( jobType );

            JobTypeInfo info = new JobTypeInfo( name, jobType, jobParameters );

            jobTypes.add( info );
        }

        return jobTypes;
    }

    @Override
    public void refreshScheduling( JobConfiguration jobConfiguration )
    {
        if ( jobConfiguration.isEnabled() )
        {
            jobConfiguration.setJobStatus( JobStatus.SCHEDULED );
        }
        else
        {
            jobConfiguration.setJobStatus( JobStatus.DISABLED );
        }

        jobConfigurationStore.update( jobConfiguration );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a list of job parameters for the given job type.
     *
     * @param jobType the {@link JobType}.
     * @return a list of {@link Property}.
     */
    private List<Property> getJobParameters( JobType jobType )
    {
        List<Property> jobParameters = new ArrayList<>();

        Class<?> clazz = jobType.getJobParameters();

        if ( clazz == null )
        {
            return jobParameters;
        }

        final Set<String> propertyNames = Stream.of( PropertyUtils.getPropertyDescriptors( clazz ) )
            .filter( pd -> pd.getReadMethod() != null && pd.getWriteMethod() != null
                && pd.getReadMethod().getAnnotation( JsonProperty.class ) != null )
            .map( PropertyDescriptor::getName )
            .collect( Collectors.toSet() );

        for ( Field field : Stream.of( clazz.getDeclaredFields() ).filter( f -> propertyNames.contains( f.getName() ) )
            .collect( Collectors.toList() ) )
        {
            Property property = new Property( Primitives.wrap( field.getType() ), null, null );
            property.setName( field.getName() );
            property.setFieldName( TextUtils.getPrettyPropertyName( field.getName() ) );

            try
            {
                field.setAccessible( true );
                property.setDefaultValue( field.get( jobType.getJobParameters().newInstance() ) );
            }
            catch ( IllegalAccessException | InstantiationException e )
            {
                log.error(
                    "Fetching default value for JobParameters properties failed for property: " + field.getName(), e );
            }

            String relativeApiElements = jobType.getRelativeApiElements() != null
                ? jobType.getRelativeApiElements().get( field.getName() )
                : "";

            if ( relativeApiElements != null && !relativeApiElements.equals( "" ) )
            {
                property.setRelativeApiEndpoint( relativeApiElements );
            }

            if ( Collection.class.isAssignableFrom( field.getType() ) )
            {
                property = new NodePropertyIntrospectorService()
                    .setPropertyIfCollection( property, field, clazz );
            }

            jobParameters.add( property );
        }

        return jobParameters;
    }
}
