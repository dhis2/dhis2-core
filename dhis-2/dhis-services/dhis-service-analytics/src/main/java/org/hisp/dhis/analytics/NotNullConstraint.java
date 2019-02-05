package org.hisp.dhis.analytics;

public enum NotNullConstraint
{
    NULL, NOT_NULL;

    public boolean isNotNull()
    {
        return this == NOT_NULL;
    }
}
