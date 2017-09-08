package org.hisp.dhis.scheduling.Parameters;

import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;

import java.util.Map;

/**
 * @author Henning Håkonsen
 */
public class DataStatisticsJobParameters
    implements JobParameters
{
    public DataStatisticsJobParameters()
    {}

    @Override
    public ErrorReport validate( Map<CronFieldName, CronField> cronFieldNameCronFieldMap )
    {
        return null;
    }
}
