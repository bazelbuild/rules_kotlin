#!/bin/bash
set -e -x

want='{data={foo={name=baz!, age=42}}}'
got=$($1 | tr -d "\n")
if [[ "$got" -eq "$want" ]]; then
  echo "Failed to execute"
  exit 1
fi