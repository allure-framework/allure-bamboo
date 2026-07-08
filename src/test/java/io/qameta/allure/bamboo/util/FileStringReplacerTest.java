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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static io.qameta.allure.bamboo.TestSupport.attachText;
import static io.qameta.allure.bamboo.TestSupport.step;
import static org.assertj.core.api.Assertions.assertThat;

public class FileStringReplacerTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void itShouldInsertReplacementLiterallyWhenItContainsRegexMetacharacters() throws Exception {
        final String prefix = ".brand { background: ";
        final String suffix = " }";
        // Custom logo URLs and build names are user-controlled and can legitimately contain '$'
        // (group reference) or '\' (escape); an unquoted replacement throws or corrupts the file.
        final String replacement = "url(https://cdn.example.com/logo$1_v2\\final.svg)";

        final Path cssFile = step("write a stylesheet containing a url('old') token", () -> {
            final Path file = tempFolder.newFile("styles.css").toPath();
            Files.write(file, (prefix + "url('old')" + suffix).getBytes(StandardCharsets.UTF_8));
            return file;
        });

        step("replace the token with a value containing regex metacharacters", () ->
                FileStringReplacer.replaceInFile(cssFile, Pattern.compile("url\\('.+'\\)"), replacement));

        step("verify the metacharacters were written verbatim, not interpreted", () -> {
            final String content = new String(Files.readAllBytes(cssFile), StandardCharsets.UTF_8);
            attachText("Rewritten stylesheet", content);
            assertThat(content).isEqualTo(prefix + replacement + suffix);
        });
    }
}
