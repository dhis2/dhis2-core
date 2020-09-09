package org.hisp.dhis.dxf2.events.importer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Component
public class ProcessingManager
{

    @Qualifier( "eventsPreInsertProcessorFactory" )
    private final EventProcessing preInsertProcessorFactory;

    @Qualifier( "eventsPostInsertProcessorFactory" )
    private final EventProcessing postInsertProcessorFactory;

    @Qualifier( "eventsPreUpdateProcessorFactory" )
    private final EventProcessing preUpdateProcessorFactory;

    @Qualifier( "eventsPostUpdateProcessorFactory" )
    private final EventProcessing postUpdateProcessorFactory;

    @Qualifier( "eventsPreDeleteProcessorFactory" )
    private final EventProcessing preDeleteProcessorFactory;

    @Qualifier( "eventsPostDeleteProcessorFactory" )
    private final EventProcessing postDeleteProcessorFactory;

}
