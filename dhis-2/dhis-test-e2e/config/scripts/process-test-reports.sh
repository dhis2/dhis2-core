#!/bin/bash
set -e

# Process DHIS2 test reports and commit to dhis2-test-reports repository
echo "Starting test reports processing..."

# Install required packages
echo "Installing dependencies..."
apt-get update && apt-get install -y git gnupg2

# Setup GPG if private key is provided
if [ -n "$GPG_PRIVATE_KEY" ]; then
    echo "Setting up GPG signing..."
    echo "$GPG_PRIVATE_KEY" | base64 -d | gpg --batch --import
    echo "$GPG_PASSPHRASE" | gpg --batch --yes --pinentry-mode loopback --passphrase-fd 0 --sign-key "$GPG_KEY_ID"
    git config --global user.signingkey "$GPG_KEY_ID"
    git config --global commit.gpgsign true
    git config --global gpg.program gpg
    echo "GPG signing configured"
fi

# Setup git
echo "Configuring git..."
git config --global user.name "$GIT_AUTHOR_NAME"
git config --global user.email "$GIT_AUTHOR_EMAIL"

# Setup GitHub authentication
if [ -n "$GITHUB_TOKEN" ]; then
    echo "Setting up GitHub authentication..."
    git config --global url."https://$GITHUB_TOKEN@github.com/".insteadOf "https://github.com/"
fi

# Clone repository and process reports
echo "Cloning dhis2-test-reports repository..."
cd /tmp
git clone https://github.com/dhis2/dhis2-test-reports.git
cd dhis2-test-reports

# Verify reports directory exists and has content
if [ ! -d "/reports" ] || [ -z "$(ls -A /reports 2>/dev/null)" ]; then
    echo "Warning: No reports found in /reports directory"
    exit 0
fi

echo "Found reports directory with $(ls /reports | wc -l) files"

# Process the reports
echo "Processing surefire reports..."
python3 ./scripts/process-surefire-reports.py --reports-dir /reports --output-dir reports/core/$TEST_TYPE $DB_TYPE  # Id DB_TYPE is empty, it will default to postgres

# Commit and push changes
echo "Committing changes..."
git add .

if git diff --cached --quiet; then
    echo "No staged changes to commit"
else
    commit_message="Add test results from $(date -Iseconds)"
    echo "Committing with message: $commit_message"
    git commit -m "$commit_message"
    
    echo "Pushing to main..."
    git push origin main
    echo "Successfully pushed test results to dhis2-test-reports"
fi

echo "Test reports processing completed"
