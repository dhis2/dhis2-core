package org.hisp.dhis.sms.command;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.util.List;
import java.util.Set;

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.command.hibernate.SMSCommandStore;
import org.hisp.dhis.sms.parse.ParserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class DefaultSMSCommandService
    implements SMSCommandService
{
    @Autowired
    private SMSCommandStore smsCommandStore;

    @Override
    public List<SMSCommand> getSMSCommands()
    {
        return smsCommandStore.getAll();
    }

    public void setSmsCommandStore( SMSCommandStore smsCommandStore )
    {
        this.smsCommandStore = smsCommandStore;
    }

    @Override
    public void save( SMSCommand cmd )
    {
        smsCommandStore.save( cmd );
    }

    @Override
    public SMSCommand getSMSCommand( int id )
    {
        return smsCommandStore.get( id );
    }

    @Override
    public SMSCommand getSMSCommand( String name )
    {
        return smsCommandStore.getByName( name );
    }

    @Override
    public void addSmsCodes( Set<SMSCode> codes, int commandId )
    {
        SMSCommand command = smsCommandStore.get( commandId );

        if ( command != null )
        {
            command.getCodes().addAll( codes);

            smsCommandStore.update( command );
        }
    }

    @Override
    public void delete( SMSCommand cmd )
    {
        smsCommandStore.delete( cmd );
    }

    @Override
    public List<SMSCommand> getJ2MESMSCommands()
    {
        return smsCommandStore.getJ2MESMSCommands();
    }

    @Override
    public SMSCommand getSMSCommand( String commandName, ParserType parserType )
    {
        return smsCommandStore.getSMSCommand( commandName, parserType );
    }

    @Override
    public void addSpecialCharacterSet( Set<SMSSpecialCharacter> specialCharacters, int commandId )
    {
        SMSCommand command = smsCommandStore.get( commandId );

        if ( command != null )
        {
            command.getSpecialCharacters().addAll( specialCharacters );

            smsCommandStore.update( command );
        }
    }

    @Override
    public void deleteCodeSet( Set<SMSCode> codes, int commandId )
    {
        SMSCommand command = smsCommandStore.get( commandId );

        command.getCodes().removeAll( codes );

        smsCommandStore.update( command );
    }

    @Override
    public int countDataSetSmsCommands( DataSet dataSet )
    {
        return smsCommandStore.countDataSetSmsCommands( dataSet );
    }

    @Override
    public void deleteSpecialCharacterSet( Set<SMSSpecialCharacter> specialCharacters, int commandId )
    {
        SMSCommand command = smsCommandStore.get( commandId );

        command.getSpecialCharacters().removeAll( specialCharacters );

        smsCommandStore.update( command );
    }
}
