package rpc;

import Entity.Request;
import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;

public abstract class UserProcessor<T> extends AbstractUserProcessor<T> {
    public void handleRequest(BizContext bizContext, AsyncContext asyncContext, T t) {

    }

    @Override
    public String interest() {
        return Request.class.getName();
    }
}
