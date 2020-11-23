package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.common.IdentifiableObject;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeforeMapping;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
public class DebugMapper
{
    private long start = 0;

    @BeforeMapping
    public void before( Object anySource )
    {
        if ( anySource != null )
        {
            String uid = "";
            if ( anySource instanceof IdentifiableObject )
            {
                uid = ((IdentifiableObject) anySource).getUid();
            }
            log.debug( anySource.getClass().getSimpleName() + " -> " + uid );
        }
        else
        {
            log.debug( "unknown source class" );
        }
        start = System.currentTimeMillis();
    }

    @AfterMapping
    public void after()
    {
        log.debug( "ms. : " + (System.currentTimeMillis() - start) );
    }
}
