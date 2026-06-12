#!/bin/bash
set -e

# This script processes DHIS2 surefire test reports and commits
# the output to the dhis2-test-reports repository for visualization.

echo "Starting test reports processing..."

apt-get update && apt-get install -y git gnupg2

if [ -n "$GPG_PRIVATE_KEY" ]; then
    echo "$GPG_PRIVATE_KEY" | gpg --batch --import

    # derive the signing key ID and the committer identity from the imported key's
    # own UID, so the committer email always matches the key GitHub verifies against.
    GPG_KEY_ID=$(gpg --list-secret-keys --with-colons | awk -F: '/^sec:/ {print $5; exit}')
    GPG_UID=$(gpg --list-secret-keys --with-colons | awk -F: '/^uid:/ {print $10; exit}')
    GPG_EMAIL="${GPG_UID##*<}"
    export GIT_AUTHOR_NAME="${GPG_UID%% <*}"
    export GIT_AUTHOR_EMAIL="${GPG_EMAIL%>}"
    export GIT_COMMITTER_NAME="$GIT_AUTHOR_NAME"
    export GIT_COMMITTER_EMAIL="$GIT_AUTHOR_EMAIL"
    echo "Signing commits as $GIT_AUTHOR_NAME <$GIT_AUTHOR_EMAIL> with key $GPG_KEY_ID"

    # unlock the secret key once so gpg-agent caches the passphrase, letting the
    # later commit sign non-interactively without prompting inside the container.
    echo "prime" | gpg --batch --yes --pinentry-mode loopback --passphrase "$GPG_PASSPHRASE" \
        --local-user "$GPG_KEY_ID" --detach-sign -o /dev/null

    git config --global user.signingkey "$GPG_KEY_ID"
    git config --global commit.gpgsign true
    git config --global gpg.program gpg
fi

git config --global user.name "$GIT_AUTHOR_NAME"
git config --global user.email "$GIT_AUTHOR_EMAIL"
if [ -n "$GITHUB_TOKEN" ]; then
    git config --global url."https://$GITHUB_TOKEN@github.com/".insteadOf "https://github.com/"
fi

cd /tmp
git clone https://github.com/dhis2/dhis2-test-reports.git
cd dhis2-test-reports

# Verify the local reports directory exists and has content
if [ ! -d "/reports" ] || [ -z "$(ls -A /reports 2>/dev/null)" ]; then
    echo "Warning: No reports found. Exiting."
    exit 0
fi


# Process the reports: if DB_TYPE is empty, it will default to postgres
python3 ./scripts/process-surefire-reports.py --reports-dir /reports --output-dir reports/core/$TEST_TYPE $DB_TYPE

git add .
if git diff --cached --quiet; then
    echo "No new test reports to commit."
else
    commit_message="Add test results from $(date -Iseconds)"
    echo "Committing with message: $commit_message"
    git commit -m "$commit_message"
    git log --show-signature -1 || true
    git push origin main
fi

echo "Test reports processing completed."

