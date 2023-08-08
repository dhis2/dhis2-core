/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.sms.command;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.command.hibernate.SMSCommandStore;
import org.hisp.dhis.sms.parse.ParserType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service("org.hisp.dhis.sms.command.SMSCommandService")
public class DefaultSMSCommandService implements SMSCommandService {
  private final SMSCommandStore smsCommandStore;

  @Override
  @Transactional(readOnly = true)
  public List<SMSCommand> getSMSCommands() {
    return smsCommandStore.getAll();
  }

  @Override
  @Transactional
  public void save(SMSCommand cmd) {
    smsCommandStore.save(cmd);
  }

  @Override
  @Transactional(readOnly = true)
  public SMSCommand getSMSCommand(long id) {
    return smsCommandStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public SMSCommand getSMSCommand(String name) {
    return smsCommandStore.getByName(name);
  }

  @Override
  @Transactional
  public void addSmsCodes(Set<SMSCode> codes, long commandId) {
    SMSCommand command = smsCommandStore.get(commandId);

    if (command != null) {
      command.getCodes().addAll(codes);

      smsCommandStore.update(command);
    }
  }

  @Override
  @Transactional
  public void delete(SMSCommand cmd) {
    smsCommandStore.delete(cmd);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SMSCommand> getJ2MESMSCommands() {
    return smsCommandStore.getJ2MESMSCommands();
  }

  @Override
  @Transactional(readOnly = true)
  public SMSCommand getSMSCommand(String commandName, ParserType parserType) {
    return smsCommandStore.getSMSCommand(commandName, parserType);
  }

  @Override
  @Transactional
  public void addSpecialCharacterSet(Set<SMSSpecialCharacter> specialCharacters, long commandId) {
    SMSCommand command = smsCommandStore.get(commandId);

    if (command != null) {
      command.getSpecialCharacters().addAll(specialCharacters);

      smsCommandStore.update(command);
    }
  }

  @Override
  @Transactional
  public void deleteCodeSet(Set<SMSCode> codes, long commandId) {
    SMSCommand command = smsCommandStore.get(commandId);
    if (command != null) {
      command.getCodes().removeAll(codes);

      smsCommandStore.update(command);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public int countDataSetSmsCommands(DataSet dataSet) {
    return smsCommandStore.countDataSetSmsCommands(dataSet);
  }

  @Override
  @Transactional
  public void deleteSpecialCharacterSet(
      Set<SMSSpecialCharacter> specialCharacters, long commandId) {
    SMSCommand command = smsCommandStore.get(commandId);
    if (command != null) {
      command.getSpecialCharacters().removeAll(specialCharacters);

      smsCommandStore.update(command);
    }
  }
}
