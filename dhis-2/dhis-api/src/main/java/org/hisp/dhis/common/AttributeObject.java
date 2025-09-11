package org.hisp.dhis.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hisp.dhis.attribute.AttributeValues;

/**
 * Interface for objects that can have attribute values.
 * <p>
 * Objects implementing this interface must have a property of type {@link AttributeValues} map to database:
 * </p>
 * <pre>
 * &#64;AuditAttribute
 * &#64;Type(type = "jsbAttributeValues")
 * private AttributeValues attributeValues = AttributeValues.empty();
 * </pre>
 */
public interface AttributeObject {

  AttributeValues getAttributeValues();

  void setAttributeValues(AttributeValues attributeValues);

  default void addAttributeValue(String attributeId, String value) {
    getAttributeValues().added(attributeId, value);
  }

  default void removeAttributeValue(String attributeId) {
    getAttributeValues().removed(attributeId);
  }

  @JsonIgnore
  default String getAttributeValue(String attributeUid) {
    return getAttributeValues().get(attributeUid);
  }
}
