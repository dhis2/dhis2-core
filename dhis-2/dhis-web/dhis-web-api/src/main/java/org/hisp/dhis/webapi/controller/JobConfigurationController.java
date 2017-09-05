package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.descriptors.JobConfigurationSchemaDescriptor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Henning Håkonsen
 */
@Controller
@RequestMapping( value = JobConfigurationSchemaDescriptor.API_ENDPOINT )
public class JobConfigurationController
    extends AbstractCrudController<JobConfiguration>
{

    /*ObjectMapper objectMapper = new ObjectMapper(  );

    protected JobConfiguration deserializeJsonEntity( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        JobConfiguration deserializedConfig = objectMapper.readValue(request.getInputStream(), JobConfiguration.class);
        String result = objectMapper.writeValueAsString(request.getInputStream());
        System.out.println("Deserialized config: " + result + ", object: " + deserializedConfig);

        return deserializedConfig;
    }*/
}
