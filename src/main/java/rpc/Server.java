package rpc;

import Entity.Request;
import Entity.Response;

public interface Server {
    void start();

    void stop();
    //处理请求，投票请求or附加日志请求
    Response handleRequestImpl(Request request);

}
