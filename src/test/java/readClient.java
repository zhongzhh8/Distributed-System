import Entity.LogEntry;
import Entity.Request;
import Entity.Response;
import client.KVRequest;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.Client;
import rpc.ClientImpl;

import java.util.List;

public class readClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(readClient.class);
    private final static Client client = new ClientImpl();
    static String baseaddr = "localhost:880";

    public static void main(String[] args) throws InterruptedException {
        Thread.sleep(3000);
        try {
            for(int i = 1; i<=5;i++){
                String addr = baseaddr + String.valueOf(i);
                KVRequest obj = new KVRequest();
                obj.setKey("a");
                obj.setType(KVRequest.GET);
                Request<KVRequest> r = new Request<KVRequest>();
                r.setObject(obj);
                r.setUrl(addr);
                r.setType(Request.CLIENT_REQ);
                Response<String> response;
                try {
                    response = client.send(r);
                    LOGGER.info("get请求: {}, 接收结点地址 : {}", obj.getKey() + "=" + response.getValue(), r.getUrl());
                } catch (Exception e) {
                    LOGGER.warn("客户端发送请求失败");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
