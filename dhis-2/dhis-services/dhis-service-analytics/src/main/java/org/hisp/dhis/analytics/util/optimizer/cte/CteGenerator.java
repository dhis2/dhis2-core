package org.hisp.dhis.analytics.util.optimizer.cte;

import org.hisp.dhis.analytics.util.optimizer.cte.data.GeneratedCte;

@FunctionalInterface
public interface CteGenerator {
    GeneratedCte generate(CteInput input);
}
