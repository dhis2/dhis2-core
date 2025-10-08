package org.hisp.dhis.scheduling.parameters;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;

import java.util.Optional;

/**
 * @author Zubair Asghar
 */
@Getter
@Setter
public class SingleEventDataSynchronizationJobParameters implements JobParameters {
    static final int PAGE_SIZE_MIN = 5;

    static final int PAGE_SIZE_MAX = 200;

    @JsonProperty
    private int pageSize = 60;

    @Override
    public Optional<ErrorReport> validate() {
        if (pageSize < PAGE_SIZE_MIN || pageSize > PAGE_SIZE_MAX) {
            return Optional.of(
                    new ErrorReport(
                            this.getClass(),
                            ErrorCode.E4008,
                            "pageSize",
                            PAGE_SIZE_MIN,
                            PAGE_SIZE_MAX,
                            pageSize));
        }

        return Optional.empty();
    }
}
