package nl.sense_os.datastorageengine;

import java.util.HashMap;
import java.util.Map;

/**
 * Object with query options for DataPoint.
 * All options are optional and can be left null.
 */
public class QueryOptions implements Cloneable{

    public enum SORT_ORDER {ASC, DESC};
    public enum INTERVAL {MINUTE, HOUR, DAY, WEEK};

    private Long mStartTime = null;
    private Long mEndTime = null;
    private Boolean mExistsInRemote = null;
    private Integer mLimit = null;
    private SORT_ORDER mSortOrder = null;
    private INTERVAL mInterval = null;

    public QueryOptions() {};

    public QueryOptions(Long startTime, Long endTime, Boolean existsInRemote, Integer limit, SORT_ORDER sortOrder){
        this.mStartTime = startTime;
        this.mEndTime = endTime;
        this.mExistsInRemote = existsInRemote;
        this.mLimit = limit;
        this.mSortOrder = sortOrder;
    }

    public Long getStartTime() { return mStartTime; }

    public void setStartTime(Long mStartTime) { this.mStartTime = mStartTime; }

    public Long getEndTime() { return mEndTime; }

    public void setEndTime(Long endTime) { this.mEndTime = endTime; }

    public Boolean getExistsInRemote() { return mExistsInRemote; }

    public void setExistsInRemote(Boolean existsInRemote) { this.mExistsInRemote = existsInRemote; }

    public Integer getLimit() { return mLimit; }

    public void setLimit(Integer limit) { this.mLimit = limit; }

    public INTERVAL getInterval() {
        return mInterval;
    }

    public void setInterval(INTERVAL interval) {
        this.mInterval = interval;
    }

    public SORT_ORDER getSortOrder() { return mSortOrder; }

    public void setSortOrder(SORT_ORDER sortOrder) { this.mSortOrder = sortOrder; }

    public QueryOptions clone() { return merge(this); }

    /**
     * Merge two or more options objects.
     * The fields in `options` which are `null` will be ignored.
     * @param options
     * @return Returns a cloned QueryOptions object
     */
    public static QueryOptions merge(QueryOptions... options) {
        QueryOptions merged = new QueryOptions();
        for(QueryOptions o: options){
            if(o.mStartTime != null)
                merged.mStartTime = o.mStartTime;
            if(o.mEndTime != null)
                merged.mEndTime = o.mEndTime;
            if(o.mExistsInRemote != null)
                merged.mExistsInRemote = o.mExistsInRemote;
            if(o.mLimit != null)
                merged.mLimit = o.mLimit;
            if(o.mSortOrder != null)
                merged.mSortOrder = o.mSortOrder;
        }
        return merged;
    }

    /**
     * Create a query parameter string like "?start_time=123&end_time=456&mLimit=1000&sort=asc".
     * Query options with null values are ignored.
     * @return
     */
    public String toQueryParams () {
        Map<String, String> params = new HashMap<>();

        if (mStartTime != null) {params.put("start_time", mStartTime.toString()); }
        if (mEndTime != null)   {params.put("end_time",   mEndTime.toString()); }
        if (mLimit != null)     {params.put("limit",      mLimit.toString()); }
        if (mInterval != null)  {params.put("interval",   mInterval.name().toLowerCase()); }
        if (mSortOrder != null) {params.put("sort",       mSortOrder.name().toLowerCase()); }

        String queryParams = "";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String separator = queryParams.isEmpty() ? "?" : "&";
            queryParams += separator + entry.getKey() + "=" + entry.getValue();
        }
        return queryParams;
    }

}
