package org.hisp.dhis.schema.descriptors;

import com.google.common.collect.Lists;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaDescriptor;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.security.AuthorityType;

/**
 * @author Zubair Asghar
 */
public class ProgramNotificationInstanceSchemaDescriptor
        implements SchemaDescriptor
{
    public static final String SINGULAR = "programNotificationInstance";

    public static final String PLURAL = "programNotificationInstances";

    public static final String API_ENDPOINT = "/" + PLURAL;

    @Override
    public Schema getSchema()
    {
        Schema schema = new Schema( ProgramNotificationInstance.class, SINGULAR, PLURAL );
        schema.setRelativeApiEndpoint( API_ENDPOINT );
        schema.setOrder( 1508 );

        schema.add( new Authority( AuthorityType.CREATE,
                Lists.newArrayList( "F_PROGRAM_PUBLIC_ADD", "F_PROGRAM_PRIVATE_ADD" ) ) );
        schema.add( new Authority( AuthorityType.DELETE, Lists.newArrayList( "F_PROGRAM_DELETE" ) ) );

        return schema;
    }
}