package org.hisp.dhis.scheduling.Parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.TaskId;

/**
 * @author Henning Håkonsen
 */
public class TestJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 10L;

    private TaskId taskId;
    private String message;

    public TestJobParameters()
    {}

    public TestJobParameters( TaskId taskId )
    {
        this.taskId = taskId;
    }

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

    public TaskId getTaskId()
    {
        return taskId;
    }

    public void setTaskId( TaskId taskId )
    {
        this.taskId = taskId;
    }
}
