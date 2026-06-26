/*
 *  Copyright 2016-2024 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.bamboo.util;

import org.junit.Test;

import static io.qameta.allure.bamboo.TestSupport.attachText;
import static io.qameta.allure.bamboo.TestSupport.step;
import static io.qameta.allure.bamboo.util.ExceptionUtil.stackTraceToString;
import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionUtilTest {

    @Test
    public void itShouldPrintStackTraceIntoString() throws Exception {
        final String trace = step("convert a thrown runtime exception into a stack trace string", () -> {
            try {
                throw new RuntimeException("Print me please");
            } catch (RuntimeException e) {
                return stackTraceToString(e);
            }
        });

        step("verify the rendered trace preserves the message and calling method", () -> {
            attachText("Rendered stack trace", trace);
            assertThat(trace).contains("RuntimeException: Print me please");
            assertThat(trace).contains("itShouldPrintStackTraceIntoString(ExceptionUtilTest.java:");
        });
    }
}
