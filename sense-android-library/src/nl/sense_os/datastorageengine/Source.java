package nl.sense_os.datastorageengine;


import org.json.JSONObject;

public interface Source {

    String getUserId();

    void setUserId(String userId) throws DatabaseHandlerException;

    String getCsId();

    void setCsId(String csId) throws DatabaseHandlerException;

    String getUuid();

    void setUuid(String uuid) throws DatabaseHandlerException;

    JSONObject getMeta();

    void setMeta(JSONObject meta) throws DatabaseHandlerException;

    String getName();

    void setName(String name) throws DatabaseHandlerException;

    String getId();

    void setId(String id) throws DatabaseHandlerException;

    boolean isSynced();

    void setSynced(boolean synced) throws DatabaseHandlerException;

}
