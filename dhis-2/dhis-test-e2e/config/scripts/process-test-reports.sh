#!/bin/bash
set -e

# This script processes DHIS2 surefire test reports and commits
# the output to the dhis2-test-reports repository for visualization.

echo "Starting test reports processing..."

apt-get update && apt-get install -y git openssh-client

if [ -n "$SSH_SIGNING_KEY" ]; then
    mkdir -p ~/.ssh
    chmod 700 ~/.ssh
    printf '%s\n' "$SSH_SIGNING_KEY" > ~/.ssh/signing_key
    chmod 600 ~/.ssh/signing_key

    # strip the passphrase from this throwaway copy so the container can sign
    # non-interactively; the decrypted key only exists in the ephemeral CI container.
    ssh-keygen -p -P "$SSH_SIGNING_PASSPHRASE" -N "" -f ~/.ssh/signing_key

    # build an allowed-signers entry so the post-commit verification below can
    # confirm the signature locally against the committer email.
    ssh-keygen -y -f ~/.ssh/signing_key > ~/.ssh/signing_key.pub
    echo "$GIT_COMMITTER_EMAIL $(cat ~/.ssh/signing_key.pub)" > ~/.ssh/allowed_signers

    git config --global gpg.format ssh
    git config --global user.signingkey ~/.ssh/signing_key
    git config --global commit.gpgsign true
    git config --global gpg.ssh.allowedSignersFile ~/.ssh/allowed_signers
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

