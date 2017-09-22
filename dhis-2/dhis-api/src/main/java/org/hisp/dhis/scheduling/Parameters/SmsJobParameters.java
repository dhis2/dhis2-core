package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobId;
import org.hisp.dhis.user.User;

import java.util.List;

/**
 * @author Henning Håkonsen
 */
public class SmsJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 9L;

    private String smsSubject;

    private String text;

    private User currentUser;

    private List<User> recipientsList;

    private String message;

    private JobId jobId;

    public SmsJobParameters()
    {}

    public SmsJobParameters( String smsSubject, String text, User currentUser, List<User> recipientsList,
        String message, JobId jobId )
    {
        this.smsSubject = smsSubject;
        this.text = text;
        this.currentUser = currentUser;
        this.recipientsList = recipientsList;
        this.message = message;
        this.jobId = jobId;
    }

    private I18n i18n;

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

    public User getCurrentUser()
    {
        return currentUser;
    }

    public void setCurrentUser( User currentUser )
    {
        this.currentUser = currentUser;
    }

    public List<User> getRecipientsList()
    {
        return recipientsList;
    }

    public void setRecipientsList( List<User> recipientsList )
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

    public JobId getJobId()
    {
        return null;
    }

    public void setJobId( JobId jobId )
    {
        this.jobId = jobId;
    }
}
