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
package org.hisp.dhis.utils;

import com.github.javafaker.Faker;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.SchemasActions;
import org.hisp.dhis.dto.schemas.PropertyType;
import org.hisp.dhis.dto.schemas.Schema;
import org.hisp.dhis.dto.schemas.SchemaProperty;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class DataGenerator {
  private static Faker faker = new Faker();

  public static String randomString() {
    return RandomStringUtils.randomAlphabetic(6);
  }

  public static String randomString(int count) {
    return RandomStringUtils.randomAlphabetic(count);
  }

  public static String randomEntityName() {
    return "AutoTest entity " + randomString();
  }

  /**
   * Generates random data for simple type schema properties;
   *
   * @param property
   * @return
   */
  public static JsonElement generateRandomValueMatchingSchema(SchemaProperty property) {
    JsonElement jsonElement;
    switch (property.getPropertyType()) {
      case STRING:
        jsonElement =
            new JsonPrimitive(
                generateStringByFieldName(
                    property.getName(),
                    property.getMin().intValue(),
                    property.getMax().intValue()));
        break;

      case DATE:
        Date date = faker.date().past(1000, TimeUnit.DAYS);
        jsonElement =
            new JsonPrimitive(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(date));
        break;

      case BOOLEAN:
        if (property.getName().equalsIgnoreCase("external")) {
          jsonElement = new JsonPrimitive(true);
          break;
        }

        jsonElement = new JsonPrimitive(String.valueOf(faker.bool().bool()));
        break;

      case CONSTANT:
        jsonElement = generateConstantValue(property);
        break;

      case NUMBER:
        jsonElement =
            new JsonPrimitive(
                faker
                    .number()
                    .numberBetween(property.getMin().intValue(), property.getMax().intValue()));
        break;

      default:
        jsonElement = new JsonPrimitive("Conversion not defined.");
        break;
    }

    return jsonElement;
  }

  public static JsonObject generateObjectMatchingSchema(List<SchemaProperty> schemaProperties) {
    JsonObject objectBody = new JsonObject();

    for (SchemaProperty prop : schemaProperties) {
      JsonElement element;

      if (prop.getPropertyType() == PropertyType.REFERENCE) {
        List<SchemaProperty> referenceProperties =
            new SchemasActions().getRequiredProperties(prop.getName());

        JsonObject referenceObject = generateObjectMatchingSchema(referenceProperties);
        String uid =
            new RestApiActions(prop.getRelativeApiEndpoint()).post(referenceObject).extractUid();
        referenceObject.addProperty("id", uid);

        element = referenceObject;
      } else if (prop.getPropertyType() == PropertyType.IDENTIFIER) {
        if (!StringUtils.containsAny(prop.getName(), "id", "uid", "code")) {

          Schema schema = new SchemasActions().getSchema(prop.getName());
          JsonObject referenceObject = generateObjectMatchingSchema(schema.getRequiredProperties());
          String uid = new RestApiActions(schema.getPlural()).post(referenceObject).extractUid();

          element = new JsonPrimitive(uid);
        } else {
          element = new JsonPrimitive(new IdGenerator().generateUniqueId());
        }
      } else {
        element = generateRandomValueMatchingSchema(prop);
      }

      objectBody.add(prop.getName(), element);
    }

    return objectBody;
  }

  public static JsonObject generateObjectForEndpoint(String schemaEndpoint) {
    List<SchemaProperty> schemaProperties =
        new SchemasActions().getRequiredProperties(schemaEndpoint);

    return generateObjectMatchingSchema(schemaProperties);
  }

  private static String generateStringByFieldName(String name, int minLength, int maxLength) {
    switch (name) {
      case "url":
        return "http://" + faker.internet().url();

      case "cronExpression":
        return "* * * * * *";

      case "periodType":
        List<String> periodTypes =
            new RestApiActions("/periodTypes").get().extractList("periodTypes.name");
        return periodTypes.get(faker.number().numberBetween(0, periodTypes.size() - 1));

      default:
        if (minLength < 1) {
          return faker.lorem().characters(6);
        }

        if (maxLength == minLength) {
          return faker.lorem().characters(maxLength);
        }

        return faker.lorem().characters(minLength, maxLength);
    }
  }

  /**
   * Generates random value from the list of constants. If the list contains "MULTI_TEXT" value, it
   * will be skipped. This is because a DataElement with valueType=MultiText must have an OptionSet,
   * but currently we don't have a way to generate an OptionSet. TODO: add a way to generate an
   * OptionSet
   *
   * @param property SchemaProperty
   * @return JsonElement
   */
  private static JsonElement generateConstantValue(SchemaProperty property) {
    int randomConstant = -1;
    JsonPrimitive element = null;
    while (randomConstant == -1
        || (property.getName().equals("valueType")
            && property.getConstants().get(randomConstant).equals("MULTI_TEXT"))) {
      randomConstant = faker.number().numberBetween(0, property.getConstants().size() - 1);
      element = new JsonPrimitive(property.getConstants().get(randomConstant));
    }

    return element;
  }
}
