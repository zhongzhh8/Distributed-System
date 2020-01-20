package rpc;

import Entity.Request;
import Entity.Response;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientImpl implements Client {
    public static Logger logger = LoggerFactory.getLogger(ClientImpl.class.getName());

    public final static RpcClient client = new RpcClient();
    static{
        client.init();
    }

    @Override
    public Response send(Request request) {
        return send(request, 200000);
    }

    @Override
    public Response send(Request request, int timeout) {
        Response response = null;
        try {
            response = (Response) client.invokeSync(request.getUrl(),request,timeout);
        } catch (RemotingException e) {
            e.printStackTrace();
            logger.info("rpc RaftRemotingException ");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return response;
    }
}
