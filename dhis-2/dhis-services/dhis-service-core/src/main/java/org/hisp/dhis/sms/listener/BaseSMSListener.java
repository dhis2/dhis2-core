package org.hisp.dhis.sms.listener;

import java.util.function.Consumer;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableMap;

@Transactional
public abstract class BaseSMSListener implements IncomingSmsListener {
    private static final Log log = LogFactory.getLog( BaseSMSListener.class );

    private static final String NO_SMS_CONFIG = "No sms configuration found";

    protected static final int INFO = 1;
    protected static final int WARNING = 2;
    protected static final int ERROR = 3;

    private static final ImmutableMap<Integer, Consumer<String>> LOGGER = new ImmutableMap.Builder<Integer, Consumer<String>>()
        .put( 1, log::info )
        .put( 2, log::warn )
        .put( 3, log::error )
        .build();
    
    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;
    
    @Autowired
    private IncomingSmsService incomingSmsService;
    
    protected void sendFeedback( String message, String sender, int logType )
    {
        LOGGER.getOrDefault( logType, log::info ).accept( message );

        if( smsSender.isConfigured() )
        {
            smsSender.sendMessage( null, message, sender );
            return;
        }

        LOGGER.getOrDefault( WARNING, log::info ).accept(  NO_SMS_CONFIG );
    }
    
    protected void update( IncomingSms sms, SmsMessageStatus status, boolean parsed )
    {
        sms.setStatus( status );
        sms.setParsed( parsed );

        incomingSmsService.update( sms );
    }

}
