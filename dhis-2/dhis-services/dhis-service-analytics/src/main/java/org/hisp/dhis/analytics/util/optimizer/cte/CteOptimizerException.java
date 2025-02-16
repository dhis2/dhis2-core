package org.hisp.dhis.analytics.util.optimizer.cte;

public class CteOptimizerException extends RuntimeException {
    public CteOptimizerException(String message, Throwable cause) {
        super(message, cause);
    }

    public CteOptimizerException(String message) {
        super(message);
    }
}
