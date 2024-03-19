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
package org.hisp.dhis.schema.transformers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class UserPropertyTransformerTest extends SingleSetupIntegrationTestBase {

  private static final UUID uuid = UUID.fromString("6507f586-f154-4ec1-a25e-d7aa51de5216");

  @Autowired
  @Qualifier("jsonMapper")
  private ObjectMapper jsonMapper;

  @Autowired
  @Qualifier("xmlMapper")
  private ObjectMapper xmlMapper;

  @Autowired private NodeService nodeService;

  @Autowired private FieldFilterService fieldFilterService;

  @Test
  void testNodeServiceSerializer() throws JsonProcessingException {
    Simple simple = new Simple(1, "Simple1");
    simple.setUser(makeUser("a"));
    simple.getUser().setUuid(uuid);
    ComplexNode complexNode = nodeService.toNode(simple);
    RootNode rootNode = NodeUtils.createRootNode(complexNode);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    nodeService.serialize(rootNode, "application/json", outputStream);
    String jsonSource = outputStream.toString();
    verifyJsonSource(jsonSource);
    Simple simpleFromJson = jsonMapper.readValue(jsonSource, Simple.class);
    assertEquals(1, simpleFromJson.getId());
    assertEquals("Simple1", simpleFromJson.getName());
    assertNotNull(simple.getUser());

    assertEquals("usernamea", simple.getUser().getUsername());
    assertEquals(uuid, simple.getUser().getUuid());
  }

  @Test
  void testFieldNodeServiceSerializer() throws JsonProcessingException {
    Simple simple = new Simple(1, "Simple1");
    simple.setUser(makeUser("a"));
    simple.getUser().setUuid(uuid);
    simple.getUsers().add(makeUser("A"));
    simple.getUsers().add(makeUser("B"));
    simple.getUsers().add(makeUser("C"));
    simple.getUsers().add(makeUser("D"));
    ComplexNode complexNode = nodeService.toNode(simple);
    RootNode rootNode = NodeUtils.createRootNode(complexNode);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    nodeService.serialize(rootNode, "application/json", outputStream);
    String jsonSource = outputStream.toString();
    verifyJsonSource(jsonSource);
    Simple simpleFromJson = jsonMapper.readValue(jsonSource, Simple.class);
    assertEquals(1, simpleFromJson.getId());
    assertEquals("Simple1", simpleFromJson.getName());
    assertNotNull(simple.getUser());

    assertEquals("usernamea", simple.getUser().getUsername());
    assertEquals(uuid, simple.getUser().getUuid());
    assertNotNull(simple.getUsers());
    assertEquals(4, simple.getUsers().size());
    FieldFilterParams params =
        new FieldFilterParams(
            Collections.singletonList(simple),
            Collections.singletonList("id,name,user[id,code],users[id,code]"));
    fieldFilterService.toComplexNode(params);
  }

  @Test
  void testFieldNodeServiceSerializerPresetStar() throws JsonProcessingException {
    Simple simple = new Simple(1, "Simple1");
    simple.setUser(makeUser("a"));
    simple.getUser().setUuid(uuid);
    simple.getUsers().add(makeUser("A"));
    simple.getUsers().add(makeUser("B"));
    simple.getUsers().add(makeUser("C"));
    simple.getUsers().add(makeUser("D"));
    ComplexNode complexNode = nodeService.toNode(simple);
    RootNode rootNode = NodeUtils.createRootNode(complexNode);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    nodeService.serialize(rootNode, "application/json", outputStream);
    String jsonSource = outputStream.toString();
    verifyJsonSource(jsonSource);
    Simple simpleFromJson = jsonMapper.readValue(jsonSource, Simple.class);
    assertEquals(1, simpleFromJson.getId());
    assertEquals("Simple1", simpleFromJson.getName());
    assertNotNull(simple.getUser());

    assertEquals("usernamea", simple.getUser().getUsername());
    assertEquals(uuid, simple.getUser().getUuid());
    assertNotNull(simple.getUsers());
    assertEquals(4, simple.getUsers().size());
    FieldFilterParams params =
        new FieldFilterParams(
            Collections.singletonList(simple),
            Collections.singletonList("id,name,user[*],users[*]"));
    fieldFilterService.toComplexNode(params);
  }

  @Test
  void testJsonSerializer() throws JsonProcessingException {
    Simple simple = new Simple(1, "Simple1");
    User user = makeUser("a");
    simple.setUser(user);
    simple.getUser().setUuid(uuid);
    String jsonSource = jsonMapper.writeValueAsString(simple);
    verifyJsonSource(jsonSource);
    Simple simpleFromJson = jsonMapper.readValue(jsonSource, Simple.class);
    assertEquals(1, simpleFromJson.getId());
    assertEquals("Simple1", simpleFromJson.getName());
    assertNotNull(simple.getUser());

    assertEquals("usernamea", simple.getUser().getUsername());
    assertEquals(user.getUid(), simple.getUser().getUid());
    assertEquals(uuid, simple.getUser().getUuid());
  }

  @Test
  void testXmlSerializer() throws JsonProcessingException {
    Simple simple = new Simple(1, "Simple1");
    User user = makeUser("a");
    simple.setUser(user);
    simple.getUser().setUuid(uuid);
    String xmlSource = xmlMapper.writeValueAsString(simple);
    verifyXmlSource(xmlSource);
    Simple simpleFromJson = xmlMapper.readValue(xmlSource, Simple.class);
    assertEquals(1, simpleFromJson.getId());
    assertEquals("Simple1", simpleFromJson.getName());
    assertNotNull(simple.getUser());

    assertEquals("usernamea", simple.getUser().getUsername());
    assertEquals(user.getUid(), simple.getUser().getUid());
    assertEquals(uuid, simple.getUser().getUuid());
  }

  private void verifyJsonSource(String jsonSource) throws JsonProcessingException {
    JsonNode root = jsonMapper.readTree(jsonSource);
    verifyJsonNode(root);
  }

  private void verifyXmlSource(String xmlSource) throws JsonProcessingException {
    JsonNode root = xmlMapper.readTree(xmlSource);
    verifyJsonNode(root);
  }

  private void verifyJsonNode(JsonNode root) {
    assertTrue(root.has("id"));
    assertTrue(root.has("name"));
    assertTrue(root.has("user"));
    JsonNode userNode = root.get("user");
    assertTrue(userNode.has("id"));
    assertTrue(userNode.has("username"));
    // assertEquals( userNode.get( "id" ).textValue(), uuid.toString() );
    assertEquals(userNode.get("id").textValue(), "userabcdefa");
    assertEquals(userNode.get("username").textValue(), "usernamea");
  }

  @JacksonXmlRootElement(localName = "simple")
  public static class Simple {

    private int id;

    private String name;

    private User user;

    private List<User> users = new ArrayList<>();

    public Simple() {}

    public Simple(int id, String name) {
      this.id = id;
      this.name = name;
    }

    @JsonProperty
    public int getId() {
      return id;
    }

    @JsonProperty
    public String getName() {
      return name;
    }

    @JsonProperty
    @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
    @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
    @PropertyTransformer(UserPropertyTransformer.class)
    public User getUser() {
      return user;
    }

    public void setUser(User user) {
      this.user = user;
    }

    @JsonProperty
    @JsonSerialize(contentUsing = UserPropertyTransformer.JacksonSerialize.class)
    @JsonDeserialize(contentUsing = UserPropertyTransformer.JacksonDeserialize.class)
    @PropertyTransformer(UserPropertyTransformer.class)
    public List<User> getUsers() {
      return users;
    }

    public void setUsers(List<User> users) {
      this.users = users;
    }
  }
}
