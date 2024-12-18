#!/bin/bash
set -e -x
have=$1
want=$2
got=$($have | tr -d "\n")
if [[ "${got}" != "${want}" ]]; then
  echo "Want: |$want|"
  echo "Got: |$got|"
  exit 1
fi