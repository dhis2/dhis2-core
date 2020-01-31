package org.hisp.dhis.tracker.validation;

import org.hisp.dhis.tracker.report.TrackerErrorReport;

import java.util.List;

public class ValidationFailFastException
    extends RuntimeException
{
    List<TrackerErrorReport> errorReportRef;

    public ValidationFailFastException( List<TrackerErrorReport> errorReportRef )
    {
        this.errorReportRef = errorReportRef;
    }

    public List<TrackerErrorReport> getErrors()
    {
        return errorReportRef;
    }
}
