package org.hisp.dhis.common;

import java.io.Serializable;

public interface UniqueObject extends Serializable
{
    long getId();

    String getUid();
}
