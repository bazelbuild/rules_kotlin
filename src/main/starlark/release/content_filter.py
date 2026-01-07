#!/usr/bin/env python3
# Copyright 2020 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Filter file content based on RELEASE-CONTENT tags.

This script extracts only content between RELEASE-CONTENT-START and
RELEASE-CONTENT-END markers, stripping everything else including the
marker lines themselves.
"""

import argparse
import sys

RELEASE_START = "# RELEASE-CONTENT-START"
RELEASE_END = "# RELEASE-CONTENT-END"


def filter_for_release(content: str) -> str:
    """Extract only content within RELEASE-CONTENT tags.

    Args:
        content: The full file content

    Returns:
        Filtered content containing only lines between RELEASE-CONTENT tags.
        If no tags are found, returns the original content unchanged.
    """
    lines = content.split('\n')
    result = []
    in_release_block = False
    has_release_tags = RELEASE_START in content

    # If no release tags, return content as-is (backwards compatibility)
    if not has_release_tags:
        return content

    for line in lines:
        stripped = line.strip()

        if stripped == RELEASE_START:
            in_release_block = True
            continue
        elif stripped == RELEASE_END:
            in_release_block = False
            continue

        if in_release_block:
            result.append(line)

    # Join with newlines and ensure file ends with newline if original did
    output = '\n'.join(result)
    if content.endswith('\n') and output and not output.endswith('\n'):
        output += '\n'

    return output


def main():
    parser = argparse.ArgumentParser(
        description="Filter file content based on RELEASE-CONTENT tags"
    )
    parser.add_argument("--input", required=True, help="Input file path")
    parser.add_argument("--output", required=True, help="Output file path")
    args = parser.parse_args()

    # Try to read as text; if it fails (binary file), copy as-is
    try:
        with open(args.input, 'r', encoding='utf-8') as f:
            content = f.read()
        filtered = filter_for_release(content)
        with open(args.output, 'w', encoding='utf-8') as f:
            f.write(filtered)
    except UnicodeDecodeError:
        # Binary file - copy unchanged
        import shutil
        shutil.copy2(args.input, args.output)


if __name__ == "__main__":
    main()
