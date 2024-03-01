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
package io.qameta.allure.bamboo.info;

import java.util.HashMap;

/**
 * Add testRun info to reports.
 */
public class AddTestRunInfo extends AbstractAddInfo {

    private static final long serialVersionUID = 1L;

    public static final String TESTRUN_JSON = "testrun.json";

    private final String name;

    private final long start;

    private final long stop;

    public AddTestRunInfo(final String name,
                          final long start,
                          final long stop) {
        this.name = name;
        this.start = start;
        this.stop = stop;
    }

    @Override
    protected Object getData() {
        final HashMap<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("start", start);
        data.put("stop", stop);
        return data;
    }

    @Override
    protected String getFileName() {
        return TESTRUN_JSON;
    }
}
