/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.testing

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.util.GradleVersion
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

class SecurityManagerIntegrationTest extends AbstractIntegrationSpec {
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    def "should not hang when running with security manager"() {
        given:
        buildFile << """ 
apply plugin:"java"

${mavenCentralRepository()}

dependencies {
    testCompile 'junit:junit:4.12'
}
"""
        file('src/test/java/SecurityManagerTest.java') << '''
import java.security.AccessControlException;

public class SecurityManagerTest {
    @org.junit.Test
    public void testSeqManagerNOTWorking() throws Exception {
        System.setSecurityManager(new SecurityManager());

        try {
            System.setProperty("TestProperty", "value");
        } catch (AccessControlException ex) {
            System.out.println(ex);
        }
        System.setProperty("AnotherProperty", "value");
    }
}
'''

        expect:
        // This test causes the test process to exit ungracefully without closing connections.  This can sometimes
        // cause connection errors to show up in stderr.
        executer.withStackTraceChecksDisabled()
        fails('test')
        failure.assertHasCause("""Process 'Gradle Test Executor 1' finished with non-zero exit value 1
This problem might be caused by incorrect test process configuration.
Please refer to the test execution section in the user guide at https://docs.gradle.org/${GradleVersion.current().version}/userguide/java_plugin.html#sec:test_execution""")
    }
}
