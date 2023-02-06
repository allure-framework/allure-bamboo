
package io.qameta.allure.bamboo.info.allurewidgets.summary;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Time {

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
     * No args constructor for use in serialization
     */
    public Time() {
    }

    /**
     * @param duration
     * @param sumDuration
     * @param minDuration
     * @param stop
     * @param start
     * @param maxDuration
     */
    public Time(Long start, Long stop, Integer duration, Integer minDuration, Integer maxDuration, Integer sumDuration) {
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

    public void setStart(Long start) {
        this.start = start;
    }

    public Time withStart(Long start) {
        this.start = start;
        return this;
    }

    public Long getStop() {
        return stop;
    }

    public void setStop(Long stop) {
        this.stop = stop;
    }

    public Time withStop(Long stop) {
        this.stop = stop;
        return this;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Time withDuration(Integer duration) {
        this.duration = duration;
        return this;
    }

    public Integer getMinDuration() {
        return minDuration;
    }

    public void setMinDuration(Integer minDuration) {
        this.minDuration = minDuration;
    }

    public Time withMinDuration(Integer minDuration) {
        this.minDuration = minDuration;
        return this;
    }

    public Integer getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(Integer maxDuration) {
        this.maxDuration = maxDuration;
    }

    public Time withMaxDuration(Integer maxDuration) {
        this.maxDuration = maxDuration;
        return this;
    }

    public Integer getSumDuration() {
        return sumDuration;
    }

    public void setSumDuration(Integer sumDuration) {
        this.sumDuration = sumDuration;
    }

    public Time withSumDuration(Integer sumDuration) {
        this.sumDuration = sumDuration;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Time.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("start");
        sb.append('=');
        sb.append(((this.start == null) ? "<null>" : this.start));
        sb.append(',');
        sb.append("stop");
        sb.append('=');
        sb.append(((this.stop == null) ? "<null>" : this.stop));
        sb.append(',');
        sb.append("duration");
        sb.append('=');
        sb.append(((this.duration == null) ? "<null>" : this.duration));
        sb.append(',');
        sb.append("minDuration");
        sb.append('=');
        sb.append(((this.minDuration == null) ? "<null>" : this.minDuration));
        sb.append(',');
        sb.append("maxDuration");
        sb.append('=');
        sb.append(((this.maxDuration == null) ? "<null>" : this.maxDuration));
        sb.append(',');
        sb.append("sumDuration");
        sb.append('=');
        sb.append(((this.sumDuration == null) ? "<null>" : this.sumDuration));
        sb.append(',');
        if (sb.charAt((sb.length() - 1)) == ',') {
            sb.setCharAt((sb.length() - 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
