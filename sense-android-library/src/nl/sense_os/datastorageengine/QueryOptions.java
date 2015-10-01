package nl.sense_os.datastorageengine;

/**
 * Object with query options for DataPoint.
 * All options are optional and can be left null.
 */
public class QueryOptions implements Cloneable{
    private Long startDate = null;
    private Long endDate = null;
    private Integer limit = null;
    private DatabaseHandler.SORT_ORDER sortOrder = null;
    private Boolean existsInCS= null;
    private Boolean requiresDeletionInCS = null;

    public QueryOptions() {};

    public QueryOptions(Long startDate, Long endDate, Boolean existsInCS, Boolean requiresDeletionInCS, Integer limit, DatabaseHandler.SORT_ORDER sortOrder){
        this.startDate = startDate;
        this.endDate = endDate;
        this.existsInCS = existsInCS;
        this.requiresDeletionInCS = requiresDeletionInCS;
        this.limit = limit;
        this.sortOrder = sortOrder;
    }

    public Long getStartDate() { return startDate; }

    public void setStartDate(Long startDate) { this.startDate = startDate; }

    public Long getEndDate() { return endDate; }

    public void setEndDate(Long endDate) { this.endDate = endDate; }

    public Boolean getExistsInCS() { return existsInCS; }

    public void setExistsInCS(Boolean existsInCS) { this.existsInCS = existsInCS; }

    public Boolean getRequiresDeletionInCS() { return requiresDeletionInCS; }

    public void setRequiresDeletionInCS(Boolean requiresDeletionInCS) { this.requiresDeletionInCS = requiresDeletionInCS; }

    public Integer getLimit() { return limit; }

    public void setLimit(Integer limit) { this.limit = limit; }

    public DatabaseHandler.SORT_ORDER getSortOrder() { return sortOrder; }

    public void setSortOrder(DatabaseHandler.SORT_ORDER sortOrder) { this.sortOrder = sortOrder; }

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
            if(o.requiresDeletionInCS != null)
                merged.requiresDeletionInCS = o.requiresDeletionInCS;
            if(o.limit != null)
                merged.limit = o.limit;
            if(o.sortOrder != null)
                merged.sortOrder = o.sortOrder;
        }
        return merged;
    }

}
