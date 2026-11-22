-- Delete tracked entity attribute values with neither a plain text nor an encrypted value.
--
-- A tracked entity attribute value row is meaningful only because of its value, so a row with a
-- NULL value and a NULL encryptedvalue carries no information and is not a state the application
-- produces: the tracker importer deletes the row when a value is cleared (it never stores NULL),
-- and the legacy TrackedEntityAttributeValueService skips the save when the value is NULL.
--
-- Rows with a NULL value but an encryptedvalue (previously confidential attributes) are kept, as
-- they still hold data; they are reported by the tracked_entity_attribute_null_value data
-- integrity check.
DELETE FROM trackedentityattributevalue
WHERE value IS NULL
AND encryptedvalue IS NULL;
