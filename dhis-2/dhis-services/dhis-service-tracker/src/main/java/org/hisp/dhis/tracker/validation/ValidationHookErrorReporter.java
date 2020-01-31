package org.hisp.dhis.tracker.validation;

import org.hisp.dhis.tracker.TrackerErrorCode;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.validation.ValidationFailFastException;

import java.util.ArrayList;
import java.util.List;

public class ValidationHookErrorReporter
{
    private final List<TrackerErrorReport> reportList;

    private final boolean isFailFast;

    private Class<?> mainKlass;

    public ValidationHookErrorReporter( TrackerBundle bundle, Class<?> mainKlass )
    {
        this.reportList = new ArrayList<>();
        this.isFailFast = bundle.getValidationMode() == ValidationMode.FAIL_FAST;
        this.mainKlass = mainKlass;
    }

    public ValidationHookErrorReporter( boolean isFailFast, Class<?> mainKlass )
    {
        this.reportList = new ArrayList<>();
        this.isFailFast = isFailFast;
        this.mainKlass = mainKlass;
    }

    public List<TrackerErrorReport> getReportList()
    {
        return reportList;
    }

    public boolean isFailFast()
    {
        return isFailFast;
    }

    public Class<?> getMainKlass()
    {
        return mainKlass;
    }

    public void setMainKlass( Class<?> mainKlass )
    {
        this.mainKlass = mainKlass;
    }

    public TrackerErrorReport raiseError( TrackerErrorCode errorCode, Object... args )
    {
        TrackerErrorReport r = new TrackerErrorReport( mainKlass, errorCode, args );
        getReportList().add( r );
        if ( isFailFast() )
        {
            throw new ValidationFailFastException(getReportList());
        }
        return r;
    }
}
