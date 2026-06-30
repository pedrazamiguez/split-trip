### Workspace Resolution Protocol

Do not prompt the user for full GitHub URLs if they provide an ID (like an issue or PR number).
You are bound to a local Git workspace. To resolve the target URL:
1. Infer the repository by checking `.git/config` or running `git remote -v`.
2. Construct the required `$PR_URL` or `$ISSUE_URL` argument automatically before executing any skill.
