# OAuth2 Authorization Server Configuration

This package contains the configuration for the OAuth2 Authorization Server in DHIS2.

## JWK Key Persistence

The JWK (JSON Web Key) used for signing JWTs (JSON Web Tokens) can be configured to be either:

1. **Ephemeral** - Generated on each server startup (default)
2. **Persistent** - Loaded from a keystore file

### Configuration Properties

The following properties control the JWK persistence:

```properties
# Path to the keystore file
oauth2.jwt.keystore.path=/path/to/keystore.jks

# Password for the keystore
oauth2.jwt.keystore.password=keystorePassword

# Alias for the key entry in the keystore
oauth2.jwt.keystore.alias=dhis2-oauth2-key

# Password for the key (optional, defaults to keystore password)
oauth2.jwt.keystore.key-password=keyPassword

# Whether to generate an ephemeral key if the keystore cannot be loaded (default: false)
oauth2.jwt.keystore.generate-if-missing=false
```

### Generating a Keystore

You can generate a keystore with an RSA key pair using the provided utility class:

```bash
java -cp dhis2.jar org.hisp.dhis.security.oidc.KeystoreGenerationTool \
  --keystore-path /path/to/keystore.jks \
  --keystore-password keystorePassword \
  --alias dhis2-oauth2-key \
  --key-password keyPassword
```

### Security Considerations

1. **Key Protection**: Store the keystore file in a secure location with appropriate file permissions
2. **Password Security**: Use strong passwords for both the keystore and the key
3. **Key Rotation**: Implement a key rotation policy and update the keystore when needed

### Error Handling

If the server fails to load the keystore or the key, it will:

1. Log detailed error messages
2. Either generate a new ephemeral key (if `oauth2.jwt.keystore.generate-if-missing=true`) or fail to start
3. When configured to require persistent keys, server startup will fail if keys cannot be loaded

### Compatibility with Clustered Environments

Using a persistent JWK is especially important in clustered environments to ensure that all nodes use the same key for token signing and verification. Copy the same keystore file to all nodes in the cluster.

### Migration from Ephemeral Keys

If you've been using ephemeral keys and want to migrate to persistent keys:

1. Generate a new keystore as described above
2. Configure the application to use the new keystore
3. Restart the application
4. All new tokens will be signed with the persistent key

Note that tokens signed with the old ephemeral key will no longer be valid after the restart. Users will need to obtain new tokens. 