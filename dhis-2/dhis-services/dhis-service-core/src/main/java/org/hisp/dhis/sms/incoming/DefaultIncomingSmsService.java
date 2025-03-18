/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.sms.incoming;

import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service("org.hisp.dhis.sms.incoming.IncomingSmsService")
public class DefaultIncomingSmsService implements IncomingSmsService {
  private static final String DEFAULT_GATEWAY = "default";

  private final IncomingSmsStore incomingSmsStore;

  @Override
  @Transactional(readOnly = true)
  public List<IncomingSms> getAll() {
    return incomingSmsStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<IncomingSms> getAll(Integer min, Integer max, boolean hasPagination) {
    return incomingSmsStore.getAll(min, max, hasPagination);
  }

  @Override
  @Transactional
  public String save(IncomingSms sms) {
    if (sms.getReceivedDate() != null) {
      sms.setSentDate(sms.getReceivedDate());
    } else {
      sms.setSentDate(new Date());
    }
    sms.setReceivedDate(new Date());

    sms.setGatewayId(StringUtils.defaultIfBlank(sms.getGatewayId(), DEFAULT_GATEWAY));

    incomingSmsStore.save(sms);
    return sms.getUid();
  }

  @Transactional
  public void delete(long id) {
    IncomingSms incomingSms = incomingSmsStore.get(id);

    if (incomingSms != null) {
      incomingSmsStore.delete(incomingSms);
    }
  }

  @Override
  @Transactional
  public void delete(String uid) {
    IncomingSms incomingSms = incomingSmsStore.getByUid(uid);

    if (incomingSms != null) {
      incomingSmsStore.delete(incomingSms);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public IncomingSms get(long id) {
    return incomingSmsStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public IncomingSms get(String id) {
    return incomingSmsStore.getByUid(id);
  }

  @Override
  @Transactional
  public void update(IncomingSms incomingSms) {
    incomingSmsStore.update(incomingSms);
  }

  @Override
  @Transactional(readOnly = true)
  public List<IncomingSms> getSmsByStatus(SmsMessageStatus status, String originator) {
    return incomingSmsStore.getSmsByStatus(status, originator);
  }

  @Override
  @Transactional(readOnly = true)
  public List<IncomingSms> getSmsByStatus(
      SmsMessageStatus status, String keyword, Integer min, Integer max, boolean hasPagination) {
    return incomingSmsStore.getSmsByStatus(status, keyword, min, max, hasPagination);
  }

  @Override
  @Transactional(readOnly = true)
  public List<IncomingSms> getAllUnparsedMessages() {
    return incomingSmsStore.getAllUnparsedMessages();
  }
}
