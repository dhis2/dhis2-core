package org.hisp.dhis.hibernate;
import org.hisp.dhis.interpretation.Mention;

public class MentionUserType extends JsonListUserType
{
    @Override
    public Class<?> returnedClass()
    {
        return Mention.class;
    }
}
