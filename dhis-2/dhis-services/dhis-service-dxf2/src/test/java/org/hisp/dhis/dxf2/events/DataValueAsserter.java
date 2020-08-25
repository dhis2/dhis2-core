package org.hisp.dhis.dxf2.events;

import lombok.Builder;
import lombok.Getter;

/**
 * @author Luciano Fiandesio
 */
@Builder
@Getter
public class DataValueAsserter
{
    private String value;

    private String dataElement;
}