package org.hisp.dhis.analytics.util.optimizer.cte;

import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;

public record CteInput(SubSelect subSelect, FoundSubSelect found, String eventTable) {
}
