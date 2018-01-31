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

import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import org.hisp.dhis.common.GenericNameableObjectStore;
import org.hisp.dhis.schema.NodePropertyIntrospectorService;
import org.hisp.dhis.schema.Property;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.hisp.dhis.scheduling.JobType.values;

/**
 * @author Henning Håkonsen
 */
@Transactional
public class DefaultJobConfigurationService
    implements JobConfigurationService
{
    private GenericNameableObjectStore<JobConfiguration> jobConfigurationStore;

    public void setJobConfigurationStore( GenericNameableObjectStore<JobConfiguration> jobConfigurationStore )
    {
        this.jobConfigurationStore = jobConfigurationStore;
    }

    @Override
    public int addJobConfiguration( JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isInMemoryJob() )
        {
            jobConfigurationStore.save( jobConfiguration );
        }
        return jobConfiguration.getId( );
    }

    @Override
    public void addJobConfigurations( List<JobConfiguration> jobConfigurations )
    {
        jobConfigurations.forEach( jobConfiguration -> jobConfigurationStore.save( jobConfiguration ));
    }

    @Override
    public int updateJobConfiguration( JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isInMemoryJob() )
        {
            jobConfigurationStore.update( jobConfiguration );
        }
        return jobConfiguration.getId();
    }

    @Override
    public void deleteJobConfiguration( JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isInMemoryJob() )
        {
            jobConfigurationStore.delete( jobConfigurationStore.getByUid( jobConfiguration.getUid() ) );
        }
    }

    @Override
    public JobConfiguration getJobConfigurationByUid( String uid )
    {
        return jobConfigurationStore.getByUid( uid );
    }

    @Override
    public JobConfiguration getJobConfiguration( int jobId )
    {
        return jobConfigurationStore.get( jobId );
    }

    @Override
    public List<JobConfiguration> getAllJobConfigurations()
    {
        return jobConfigurationStore.getAll();
    }

    @Override
    public List<JobConfiguration> getAllJobConfigurationsSorted()
    {
        List<JobConfiguration> jobConfigurations = getAllJobConfigurations();

        Collections.sort( jobConfigurations );

        return jobConfigurations;
    }

    @Override
    public Map<String, Map<String, Property>> getJobParametersSchema()
    {
        Map<String, Map<String, Property>> propertyMap = Maps.newHashMap();

        for ( JobType jobType : values() )
        {
            Map<String, Property> jobParameters = new LinkedHashMap<>( );

            if ( !jobType.isConfigurable() )
            {
                continue;
            }

            Class<?> clazz = jobType.getJobParameters();
            if ( clazz == null )
            {
                propertyMap.put( jobType.name(), null );
                continue;
            }

            for ( Field field : clazz.getDeclaredFields() )
            {
                if ( Arrays.stream( field.getAnnotations() )
                    .anyMatch( f -> f.annotationType().getSimpleName().equals( "Property" ) ) )
                {
                    Property property = new Property( Primitives.wrap( field.getType() ), null, null );
                    property.setName( field.getName() );
                    property.setFieldName( prettyPrint( field.getName() ) );

                    String relativeApiElements = jobType.getRelativeApiElements() != null ?
                        jobType.getRelativeApiElements().get( field.getName() ) : "";
                    if ( relativeApiElements != null && !relativeApiElements.equals( "" ) )
                    {
                        property.setRelativeApiEndpoint( relativeApiElements );
                    }

                    if ( Collection.class.isAssignableFrom( field.getType() ) )
                    {
                        property = new NodePropertyIntrospectorService()
                            .setPropertyIfCollection( property, field, clazz );
                    }

                    jobParameters.put( property.getName(), property );
                }
            }
            propertyMap.put( jobType.name(), jobParameters );
        }

        return propertyMap;
    }

    private String prettyPrint( String field )
    {
        List<String> fieldStrings = Arrays.stream( field.split( "(?=[A-Z])" ) ).map( String::toLowerCase )
            .collect( Collectors.toList() );

        fieldStrings
            .set( 0, fieldStrings.get( 0 ).substring( 0, 1 ).toUpperCase() + fieldStrings.get( 0 ).substring( 1 ) );

        return String.join( " ", fieldStrings );
    }
}
