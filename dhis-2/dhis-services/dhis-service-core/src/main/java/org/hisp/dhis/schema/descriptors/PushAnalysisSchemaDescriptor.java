package org.hisp.dhis.schema.descriptors;

import com.google.common.collect.Lists;
import org.hisp.dhis.pushanalysis.PushAnalysis;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaDescriptor;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.security.AuthorityType;

/**
 * @author Stian Sandvold
 */

public class PushAnalysisSchemaDescriptor
    implements SchemaDescriptor
{
    public static final String SINGULAR = "pushAnalysis";

    public static final String PLURAL = "pushAnalysis";

    public static final String API_ENDPOINT = "/" + PLURAL;

    @Override
    public Schema getSchema()
    {
        Schema schema = new Schema( PushAnalysis.class, SINGULAR, PLURAL );
        schema.setRelativeApiEndpoint( API_ENDPOINT );
        schema.setOrder( 1000 );

        schema.getAuthorities()
            .add( new Authority( AuthorityType.CREATE, Lists.newArrayList( "F_PUSHANALYSIS_ADD" ) ) );

        return schema;
    }
}
