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
package io.qameta.allure.bamboo.info.allurewidgets.summary;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Summary extends AbstractSummary {

    private static final long serialVersionUID = 1L;

    @SerializedName("reportName")
    @Expose
    private String reportName;
    @SerializedName("testRuns")
    @Expose
    private List<Object> testRuns;
    @SerializedName("statistic")
    @Expose
    private Statistic statistic;
    @SerializedName("time")
    @Expose
    private Time time;

    /**
     * No args constructor for use in serialization.
     */
    public Summary() {
        //empty
    }

    /**
     * @param statistic  statistic
     * @param reportName report name
     * @param time       time
     * @param testRuns   test runs
     */
    public Summary(final String reportName,
                   final List<Object> testRuns,
                   final Statistic statistic,
                   final Time time) {
        super();
        this.reportName = reportName;
        this.testRuns = testRuns;
        this.statistic = statistic;
        this.time = time;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(final String reportName) {
        this.reportName = reportName;
    }

    public Summary withReportName(final String reportName) {
        this.reportName = reportName;
        return this;
    }

    public List<Object> getTestRuns() {
        return testRuns;
    }

    public void setTestRuns(final List<Object> testRuns) {
        this.testRuns = testRuns;
    }

    public Summary withTestRuns(final List<Object> testRuns) {
        this.testRuns = testRuns;
        return this;
    }

    public Statistic getStatistic() {
        return statistic;
    }

    public void setStatistic(final Statistic statistic) {
        this.statistic = statistic;
    }

    public Summary withStatistic(final Statistic statistic) {
        this.statistic = statistic;
        return this;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(final Time time) {
        this.time = time;
    }

    public Summary withTime(final Time time) {
        this.time = time;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(100);
        sb.append(Summary.class.getName()).append('@')
                .append(Integer.toHexString(System.identityHashCode(this)))
                .append("[reportName=")
                .append(toStringOrNullPlaceholder(this.reportName))
                .append(",testRuns=")
                .append(toStringOrNullPlaceholder(this.testRuns))
                .append(",statistic=")
                .append(toStringOrNullPlaceholder(this.statistic))
                .append(",time=")
                .append(toStringOrNullPlaceholder(this.time))
                .append(COMMA_CHAR);
        final int lastIndex = sb.length() - 1;
        if (sb.charAt(lastIndex) == COMMA_CHAR) {
            sb.setCharAt(lastIndex, ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
