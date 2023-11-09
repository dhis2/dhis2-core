#!/usr/bin/env bash

# Example: ./backport.sh 'feature/im-play-nightly' 47bc4b2ade1baffbb6693ae149bca6126c2f352e 2.40 2.39 2.38 2.37

set -euo pipefail

if ! command -v git &> /dev/null; then
    echo "git could not be found."
    exit 1
fi

if ! command -v gh &> /dev/null; then
    echo "git could not be found."
    exit 1
fi

if [[ -z "${1:-}" ]]; then
  echo "No base branch specified."
  exit 1
fi
base_branch="$1"

if [[ -z "${2:-}" ]]; then
  echo "No commit specified."
  exit 1
fi
commit="$2"

shift 2

if [[ -z "${*:-}" ]]; then
  echo "No backport branches specified."
  exit 1
fi

pr_id=$(gh pr list --search "$commit" --state merged --head "$base_branch" --json number --jq '.[0].number')
pr_url=$(gh pr view "$pr_id" --json url --jq '.url')
pr_title=$(gh pr view "$pr_id" --json title --jq '.title')
pr_reviewer=$(gh pr view "$pr_id" --json reviews --jq '.reviews | map(.author.login) | join(",")')

branches=("$@")
for branch in "${branches[@]}"; do
    echo git checkout "$branch"
    echo git pull origin "$branch"
    echo git checkout -b "${base_branch}_${branch}"
    echo git cherry-pick "$commit"
    echo git push origin "${base_branch}_${branch}"
    echo gh pr create --head "${base_branch}_${branch}" --title "backport: $pr_title" --body "$pr_url" --reviewer "$pr_reviewer"
done
