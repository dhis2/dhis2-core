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
package org.hisp.dhis.hibernate.jsonb.type;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.hisp.dhis.commons.jackson.config.EmptyStringToNullStdDeserializer;
import org.hisp.dhis.commons.jackson.config.ParseDateStdDeserializer;
import org.hisp.dhis.commons.jackson.config.WriteDateStdSerializer;
import org.postgresql.util.PGobject;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Stian Sandvold <stian@dhis2.org>
 */
public class JsonBinaryType implements UserType, ParameterizedType {
  public static final ObjectMapper MAPPER = new ObjectMapper();

  public static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT_TYPE_REFERENCE =
      new TypeReference<>() {};

  static {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(String.class, new EmptyStringToNullStdDeserializer());
    module.addDeserializer(Date.class, new ParseDateStdDeserializer());
    module.addSerializer(Date.class, new WriteDateStdSerializer());

    MAPPER.registerModules(module, new JtsModule(), new JavaTimeModule(), new Jdk8Module());
    MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  ObjectWriter writer;

  ObjectReader reader;

  Class<?> returnedClass;

  ObjectMapper resultingMapper;

  JavaType resultingJavaType;

  @Override
  public int[] sqlTypes() {
    return new int[] {Types.JAVA_OBJECT};
  }

  @Override
  public Class<?> returnedClass() {
    return returnedClass;
  }

  @Override
  public boolean equals(Object x, Object y) {
    return x == y || (x != null && y != null && (x.equals(y) || safeContentBasedEquals(x, y)));
  }

  private boolean safeContentBasedEquals(Object x, Object y) {
    Optional<Map<String, Object>> first = safelyConvertToMap(x);
    Optional<Map<String, Object>> second = safelyConvertToMap(y);

    if (first.isPresent() && second.isPresent()) {
      return first.equals(second);
    }

    return false;
  }

  private Optional<Map<String, Object>> safelyConvertToMap(Object o) {
    try {
      return Optional.of(
          resultingMapper.readValue(
              resultingMapper.writeValueAsString(o), MAP_STRING_OBJECT_TYPE_REFERENCE));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public int hashCode(Object x) throws HibernateException {
    return null == x ? 0 : x.hashCode();
  }

  @Override
  public Object nullSafeGet(
      ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
      throws HibernateException, SQLException {
    final Object result = rs.getObject(names[0]);

    if (!rs.wasNull()) {
      String content = null;

      if (result instanceof String) {
        content = (String) result;
      } else if (result instanceof PGobject) {
        content = ((PGobject) result).getValue();
      }

      // Other types currently ignored

      if (content != null) {
        return convertJsonToObject(content);
      }
    }

    return null;
  }

  @Override
  public void nullSafeSet(
      PreparedStatement ps, Object value, int idx, SharedSessionContractImplementor session)
      throws HibernateException, SQLException {
    if (value == null) {
      ps.setObject(idx, null);
      return;
    }

    PGobject pg = new PGobject();
    pg.setType("jsonb");
    pg.setValue(convertObjectToJson(value));

    ps.setObject(idx, pg);
  }

  @Override
  public Object deepCopy(Object value) throws HibernateException {
    if (value == null) {
      return null;
    }

    final TokenBuffer tb = new TokenBuffer(resultingMapper, false);
    try {
      writeValue(tb, value);
      return readValue(tb.asParser());
    } catch (IOException e) {
      throw new HibernateException("Could not deep copy JSONB object.", e);
    }
  }

  @Override
  public boolean isMutable() {
    return true;
  }

  @Override
  public Serializable disassemble(Object value) throws HibernateException {
    return (Serializable) this.deepCopy(value);
  }

  @Override
  public Object assemble(Serializable cached, Object owner) throws HibernateException {
    return this.deepCopy(cached);
  }

  @Override
  public Object replace(Object original, Object target, Object owner) throws HibernateException {
    return this.deepCopy(original);
  }

  @Override
  public void setParameterValues(Properties parameters) {
    final String clazz = (String) parameters.get("clazz");

    if (clazz == null) {
      throw new IllegalArgumentException(
          String.format("Required parameter '%s' is not configured", "clazz"));
    }

    try {
      init(classForName(clazz));
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Class: " + clazz + " is not a known class type.");
    }
  }

  protected void init(Class<?> klass) {
    resultingMapper = getResultingMapper();
    resultingJavaType = getResultingJavaType(klass);
    reader = resultingMapper.readerFor(resultingJavaType);
    writer = resultingMapper.writerFor(resultingJavaType);
    returnedClass = klass;
  }

  protected ObjectMapper getResultingMapper() {
    return MAPPER;
  }

  protected JavaType getResultingJavaType(Class<?> returnedClass) {
    return MAPPER.getTypeFactory().constructType(returnedClass);
  }

  static Class<?> classForName(String name) throws ClassNotFoundException {
    try {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

      if (classLoader != null) {
        return classLoader.loadClass(name);
      }
    } catch (Throwable ignore) {
    }

    return Class.forName(name);
  }

  /**
   * Serializes an object to JSON.
   *
   * @param object the object to convert.
   * @return JSON content.
   */
  protected String convertObjectToJson(Object object) {
    try {
      return writer.writeValueAsString(object);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Deserializes JSON content to an object.
   *
   * @param content the JSON content.
   * @return an object.
   */
  protected Object convertJsonToObject(String content) {
    try {
      return reader.readValue(content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void writeValue(JsonGenerator gen, Object value) throws IOException {
    writer.writeValue(gen, value);
  }

  protected <T> T readValue(JsonParser p) throws IOException {
    return reader.readValue(p);
  }
}
