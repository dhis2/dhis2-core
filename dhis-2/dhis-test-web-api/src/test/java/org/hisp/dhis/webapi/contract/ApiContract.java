package org.hisp.dhis.webapi.contract;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.hisp.dhis.http.HttpMethod;

public record ApiContract(
    String name,
    HttpMethod httpMethod,
    String requestUrl,
    int responseStatus,
    @JsonDeserialize(using = JsonSchemaDeserializer.class) JsonSchema jsonSchema) {

  @Nonnull
  @Override
  public String toString() {
    return name;
  }
}

/**
 * This deserializer takes in a path to a json schema e.g. <code>
 * contracts/category/category-json-schema.json</code> which is expected to be available on the
 * classpath. The contract defines where the schema is located.
 */
class JsonSchemaDeserializer extends JsonDeserializer<JsonSchema> {
  @Override
  public JsonSchema deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    return factory.getSchema(
        getClass().getClassLoader().getResourceAsStream(parser.getValueAsString()));
  }
}
