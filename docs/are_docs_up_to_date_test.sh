#!/bin/bash

function fail {
  echo >&2 "#####   $1   ######"
  exit 1
}

current="$1"
new="$2"
[[ -f $new ]] || fail "missing new docs"
[[ -f $current ]] || fail "missing current docs"

diff -u "$1" "$2" || fail "kotlin docs are out of date"
