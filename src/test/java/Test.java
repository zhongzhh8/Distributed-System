import raft.Node;
import raft.NodeConfig;
import raft.NodeImpl;

import java.util.Arrays;

public class Test {

    public static void main(String[] args) throws Throwable {
        System.out.println(System.getProperty("serverPort")); //获取命令行传进来的参数
        String[] peerAddr = {"localhost:8801","localhost:8802","localhost:8803","localhost:8804","localhost:8805"};
        NodeConfig config = new NodeConfig();//只包含自身地址和其他节点地址这两个变量
        // 设置自身节点的地址
        config.setSelfPort(Integer.valueOf(System.getProperty("serverPort")));
        // 设置其他节点地址
        config.setPeerAddrs(Arrays.asList(peerAddr));


        final Node node = new NodeImpl();
        node.setConfig(config);
        node.init();

        //在JVM销毁前执行的一个线程，销毁此节点
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    node.destroy();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }));
    }
}
