package org.hisp.dhis.dxf2.events.importer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Component
public class ProcessingManager
{

    @NonNull
    @Qualifier( "eventsPreInsertProcessorFactory" )
    private final EventProcessing preInsertProcessorFactory;

    @NonNull
    @Qualifier( "eventsPostInsertProcessorFactory" )
    private final EventProcessing postInsertProcessorFactory;

    @NonNull
    @Qualifier( "eventsPreUpdateProcessorFactory" )
    private final EventProcessing preUpdateProcessorFactory;

    @NonNull
    @Qualifier( "eventsPostUpdateProcessorFactory" )
    private final EventProcessing postUpdateProcessorFactory;

    @NonNull
    @Qualifier( "eventsPreDeleteProcessorFactory" )
    private final EventProcessing preDeleteProcessorFactory;

    @NonNull
    @Qualifier( "eventsPostDeleteProcessorFactory" )
    private final EventProcessing postDeleteProcessorFactory;

}
