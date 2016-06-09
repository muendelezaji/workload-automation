#    Copyright 2014-2016 ARM Limited
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import os
import logging
import re
import time

from wlauto import AndroidUiAutoBenchmark, Parameter


class MsWord(AndroidUiAutoBenchmark):

    name = 'msword'
    package = 'com.microsoft.office.word'
    # version = '16.0.6925.1017'
    activity = 'com.microsoft.office.apphost.LaunchActivity'
    description = 'This is an placeholder description'

    view = [
        package + '/com.microsoft.office.word.WordActivity',
        package + '/com.microsoft.office.apphost.LaunchActivity',
    ]

    parameters = [
        Parameter('dumpsys_enabled', kind=bool, default=True,
                  description='''
                  If ``True``, dumpsys captures will be carried out during the test run.
                  The output is piped to log files which are then pulled from the phone.
                  '''),
        Parameter('login_email', kind=str, mandatory=True,
                  description='Office 365 account to use when logging into the application'),
        Parameter('login_pass', kind=str, mandatory=True,
                  description='Password associated with the Microsoft account'),
        Parameter('test_type', kind=str, mandatory=True, allowed_values=['create', 'existing'],
                  description='''
                  When set to ``existing`` will use a locally pushed Microsoft Word document.
                  Using ``create`` will create a new document in the app. In both cases, the
                  name of the document to push or create and save must be specified.
                  '''),
        Parameter('document_name', kind=str, mandatory=True,
                  description='Document to push to device, or name to save as when created in app'),
    ]

    instrumentation_log = '{}_instrumentation.log'.format(name)

    def __init__(self, device, **kwargs):
        super(MsWord, self).__init__(device, **kwargs)
        self.run_timeout = 300
        self.output_file = os.path.join(self.device.working_directory, self.instrumentation_log)
        self.local_dir = self.dependencies_directory
        # Use Android documents folder as it is one of the default folders that appear in
        # Word's file picker, and not WA's working directory. Improves test reliability by
        # not having to navigate around the filesystem to locate pushed file.
        self.device_dir = os.path.join(self.device.working_directory, '..', 'Documents')

    def validate(self):
        super(MsWord, self).validate()
        self.uiauto_params['package'] = self.package
        self.uiauto_params['output_dir'] = self.device.working_directory
        self.uiauto_params['output_file'] = self.output_file
        self.uiauto_params['dumpsys_enabled'] = self.dumpsys_enabled
        self.uiauto_params['login_email'] = self.login_email
        self.uiauto_params['login_pass'] = self.login_pass
        self.uiauto_params['test_type'] = self.test_type
        self.uiauto_params['document_name'] = self.document_name

    def setup(self, context):
        super(MsWord, self).setup(context)
        # push existing document
        if self.test_type == 'existing':
            for entry in os.listdir(self.local_dir):
                if entry == self.document_name:
                    self.device.push_file(os.path.join(self.local_dir, self.document_name),
                                          os.path.join(self.device_dir, self.document_name),
                                          timeout=60)

    def update_result(self, context):
        super(MsWord, self).update_result(context)
        if self.dumpsys_enabled:
            self.device.pull_file(self.output_file, context.output_directory)
            result_file = os.path.join(context.output_directory, self.instrumentation_log)
            # pull instrumentation data
            with open(result_file, 'r') as wfh:
                regex = re.compile(r'(?P<key>\w+)\s+(?P<value1>\d+)\s+(?P<value2>\d+)\s+(?P<value3>\d+)')
                for line in wfh:
                    match = regex.search(line)
                    if match:
                        context.result.add_metric((match.group('key') + "_start"),
                                                  match.group('value1'), units='ms')
                        context.result.add_metric((match.group('key') + "_finish"),
                                                  match.group('value2'), units='ms')
                        context.result.add_metric((match.group('key') + "_duration"),
                                                  match.group('value3'), units='ms')

    def teardown(self, context):
        super(MsWord, self).teardown(context)
        # delete pushed document
        if self.test_type == 'existing':
            for entry in self.device.listdir(self.device_dir):
                if entry == self.document_name:
                    self.device.delete_file(os.path.join(self.device_dir, entry))
        # pull logs
        for entry in self.device.listdir(self.device.working_directory):
            if entry.startswith(self.name) and entry.endswith(".log"):
                self.logger.debug("Pulling file '{}'".format(entry))
                self.device.pull_file(os.path.join(self.device.working_directory, entry), context.output_directory)
                self.device.delete_file(os.path.join(self.device.working_directory, entry))
