/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.test.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs direct JDBC batch inserts of DHIS2 users from JSON files, bypassing the slow metadata
 * import API. Used in Gatling performance tests to quickly seed test users.
 */
public class JdbcBatchUserImporter {

  private static final Logger logger = LoggerFactory.getLogger(JdbcBatchUserImporter.class);

  private static final int BATCH_SIZE = 1000;

  /**
   * Pre-computed BCrypt hash for the password "Test123!". This avoids a dependency on Spring
   * Security or jBCrypt at runtime and eliminates the cost of hashing per user. Generated via: new
   * BCryptPasswordEncoder().encode("Test123!")
   */
  private static final String BCRYPT_HASH_TEST123 =
      "$2a$10$wNKkjFPfaxNwyFxBMjSMp.oAPHQMaEoTtr9TYn9FKTqhjsBqPNpSG";

  private static final String INSERT_USERINFO_SQL =
      "INSERT INTO userinfo (userinfoid, uid, username, firstname, surname, password,"
          + " passwordlastupdated, created, lastupdated, uuid, twofactortype,"
          + " disabled, externalauth, selfregistered, invitation)"
          + " VALUES (nextval('hibernate_sequence'), ?, ?, ?, ?, ?, NOW(), NOW(), NOW(),"
          + " gen_random_uuid(), 'NOT_ENABLED',"
          + " false, false, false, false)"
          + " ON CONFLICT (uid) DO NOTHING";

  private static final String SELECT_USERINFO_IDS_SQL =
      "SELECT userinfoid, uid FROM userinfo WHERE uid = ANY(?)";

  private static final String INSERT_USERROLEMEMBERS_SQL =
      "INSERT INTO userrolemembers (userid, userroleid) VALUES (?, ?) ON CONFLICT DO NOTHING";

  private static final String INSERT_USERMEMBERSHIP_SQL =
      "INSERT INTO usermembership (userinfoid, organisationunitid)"
          + " VALUES (?, ?) ON CONFLICT DO NOTHING";

  private static final String INSERT_USERDATAVIEWORGUNITS_SQL =
      "INSERT INTO userdatavieworgunits (userinfoid, organisationunitid)"
          + " VALUES (?, ?) ON CONFLICT DO NOTHING";

  private static final String INSERT_USERGROUPMEMBERS_SQL =
      "INSERT INTO usergroupmembers (userid, usergroupid)"
          + " VALUES (?, ?) ON CONFLICT DO NOTHING";

  private JdbcBatchUserImporter() {
    // static utility class
  }

  /**
   * Import users from multiple JSON classpath resources using the naming convention
   * "platform/users/users_NNNN.json".
   *
   * @param fileCount number of user files to import (0000 to fileCount-1)
   * @param dbUrl JDBC URL (e.g. "jdbc:postgresql://localhost:5432/dhis2")
   * @param dbUser database username
   * @param dbPassword database password
   * @return total number of users inserted across all files
   */
  public static int importAllUserFiles(
      int fileCount, String dbUrl, String dbUser, String dbPassword) {
    int totalInserted = 0;
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < fileCount; i++) {
      String fileName = String.format("platform/users/users_%04d.json", i);
      totalInserted += importUsersFromJsonFile(fileName, dbUrl, dbUser, dbPassword);
    }

    long elapsed = System.currentTimeMillis() - startTime;
    logger.info(
        "All user files imported: {} users in {} ms ({} files)", totalInserted, elapsed, fileCount);
    return totalInserted;
  }

  /**
   * Import users from a JSON classpath resource directly via JDBC batch inserts. This bypasses the
   * DHIS2 metadata import API for dramatically faster imports.
   *
   * @param fileName classpath resource (e.g. "platform/users/users_0000.json")
   * @param dbUrl JDBC URL (e.g. "jdbc:postgresql://localhost:5432/dhis2")
   * @param dbUser database username
   * @param dbPassword database password
   * @return number of users inserted
   */
  public static int importUsersFromJsonFile(
      String fileName, String dbUrl, String dbUser, String dbPassword) {
    long startTime = System.currentTimeMillis();
    logger.info("Starting JDBC batch import of users from: {}", fileName);

    List<UserRecord> users = parseUsersFromJson(fileName);
    if (users.isEmpty()) {
      logger.warn("No users found in file: {}", fileName);
      return 0;
    }

    logger.info("Parsed {} users from {}", users.size(), fileName);

    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      conn.setAutoCommit(false);

      // Build lookup maps for userroles, orgunits, and usergroups (UID -> database PK)
      Map<String, Integer> userRoleLookup = buildUserRoleLookup(conn);
      Map<String, Integer> orgUnitLookup = buildOrgUnitLookup(conn);
      Map<String, Integer> userGroupLookup = buildUserGroupLookup(conn);

      int totalInserted = 0;

      // Process in batches
      for (int batchStart = 0; batchStart < users.size(); batchStart += BATCH_SIZE) {
        int batchEnd = Math.min(batchStart + BATCH_SIZE, users.size());
        List<UserRecord> batch = users.subList(batchStart, batchEnd);

        int inserted = insertUserBatch(conn, batch, userRoleLookup, orgUnitLookup, userGroupLookup);
        totalInserted += inserted;

        if (batchEnd % 1000 == 0 || batchEnd == users.size()) {
          logger.info("Progress: {}/{} users processed from {}", batchEnd, users.size(), fileName);
        }
      }

      long elapsed = System.currentTimeMillis() - startTime;
      logger.info(
          "JDBC batch import complete: {} users from {} in {} ms", totalInserted, fileName, elapsed);
      return totalInserted;

    } catch (SQLException e) {
      logger.error("Database error during JDBC batch import of {}: {}", fileName, e.getMessage());
      throw new RuntimeException("Failed to import users from " + fileName, e);
    }
  }

  private static List<UserRecord> parseUsersFromJson(String fileName) {
    ObjectMapper mapper = new ObjectMapper();
    List<UserRecord> users = new ArrayList<>();

    try (InputStream is =
        JdbcBatchUserImporter.class.getClassLoader().getResourceAsStream(fileName)) {
      if (is == null) {
        throw new RuntimeException("Resource not found on classpath: " + fileName);
      }

      JsonNode root = mapper.readTree(is);
      JsonNode usersNode = root.get("users");
      if (usersNode == null || !usersNode.isArray()) {
        logger.warn("No 'users' array found in {}", fileName);
        return users;
      }

      for (JsonNode userNode : usersNode) {
        UserRecord user = new UserRecord();
        user.uid = userNode.get("id").asText();
        user.username = userNode.get("username").asText();
        user.firstName = userNode.get("firstName").asText();
        user.surname = userNode.get("surname").asText();

        // Parse user role UIDs
        JsonNode rolesNode = userNode.get("userRoles");
        if (rolesNode != null && rolesNode.isArray()) {
          for (JsonNode roleNode : rolesNode) {
            user.userRoleUids.add(roleNode.get("id").asText());
          }
        }

        // Parse organisation unit UIDs
        JsonNode orgUnitsNode = userNode.get("organisationUnits");
        if (orgUnitsNode != null && orgUnitsNode.isArray()) {
          for (JsonNode ouNode : orgUnitsNode) {
            user.orgUnitUids.add(ouNode.get("id").asText());
          }
        }

        // Parse data view organisation unit UIDs
        JsonNode dataViewOuNode = userNode.get("dataViewOrganisationUnits");
        if (dataViewOuNode != null && dataViewOuNode.isArray()) {
          for (JsonNode ouNode : dataViewOuNode) {
            user.dataViewOrgUnitUids.add(ouNode.get("id").asText());
          }
        }

        // Parse user group UIDs
        JsonNode groupsNode = userNode.get("userGroups");
        if (groupsNode != null && groupsNode.isArray()) {
          for (JsonNode gNode : groupsNode) {
            user.userGroupUids.add(gNode.get("id").asText());
          }
        }

        users.add(user);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse users from " + fileName, e);
    }

    return users;
  }

  private static Map<String, Integer> buildUserRoleLookup(Connection conn) throws SQLException {
    Map<String, Integer> lookup = new HashMap<>();
    try (PreparedStatement ps = conn.prepareStatement("SELECT uid, userroleid FROM userrole")) {
      ps.setFetchSize(500);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          lookup.put(rs.getString("uid"), rs.getInt("userroleid"));
        }
      }
    }
    logger.info("Loaded {} user roles into lookup map", lookup.size());
    return lookup;
  }

  private static Map<String, Integer> buildOrgUnitLookup(Connection conn) throws SQLException {
    Map<String, Integer> lookup = new HashMap<>();
    try (PreparedStatement ps =
            conn.prepareStatement("SELECT uid, organisationunitid FROM organisationunit")) {
      ps.setFetchSize(1000);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          lookup.put(rs.getString("uid"), rs.getInt("organisationunitid"));
        }
      }
    }
    logger.info("Loaded {} organisation units into lookup map", lookup.size());
    return lookup;
  }

  private static Map<String, Integer> buildUserGroupLookup(Connection conn) throws SQLException {
    Map<String, Integer> lookup = new HashMap<>();
    try (PreparedStatement ps =
        conn.prepareStatement("SELECT uid, usergroupid FROM usergroup")) {
      ps.setFetchSize(500);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          lookup.put(rs.getString("uid"), rs.getInt("usergroupid"));
        }
      }
    }
    logger.info("Loaded {} user groups into lookup map", lookup.size());
    return lookup;
  }

  private static int insertUserBatch(
      Connection conn,
      List<UserRecord> batch,
      Map<String, Integer> userRoleLookup,
      Map<String, Integer> orgUnitLookup,
      Map<String, Integer> userGroupLookup)
      throws SQLException {

    // Step 1: Batch insert into userinfo
    try (PreparedStatement ps = conn.prepareStatement(INSERT_USERINFO_SQL)) {
      for (UserRecord user : batch) {
        ps.setString(1, user.uid);
        ps.setString(2, user.username);
        ps.setString(3, user.firstName);
        ps.setString(4, user.surname);
        ps.setString(5, BCRYPT_HASH_TEST123);
        ps.addBatch();
      }
      ps.executeBatch();
      conn.commit();
    }

    // Step 2: Query back the userinfoid values for the batch
    String[] uids = batch.stream().map(u -> u.uid).toArray(String[]::new);
    Map<String, Integer> uidToDbId = new HashMap<>();

    try (PreparedStatement ps = conn.prepareStatement(SELECT_USERINFO_IDS_SQL)) {
      Array uidArray = conn.createArrayOf("VARCHAR", uids);
      ps.setArray(1, uidArray);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          uidToDbId.put(rs.getString("uid"), rs.getInt("userinfoid"));
        }
      }
      uidArray.free();
    }

    // Step 3: Batch insert into userrolemembers
    try (PreparedStatement ps = conn.prepareStatement(INSERT_USERROLEMEMBERS_SQL)) {
      int count = 0;
      for (UserRecord user : batch) {
        Integer userId = uidToDbId.get(user.uid);
        if (userId == null) continue;

        for (String roleUid : user.userRoleUids) {
          Integer roleId = userRoleLookup.get(roleUid);
          if (roleId == null) {
            logger.warn("User role UID {} not found in database, skipping for user {}", roleUid,
                user.username);
            continue;
          }
          ps.setInt(1, userId);
          ps.setInt(2, roleId);
          ps.addBatch();
          count++;
        }
      }
      if (count > 0) {
        ps.executeBatch();
      }
    }

    // Step 4: Batch insert into usermembership
    try (PreparedStatement ps = conn.prepareStatement(INSERT_USERMEMBERSHIP_SQL)) {
      int count = 0;
      for (UserRecord user : batch) {
        Integer userId = uidToDbId.get(user.uid);
        if (userId == null) continue;

        for (String ouUid : user.orgUnitUids) {
          Integer ouId = orgUnitLookup.get(ouUid);
          if (ouId == null) {
            logger.warn(
                "Organisation unit UID {} not found in database, skipping for user {}",
                ouUid,
                user.username);
            continue;
          }
          ps.setInt(1, userId);
          ps.setInt(2, ouId);
          ps.addBatch();
          count++;
        }
      }
      if (count > 0) {
        ps.executeBatch();
      }
    }

    // Step 5: Batch insert into userdatavieworgunits
    try (PreparedStatement ps = conn.prepareStatement(INSERT_USERDATAVIEWORGUNITS_SQL)) {
      int count = 0;
      for (UserRecord user : batch) {
        Integer userId = uidToDbId.get(user.uid);
        if (userId == null) continue;

        for (String ouUid : user.dataViewOrgUnitUids) {
          Integer ouId = orgUnitLookup.get(ouUid);
          if (ouId == null) {
            logger.warn(
                "Data view org unit UID {} not found in database, skipping for user {}",
                ouUid,
                user.username);
            continue;
          }
          ps.setInt(1, userId);
          ps.setInt(2, ouId);
          ps.addBatch();
          count++;
        }
      }
      if (count > 0) {
        ps.executeBatch();
      }
    }

    // Step 6: Batch insert into usergroupmembers
    try (PreparedStatement ps = conn.prepareStatement(INSERT_USERGROUPMEMBERS_SQL)) {
      int count = 0;
      for (UserRecord user : batch) {
        Integer userId = uidToDbId.get(user.uid);
        if (userId == null) continue;

        for (String groupUid : user.userGroupUids) {
          Integer groupId = userGroupLookup.get(groupUid);
          if (groupId == null) {
            logger.warn(
                "User group UID {} not found in database, skipping for user {}",
                groupUid,
                user.username);
            continue;
          }
          ps.setInt(1, userId);
          ps.setInt(2, groupId);
          ps.addBatch();
          count++;
        }
      }
      if (count > 0) {
        ps.executeBatch();
      }
    }

    conn.commit();
    return uidToDbId.size();
  }

  /** Internal record holding parsed user data from JSON. */
  private static class UserRecord {
    String uid;
    String username;
    String firstName;
    String surname;
    List<String> userRoleUids = new ArrayList<>();
    List<String> orgUnitUids = new ArrayList<>();
    List<String> dataViewOrgUnitUids = new ArrayList<>();
    List<String> userGroupUids = new ArrayList<>();
  }
}
