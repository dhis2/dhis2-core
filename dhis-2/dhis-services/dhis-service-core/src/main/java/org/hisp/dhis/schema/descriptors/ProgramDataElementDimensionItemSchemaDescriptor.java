package org.hisp.dhis.schema.descriptors;

import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaDescriptor;

public class ProgramDataElementDimensionItemSchemaDescriptor implements SchemaDescriptor
{
    public static final String SINGULAR = "programDataElement";

    public static final String PLURAL = "programDataElements";

    public static final String API_ENDPOINT = "/" + PLURAL;

    @Override
    public Schema getSchema()
    {
        Schema schema = new Schema( ProgramDataElementDimensionItem.class, SINGULAR, PLURAL );
        schema.setRelativeApiEndpoint( API_ENDPOINT );
        schema.setMetadata( false );

        return schema;
    }
}
