package org.hisp.dhis.scheduling.Parameters;

import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;

import java.util.Map;

/**
 * Created by henninghakonsen on 04/09/2017.
 * Project: dhis-2.
 */
public class TestJobParameters
    implements JobParameters
{
    public TestJobParameters()
    {}

    private String message;

    @JacksonXmlProperty
    @JsonProperty
    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    @Override
    public ErrorReport validate( Map<CronFieldName, CronField> cronFieldNameCronFieldMap )
    {
        cronFieldNameCronFieldMap.forEach( (k, v) -> System.out.println(k + ", " + v) );

        return null;
    }
}
