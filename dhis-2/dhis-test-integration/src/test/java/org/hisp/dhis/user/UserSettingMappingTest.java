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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.ComponentType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.StringType;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link UserSetting} mapping: the {@code usersetting} table keyed by user and setting
 * name, with the value stored as a Java-serialized {@code bytea}.
 */
@Transactional
class UserSettingMappingTest extends PostgresIntegrationTestBase {

  @Autowired private UserSettingStore userSettingStore;

  @Autowired private JdbcTemplate jdbcTemplate;

  private AbstractEntityPersister persister;

  private User user;

  @BeforeEach
  void setUp() {
    persister =
        (AbstractEntityPersister)
            entityManager
                .getEntityManagerFactory()
                .unwrap(SessionFactoryImplementor.class)
                .getMetamodel()
                .entityPersister(UserSetting.class);
    user = createAndAddUser("settingsuser");
    entityManager.flush();
  }

  @Test
  void shouldMapTableAndCompositeIdOfUserAndName() {
    assertEquals("usersetting", persister.getTableName());
    assertTrue(persister.getEntityMetamodel().getIdentifierProperty().isEmbedded());
    assertArrayEquals(new String[] {"userinfoid", "name"}, persister.getIdentifierColumnNames());

    ComponentType id = assertInstanceOf(ComponentType.class, persister.getIdentifierType());
    assertArrayEquals(new String[] {"user", "name"}, id.getPropertyNames());
    ManyToOneType userType = assertInstanceOf(ManyToOneType.class, id.getSubtypes()[0]);
    assertEquals(User.class.getName(), userType.getAssociatedEntityName());
    assertFalse(userType.isEager(null));
    assertInstanceOf(StringType.class, id.getSubtypes()[1]);
  }

  @Test
  void shouldMapValueAsNullableSerializableColumn() {
    assertArrayEquals(new String[] {"value"}, persister.getPropertyNames());
    assertArrayEquals(new String[] {"value"}, persister.getPropertyColumnNames("value"));
    assertInstanceOf(SerializableType.class, persister.getPropertyType("value"));
    assertTrue(persister.getPropertyNullability()[0]);
  }

  @Test
  void shouldWriteValueAsSerializedObject() throws Exception {
    entityManager.persist(new UserSetting(user, "keyStyle", "light_blue/light_blue.css"));
    entityManager.flush();

    byte[] stored =
        jdbcTemplate.queryForObject(
            "select value from usersetting where userinfoid = ? and name = ?",
            byte[].class,
            user.getId(),
            "keyStyle");

    assertEquals("light_blue/light_blue.css", deserialize(stored));
  }

  @Test
  void shouldFindSettingByUserAndName() throws IOException {
    jdbcTemplate.update(
        "insert into usersetting (userinfoid, name, value) values (?, ?, ?)",
        user.getId(),
        "keyDbLocale",
        serialize("fr"));
    jdbcTemplate.update(
        "insert into usersetting (userinfoid, name, value) values (?, ?, null)",
        user.getId(),
        "keyMessageEmailNotification");

    UserSetting setting =
        entityManager.find(UserSetting.class, new UserSetting(user, "keyDbLocale", null));
    UserSetting withoutValue =
        entityManager.find(
            UserSetting.class, new UserSetting(user, "keyMessageEmailNotification", null));

    assertNotNull(setting);
    assertEquals(user.getUid(), setting.getUser().getUid());
    assertEquals("keyDbLocale", setting.getName());
    assertEquals("fr", setting.getValue());
    assertNotNull(withoutValue);
    assertNull(withoutValue.getValue());
  }

  @Test
  void shouldReadStoreWrittenValueThroughMapping() {
    userSettingStore.put(user.getUsername(), "keyUiLocale", "es");

    UserSetting setting =
        entityManager.find(UserSetting.class, new UserSetting(user, "keyUiLocale", null));

    assertEquals("es", setting.getValue());
    assertEquals(Map.of("keyUiLocale", "es"), userSettingStore.getAll(user.getUsername()));
  }

  @Test
  void shouldJoinUiLocaleSettingInNotifiableUsersQuery() {
    ZonedDateTime now = ZonedDateTime.now();
    User withLocale = createAndAddUser("localeuser");
    withLocale.setEmail("locale@example.org");
    withLocale.setLastLogin(Date.from(now.minusDays(10).toInstant()));
    user.setEmail("nolocale@example.org");
    user.setLastLogin(Date.from(now.minusDays(10).toInstant()));
    userService.updateUser(withLocale);
    userService.updateUser(user);
    entityManager.flush();
    userSettingStore.put(withLocale.getUsername(), "keyUiLocale", "fr");
    userSettingStore.put(user.getUsername(), "keyDbLocale", "es");

    Map<String, Optional<Locale>> users =
        userService.findNotifiableUsersWithLastLoginBetween(
            Date.from(now.minusDays(20).toInstant()), Date.from(now.toInstant()));

    assertEquals(
        Map.of(
            "locale@example.org", Optional.of(Locale.of("fr")),
            "nolocale@example.org", Optional.empty()),
        users);
  }

  private static byte[] serialize(String value) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(value);
    }
    return bytes.toByteArray();
  }

  private static Object deserialize(byte[] value) throws Exception {
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(value))) {
      return in.readObject();
    }
  }
}
