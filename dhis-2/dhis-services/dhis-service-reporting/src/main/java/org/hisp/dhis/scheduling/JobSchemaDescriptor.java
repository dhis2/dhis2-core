package org.hisp.dhis.scheduling;

import com.google.common.collect.Lists;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaDescriptor;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.security.AuthorityType;

/**
 * Created by henninghakonsen on 25/08/2017.
 * Project: dhis-2.
 */
public class JobSchemaDescriptor
    implements SchemaDescriptor
{
    public static final String SINGULAR = "job";

    public static final String PLURAL = "jobs";

    public static final String API_ENDPOINT = "/" + PLURAL;

    @Override
    public Schema getSchema()
    {
        // TODO verify
        Schema schema = new Schema( Constant.class, SINGULAR, PLURAL );
        schema.setRelativeApiEndpoint( API_ENDPOINT );
        schema.setOrder( 1030 );

        schema.getAuthorities().add( new Authority( AuthorityType.CREATE, Lists.newArrayList( "F_JOB_ADD" ) ) );
        schema.getAuthorities().add( new Authority( AuthorityType.DELETE, Lists.newArrayList( "F_JOB_DELETE" ) ) );

        return schema;
    }
}
