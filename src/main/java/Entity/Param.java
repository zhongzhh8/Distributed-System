package Entity;

import java.io.Serializable;

public class Param implements Serializable {
    //任期号
    public long term;
    //被请求者ID(ip:selfPort)
    public String serverId;

    public void setTerm(long term) {
        this.term = term;
    }

    public long getTerm() {
        return term;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getServerId() {
        return serverId;
    }
}
