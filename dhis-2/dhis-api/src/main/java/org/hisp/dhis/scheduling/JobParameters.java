package org.hisp.dhis.scheduling;

import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import org.hisp.dhis.feedback.ErrorReport;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Henning Håkonsen
 */
public interface JobParameters
    extends Serializable
{
    ErrorReport validate( Map<CronFieldName, CronField> cronFieldNameCronFieldMap );
}
