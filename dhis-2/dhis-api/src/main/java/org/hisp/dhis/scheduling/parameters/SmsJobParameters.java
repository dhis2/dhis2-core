package org.hisp.dhis.scheduling.parameters;

import com.fasterxml.jackson.databind.JsonNode;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

import java.io.IOException;
import java.util.List;

/**
 * @author Henning Håkonsen
 */
public class SmsJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = -6116489359345047961L;

    @Property
    private String smsSubject;

    @Property
    private List<String> recipientsList;

    @Property
    private String message;

    public SmsJobParameters()
    {
    }

    public SmsJobParameters( String smsSubject, String message, List<String> recipientsList )
    {
        this.smsSubject = smsSubject;
        this.recipientsList = recipientsList;
        this.message = message;
    }

    public String getSmsSubject()
    {
        return smsSubject;
    }

    public void setSmsSubject( String smsSubject )
    {
        this.smsSubject = smsSubject;
    }

    public List<String> getRecipientsList()
    {
        return recipientsList;
    }

    public void setRecipientsList( List<String> recipientsList )
    {
        this.recipientsList = recipientsList;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    @Override
    public JobParameters mapParameters( JsonNode parameters )
        throws IOException
    {
        return null;
    }
}
