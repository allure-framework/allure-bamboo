
package io.qameta.allure.bamboo.info.allurewidgets.summary;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Summary {

    @SerializedName("reportName")
    @Expose
    private String reportName;
    @SerializedName("testRuns")
    @Expose
    private List<Object> testRuns = null;
    @SerializedName("statistic")
    @Expose
    private Statistic statistic;
    @SerializedName("time")
    @Expose
    private Time time;

    /**
     * No args constructor for use in serialization
     */
    public Summary() {
    }

    /**
     * @param statistic
     * @param reportName
     * @param time
     * @param testRuns
     */
    public Summary(String reportName, List<Object> testRuns, Statistic statistic, Time time) {
        super();
        this.reportName = reportName;
        this.testRuns = testRuns;
        this.statistic = statistic;
        this.time = time;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public Summary withReportName(String reportName) {
        this.reportName = reportName;
        return this;
    }

    public List<Object> getTestRuns() {
        return testRuns;
    }

    public void setTestRuns(List<Object> testRuns) {
        this.testRuns = testRuns;
    }

    public Summary withTestRuns(List<Object> testRuns) {
        this.testRuns = testRuns;
        return this;
    }

    public Statistic getStatistic() {
        return statistic;
    }

    public void setStatistic(Statistic statistic) {
        this.statistic = statistic;
    }

    public Summary withStatistic(Statistic statistic) {
        this.statistic = statistic;
        return this;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        this.time = time;
    }

    public Summary withTime(Time time) {
        this.time = time;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Summary.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("reportName");
        sb.append('=');
        sb.append(((this.reportName == null) ? "<null>" : this.reportName));
        sb.append(',');
        sb.append("testRuns");
        sb.append('=');
        sb.append(((this.testRuns == null) ? "<null>" : this.testRuns));
        sb.append(',');
        sb.append("statistic");
        sb.append('=');
        sb.append(((this.statistic == null) ? "<null>" : this.statistic));
        sb.append(',');
        sb.append("time");
        sb.append('=');
        sb.append(((this.time == null) ? "<null>" : this.time));
        sb.append(',');
        if (sb.charAt((sb.length() - 1)) == ',') {
            sb.setCharAt((sb.length() - 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

}
