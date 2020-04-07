# Copyright 2018 The Bazel Authors. All rights reserved.
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
import argparse
import os
import re
import shutil
import tempfile
import zipfile


def _is_jar(jar):
    if not os.path.exists(jar):
        raise argparse.ArgumentTypeError("jar:{0} does not exist".format(jar))
    else:
        return zipfile.ZipFile(jar)


def _extract_root_entry(jar, filename_pattern, output_path, touch=False):
    """
    Extracts a root entry from a jar. and write it to a path.

    output_path is absolute and the basename is used to extract the entry from the jar.

    :param jar: The jar from which to make the extraction.
    :param filename_pattern: Regular expression to match when searching for the file in the jar.
    :param output_path: An absolute file path to where the entry should be written.
    :param touch: Should the file be touched if it was not found in the jar.
    """
    target = None
    for filename in jar.namelist():
        if filename_pattern.match(filename):
            target = filename
            break

    if not target:
        if touch:
            f = open(output_path, 'a')
            f.close()
            return
        else:
            raise FileNotFoundError("No file matching {0} was found in jar".format(filename_pattern))

    # Extract the target file to a temporary location.
    temp_dir = tempfile.gettempdir()
    temp_file = os.path.join(temp_dir, os.path.basename(target))
    jar.extract(target, path=temp_dir)

    # Move the temp file into the final output location.
    shutil.move(temp_file, output_path)


def _main(p):
    args = p.parse_args()
    _extract_root_entry(args.jar, args.import_pattern, args.import_out)
    for (p, e) in zip(args.aux_pattern, args.aux_out):
        _extract_root_entry(args.jar, p, e, touch=True)


parser = argparse.ArgumentParser()

parser.add_argument("--jar", type=_is_jar, required=True)
parser.add_argument("--import_pattern", required=True, type=re.compile,
                    help="regular expression to match when searching the jar for the KotlinJS file")
parser.add_argument("--import_out", required=True, help="path where the extracted KotlinJS import should be stored")
parser.add_argument(
    "--aux_pattern", nargs="*", type=re.compile,
    help="""regular expressions to match when searching the jar for additional files""")
parser.add_argument(
    "--aux_out", nargs="*",
    help="""paths where additonal files from the jar should be stored, if the files do not exist these paths are touched.""")

_main(parser)
