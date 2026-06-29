#!/bin/bash

# Fetch changes and prune deleted remote branches
git fetch -p

# List branches, find the ones marked 'gone', and delete them
# We use -D to force delete, assuming you trust the remote status
git branch -vv | grep ": gone]" | awk '{print ($1 == "*" || $1 == "+") ? $2 : $1}' | xargs git branch -D
