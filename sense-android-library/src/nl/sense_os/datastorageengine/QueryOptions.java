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

    private Long startTime = null;
    private Long endTime = null;
    private Boolean existsInRemote = null;
    private Integer limit = null;
    private SORT_ORDER sortOrder = null;
    private INTERVAL interval = null;

    public QueryOptions() {};

    public QueryOptions(Long startTime, Long endTime, Boolean existsInRemote, Integer limit, SORT_ORDER sortOrder){
        this.startTime = startTime;
        this.endTime = endTime;
        this.existsInRemote = existsInRemote;
        this.limit = limit;
        this.sortOrder = sortOrder;
    }

    public Long getStartTime() { return startTime; }

    public void setStartTime(Long startTime) { this.startTime = startTime; }

    public Long getEndTime() { return endTime; }

    public void setEndTime(Long endTime) { this.endTime = endTime; }

    public Boolean getExistsInRemote() { return existsInRemote; }

    public void setExistsInRemote(Boolean existsInRemote) { this.existsInRemote = existsInRemote; }

    public Integer getLimit() { return limit; }

    public void setLimit(Integer limit) { this.limit = limit; }

    public INTERVAL getInterval() {
        return interval;
    }

    public void setInterval(INTERVAL interval) {
        this.interval = interval;
    }

    public SORT_ORDER getSortOrder() { return sortOrder; }

    public void setSortOrder(SORT_ORDER sortOrder) { this.sortOrder = sortOrder; }

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
            if(o.startTime != null)
                merged.startTime = o.startTime;
            if(o.endTime != null)
                merged.endTime = o.endTime;
            if(o.existsInRemote != null)
                merged.existsInRemote = o.existsInRemote;
            if(o.limit != null)
                merged.limit = o.limit;
            if(o.sortOrder != null)
                merged.sortOrder = o.sortOrder;
        }
        return merged;
    }

    /**
     * Create a query parameter string like "?start_time=123&end_time=456&limit=1000&sort=asc".
     * Query options with null values are ignored.
     * @return
     */
    public String toQueryParams () {
        Map<String, String> params = new HashMap<>();

        if (startTime != null) {params.put("start_time", startTime.toString()); }
        if (endTime != null)   {params.put("end_time",   endTime.toString()); }
        if (limit != null)     {params.put("limit",      limit.toString()); }
        if (interval != null)  {params.put("interval",   interval.name().toLowerCase()); }
        if (sortOrder != null) {params.put("sort",       sortOrder.name().toLowerCase()); }

        String queryParams = "";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String separator = queryParams.isEmpty() ? "?" : "&";
            queryParams += separator + entry.getKey() + "=" + entry.getValue();
        }
        return queryParams;
    }

}
