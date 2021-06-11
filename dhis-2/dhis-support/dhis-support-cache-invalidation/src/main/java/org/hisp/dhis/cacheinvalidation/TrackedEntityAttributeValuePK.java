package org.hisp.dhis.cacheinvalidation;

import java.io.Serializable;
import java.util.Objects;

import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class TrackedEntityAttributeValuePK implements Serializable
{
    private Long entityInstance;

    private Long attribute;

    public TrackedEntityAttributeValuePK()
    {

    }

    public TrackedEntityAttributeValuePK( Long entityInstance, Long attribute )
    {
        this.entityInstance = entityInstance;
        this.attribute = attribute;
    }

    public Long getEntityInstance()
    {
        return entityInstance;
    }

    public void setEntityInstance( Long entityInstance )
    {
        this.entityInstance = entityInstance;
    }

    public Long getAttribute()
    {
        return attribute;
    }

    public void setAttribute( Long attribute )
    {
        this.attribute = attribute;
    }
}
