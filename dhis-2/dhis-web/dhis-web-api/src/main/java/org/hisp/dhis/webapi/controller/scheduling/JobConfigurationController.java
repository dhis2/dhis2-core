package org.hisp.dhis.webapi.controller.scheduling;

import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.schema.NodePropertyIntrospectorService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.descriptors.JobConfigurationSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Simple controller for API endpoints
 *
 * @author Henning Håkonsen
 */
@Controller
@RequestMapping( value = JobConfigurationSchemaDescriptor.API_ENDPOINT )
public class JobConfigurationController
    extends AbstractCrudController<JobConfiguration>
{
    @Autowired
    private FieldFilterService fieldFilterService;

    @RequestMapping( value = "/jobTypes", method = RequestMethod.GET, produces = { "application/json", "application/javascript" } )
    public @ResponseBody
    List<JobType> getJobTypes()
    {
        return Arrays.asList( JobType.values() );
    }

    @RequestMapping( value = "/jobTypesExtended", method = RequestMethod.GET, produces = { "application/json", "application/javascript" } )
    public @ResponseBody
    Map<String, Map<String, Property>> getJobParams()
    {
        Map<String, Map<String, Property>> propertyMap = Maps.newHashMap();

        for ( JobType jobType : JobType.values() )
        {
            Map<String, Property> jobParameters = Maps.newHashMap();

            Class clazz = jobType.getClazz();
            if ( clazz == null ) continue;

            for ( Field field : clazz.getDeclaredFields() )
            {
                Property property = null;

                if( Arrays.stream( field.getAnnotations() ).anyMatch( f -> f.annotationType().getSimpleName().equals( "Property" ) ) )
                {
                    property = new Property( Primitives.wrap( field.getType() ), null, null );
                    property.setName( field.getName() );
                    property.setFieldName( field.getName() );

                    String relativeApiElements = jobType.getRelativeApiElements() != null ? jobType.getRelativeApiElements().get( field.getName() ) : "";
                    if( relativeApiElements != null && !relativeApiElements.equals( "" ) ) property.setRelativeApiEndpoint( relativeApiElements );

                    if ( Collection.class.isAssignableFrom( field.getType() ) )
                    {
                        property = new NodePropertyIntrospectorService().setPropertyIfCollection( property, field, clazz );
                    }
                }

                if ( property != null )
                {
                    if ( property.isCollection() )
                    {
                        jobParameters.put( property.getCollectionName(), property );
                    }
                    else
                    {
                        jobParameters.put( property.getName(), property );
                    }
                }
            }
            propertyMap.put( jobType.name(), jobParameters );
        }

        return propertyMap;
    }
}
