package rpc;

import Entity.Request;
import Entity.Response;

public interface Client {
    Response send(Request request);

    Response send(Request request, int timeout);

}
