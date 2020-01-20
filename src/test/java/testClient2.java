import Entity.LogEntry;
import Entity.Request;
import Entity.Response;
import client.KVRequest;
import com.alipay.remoting.exception.RemotingException;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.Client;
import rpc.ClientImpl;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class testClient2 {
    private static final Logger LOGGER = LoggerFactory.getLogger(testClient2.class);
    private final static Client client = new ClientImpl();
    static String addr = "localhost:8801";
    static List<String> list = Lists.newArrayList("localhost:8801", "localhost:8802", "localhost:8803","localhost:8804","localhost:8805");
    public static void main(String[] args) throws RemotingException, InterruptedException {
        Thread.sleep(3000);
        try {
            KVRequest obj1 = new KVRequest();
            obj1.setKey("a");
            obj1.setType(KVRequest.GET);
            Request<KVRequest> r1 = new Request<KVRequest>();
            r1.setObject(obj1);
            r1.setUrl(addr);
            r1.setType(Request.CLIENT_REQ);
            Response<String> response1;
            try {
                response1 = client.send(r1);
                KVRequest obj2 = new KVRequest();
                obj2.setKey("a");
                if(response1.getValue()==null){             //为空，设为0
                    obj2.setValue("0");
                }else{                                      //不为空，加1
                    obj2.setValue(String.valueOf(Integer.valueOf(response1.getValue())+1));
                }
                obj2.setType(KVRequest.PUT);
                Request<KVRequest> r2 = new Request<KVRequest>();
                r2.setObject(obj2);
                r2.setUrl(addr);
                r2.setType(Request.CLIENT_REQ);
                Response<String> response2;
                response2 = client.send(r2);
                LOGGER.info("put请求: {}, 接收结点地址 : {}", "key: "+obj2.getKey() + " value: " + obj2.getValue(), r2.getUrl());
            } catch (Exception e) {
                LOGGER.warn("客户端发送请求失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
