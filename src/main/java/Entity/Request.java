package Entity;

import java.io.Serializable;

public class Request<T> implements Serializable {
    /** 请求投票 */
    public static final int VOTE = 0;
    //附加日志
    public static final int ENTRIES = 1;
    //客户端
    public static final int CLIENT_REQ = 2;
    //配置变更add
    public static final int CHANGE_CONFIG_ADD = 3;
    //配置变更remove
    public static final int CHANGE_CONFIG_REMOVE = 4;

    //请求类型
    private int type = -1;

    private T object;

    private String url;

    public Request(){
    }

    public Request(int type, T object, String url) {
        this.type = type;
        this.object = object;
        this.url = url;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
