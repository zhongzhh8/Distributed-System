import Entity.LogEntry;
import Entity.Request;
import Entity.Response;
import client.KVRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.Client;
import rpc.ClientImpl;

public class deleteClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(deleteClient.class);
    private final static Client client = new ClientImpl();
    static String addr = "localhost:8804";

    public static void main(String[] args) throws InterruptedException {
        Thread.sleep(3000);
        try {
            KVRequest obj = new KVRequest();
            obj.setKey("a");
            obj.setType(KVRequest.DEL);
            Request<KVRequest> r = new Request<KVRequest>();
            r.setObject(obj);
            r.setUrl(addr);
            r.setType(Request.CLIENT_REQ);
            Response<String> response;
            try {
                response = client.send(r);
            } catch (Exception e) {
                LOGGER.warn("客户端发送请求失败");
            }
            LOGGER.info("DEL请求: {}, 接收结点地址 : {}", "key: "+obj.getKey() + " value: " + obj.getValue(), r.getUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
