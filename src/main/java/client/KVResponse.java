package client;

import java.io.Serializable;

public class KVResponse implements Serializable {
    Object response;
    String value;
    public KVResponse() {
    }

    public KVResponse(Object response) {
        this.response = response;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public static KVResponse ok() {
        return new KVResponse("ok");
    }

    public static KVResponse fail() {
        return new KVResponse("fail");
    }
}
