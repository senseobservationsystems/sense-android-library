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

    private Long startDate = null;
    private Long endDate = null;
    private Boolean existsInCS= null;
    private Integer limit = null;
    private SORT_ORDER sortOrder = null;
    private INTERVAL interval = null;

    public QueryOptions() {};

    public QueryOptions(Long startDate, Long endDate, Boolean existsInCS, Integer limit, SORT_ORDER sortOrder){
        this.startDate = startDate;
        this.endDate = endDate;
        this.existsInCS = existsInCS;
        this.limit = limit;
        this.sortOrder = sortOrder;
    }

    public Long getStartDate() { return startDate; }

    public void setStartDate(Long startDate) { this.startDate = startDate; }

    public Long getEndDate() { return endDate; }

    public void setEndDate(Long endDate) { this.endDate = endDate; }

    public Boolean getExistsInCS() { return existsInCS; }

    public void setExistsInCS(Boolean existsInCS) { this.existsInCS = existsInCS; }

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
            if(o.startDate != null)
                merged.startDate = o.startDate;
            if(o.endDate != null)
                merged.endDate = o.endDate;
            if(o.existsInCS != null)
                merged.existsInCS = o.existsInCS;
            if(o.limit != null)
                merged.limit = o.limit;
            if(o.sortOrder != null)
                merged.sortOrder = o.sortOrder;
        }
        return merged;
    }

    /**
     * Create a query parameter string like "?start_date=123&end_date=456&limit=1000&sort=asc".
     * Query options with null values are ignored.
     * @return
     */
    public String toQueryParams () {
        Map<String, String> params = new HashMap<>();

        if (startDate != null) {params.put("start_date", startDate.toString()); }
        if (endDate != null)   {params.put("end_date",   endDate.toString()); }
        if (limit != null)     {params.put("limit",      limit.toString()); }
        if (interval != null)  {params.put("interval",   interval.name().toLowerCase()); }
        if (sortOrder != null) {params.put("sort",       interval.name().toLowerCase()); }

        String queryParams = "";
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String separator = queryParams.isEmpty() ? "?" : "&";
            queryParams += separator + entry.getKey() + "=" + entry.getValue();
        }
        return queryParams;
    }

}
