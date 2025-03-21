#!/usr/bin/env python3
"""
DHIS2 Upgrade Script

This script handles the upgrade process for DHIS2 servers.
It is designed to be called from the dhis2-server.sh script
but can also be run directly.

No external dependencies required - uses only the standard library.
"""

import argparse
import json
import os
import platform
import re
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request
from datetime import datetime
from urllib.error import URLError

# Constants
DHIS2_RELEASES_URL = "https://releases.dhis2.org/v1/versions/stable.json"
JAVA_MIN_VERSION_LEGACY = 8   # For DHIS2 <= 2.37
JAVA_MIN_VERSION_MIDDLE = 11  # For DHIS2 >= 2.38 and < 2.41
JAVA_MIN_VERSION_MODERN = 17  # For DHIS2 >= 2.41
DHIS2_VERSION_THRESHOLD_MIDDLE = "2.38"
DHIS2_VERSION_THRESHOLD_MODERN = "2.41"


class Colors:
  """Terminal colors for improved output"""

  HEADER = "\033[95m"
  BLUE = "\033[94m"
  GREEN = "\033[92m"
  YELLOW = "\033[93m"
  RED = "\033[91m"
  BOLD = "\033[1m"
  UNDERLINE = "\033[4m"
  END = "\033[0m"

  @staticmethod
  def supports_color():
    """Check if the terminal supports colors"""
    return (
        hasattr(sys.stdout, "isatty")
        and sys.stdout.isatty()
        and platform.system() != "Windows"
    )


def print_header(message):
  """Print a formatted header message"""
  if Colors.supports_color():
    print(f"\n{Colors.HEADER}{Colors.BOLD}{'=' * 44}{Colors.END}")
    print(f"{Colors.HEADER}{Colors.BOLD}    {message}{Colors.END}")
    print(f"{Colors.HEADER}{Colors.BOLD}{'=' * 44}{Colors.END}\n")
  else:
    print("\n" + "=" * 44)
    print(f"    {message}")
    print("=" * 44 + "\n")


def print_warning(message):
  """Print a warning message"""
  if Colors.supports_color():
    print(f"{Colors.YELLOW}{Colors.BOLD}Warning: {message}{Colors.END}")
  else:
    print(f"Warning: {message}")


def print_error(message):
  """Print an error message"""
  if Colors.supports_color():
    print(f"{Colors.RED}{Colors.BOLD}Error: {message}{Colors.END}")
  else:
    print(f"Error: {message}")


def print_success(message):
  """Print a success message"""
  if Colors.supports_color():
    print(f"{Colors.GREEN}{Colors.BOLD}{message}{Colors.END}")
  else:
    print(message)


def get_current_version(war_path, pom_path=None):
  """
  Attempt to determine the current DHIS2 version from the WAR file
  or POM file if WAR is not available
  """
  version = None

  # Try to extract version from WAR file
  if os.path.exists(war_path):
    try:
      # Create a temporary directory
      with tempfile.TemporaryDirectory() as temp_dir:
        # Extract MANIFEST.MF using jar command
        jar_cmd = ["jar", "xf", war_path, "META-INF/MANIFEST.MF"]
        try:
          result = subprocess.run(
            jar_cmd,
            cwd=temp_dir,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
          )

          # Read the MANIFEST.MF file
          manifest_path = os.path.join(temp_dir, "META-INF", "MANIFEST.MF")
          if os.path.exists(manifest_path):
            with open(manifest_path, "r") as manifest_file:
              content = manifest_file.read()
              match = re.search(
                r"Implementation-Version:\s*([^\s]+)", content
              )
              if match:
                version = match.group(1)
        except subprocess.CalledProcessError:
          print_warning("Could not extract MANIFEST.MF from WAR file")
    except Exception as e:
      print_warning(f"Error while trying to get version from WAR file: {e}")

  # Try to extract version from POM file if version is still not found
  if not version and pom_path and os.path.exists(pom_path):
    try:
      with open(pom_path, "r") as pom_file:
        content = pom_file.read()
        match = re.search(r"<version>([^<]+)</version>", content)
        if match:
          version = match.group(1)
    except Exception as e:
      print_warning(f"Error while trying to get version from POM file: {e}")

  return version


def fetch_available_versions():
  """Fetch available DHIS2 versions from the releases server"""
  print("Fetching available DHIS2 versions...")

  try:
    with urllib.request.urlopen(DHIS2_RELEASES_URL, timeout=10) as response:
      return json.loads(response.read().decode("utf-8"))
  except URLError as e:
    print_error(f"Could not fetch available versions: {e}")
    return None


def show_available_versions(versions_data):
  """Display available DHIS2 versions"""
  if not versions_data:
    return

  print_header("Available DHIS2 Versions")

  # Extract and sort versions
  supported_versions = []
  unsupported_versions = []

  for version_data in versions_data.get("versions", []):
    if version_data.get("supported"):
      supported_versions.append(version_data)
    else:
      unsupported_versions.append(version_data)

  # Sort versions by name in descending order (newest first)
  try:
    supported_versions.sort(
      key=lambda x: [int(p) for p in x.get("name", "0.0").split(".")],
      reverse=True
    )
    unsupported_versions.sort(
      key=lambda x: [int(p) for p in x.get("name", "0.0").split(".")],
      reverse=True
    )
  except (ValueError, TypeError):
    print_warning("Could not sort versions properly due to unexpected version format")

  # Display supported versions
  print("Supported Versions:")
  print("-----------------")
  for version_data in supported_versions:
    version_name = version_data.get("name", "")
    latest_patch = version_data.get("latestPatchVersion")
    release_date = version_data.get("releaseDate", "Unknown")
    jdk_version = version_data.get("jdk")

    version_str = f"  • DHIS2 {version_name}"
    if latest_patch:
      version_str += f" (Latest patch version: {version_name}.{latest_patch})"
    version_str += f" - Released: {release_date}"

    if jdk_version:
      version_str += f" - Requires Java {jdk_version}"
    elif int(version_name.split(".")[1]) >= 41:
      version_str += f" - Requires Java 17+"
    else:
      version_str += f" - Requires Java 8"

    print(version_str)

  # Display unsupported versions
  print("\nEnd-of-Support Versions:")
  print("---------------------")
  for version_data in unsupported_versions:
    version_name = version_data.get("name", "")
    release_date = version_data.get("releaseDate", "Unknown")
    jdk_version = version_data.get("jdk")

    version_str = f"  • DHIS2 {version_name} - Released: {release_date}"

    if jdk_version:
      version_str += f" - Requires Java {jdk_version}"
    elif int(version_name.split(".")[1]) >= 41:
      version_str += f" - Requires Java 17+"
    else:
      version_str += f" - Requires Java 8"

    print(version_str)


def check_java_compatibility(java_cmd, target_version=None,
    current_version=None):
  """
  Check if the installed Java version is compatible with the DHIS2 version
  Returns (is_compatible, java_version)
  """
  # Check Java existence
  try:
    java_version_output = subprocess.check_output(
      [java_cmd, "-version"], stderr=subprocess.STDOUT, universal_newlines=True
    )
  except (subprocess.SubprocessError, FileNotFoundError):
    print_error(
      f"Java not found or could not be executed. Please install Java and/or set the correct path."
    )
    return False, None

  # Extract Java version
  if match := re.search(r'version "(\d+)', java_version_output):
    # Java 9 and above
    java_version = int(match.group(1))
  elif match := re.search(r'version "1\.(\d+)', java_version_output):
    # Java 8 and below
    java_version = int(match.group(1))
  else:
    print_error("Unable to determine Java version.")
    return False, java_version_output

  # If no target version provided, use current version
  if not target_version:
    target_version = current_version

  # Set default Java requirements
  if not target_version or target_version == "local":
    print_warning(
      "Unable to detect DHIS2 version. Will use default minimum Java version requirement (Java 17)."
    )
    required_java = JAVA_MIN_VERSION_MODERN
    required_java_desc = "17 or higher"
  else:
    # Extract major.minor from DHIS2 version
    if match := re.match(r"^(\d+\.\d+)", target_version):
      dhis2_major_minor = match.group(1)
      
      # Version comparison using string comparison
      # Default to latest requirement
      required_java = JAVA_MIN_VERSION_MODERN
      required_java_desc = "17 or higher"
      
      # Check if version is less than threshold for modern Java (< 2.41)
      if dhis2_major_minor < DHIS2_VERSION_THRESHOLD_MODERN:
        # Check if version is less than middle threshold (< 2.38)
        if dhis2_major_minor < DHIS2_VERSION_THRESHOLD_MIDDLE:
          # For DHIS2 < 2.38, require Java 8
          required_java = JAVA_MIN_VERSION_LEGACY
          required_java_desc = "8"
        else:
          # For DHIS2 between 2.38 and 2.41, require Java 11+
          required_java = JAVA_MIN_VERSION_MIDDLE
          required_java_desc = "11 or higher"
      
      print(f"DHIS2 version {target_version} requires Java {required_java_desc}.")
    else:
      print_warning(
        "Unable to parse DHIS2 version format. Will use default minimum Java version requirement (Java 17)."
      )
      required_java = JAVA_MIN_VERSION_MODERN
      required_java_desc = "17 or higher"

  # Check if Java version meets requirements
  if required_java == JAVA_MIN_VERSION_LEGACY:
    # For versions < 2.38, Java 8 is required
    if java_version != required_java:
      print_error(f"Java version {java_version} is not supported.")
      print_error(f"DHIS2 version {target_version} requires Java {required_java_desc}.")
      print_error("Please install Java 8 and/or set the correct path.")
      return False, java_version
  else:
    # For other versions, minimum Java version is required
    if java_version < required_java:
      print_error(f"Java version {java_version} is not supported.")
      version_text = "unknown" if not target_version else target_version
      print_error(f"DHIS2 version {version_text} requires Java {required_java_desc}.")
      print_error(
        f"Please install a compatible Java version and/or set the correct path."
      )
      return False, java_version

  print_success(f"Java version check passed. Using Java {java_version}.")
  return True, java_version


def check_db_connections(dhis2_home_dir):
  """Check for active database connections"""
  print("Checking for active database connections...")

  # Path to dhis.conf
  dhis_conf_path = os.path.join(dhis2_home_dir, "dhis.conf")

  if not os.path.exists(dhis_conf_path):
    print_warning(f"dhis.conf not found at {dhis_conf_path}")
    print_warning(
      "Could not check database connections. Please ensure no other applications are using the database."
    )
    return True  # Continue anyway as we can't check

  # Extract database connection information
  db_url = None
  db_username = None
  db_password = None

  try:
    with open(dhis_conf_path, "r") as conf_file:
      for line in conf_file:
        line = line.strip()
        if line.startswith("connection.url="):
          db_url = line.split("=", 1)[1].strip()
        elif line.startswith("connection.username="):
          db_username = line.split("=", 1)[1].strip()
        elif line.startswith("connection.password="):
          db_password = line.split("=", 1)[1].strip()
  except Exception as e:
    print_warning(f"Could not read dhis.conf: {e}")
    print_warning(
      "Please ensure no other applications are using the database before continuing."
    )
    return True  # Continue anyway as we can't check

  # Check if we have PostgreSQL connection info
  if db_url and "postgresql" in db_url:
    # Try to extract database name from URL
    db_name_match = re.search(r"/([^?/]+)(\?|$)", db_url)
    if not db_name_match:
      print_warning("Could not extract database name from URL")
      return True

    db_name = db_name_match.group(1)

    # Check if psql is available
    try:
      subprocess.run(
        ["psql", "--version"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=True,
      )
    except (subprocess.SubprocessError, FileNotFoundError):
      print_warning(
        "psql command not available. Could not check active database connections."
      )
      print_warning(
        "Please manually ensure no other applications are using the database before continuing."
      )
      return True

    # Connect to database and check for active connections
    try:
      cmd = [
        "psql",
        "-h",
        "localhost",
        "-U",
        db_username,
        "-d",
        db_name,
        "-c",
        f"SELECT count(*) FROM pg_stat_activity WHERE datname = '{db_name}' AND pid <> pg_backend_pid();",
      ]

      env = os.environ.copy()
      env["PGPASSWORD"] = db_password

      result = subprocess.run(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        env=env,
        universal_newlines=True,
        check=True,
      )

      # Extract connection count from result
      connections = result.stdout.strip().split("\n")[2].strip()
      try:
        connections = int(connections)
      except ValueError:
        print_warning(f"Could not parse connection count: {connections}")
        return True

      if connections > 0:
        print_warning(
          f"{connections} active connections detected to the database."
        )
        print_warning(
          "It is highly recommended to ensure no other applications are using the database during upgrade."
        )
        response = (
          input("Do you want to continue anyway? (y/n): ").strip().lower()
        )
        return response == "y"
      else:
        print_success(
          "No active database connections detected. Safe to proceed."
        )
        return True
    except subprocess.SubprocessError as e:
      print_warning(f"Could not check active database connections: {e}")
      print_warning(
        "Please ensure no other applications are using the database before continuing."
      )
      return True
  else:
    print_warning("Could not determine database connection information.")
    print_warning(
      "Please manually ensure no other applications are using the database before continuing."
    )
    return True


def backup_database(dhis2_home_dir, script_dir):
  """Create a backup of the database"""
  print("Creating database backup...")

  # Path to dhis.conf
  dhis_conf_path = os.path.join(dhis2_home_dir, "dhis.conf")

  if not os.path.exists(dhis_conf_path):
    print_warning(f"dhis.conf not found at {dhis_conf_path}")
    print_warning(
      "Could not create database backup automatically. Please ensure you have a backup."
    )

    response = (
      input("Do you confirm that you've created a manual backup? (y/n): ")
      .strip()
      .lower()
    )
    return response == "y"

  # Extract database connection information
  db_url = None
  db_username = None
  db_password = None

  try:
    with open(dhis_conf_path, "r") as conf_file:
      for line in conf_file:
        line = line.strip()
        if line.startswith("connection.url="):
          db_url = line.split("=", 1)[1].strip()
        elif line.startswith("connection.username="):
          db_username = line.split("=", 1)[1].strip()
        elif line.startswith("connection.password="):
          db_password = line.split("=", 1)[1].strip()
  except Exception as e:
    print_warning(f"Could not read dhis.conf: {e}")
    print_warning(
      "Could not create database backup automatically. Please ensure you have a backup."
    )

    response = (
      input("Do you confirm that you've created a manual backup? (y/n): ")
      .strip()
      .lower()
    )
    return response == "y"

  # Check if we have PostgreSQL connection info
  if db_url and "postgresql" in db_url:
    # Try to extract database name from URL
    db_name_match = re.search(r"/([^?/]+)(\?|$)", db_url)
    if not db_name_match:
      print_warning("Could not extract database name from URL")

      response = (
        input("Do you confirm that you've created a manual backup? (y/n): ")
        .strip()
        .lower()
      )
      return response == "y"

    db_name = db_name_match.group(1)

    # Check if pg_dump is available
    try:
      subprocess.run(
        ["pg_dump", "--version"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=True,
      )
    except (subprocess.SubprocessError, FileNotFoundError):
      print_warning(
        "pg_dump command not available. Could not create database backup."
      )
      print_warning(
        "Please create a manual backup of your database before continuing."
      )

      response = (
        input("Do you confirm that you've created a manual backup? (y/n): ")
        .strip()
        .lower()
      )
      return response == "y"

    # Create backup
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_file = os.path.join(
      script_dir, f"dhis2_backup_{db_name}_{timestamp}.sql"
    )

    print(
      f"Creating PostgreSQL backup of database '{db_name}' to {backup_file}...")

    try:
      cmd = [
        "pg_dump",
        "-h",
        "localhost",
        "-U",
        db_username,
        "-d",
        db_name,
        "-f",
        backup_file,
      ]

      env = os.environ.copy()
      env["PGPASSWORD"] = db_password

      subprocess.run(
        cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env, check=True
      )

      print_success(f"Database backup created successfully: {backup_file}")

      # Compress the backup if gzip is available
      try:
        subprocess.run(
          ["gzip", "--version"],
          stdout=subprocess.PIPE,
          stderr=subprocess.PIPE,
          check=True,
        )
        print("Compressing backup file...")
        subprocess.run(["gzip", backup_file], check=True)
        print_success(f"Compressed backup file: {backup_file}.gz")
      except (subprocess.SubprocessError, FileNotFoundError):
        # gzip not available, continue without compression
        pass

      return True
    except subprocess.SubprocessError as e:
      print_error(f"Database backup failed: {e}")
      return False
  else:
    print_warning(
      "Could not determine database connection information or database is not PostgreSQL."
    )
    print_warning(
      "Please create a manual backup of your database before continuing."
    )

    response = (
      input("Do you confirm that you've created a manual backup? (y/n): ")
      .strip()
      .lower()
    )
    return response == "y"


def check_server_running(pid_file):
  """Check if the server is running"""
  if os.path.exists(pid_file):
    try:
      with open(pid_file, "r") as f:
        pid = int(f.read().strip())

      # Check if process is running
      try:
        os.kill(pid, 0)  # Send signal 0 to check if process exists
        return True, pid
      except OSError:
        # Process not found but PID file exists, clean up
        try:
          os.remove(pid_file)
        except:
          pass
        return False, None
    except:
      return False, None

  return False, None


def stop_server(pid_file):
  """Stop the DHIS2 server if it's running"""
  running, pid = check_server_running(pid_file)

  if not running:
    print("DHIS2 server is not running.")
    return True

  print(f"Stopping DHIS2 server (PID: {pid})...")

  # Try graceful shutdown first
  try:
    os.kill(pid, 15)  # SIGTERM

    # Wait for the process to stop
    timeout = 60
    for i in range(timeout):
      try:
        os.kill(pid, 0)  # Check if process still exists
        print(f"Waiting for server to shutdown... ({i + 1}/{timeout})")
        time.sleep(1)
      except OSError:
        # Process has stopped
        break

    # If still running, force kill
    try:
      os.kill(pid, 0)
      print("Server not responding, sending SIGKILL...")
      os.kill(pid, 9)  # SIGKILL
      time.sleep(1)
    except OSError:
      # Process has stopped
      pass

    # Clean up PID file
    try:
      os.remove(pid_file)
    except:
      pass

    print("DHIS2 server stopped.")
    return True
  except OSError as e:
    print_error(f"Error stopping server: {e}")
    return False


def auto_upgrade(
    current_version,
    versions_data,
    dhis2_home_dir,
    dhis2_war_path,
    pid_file,
    script_dir,
    java_cmd,
):
  """Automatic upgrade to the latest appropriate version"""
  if not versions_data:
    print_error("Failed to fetch version information")
    return False

  # Parse current version
  try:
    if not current_version:
      print_warning(
        "Unable to detect current DHIS2 version. Manual version selection is recommended."
      )
      return manual_version_upgrade(
        versions_data,
        dhis2_home_dir,
        dhis2_war_path,
        pid_file,
        script_dir,
        java_cmd,
      )

    version_parts = current_version.split(".")
    current_major = int(version_parts[0])
    current_minor = int(version_parts[1])
    current_patch = (
      int(re.match(r"(\d+)", version_parts[2]).group(1))
      if len(version_parts) > 2
      else 0
    )
  except (ValueError, IndexError, AttributeError):
    print_warning(f"Unable to parse current version format: {current_version}")
    print_warning("Manual version selection is recommended.")
    return manual_version_upgrade(
      versions_data,
      dhis2_home_dir,
      dhis2_war_path,
      pid_file,
      script_dir,
      java_cmd,
    )

  print_header("Available Upgrade Paths")

  # Initialize upgrade options
  upgrade_options = []

  # 1. Check for patch/hotfix upgrades within same major.minor
  same_version_group = f"{current_major}.{current_minor}"

  # Find matching version in the data
  for version_data in versions_data.get("versions", []):
    if version_data.get("name") == same_version_group:
      try:
        latest_patch = int(version_data.get("latestPatchVersion", 0))
        latest_hotfix = int(version_data.get("latestHotfixVersion", 0))

        # Check if there's a newer patch or hotfix version
        if latest_patch > current_patch or (
            latest_patch == current_patch and latest_hotfix > 0
        ):
          latest_version = f"{same_version_group}.{latest_patch}"
          if latest_hotfix > 0:
            latest_version = f"{latest_version}.{latest_hotfix}"

          # Get download URL
          download_url = ""
          for v in versions_data.get("versions", []):
            if v.get("name") == latest_version:
              download_url = v.get("url", "")

          if download_url:
            print(
              f"1. RECOMMENDED: Update to {latest_version} (patch/hotfix update)"
            )
            print(
              "   This is a low-risk update with bug fixes and security patches."
            )
            upgrade_options.append(
              (latest_version, download_url, "patch/hotfix update")
            )
        else:
          print("1. No patch/hotfix updates available for your version")
      except (ValueError, TypeError):
        print_warning(f"Could not process version data for {same_version_group} due to unexpected format")
        print("1. No patch/hotfix updates available for your version")
      break
  else:
    print("1. No patch/hotfix updates available for your version")

  # 2. Find next minor version upgrade
  next_minor = f"{current_major}.{current_minor + 1}"
  next_minor_data = None

  for version_data in versions_data.get("versions", []):
    if version_data.get("name") == next_minor:
      next_minor_data = version_data
      break

  if next_minor_data:
    next_minor_patch = next_minor_data.get("latestPatchVersion", 0)
    next_minor_version = f"{next_minor}.{next_minor_patch}"

    # Get download URL
    download_url = next_minor_data.get("latestStableUrl", "")

    if download_url:
      print(f"2. Update to {next_minor_version} (minor version upgrade)")
      print(
        "   This update includes new features along with bug fixes and improvements."
      )
      print("   May require database schema updates.")
      upgrade_options.append(
        (next_minor_version, download_url, "minor version upgrade")
      )
  else:
    print("2. No minor version upgrade available")

  # 3. Find latest stable version
  supported_versions = [
    v for v in versions_data.get("versions", []) if v.get("supported")
  ]
  supported_versions.sort(
    key=lambda x: [int(p) for p in x.get("name", "0.0").split(".")],
    reverse=False
  )

  if supported_versions:
    latest_supported = supported_versions[-1]
    latest_supported_name = latest_supported.get("name")
    latest_patch = latest_supported.get("latestPatchVersion", 0)
    latest_stable = f"{latest_supported_name}.{latest_patch}"

    # Get download URL
    download_url = latest_supported.get("latestStableUrl", "")

    if (
        download_url
        and latest_supported_name != same_version_group
        and latest_supported_name != next_minor
    ):
      print(f"3. Update to {latest_stable} (major upgrade - CAUTION)")
      print("   This is the latest stable version with major new features.")
      print(
        "   Major upgrades require careful testing and may have significant changes."
      )
      upgrade_options.append(
        (latest_stable, download_url, "major version upgrade")
      )
  else:
    print("3. Could not determine latest stable version")

  # Let user select an option
  if not upgrade_options:
    print("\nNo upgrade options found for your current version.")
    print(
      "You might be on the latest version already, or you can try a manual upgrade."
    )
    return False

  print("\nSelect an upgrade option:")
  for i, (version, _, description) in enumerate(upgrade_options, 1):
    print(f"{i}. Update to version {version} ({description})")

  try:
    choice = int(input("\nEnter your choice (1-3, or 0 to exit): "))
    if choice == 0:
      return False

    if 1 <= choice <= len(upgrade_options):
      selected_version, download_url, _ = upgrade_options[choice - 1]
      print(f"You've selected to upgrade to version {selected_version}")
      return perform_upgrade(
        selected_version,
        download_url,
        dhis2_home_dir,
        dhis2_war_path,
        pid_file,
        script_dir,
        java_cmd,
      )
    else:
      print_error("Invalid choice")
      return False
  except ValueError:
    print_error("Invalid input, please enter a number")
    return False


def manual_version_upgrade(
    versions_data, dhis2_home_dir, dhis2_war_path, pid_file, script_dir,
    java_cmd
):
  """Upgrade to manually specified version"""
  if not versions_data:
    print_error("Failed to fetch version information")
    return False

  # Display available versions
  show_available_versions(versions_data)

  # Ask for specific version
  target_version = input(
    "\nEnter the specific version you want to install (e.g., 2.38.1, or 0 to exit): "
  ).strip()

  if not target_version or target_version == "0":
    print("Upgrade aborted.")
    return False

  # Find download URL for this version
  download_url = None
  for version_data in versions_data.get("versions", []):
    if version_data.get("name") == target_version:
      download_url = version_data.get("latestStableUrl", "")
      break

  if not download_url:
    print_error(f"Could not find download URL for version {target_version}")
    print_error("Check if the version number is correct.")
    return False

  return perform_upgrade(
    target_version,
    download_url,
    dhis2_home_dir,
    dhis2_war_path,
    pid_file,
    script_dir,
    java_cmd,
  )


def local_build_upgrade(dhis2_home_dir, dhis2_war_path, pid_file, script_dir,
    java_cmd):
  """Upgrade with locally built WAR file"""
  print("Using locally built WAR file for upgrade.")
  print("This option is primarily for developers.")

  # Warning
  response = input(
    "Have you already built the application? (y/n): ").strip().lower()
  if response != "y":
    print("Building the application...")
    try:
      subprocess.run(
        [
          "mvn",
          "clean",
          "package",
          "-DskipTests",
          "--activate-profiles",
          "embedded",
        ],
        check=True,
      )
    except subprocess.SubprocessError as e:
      print_error(f"Build failed: {e}")
      print_error("Upgrade aborted.")
      return False

  # Check if the WAR file exists
  if not os.path.exists(dhis2_war_path):
    print_error(f"DHIS2 WAR file not found at {dhis2_war_path}")
    return False

  # Perform the upgrade without download
  return perform_upgrade(
    "local", "", dhis2_home_dir, dhis2_war_path, pid_file, script_dir, java_cmd
  )


def perform_upgrade(
    version,
    download_url,
    dhis2_home_dir,
    dhis2_war_path,
    pid_file,
    script_dir,
    java_cmd,
):
  """Common upgrade procedure"""
  print_header("Starting Upgrade Process")

  # Safety checks
  print("Performing safety checks...")

  # 1. Check Java version for target DHIS2 version
  if version != "local":
    print(f"Checking Java compatibility for DHIS2 version {version}...")
    compatible, _ = check_java_compatibility(java_cmd, version)
    if not compatible:
      return False
  else:
    # For local upgrade, check current Java version
    compatible, _ = check_java_compatibility(java_cmd)
    if not compatible:
      return False

  # 2. Check if database is in use by other clients
  if not check_db_connections(dhis2_home_dir):
    print("Upgrade aborted due to database connection issues.")
    return False

  # 3. Create backup
  do_backup = (
    input(
      "\nCreating database backup is strongly recommended before upgrading.\nDo you want to create a database backup? (y/n): "
    )
    .strip()
    .lower()
  )
  if do_backup == "y":
    if not backup_database(dhis2_home_dir, script_dir):
      print_error("Database backup failed. Upgrade aborted.")
      return False
  else:
    print_warning("Proceeding without database backup.")
    confirm = (
      input("Are you sure you want to continue without a backup? (y/n): ")
      .strip()
      .lower()
    )
    if confirm != "y":
      print("Upgrade aborted.")
      return False

  # Stop the server if it's running
  running, _ = check_server_running(pid_file)
  if running:
    print("Stopping the server before upgrade...")
    if not stop_server(pid_file):
      print_error(
        "Failed to stop the server. Please stop it manually before continuing."
      )
      return False

  # Create a backup of the current WAR file if it exists
  if os.path.exists(dhis2_war_path):
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_war = f"{dhis2_war_path}.backup_{timestamp}"
    print(f"Creating backup of current WAR file: {backup_war}")
    try:
      shutil.copy2(dhis2_war_path, backup_war)
    except Exception as e:
      print_error(f"Failed to backup current WAR file: {e}")
      print_error("Upgrade aborted.")
      return False

  # If we have a download URL, download the new version
  if download_url:
    with tempfile.TemporaryDirectory() as temp_dir:
      war_file = os.path.join(temp_dir, "dhis.war")

      print(f"Downloading DHIS2 version {version} from {download_url}...")

      try:
        # Download the file
        with (
          urllib.request.urlopen(download_url) as response,
          open(war_file, "wb") as out_file,
        ):
          shutil.copyfileobj(response, out_file)

        # Check if download was successful
        if not os.path.exists(war_file) or os.path.getsize(war_file) == 0:
          print_error("Failed to download WAR file or file is empty.")
          return False

        # Move the downloaded WAR file to the correct location
        print("Installing new DHIS2 version...")
        shutil.move(war_file, dhis2_war_path)
      except Exception as e:
        print_error(f"Error during download: {e}")
        return False
  else:
    # For local builds, the WAR file is already in place
    print(f"Using locally built WAR file: {dhis2_war_path}")

  # Print success message and instructions
  print_header("Upgrade Completed")

  if version != "local":
    print_success(f"DHIS2 has been upgraded to version {version}.")
  else:
    print_success("DHIS2 has been upgraded with the locally built WAR file.")

  print("\nRecommendations:")
  print("1. Start the server with: ./dhis2-server.sh start")
  print("2. Check the logs for any errors with: ./dhis2-server.sh logs")
  print(
    "3. If you encounter issues, you can restore the previous version from backup."
  )
  print("\nThe previous WAR file was backed up and can be restored if needed.")

  return True


def main():
  """Main function to handle command line arguments and perform the upgrade"""
  parser = argparse.ArgumentParser(description="DHIS2 Upgrade Tool")
  parser.add_argument(
    "--dhis2-home",
    dest="dhis2_home_dir",
    default="/opt/dhis2",
    help="Path to the DHIS2 home directory (default: /opt/dhis2)",
  )
  parser.add_argument(
    "--war-path",
    dest="dhis2_war_path",
    required=True,
    help="Path to the DHIS2 WAR file",
  )
  parser.add_argument(
    "--pid-file", dest="pid_file", required=True, help="Path to the PID file"
  )
  parser.add_argument(
    "--java",
    dest="java_cmd",
    default="java",
    help="Java executable path (default: java)",
  )
  parser.add_argument(
    "--script-dir",
    dest="script_dir",
    required=True,
    help="Path to the script directory",
  )
  parser.add_argument(
    "--pom-path", dest="pom_path", help="Path to the POM file (optional)"
  )

  args = parser.parse_args()

  print_header("DHIS2 Server Smart Upgrade")

  # Check if the server is running
  running, pid = check_server_running(args.pid_file)
  if running:
    print_warning(f"Server is currently running (PID: {pid}).")
    response = (
      input(
        "Do you want to continue? Server will be stopped for upgrade (y/n): ")
      .strip()
      .lower()
    )
    if response != "y":
      print("Upgrade aborted.")
      return

  # Detect current version
  current_version = get_current_version(args.dhis2_war_path, args.pom_path)
  if current_version:
    print(f"Current DHIS2 version: {current_version}")
  else:
    print_warning("Unable to detect current DHIS2 version.")
    print_warning("You can still proceed with upgrade to a specific version.")
    current_version = "unknown"

  # Fetch available versions
  versions_data = fetch_available_versions()

  # Determine upgrade mode
  print("\nAvailable upgrade modes:")
  print("  1. Download and upgrade to latest stable version")
  print("  2. Download and upgrade to a specific version")
  print("  3. Upgrade with locally built WAR file (developer option)")
  print("  4. View available versions and exit")

  try:
    upgrade_mode = int(input("\nSelect upgrade mode (1-4): "))

    if upgrade_mode == 1:
      auto_upgrade(
        current_version,
        versions_data,
        args.dhis2_home_dir,
        args.dhis2_war_path,
        args.pid_file,
        args.script_dir,
        args.java_cmd,
      )
    elif upgrade_mode == 2:
      manual_version_upgrade(
        versions_data,
        args.dhis2_home_dir,
        args.dhis2_war_path,
        args.pid_file,
        args.script_dir,
        args.java_cmd,
      )
    elif upgrade_mode == 3:
      local_build_upgrade(
        args.dhis2_home_dir,
        args.dhis2_war_path,
        args.pid_file,
        args.script_dir,
        args.java_cmd,
      )
    elif upgrade_mode == 4:
      if versions_data:
        show_available_versions(versions_data)
    else:
      print_error("Invalid option. Upgrade aborted.")
  except ValueError:
    print_error("Invalid input. Please enter a number between 1 and 4.")


if __name__ == "__main__":
  main()
