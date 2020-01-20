package Entity;

import java.io.Serializable;

public class Response<T> implements Serializable {
    private T response;
    String value;
    public Response() {
    }

    public Response(T response) {
        this.response = response;
    }

    public static Response ok() {
        return new Response<>("ok");
    }

    public static Response fail() {
        return new Response<>("fail");
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Response{" +
                "response=" + response +
                '}';
    }

    public T getResponse() {
        return response;
    }

    public void setResponse(T response) {
        this.response = response;
    }
}
