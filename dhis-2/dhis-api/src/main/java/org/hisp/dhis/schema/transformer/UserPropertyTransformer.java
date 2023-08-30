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
package org.hisp.dhis.schema.transformer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.schema.AbstractPropertyTransformer;
import org.hisp.dhis.user.User;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class UserPropertyTransformer extends AbstractPropertyTransformer<User> {
  public UserPropertyTransformer() {
    super(UserDto.class);
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Object transform(Object o) {
    if (!(o instanceof User)) {
      if (o instanceof Collection) {
        Collection collection = (Collection) o;

        if (collection.isEmpty()) {
          return o;
        }

        Object next = collection.iterator().next();

        if (!(next instanceof User)) {
          return o;
        }

        Collection<UserDto> userDtoCollection =
            newCollectionInstance(HibernateProxyUtils.getRealClass(collection));
        collection.forEach(user -> userDtoCollection.add(buildUserDto((User) user)));

        return userDtoCollection;
      }

      return o;
    }

    return buildUserDto((User) o);
  }

  private UserDto buildUserDto(User user) {
    UserDto.UserDtoBuilder builder =
        UserDto.builder()
            .id(user.getUid())
            .code(user.getCode())
            .displayName(user.getDisplayName())
            .name(user.getName())
            .username(user.getUsername());

    return builder.build();
  }

  @Data
  @Builder
  public static class UserDto {
    private String id;

    private String code;

    private String name;

    private String displayName;

    private String username;

    @JsonProperty
    public String getId() {
      return id;
    }

    @JsonProperty
    public String getCode() {
      return code;
    }

    @JsonProperty
    public String getName() {
      return name;
    }

    @JsonProperty
    public String getDisplayName() {
      return displayName;
    }

    @JsonProperty
    public String getUsername() {
      return username;
    }
  }

  public static final class JacksonSerialize extends StdSerializer<User> {
    public JacksonSerialize() {
      super(User.class);
    }

    @Override
    public void serialize(User user, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeStartObject();
      gen.writeStringField("id", user.getUid());
      gen.writeStringField("code", user.getCode());
      gen.writeStringField("name", user.getName());
      gen.writeStringField("displayName", user.getDisplayName());
      gen.writeStringField("username", user.getUsername());

      gen.writeEndObject();
    }
  }

  public static final class JacksonDeserialize extends StdDeserializer<User> {
    public JacksonDeserialize() {
      super(User.class);
    }

    @Override
    public User deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      User user = new User();

      JsonNode node = jp.getCodec().readTree(jp);

      if (node.has("id")) {
        String identifier = node.get("id").asText();

        if (CodeGenerator.isValidUid(identifier)) {
          user.setUid(identifier);
        } else {
          throw new JsonParseException(jp, "Invalid user identifier: " + identifier);
        }
      }

      if (node.has("code")) {
        String code = node.get("code").asText();

        user.setCode(code);
      }

      if (node.has("username")) {
        user.setUsername(node.get("username").asText());
      }

      return user;
    }
  }

  private static <E> Collection<E> newCollectionInstance(Class<?> clazz) {
    if (List.class.isAssignableFrom(clazz)) {
      return new ArrayList<>();
    } else if (Set.class.isAssignableFrom(clazz)) {
      return new HashSet<>();
    } else {
      throw new RuntimeException("Unknown Collection type.");
    }
  }
}
