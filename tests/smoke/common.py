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
import os
import subprocess
import unittest
import zipfile

import sys

DEVNULL = open(os.devnull, 'wb')


def _do_exec(command, silent=True):
    out = sys.stdout
    if silent:
        out = DEVNULL
    if subprocess.call(command, stdout=out, stderr=out) != 0:
        raise Exception("command " + " ".join(command) + " failed")


def _do_exec_expect_fail(command, silent=True):
    out = sys.stdout
    if silent:
        out = DEVNULL
    if subprocess.call(command, stdout=out, stderr=out) == 0:
        raise Exception("command " + " ".join(command) + " should have failed")


class BazelKotlinTestCase(unittest.TestCase):
    _pkg = None

    def _target(self, target_name):
        return "//%s:%s" % (self._pkg, target_name)

    def _bazel_bin(self, file):
        return "bazel-bin/" + self._pkg + "/" + file

    def _open_bazel_bin(self, file):
        return open(self._bazel_bin(file))

    def _query(self, query, implicits=False):
        res = []
        q = ['bazel', 'query', query]
        if not implicits:
            q.append('--noimplicit_deps')
        self._last_command = " ".join(q)

        p = subprocess.Popen(self._last_command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        for line in p.stdout.readlines():
            res.append(line.replace("\n", ""))
        ret = p.wait()
        if ret != 0:
            raise Exception("error (%d) evaluating query: %s" % (ret, self._last_command))
        else:
            return res

    def libQuery(self, label, implicits=False):
        return self._query('\'kind("java_import|.*_library", deps(%s))\'' % label, implicits)

    def assertJarContains(self, jar, *files):
        curr = ""
        try:
            for f in files:
                curr = f
                jar.getinfo(f)
        except Exception as ex:
            raise Exception("jar does not contain file [%s]" % curr)

    def buildJar(self, target, silent=True):
        _do_exec(["bazel", "build", self._target(target)], silent)

    def buildJarExpectingFail(self, target, silent=True):
        _do_exec_expect_fail(["bazel", "build", self._target(target)], silent)

    def buildJarGetZipFile(self, name, extension):
        jar_file = name + "." + extension
        self.buildJar(jar_file)
        return zipfile.ZipFile(self._open_bazel_bin(jar_file))

    def buildLaunchExpectingSuccess(self, target, command="run"):
        self.buildJar(target, silent=False)
        res = subprocess.call(["bazel", command, self._target(target)], stdout=sys.stdout, stderr=sys.stdout)
        if not res == 0:
            raise Exception("could not launch jar [%s]" % target)
