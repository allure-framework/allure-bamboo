
package io.qameta.allure.bamboo.info.allurewidgets.summary;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Statistic {

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
     * No args constructor for use in serialization
     *
     */
    public Statistic() {
    }

    /**
     *
     * @param broken
     * @param total
     * @param failed
     * @param passed
     * @param skipped
     * @param unknown
     */
    public Statistic(Integer failed, Integer broken, Integer skipped, Integer passed, Integer unknown, Integer total) {
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

    public void setFailed(Integer failed) {
        this.failed = failed;
    }

    public Statistic withFailed(Integer failed) {
        this.failed = failed;
        return this;
    }

    public Integer getBroken() {
        return broken;
    }

    public void setBroken(Integer broken) {
        this.broken = broken;
    }

    public Statistic withBroken(Integer broken) {
        this.broken = broken;
        return this;
    }

    public Integer getSkipped() {
        return skipped;
    }

    public void setSkipped(Integer skipped) {
        this.skipped = skipped;
    }

    public Statistic withSkipped(Integer skipped) {
        this.skipped = skipped;
        return this;
    }

    public Integer getPassed() {
        return passed;
    }

    public void setPassed(Integer passed) {
        this.passed = passed;
    }

    public Statistic withPassed(Integer passed) {
        this.passed = passed;
        return this;
    }

    public Integer getUnknown() {
        return unknown;
    }

    public void setUnknown(Integer unknown) {
        this.unknown = unknown;
    }

    public Statistic withUnknown(Integer unknown) {
        this.unknown = unknown;
        return this;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Statistic withTotal(Integer total) {
        this.total = total;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Statistic.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("failed");
        sb.append('=');
        sb.append(((this.failed == null)?"<null>":this.failed));
        sb.append(',');
        sb.append("broken");
        sb.append('=');
        sb.append(((this.broken == null)?"<null>":this.broken));
        sb.append(',');
        sb.append("skipped");
        sb.append('=');
        sb.append(((this.skipped == null)?"<null>":this.skipped));
        sb.append(',');
        sb.append("passed");
        sb.append('=');
        sb.append(((this.passed == null)?"<null>":this.passed));
        sb.append(',');
        sb.append("unknown");
        sb.append('=');
        sb.append(((this.unknown == null)?"<null>":this.unknown));
        sb.append(',');
        sb.append("total");
        sb.append('=');
        sb.append(((this.total == null)?"<null>":this.total));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
