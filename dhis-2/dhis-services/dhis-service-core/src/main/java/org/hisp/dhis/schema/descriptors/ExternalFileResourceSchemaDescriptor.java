package org.hisp.dhis.schema.descriptors;

import com.google.common.collect.Lists;
import org.hisp.dhis.fileresource.ExternalFileResource;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaDescriptor;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.security.AuthorityType;

/**
 * @author Stian Sandvold
 */

public class ExternalFileResourceSchemaDescriptor
    implements SchemaDescriptor
{
    public static final String SINGULAR = "externalFileResource";

    public static final String PLURAL = "externalFileResources";

    public static final String API_ENDPOINT = "/" + PLURAL;

    @Override
    public Schema getSchema()
    {
        Schema schema = new Schema( ExternalFileResource.class, SINGULAR, PLURAL );
        schema.setRelativeApiEndpoint( API_ENDPOINT );
        schema.setOrder( 1000 );

        schema.getAuthorities()
            .add( new Authority( AuthorityType.CREATE, Lists.newArrayList( "F_EXTERNALFILERESOURCE_ADD" ) ) );

        return schema;
    }
}
