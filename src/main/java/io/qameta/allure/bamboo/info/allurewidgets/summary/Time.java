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

public class Time extends AbstractSummary {

    private static final long serialVersionUID = 1L;

    @SerializedName("start")
    @Expose
    private Long start;
    @SerializedName("stop")
    @Expose
    private Long stop;
    @SerializedName("duration")
    @Expose
    private Integer duration;
    @SerializedName("minDuration")
    @Expose
    private Integer minDuration;
    @SerializedName("maxDuration")
    @Expose
    private Integer maxDuration;
    @SerializedName("sumDuration")
    @Expose
    private Integer sumDuration;

    /**
     * No args constructor for use in serialization.
     */
    public Time() {
        // empty.
    }

    /**
     * @param duration    duration
     * @param sumDuration sum of duration
     * @param minDuration min duration
     * @param stop        stop time
     * @param start       start time
     * @param maxDuration max duration
     */
    public Time(final Long start,
                final Long stop,
                final Integer duration,
                final Integer minDuration,
                final Integer maxDuration,
                final Integer sumDuration) {
        super();
        this.start = start;
        this.stop = stop;
        this.duration = duration;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
        this.sumDuration = sumDuration;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(final Long start) {
        this.start = start;
    }

    public Time withStart(final Long start) {
        this.start = start;
        return this;
    }

    public Long getStop() {
        return stop;
    }

    public void setStop(final Long stop) {
        this.stop = stop;
    }

    public Time withStop(final Long stop) {
        this.stop = stop;
        return this;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(final Integer duration) {
        this.duration = duration;
    }

    public Time withDuration(final Integer duration) {
        this.duration = duration;
        return this;
    }

    public Integer getMinDuration() {
        return minDuration;
    }

    public void setMinDuration(final Integer minDuration) {
        this.minDuration = minDuration;
    }

    public Time withMinDuration(final Integer minDuration) {
        this.minDuration = minDuration;
        return this;
    }

    public Integer getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(final Integer maxDuration) {
        this.maxDuration = maxDuration;
    }

    public Time withMaxDuration(final Integer maxDuration) {
        this.maxDuration = maxDuration;
        return this;
    }

    public Integer getSumDuration() {
        return sumDuration;
    }

    public void setSumDuration(final Integer sumDuration) {
        this.sumDuration = sumDuration;
    }

    public Time withSumDuration(final Integer sumDuration) {
        this.sumDuration = sumDuration;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(100);
        sb.append(Time.class.getName()).append('@')
                .append(Integer.toHexString(System.identityHashCode(this)))
                .append("[start=")
                .append(toStringOrNullPlaceholder(this.start))
                .append(",stop=")
                .append(toStringOrNullPlaceholder(this.stop))
                .append(",duration=")
                .append(toStringOrNullPlaceholder(this.duration))
                .append(",minDuration=")
                .append(toStringOrNullPlaceholder(this.minDuration))
                .append(",maxDuration=")
                .append(toStringOrNullPlaceholder(this.maxDuration))
                .append(",sumDuration=")
                .append(toStringOrNullPlaceholder(this.sumDuration))
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
