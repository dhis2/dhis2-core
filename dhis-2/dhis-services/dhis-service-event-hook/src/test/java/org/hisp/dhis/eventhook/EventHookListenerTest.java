/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.eventhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.eventhook.targets.ConsoleTarget;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.AuthenticationService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

class EventHookListenerTest {

  private final ObjectMapper objectMapper = JacksonObjectMapperConfig.staticJsonMapper();
  private final AuthenticationService mockAuthenticationService =
      new AuthenticationService() {
        @Override
        public void obtainAuthentication(String userId) {}

        @Override
        public void obtainAuthentication(UserDetails userDetails) {}

        @Override
        public void obtainSystemAuthentication() {}

        @Override
        public void clearAuthentication() {}
      };
  private final IdentifiableObject mockIdentifiableObject =
      new IdentifiableObject() {

        @Override
        public String getCode() {
          return "";
        }

        @Override
        public String getName() {
          return "";
        }

        @Override
        public String getDisplayName() {
          return "";
        }

        @Override
        public Date getCreated() {
          throw new UnsupportedOperationException();
        }

        @Override
        public Date getLastUpdated() {
          throw new UnsupportedOperationException();
        }

        @Override
        public User getLastUpdatedBy() {
          throw new UnsupportedOperationException();
        }

        @Override
        public AttributeValues getAttributeValues() {
          throw new UnsupportedOperationException();
        }

        @Override
        public void setAttributeValues(AttributeValues attributeValues) {}

        @Override
        public void addAttributeValue(String attributeUid, String value) {}

        @Override
        public void removeAttributeValue(String attributeId) {}

        @Override
        public Set<Translation> getTranslations() {
          return Set.of();
        }

        @Override
        public void setAccess(Access access) {}

        @Override
        public User getCreatedBy() {
          throw new UnsupportedOperationException();
        }

        @Override
        public User getUser() {
          throw new UnsupportedOperationException();
        }

        @Override
        public void setCreatedBy(User createdBy) {}

        @Override
        public void setUser(User user) {}

        @Override
        public Access getAccess() {
          throw new UnsupportedOperationException();
        }

        @Override
        public Sharing getSharing() {
          throw new UnsupportedOperationException();
        }

        @Override
        public void setSharing(Sharing sharing) {}

        @Override
        public String getPropertyValue(IdScheme idScheme) {
          return "";
        }

        @Override
        public String getDisplayPropertyValue(IdScheme idScheme) {
          return "";
        }

        @Override
        public void setId(long id) {}

        @Override
        public void setUid(String uid) {}

        @Override
        public void setName(String name) {}

        @Override
        public void setCode(String code) {}

        @Override
        public void setOwner(String owner) {}

        @Override
        public void setTranslations(Set<Translation> translations) {}

        @Override
        public void setLastUpdated(Date lastUpdated) {}

        @Override
        public void setLastUpdatedBy(User user) {}

        @Override
        public void setCreated(Date created) {}

        @Override
        public String getHref() {
          return "";
        }

        @Override
        public void setHref(String link) {}

        @Override
        public long getId() {
          return 1;
        }

        @Override
        public String getUid() {
          return "";
        }
      };
  private final FieldFilterService fieldFilterService = mock(FieldFilterService.class);

  @Test
  void testOnEventEmitsMetadata() throws NotFoundException, JsonProcessingException {
    User user = new User();
    user.setUid(CodeGenerator.generateUid());
    EventHookListener eventHookListener =
        new EventHookListener(
            null, objectMapper, fieldFilterService, null, mockAuthenticationService);

    EventHook eventHook = createMockEventHook(user);
    eventHookListener.getEventHookContext().setEventHooks(List.of(eventHook));
    CountDownLatch countDownLatch = new CountDownLatch(1);
    eventHookListener
        .getEventHookContext()
        .setTargets(
            Map.of(
                eventHook.getUid(), List.of((eh, event, payload) -> countDownLatch.countDown())));

    eventHookListener.onEvent(EventUtils.metadataCreate(mockIdentifiableObject));

    assertEquals(0, countDownLatch.getCount());
  }

  private EventHook createMockEventHook(User user) {
    Source source = new Source();
    source.setPath("metadata..");

    EventHook eventHook = new EventHook();
    eventHook.setUser(user);
    eventHook.setTargets(List.of(new ConsoleTarget()));
    eventHook.setUid(CodeGenerator.generateUid());
    eventHook.setSource(source);

    return eventHook;
  }
}
