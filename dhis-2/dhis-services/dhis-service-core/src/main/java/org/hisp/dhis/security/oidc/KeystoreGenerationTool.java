package org.hisp.dhis.security.oidc;

import com.nimbusds.jose.jwk.RSAKey;
import java.io.File;
import java.io.Console;

/**
 * Command line utility to generate a keystore with an RSA key pair for OAuth2 authorization server
 * 
 * @author YourName
 */
public class KeystoreGenerationTool {
  
  private static final String USAGE = 
      "Usage: java -cp [...] org.hisp.dhis.security.oidc.KeystoreGenerationTool [options]\n" +
      "Options:\n" +
      "  --keystore-path <path>           Path to the keystore file (required)\n" +
      "  --keystore-password <password>   Password for the keystore (required)\n" +
      "  --alias <alias>                  Alias for the key entry (default: dhis2-oauth2-key)\n" +
      "  --key-password <password>        Password for the key (default: same as keystore password)\n" +
      "  --force                          Overwrite the keystore file if it exists\n" +
      "  --help                           Display this help message";
  
  public static void main(String[] args) {
    try {
      // Default values
      String keystorePath = null;
      String keystorePassword = null;
      String alias = "dhis2-oauth2-key";
      String keyPassword = null;
      boolean force = false;
      
      // Parse command line arguments
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--keystore-path":
            if (i + 1 < args.length) {
              keystorePath = args[++i];
            }
            break;
          case "--keystore-password":
            if (i + 1 < args.length) {
              keystorePassword = args[++i];
            }
            break;
          case "--alias":
            if (i + 1 < args.length) {
              alias = args[++i];
            }
            break;
          case "--key-password":
            if (i + 1 < args.length) {
              keyPassword = args[++i];
            }
            break;
          case "--force":
            force = true;
            break;
          case "--help":
            System.out.println(USAGE);
            return;
        }
      }
      
      // Validate required parameters
      if (keystorePath == null || keystorePath.isEmpty()) {
        System.err.println("Error: keystore-path is required");
        System.out.println(USAGE);
        System.exit(1);
      }
      
      // Prompt for passwords if not provided
      Console console = System.console();
      if (console != null) {
        if (keystorePassword == null || keystorePassword.isEmpty()) {
          char[] passwordChars = console.readPassword("Enter keystore password: ");
          keystorePassword = new String(passwordChars);
          
          // Confirm password
          char[] confirmPasswordChars = console.readPassword("Confirm keystore password: ");
          String confirmPassword = new String(confirmPasswordChars);
          
          if (!keystorePassword.equals(confirmPassword)) {
            System.err.println("Error: Passwords do not match");
            System.exit(1);
          }
        }
        
        if (keyPassword == null || keyPassword.isEmpty()) {
          char[] keyPasswordChars = console.readPassword("Enter key password (press enter to use keystore password): ");
          keyPassword = keyPasswordChars.length > 0 ? new String(keyPasswordChars) : keystorePassword;
          
          if (keyPasswordChars.length > 0) {
            // Confirm password
            char[] confirmKeyPasswordChars = console.readPassword("Confirm key password: ");
            String confirmKeyPassword = new String(confirmKeyPasswordChars);
            
            if (!keyPassword.equals(confirmKeyPassword)) {
              System.err.println("Error: Key passwords do not match");
              System.exit(1);
            }
          }
        }
      } else {
        if (keystorePassword == null || keystorePassword.isEmpty() ||
            keyPassword == null || keyPassword.isEmpty()) {
          System.err.println("Error: Console not available for password input. Please provide passwords as arguments.");
          System.exit(1);
        }
      }
      
      // Default key password to keystore password if not provided
      if (keyPassword == null || keyPassword.isEmpty()) {
        keyPassword = keystorePassword;
      }
      
      // Check if the keystore file already exists
      File keystoreFile = new File(keystorePath);
      if (keystoreFile.exists() && !force) {
        System.err.println("Error: Keystore file already exists. Use --force to overwrite.");
        System.exit(1);
      }
      
      // Generate the keystore
      System.out.println("Generating keystore...");
      RSAKey rsaKey = KeyStoreUtil.generateKeystoreWithRsaKey(
          keystorePath, keystorePassword, alias, keyPassword);
      
      System.out.println("Keystore generated successfully at: " + keystorePath);
      System.out.println("Key alias: " + alias);
      System.out.println("Key ID: " + rsaKey.getKeyID());
      
      // Print instructions for configuration
      System.out.println("\nTo use this keystore, configure the following properties in your application:");
      System.out.println("oauth2.jwt.keystore.path=" + keystorePath);
      System.out.println("oauth2.jwt.keystore.password=" + keystorePassword);
      System.out.println("oauth2.jwt.keystore.alias=" + alias);
      System.out.println("oauth2.jwt.keystore.key-password=" + keyPassword);
      
    } catch (Exception e) {
      System.err.println("Error generating keystore: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
} 