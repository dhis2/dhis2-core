package org.hisp.dhis.tracker.validation.hooks;

import org.hisp.dhis.tracker.report.ValidationErrorReporter;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@FunctionalInterface
public interface ValidationFunction<TrackerDto>
{
    void validateTrackerDto( TrackerDto obj, ValidationErrorReporter reportFork );
}
