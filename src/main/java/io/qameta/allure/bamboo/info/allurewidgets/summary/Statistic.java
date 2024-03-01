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

public class Statistic extends AbstractSummary {

    private static final long serialVersionUID = 1L;

    @SerializedName("failed")
    @Expose
    private Integer failed;
    @SerializedName("broken")
    @Expose
    private Integer broken;
    @SerializedName("skipped")
    @Expose
    private Integer skipped;
    @SerializedName("passed")
    @Expose
    private Integer passed;
    @SerializedName("unknown")
    @Expose
    private Integer unknown;
    @SerializedName("total")
    @Expose
    private Integer total;

    /**
     * No args constructor for use in serialization.
     */
    public Statistic() {
        // empty
    }

    public Statistic(final Integer failed,
                     final Integer broken,
                     final Integer skipped,
                     final Integer passed,
                     final Integer unknown,
                     final Integer total) {
        super();
        this.failed = failed;
        this.broken = broken;
        this.skipped = skipped;
        this.passed = passed;
        this.unknown = unknown;
        this.total = total;
    }

    public Integer getFailed() {
        return failed;
    }

    public void setFailed(final Integer failed) {
        this.failed = failed;
    }

    public Statistic withFailed(final Integer failed) {
        this.failed = failed;
        return this;
    }

    public Integer getBroken() {
        return broken;
    }

    public void setBroken(final Integer broken) {
        this.broken = broken;
    }

    public Statistic withBroken(final Integer broken) {
        this.broken = broken;
        return this;
    }

    public Integer getSkipped() {
        return skipped;
    }

    public void setSkipped(final Integer skipped) {
        this.skipped = skipped;
    }

    public Statistic withSkipped(final Integer skipped) {
        this.skipped = skipped;
        return this;
    }

    public Integer getPassed() {
        return passed;
    }

    public void setPassed(final Integer passed) {
        this.passed = passed;
    }

    public Statistic withPassed(final Integer passed) {
        this.passed = passed;
        return this;
    }

    public Integer getUnknown() {
        return unknown;
    }

    public void setUnknown(final Integer unknown) {
        this.unknown = unknown;
    }

    public Statistic withUnknown(final Integer unknown) {
        this.unknown = unknown;
        return this;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(final Integer total) {
        this.total = total;
    }

    public Statistic withTotal(final Integer total) {
        this.total = total;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(100);
        sb.append(Statistic.class.getName())
                .append('@').append(Integer.toHexString(System.identityHashCode(this)))
                .append("[failed=")
                .append(toStringOrNullPlaceholder(this.failed))
                .append(",broken=")
                .append(toStringOrNullPlaceholder(this.broken))
                .append(",skipped=")
                .append(toStringOrNullPlaceholder(this.skipped))
                .append(",passed=")
                .append(toStringOrNullPlaceholder(this.passed))
                .append(",unknown=")
                .append(toStringOrNullPlaceholder(this.unknown))
                .append(",total=")
                .append(toStringOrNullPlaceholder(this.total))
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
