package org.hisp.dhis.scheduling;

import org.hisp.dhis.common.CodeGenerator;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.hisp.dhis.scheduling.JobProgress.FaultTolerance.SKIP_ITEM;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlledJobProgressTest
{
    private final JobConfiguration config = createJobConfig();

    private final JobProgress progress = new ControlledJobProgress( config );

    private final Consumer<Integer> alwaysFail =  item -> {
        throw new IllegalArgumentException("failing");
    };

    @Test
    void testSkipItem_FirstItemFails() {
        progress.startingStage( "test", 3, SKIP_ITEM );
        boolean stageSuccessful = progress.runStage( Stream.of(1,2,3), String::valueOf, alwaysFail,
            (successes, failures) -> {
                assertEquals( 0, successes );
                assertEquals( 3, failures );
                return null;
            });

        assertTrue( stageSuccessful, "the stage should be considered successful as we skip failing items" );
        assertDoesNotThrow( () -> progress.startingStage( "another" ),
            "execution should be possible to continue with next stage");
    }

    private static JobConfiguration createJobConfig()
    {
        JobConfiguration config = new JobConfiguration();
        config.setJobType( JobType.PREDICTOR );
        config.setUid( CodeGenerator.generateUid() );
        return config;
    }
}
