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
import java.util.function.Function;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.eventhook.targets.ConsoleTarget;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.AuthenticationService;
import org.hisp.dhis.user.CurrentUserGroupInfo;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

public class EventHookListenerTest {

  private static class MockAclService implements AclService {

    private final String canReadUserUid;

    public MockAclService(String canReadUserUid) {
      this.canReadUserUid = canReadUserUid;
    }

    @Override
    public CurrentUserGroupInfo getCurrentUserGroupInfo(
        String userUid, Function<String, CurrentUserGroupInfo> cacheSupplier) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void invalidateCurrentUserGroupInfoCache() {}

    @Override
    public void invalidateCurrentUserGroupInfoCache(String userUid) {}

    @Override
    public boolean isSupported(IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean isClassShareable(Class<T> klass) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean isDataClassShareable(Class<T> klass) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShareable(String type) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShareable(IdentifiableObject o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canRead(UserDetails userDetails, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canRead(User user, IdentifiableObject object) {
      return user.getUid().equals(canReadUserUid);
    }

    @Override
    public <T extends IdentifiableObject> boolean canRead(
        UserDetails userDetails, T object, Class<? extends T> objType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canDataRead(UserDetails userDetails, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canDataRead(User user, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canDataOrMetadataRead(UserDetails userDetails, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canDataOrMetadataRead(User user, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canWrite(UserDetails userDetails, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canWrite(User user, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canDataWrite(UserDetails userDetails, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canDataWrite(User user, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canUpdate(UserDetails userDetails, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canUpdate(User user, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canDelete(UserDetails userDetails, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canDelete(User user, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canManage(UserDetails userDetails, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean canManage(User user, IdentifiableObject object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean canRead(UserDetails userDetails, Class<T> klass) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean canCreate(
        UserDetails userDetails, Class<T> klass) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean canCreate(User user, Class<T> klass) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean canMakePublic(UserDetails userDetails, T object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean canMakeClassPublic(
        UserDetails userDetails, Class<T> klass) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean canMakeClassPublic(User user, Class<T> klass) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean canMakePrivate(
        UserDetails userDetails, T object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean canMakeClassPrivate(
        UserDetails userDNonNuletails, Class<T> klass) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean canMakeClassPrivate(User user, Class<T> klass) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean defaultPrivate(Class<T> klass) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> boolean defaultPublic(T object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Class<? extends IdentifiableObject> classForType(String type) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> Access getAccess(T object, UserDetails userDetails) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> Access getAccess(T object, User user) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> Access getAccess(
        T object, UserDetails userDetails, Class<? extends T> objType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> void resetSharing(T object, UserDetails userDetails) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> void resetSharing(T object, User user) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> void clearSharing(T object, UserDetails userDetails) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> verifySharing(
        T object, UserDetails userDetails) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> verifySharing(T object, User user) {
      throw new UnsupportedOperationException();
    }
  }

  private final ObjectMapper objectMapper = JacksonObjectMapperConfig.staticJsonMapper();
  private final AuthenticationService mockAuthenticationService =
      new AuthenticationService() {
        @Override
        public void obtainAuthentication(String userId) {}

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
  void testOnEventDropsMetadataWhenEventHookUserHasMetadataReadAccess()
      throws NotFoundException, JsonProcessingException {
    User user = new User();
    user.setUid("11111111-1111-1111-1111-11111111111");
    EventHookListener eventHookListener =
        new EventHookListener(
            null,
            null,
            null,
            null,
            mockAuthenticationService,
            new MockAclService("22222222-2222-2222-2222-22222222222"));

    eventHookListener.getEventHookContext().setEventHooks(List.of(createMockEventHook(user)));
    CountDownLatch countDownLatch = new CountDownLatch(1);
    eventHookListener
        .getEventHookContext()
        .setTargets(
            Map.of(
                "00000000-0000-0000-0000-000000000000",
                List.of((eh, event, payload) -> countDownLatch.countDown())));

    eventHookListener.onEvent(EventUtils.metadataCreate(mockIdentifiableObject));

    assertEquals(1, countDownLatch.getCount());
  }

  @Test
  void testOnEventEmitsMetadataWhenEventHookUserHasMetadataReadAccess()
      throws NotFoundException, JsonProcessingException {
    User user = new User();
    user.setUid("11111111-1111-1111-1111-11111111111");
    EventHookListener eventHookListener =
        new EventHookListener(
            null,
            objectMapper,
            fieldFilterService,
            null,
            mockAuthenticationService,
            new MockAclService(user.getUid()));

    eventHookListener.getEventHookContext().setEventHooks(List.of(createMockEventHook(user)));
    CountDownLatch countDownLatch = new CountDownLatch(1);
    eventHookListener
        .getEventHookContext()
        .setTargets(
            Map.of(
                "00000000-0000-0000-0000-000000000000",
                List.of((eh, event, payload) -> countDownLatch.countDown())));

    eventHookListener.onEvent(EventUtils.metadataCreate(mockIdentifiableObject));

    assertEquals(0, countDownLatch.getCount());
  }

  private EventHook createMockEventHook(User user) {
    Source source = new Source();
    source.setPath("metadata..");

    EventHook eventHook = new EventHook();
    eventHook.setUser(user);
    eventHook.setTargets(List.of(new ConsoleTarget()));
    eventHook.setUid("00000000-0000-0000-0000-000000000000");
    eventHook.setSource(source);

    return eventHook;
  }
}
