package org.hisp.dhis.sms.listener;

/*
 * Copyright (c) 2004-2016, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class ProgramStageDataEntrySMSListener
    implements IncomingSmsListener
{
    private static final String defaultPattern = "([a-zA-Z]+)\\s*(\\d+)";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SMSCommandService smsCommandService;

    @Autowired
    private UserService userService;

    // -------------------------------------------------------------------------
    // IncomingSmsListener implementation
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public boolean accept( IncomingSms sms )
    {
        return smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ),
            ParserType.PROGRAM_STAGE_DATAENTRY_PARSER ) != null;
    }

    @Transactional
    @Override
    public void receive( IncomingSms sms )
    {
        String message = sms.getText();
        SMSCommand smsCommand = smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ),
            ParserType.PROGRAM_STAGE_DATAENTRY_PARSER );

        this.parse( message, smsCommand );

        SmsUtils.lookForDate( message );

        String senderPhoneNumber = StringUtils.replace( sms.getOriginator(), "+", "" );
        Collection<OrganisationUnit> orgUnits = SmsUtils.getOrganisationUnitsByPhoneNumber( senderPhoneNumber,
            userService.getUsersByPhoneNumber( senderPhoneNumber ) );

        if ( orgUnits == null || orgUnits.size() == 0 )
        {
            if ( StringUtils.isEmpty( smsCommand.getNoUserMessage() ) )
            {
                throw new SMSParserException( SMSCommand.NO_USER_MESSAGE );
            }
            else
            {
                throw new SMSParserException( smsCommand.getNoUserMessage() );
            }
        }
    }

    private Map<String, String> parse( String message, SMSCommand smsCommand )
    {
        HashMap<String, String> output = new HashMap<>();
        Pattern pattern = Pattern.compile( defaultPattern );
        
        if ( !StringUtils.isBlank( smsCommand.getSeparator() ) )
        {
            String x = "(\\w+)\\s*\\" + smsCommand.getSeparator().trim() + "\\s*([\\w ]+)\\s*(\\"
                + smsCommand.getSeparator().trim() + "|$)*\\s*";
            pattern = Pattern.compile( x );
        }
        
        Matcher matcher = pattern.matcher( message );
        
        while ( matcher.find() )
        {
            String key = matcher.group( 1 );
            String value = matcher.group( 2 );

            if ( !StringUtils.isEmpty( key ) && !StringUtils.isEmpty( value ) )
            {
                output.put( key.toUpperCase(), value );
            }
        }

        return output;
    }
}
