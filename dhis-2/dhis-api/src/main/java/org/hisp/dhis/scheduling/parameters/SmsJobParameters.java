package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

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
    private String text;

    @Property
    private List<String> recipientsList;

    @Property
    private String message;

    private I18n i18n;

    public SmsJobParameters()
    {
    }

    public SmsJobParameters( String smsSubject, String text, List<String> recipientsList,
        String message )
    {
        this.smsSubject = smsSubject;
        this.text = text;
        this.recipientsList = recipientsList;
        this.message = message;
    }

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    public I18n getI18n()
    {
        return i18n;
    }

    public String getSmsSubject()
    {
        return smsSubject;
    }

    public void setSmsSubject( String smsSubject )
    {
        this.smsSubject = smsSubject;
    }

    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        this.text = text;
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
}
