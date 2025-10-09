package org.hisp.dhis.common;

public interface SoftDeletable {

  boolean isDeleted();

  void setDeleted(boolean deleted);
  
  default boolean objectEquals(SoftDeletableObject that) {
    return isDeleted() == that.deleted;
  }
}
