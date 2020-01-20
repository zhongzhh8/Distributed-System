package client;

import java.io.Serializable;

public class KVRequest implements Serializable {
    public static int PUT = 0;
    public static int GET = 1;
    public static int DEL = 2;
    //request的类型，PUT or GET or DEL
    private int type;

    private String key;

    private String value;

    public KVRequest() {
    }

    public KVRequest(int type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
