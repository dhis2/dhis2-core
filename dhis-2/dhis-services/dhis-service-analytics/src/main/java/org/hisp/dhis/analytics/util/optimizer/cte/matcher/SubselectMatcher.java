package org.hisp.dhis.analytics.util.optimizer.cte.matcher;

import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;

import java.util.Optional;

public interface SubselectMatcher {
    Optional<FoundSubSelect> match(SubSelect subSelect);
}
