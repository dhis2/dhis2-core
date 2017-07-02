package org.hisp.dhis.schema.descriptors;

import com.google.common.collect.Lists;
import org.hisp.dhis.dataset.notification.DataSetNotificationTemplate;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaDescriptor;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.security.AuthorityType;

/**
 * Created by zubair on 02.07.17.
 */
public class DataSetNotificationTemplateSchemaDescriptor
    implements SchemaDescriptor
{
    public static final String SINGULAR = "dataSetNotificationTemplate";

    public static final String PLURAL = "dataSetNotificationTemplates";

    public static final String API_ENDPOINT = "/" + PLURAL;

    @Override
    public Schema getSchema()
    {
        Schema schema = new Schema( DataSetNotificationTemplate.class, SINGULAR, PLURAL );
        schema.setRelativeApiEndpoint( API_ENDPOINT );
        schema.setOrder( 1311 );

        schema.getAuthorities().add( new Authority( AuthorityType.CREATE_PUBLIC, Lists.newArrayList( "F_DATASET_PUBLIC_ADD" ) ) );
        schema.getAuthorities().add( new Authority( AuthorityType.CREATE_PRIVATE, Lists.newArrayList( "F_DATASET_PRIVATE_ADD" ) ) );
        schema.getAuthorities().add( new Authority( AuthorityType.DELETE, Lists.newArrayList( "F_DATASET_DELETE" ) ) );

        return schema;
    }
}
