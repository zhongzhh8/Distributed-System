package raft;

import Entity.*;
import client.KVRequest;
import client.KVResponse;
import config.NodeChange;
import config.NodeChangeImpl;
import config.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.Client;
import rpc.ClientImpl;
import rpc.Server;
import rpc.ServerImpl;
import thread.RaftThreadPool;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static raft.NodeStatus.LEADER;

public class NodeImpl<T> implements Node<T>,NodeChange {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeImpl.class);

    //选举时间间隔基数
    public volatile long electionTime = 15 * 1000;
    //上一次选举时间
    public volatile long preElectionTime = 0;
    //上一次心跳时间戳
    public volatile long preHeartBeatTime = 0;
    //心跳间隔基数
    public final long heartBeatTick = 5 * 1000;
    //节点当前状态
    public volatile int status = NodeStatus.FOLLOWER;

    private HeartBeatTask heartBeatTask = new HeartBeatTask();
    private ElectionTask electionTask = new ElectionTask();
    private ReplicationFailQueueConsumer replicationFailQueueConsumer = new ReplicationFailQueueConsumer();
    private LinkedBlockingQueue<ReplicationFailModel> replicationFailQueue = new LinkedBlockingQueue<>(2048);

    public PeerSet peerSet;
    //服务器最后一次知道的任期号
    private volatile long currentTerm = 0;
    //在当前获得选票的Candidate的Id
    private volatile String votedFor;

    private LogModule logModule;
    //已知的最大的已经被提交的日志条目的索引值
    private volatile long commitIndex;
    //最后被应用到状态机的日志条目索引值
    private volatile long lastApplied = 0;
    //对于每一个服务器，需要发送给他的下一个日志条目的索引值（初始化为领导人最后索引值加一）
    private Map<Peer, Long> nextIndexs;
    //对于每一个服务器，已经复制给他的日志的最高索引值
    private Map<Peer, Long> matchIndexs;
    public volatile boolean started;

    public NodeConfig config;

    public static Server RPC_SERVER;

    public Client RPC_CLIENT = new ClientImpl();

    public StateMachine stateMachine;

    RequestAction requestAction;

    NodeChange delegate;

    public NodeImpl() {
    }

    @Override
    public void init() throws Throwable {
        if (started) {
            return;
        }
        synchronized (this) {//被synchronized修饰的区域每次只有一个线程可以访问，从而满足线程安全的目的
            if (started) {
                return;
            }
            RPC_SERVER.start();
            requestAction = new RequestActionImpl(this);
            delegate = new NodeChangeImpl(this);

            RaftThreadPool.scheduleWithFixedDelay(heartBeatTask, 500);
            RaftThreadPool.scheduleAtFixedRate(electionTask, 6000, 500);
            RaftThreadPool.execute(replicationFailQueueConsumer);

            LogEntry logEntry = logModule.getLast();
            if (logEntry != null) {
                currentTerm = logEntry.getTerm();
            }
            started = true;
            LOGGER.info("成功启动, 自身Id : {} ", peerSet.getSelf());
        }
    }

    @Override
    public void setConfig(NodeConfig config) {
        this.config = config;
        stateMachine = new StateMachineImpl();
        logModule = new LogModuleImpl();
        peerSet = new PeerSet();
        for (String s : config.getPeerAddrs()) {
            Peer peer = new Peer(s);
            peerSet.addPeer(peer);
            if (s.equals("localhost:" + config.getSelfPort())) {
                peerSet.setSelf(peer);
            }
        }
        RPC_SERVER = new ServerImpl(config.selfPort, this);
    }

    @Override
    public VoteResponse handleRequestVote(VoteParam param) {
        //LOGGER.warn("handlerRequestVote will be invoke, param info : {}", param);
        return requestAction.requestVote(param);
    }

    @Override
    public EntryResponse handleAppendEntries(EntryParam param) {
        if (param.getEntries() != null) {
            LOGGER.warn("node receive node {} append entry, entry content = {}", param.getLeaderId(), param.getEntries());
        }
        return requestAction.appendEntries(param);
    }

    /**
     * 客户端的每一个请求都包含一条被复制状态机执行的指令。
     * 领导人把这条指令作为一条新的日志条目附加到日志中去，然后并行的发起附加条目 RPCs 给其他的服务器，让他们复制这条日志条目。
     * 当这条日志条目被安全的复制（下面会介绍），领导人会应用这条日志条目到它的状态机中然后把执行的结果返回给客户端。
     * 如果跟随者崩溃或者运行缓慢，再或者网络丢包，
     *  领导人会不断的重复尝试附加日志条目 RPCs （尽管已经回复了客户端）直到所有的跟随者都最终存储了所有的日志条目。
     */
    @Override
    public synchronized KVResponse handleClientRequest(KVRequest request) {
        if(request.getType()==0){
            LOGGER.warn("处理客户请求 {} （0是put，1是get, 2是del） key : [{}], value : [{}]",
                    request.getType(), request.getKey(), request.getValue());
        }else{
            LOGGER.warn("处理客户请求 {} （0是put，1是get, 2是del） key : [{}]",
                    request.getType(), request.getKey());
        }
        if (status != LEADER) {
            LOGGER.warn("我不是leader , 只将请求重定向到leader, leader的地址 : {}, 我的地址 : {}",
                    peerSet.getLeader(), peerSet.getSelf().getAddr());
            return redirect(request);
        }

        if (request.getType() == KVRequest.GET) {
            LogEntry logEntry = stateMachine.get(request.getKey());

            if (logEntry != null) {
                KVResponse response = new KVResponse(logEntry.getCommand());
                response.setValue(logEntry.getCommand().getValue());
                return response;
            }
            return new KVResponse(null);
        }
        Command command = new Command();
        command.setKey(request.getKey());
        command.setValue(request.getValue());
        LogEntry logEntry = new LogEntry();
        logEntry.setCommand(command);
        logEntry.setTerm(currentTerm);

        // 预提交到本地日志
        logModule.write(logEntry);
        LOGGER.info("成功写入日志, 日志 : {}, 日志index : {}", logEntry, logEntry.getIndex());

        final AtomicInteger success = new AtomicInteger(0);

        List<Future<Boolean>> futureList = new CopyOnWriteArrayList<>();

        int count = 0;
        //  复制到其他机器
        for (Peer peer : peerSet.getPeersWithOutSelf()) {
            count++;
            // 并行发起 RPC 复制.
            futureList.add(replication(peer, logEntry));
        }

        CountDownLatch latch = new CountDownLatch(futureList.size());
        List<Boolean> resultList = new CopyOnWriteArrayList<>();

        getRPCAppendResult(futureList, latch, resultList);

        try {
            latch.await(4000, MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Boolean aBoolean : resultList) {
            if (aBoolean) {
                success.incrementAndGet();
            }
        }

        // 如果存在一个满足N > commitIndex的 N，并且大多数的matchIndex[i] ≥ N成立，
        // 并且log[N].term == currentTerm成立，那么令 commitIndex 等于这个 N
        List<Long> matchIndexList = new ArrayList<>(matchIndexs.values());
        // 小于 2, 没有意义
        int median = 0;
        if (matchIndexList.size() >= 2) {
            Collections.sort(matchIndexList);
            median = matchIndexList.size() / 2;
        }
        Long N = matchIndexList.get(median);
        if (N > commitIndex) {
            LogEntry entry = logModule.read(N);
            if (entry != null && entry.getTerm() == currentTerm) {
                commitIndex = N;
            }
        }
        //  响应客户端(成功一半)
        if (success.get() >= (count / 2)) {
            // 更新
            commitIndex = logEntry.getIndex();
            if(request.getType() == KVRequest.PUT){
                //  应用到状态机
                stateMachine.setString("PutOrDel", "1");
                stateMachine.apply(logEntry);
                lastApplied = commitIndex;
                LOGGER.info("从状态机写入键值对成功,  logEntry : {}  key : {}  value : {}" , logEntry,request.getKey(),request.getValue());
                // 返回成功.
                return KVResponse.ok();
            }else{
                //  应用到状态机
                stateMachine.setString("PutOrDel", "2");
                stateMachine.delString(request.getKey());
                lastApplied = commitIndex;
                LOGGER.info("从状态机删除键值对成功,  logEntry : {}  key : {}  value : {}" , logEntry,request.getKey(),request.getValue());
                // 返回成功.
                return KVResponse.ok();
            }

        } else {
            // 回滚已经提交的日志.
            logModule.removeOnStartIndex(logEntry.getIndex());
            LOGGER.warn("日志写入状态机失败,  logEntry : {}", logEntry);
            // 不应用到状态机,但已经记录到日志中.由定时任务从重试队列取出,然后重复尝试,当达到条件时,应用到状态机.
            // 这里应该返回错误, 因为没有成功复制过半机器.
            return KVResponse.fail();
        }
    }

    private void getRPCAppendResult(List<Future<Boolean>> futureList, CountDownLatch latch, List<Boolean> resultList) {
        for (Future<Boolean> future : futureList) {
            RaftThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        resultList.add(future.get(3000, MILLISECONDS));
                    } catch (CancellationException | TimeoutException | ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        resultList.add(false);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
    }

    @Override
    public KVResponse redirect(KVRequest request) {
        Request<KVRequest> r = new Request<KVRequest>();
        r.setObject(request);
        r.setType(Request.CLIENT_REQ);
        r.setUrl(peerSet.getLeader().getAddr());
        Response response = RPC_CLIENT.send(r);
        return (KVResponse) response.getResponse();
    }

    /** 复制到其他机器  */
    public Future<Boolean> replication(Peer peer, LogEntry entry) {
        return RaftThreadPool.submit(new Callable() {
            @Override
            public Boolean call() throws Exception {

                long start = System.currentTimeMillis(), end = start;

                // 20 秒重试时间
                while (end - start < 20 * 1000L) {
                    EntryParam entryParam = new EntryParam();
                    entryParam.setTerm(currentTerm);
                    entryParam.setServerId(peer.getAddr());
                    entryParam.setLeaderId(peerSet.getSelf().getAddr());

                    entryParam.setLeaderCommit(commitIndex);

                    // 以我这边为准, 这个行为通常是成为 leader 后,首次进行 RPC 才有意义.
                    Long nextIndex = nextIndexs.get(peer);
                    LinkedList<LogEntry> logEntries = new LinkedList<>();
                    if (entry.getIndex() >= nextIndex) {
                        for (long i = nextIndex; i <= entry.getIndex(); i++) {
                            LogEntry l = logModule.read(i);
                            if (l != null) {
                                logEntries.add(l);
                            }
                        }
                    } else {
                        logEntries.add(entry);
                    }
                    // 最小的那个日志.
                    LogEntry preLog = getPreLog(logEntries.getFirst());
                    entryParam.setPreLogTerm(preLog.getTerm());
                    entryParam.setPrevLogIndex(preLog.getIndex());

                    entryParam.setEntries(logEntries.toArray(new LogEntry[0]));

                    Request request = new Request();
                    request.setUrl(peer.getAddr());
                    request.setType(Request.ENTRIES);
                    request.setObject(entryParam);

                    try {
                        Response response = RPC_CLIENT.send(request);
                        if (response == null) {
                            return false;
                        }
                        EntryResponse result = (EntryResponse) response.getResponse();
                        //LOGGER.info("EntryResponse=[{}]", result.toString());
                        if (result != null && result.isSuccess()) {
                            LOGGER.info("成功给follower发送日志 , follower=[{}], 日志=[{}]", peer, entryParam.getEntries());
                            // update 这两个追踪值
                            nextIndexs.put(peer, entry.getIndex() + 1);
                            matchIndexs.put(peer, entry.getIndex());
                            return true;
                        } else if (result != null) {
                            // 对方比我大
                            if (result.getTerm() > currentTerm) {
                                LOGGER.warn("follower [{}] 的term [{}] 比我大, 我的term是 [{}], 所以我将成为follower",
                                        peer, result.getTerm(), currentTerm);
                                currentTerm = result.getTerm();
                                status = NodeStatus.FOLLOWER;
                                return false;
                            } // 没我大, 却失败了,说明 index 不对.或者 term 不对.
                            else {
                                // 递减
                                if (nextIndex == 0) {
                                    nextIndex = 1L;
                                }
                                nextIndexs.put(peer, nextIndex - 1);
                                LOGGER.warn("follower的 {} nextIndex 不匹配, 将会减小nextIndex并重新发送附加日志请求, nextIndex : [{}]", peer.getAddr(),
                                        nextIndex);
                                // 重来, 直到成功.
                            }
                        }

                        end = System.currentTimeMillis();

                    } catch (Exception e) {
                        LOGGER.warn(e.getMessage(), e);
                        return false;
                    }
                }
                // 超时了,没办法了
                return false;
            }
        });

    }

    @Override
    public Result addPeer(Peer newPeer) {
        return delegate.addPeer(newPeer);
    }

    @Override
    public Result removePeer(Peer oldPeer) {
        return delegate.removePeer(oldPeer);
    }

    class HeartBeatTask implements Runnable {

        @Override
        public void run() {
            if (status != LEADER) {
                return;
            }
            long current = System.currentTimeMillis();
            if (current - preHeartBeatTime < heartBeatTick) {
                return;
            }
            LOGGER.info("==========================");
            for (Peer peer : peerSet.getPeersWithOutSelf()) {
                LOGGER.info("结点 {} nextIndex={}", peer.getAddr(), nextIndexs.get(peer));
            }
            preHeartBeatTime = System.currentTimeMillis();
            // 心跳只关心 term 和 leaderID
            for (Peer peer : peerSet.getPeersWithOutSelf()) {
                EntryParam param = new EntryParam();
                param.setEntries(null);//心跳不需要日志
                param.setLeaderId(peerSet.getSelf().getAddr());
                param.setServerId(peer.getAddr());
                param.setTerm(currentTerm);

                Request<EntryParam> request = new Request<>(
                        Request.ENTRIES,
                        param,
                        peer.getAddr());

                RaftThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Response response = RPC_CLIENT.send(request);
                            EntryResponse entryResult = (EntryResponse) response.getResponse();
                            long term = entryResult.getTerm();
                            if (term > currentTerm) {
                                LOGGER.error("我将成为follower, 对方的term : {}, 我的term : {}", term, currentTerm);
                                currentTerm = term;
                                votedFor = "";
                                status = NodeStatus.FOLLOWER;
                            }
                        } catch (Exception e) {
                            LOGGER.error("心跳发送失败, 接收方地址 : {} ", request.getUrl());
                        }
                    }
                }, false);
            }
        }
    }

    /**
     * 1. 在转变成候选人后就立即开始选举过程
     *      自增当前的任期号（currentTerm）
     *      给自己投票
     *      重置选举超时计时器
     *      发送请求投票的 RPC 给其他所有服务器
     * 2. 如果接收到大多数服务器的选票，那么就变成领导人
     * 3. 如果接收到来自新的领导人的附加日志 RPC，转变成跟随者
     * 4. 如果选举过程超时，再次发起一轮选举
     */
    class ElectionTask implements Runnable {

        @Override
        public void run() {

            if (status == LEADER) {
                return;
            }

            long current = System.currentTimeMillis();
            // 基于 RAFT 的随机时间,解决冲突.
            electionTime = electionTime + ThreadLocalRandom.current().nextInt(50);
            if (current - preElectionTime < electionTime) {
                return;
            }
            status = NodeStatus.CANDIDATE;
            LOGGER.info("结点 {} 成为CANDIDATE并开始竞选leader, 原term={},自增后term={}, 最新的日志 : [{}]",
                    peerSet.getSelf(), currentTerm, currentTerm+1, logModule.getLast());

            preElectionTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(200) + 150;

            currentTerm = currentTerm + 1;
            // 推荐自己.
            votedFor = peerSet.getSelf().getAddr();

            List<Peer> peers = peerSet.getPeersWithOutSelf();

            ArrayList<Future> futureArrayList = new ArrayList<>();

            LOGGER.info("向这些结点发送选举请求 : {}",  peers);

            // 发送请求
            for (Peer peer : peers) {
                LOGGER.info("发送选举请求给 : {}",  peer);
                futureArrayList.add(RaftThreadPool.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        long lastTerm = 0L;
                        LogEntry last = logModule.getLast();
                        if (last != null) {
                            lastTerm = last.getTerm();
                        }
                        VoteParam param = new VoteParam();
                        param.setTerm(currentTerm);
                        param.setCandidateId(peerSet.getSelf().getAddr());
                        param.setLastLogIndex(LongConvert.convert(logModule.getLastIndex()));
                        param.setLastLogTerm(lastTerm);

                        Request request = new Request();
                        request.setType(Request.VOTE);
                        request.setObject(param);
                        request.setUrl(peer.getAddr());
                        try {
                            @SuppressWarnings("unchecked")
                            Response<VoteResponse> response = RPC_CLIENT.send(request);
                            return response;

                        } catch (RuntimeException e) {
                            LOGGER.error("请求投票失败 , 接收方地址 : " + request.getUrl());
                            return null;
                        }
                    }
                }));
            }

            AtomicInteger success2 = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(futureArrayList.size());

            //LOGGER.info("futureArrayList.size() : {}", futureArrayList.size());
            // 等待结果.
            for (Future future : futureArrayList) {
                RaftThreadPool.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        try {

                            @SuppressWarnings("unchecked")
                            Response<VoteResponse> response = (Response<VoteResponse>) future.get(3000, MILLISECONDS);
                            if (response == null) {
                                return -1;
                            }
                            boolean isVoteGranted = response.getResponse().isVoteGranted();

                            if (isVoteGranted) {
                                success2.incrementAndGet();
                            } else {
                                // 更新自己的任期.
                                long resTerm = response.getResponse().getTerm();
                                if (resTerm >= currentTerm) {
                                    currentTerm = resTerm;
                                }
                            }
                            return 0;
                        } catch (Exception e) {
                            LOGGER.error("future.get exception , e : ", e);
                            return -1;
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }

            try {
                // 稍等片刻
                latch.await(3500, MILLISECONDS);
            } catch (InterruptedException e) {
                LOGGER.warn("InterruptedException By Master election Task");
            }

            int success = success2.get();
            LOGGER.info("结点 {} 可能成为leader , 票数 = {} , 状态 : {}", peerSet.getSelf(), success, NodeStatus.status);
            // 如果投票期间,有其他服务器发送 appendEntry , 就可能变成 follower ,这时,应该停止.
            if (status == NodeStatus.FOLLOWER) {
                return;
            }
            // 加上自身.
            if (success >= peers.size() / 2) {
                LOGGER.warn("结点 {} 成为leader ", peerSet.getSelf());
                status = LEADER;
                peerSet.setLeader(peerSet.getSelf());
                votedFor = "";
                becomeLeaderToDoThing();
            } else {
                // else 重新选举
                votedFor = "";
            }

        }
    }

    /**
     * 初始化所有的 nextIndex 值为自己的最后一条日志的 index + 1. 如果下次 RPC 时, 跟随者和leader 不一致,就会失败.
     * 那么 leader 尝试递减 nextIndex 并进行重试.最终将达成一致.
     */
    private void becomeLeaderToDoThing() {
        nextIndexs = new ConcurrentHashMap<>();
        matchIndexs = new ConcurrentHashMap<>();
        for (Peer peer : peerSet.getPeersWithOutSelf()) {
            nextIndexs.put(peer, logModule.getLastIndex() + 1);
            matchIndexs.put(peer, 0L);
        }
    }

    private LogEntry getPreLog(LogEntry logEntry) {
        LogEntry entry = logModule.read(logEntry.getIndex() - 1);

        if (entry == null) {
            LOGGER.warn("上一个日志为空, 当前日志 : {}", logEntry);
            entry = new LogEntry();
            entry.setIndex(0L);
            entry.setTerm(0);
            entry.setCommand(null);
        }
        return entry;
    }

    class ReplicationFailQueueConsumer implements Runnable {
        /** 一分钟 */
        long intervalTime = 1000 * 60;
        @Override
        public void run() {
            for (; ; ) {
                try {
                    ReplicationFailModel model = replicationFailQueue.take();
                    if (status != LEADER) {
                        // 应该清空
                        replicationFailQueue.clear();
                        continue;
                    }
                    LOGGER.warn("replication Fail Queue Consumer take a task, will be retry replication, content detail : [{}]", model.logEntry);
                    long offerTime = model.offerTime;
                    if (System.currentTimeMillis() - offerTime > intervalTime) {
                        LOGGER.warn("replication Fail event Queue maybe full or handler slow");
                    }

                    Callable callable = model.callable;
                    Future<Boolean> future = RaftThreadPool.submit(callable);
                    Boolean r = future.get(3000, MILLISECONDS);
                    // 重试成功.
                    if (r) {
                        // 可能有资格应用到状态机.
                        tryApplyStateMachine(model);
                    }
                } catch (InterruptedException e) {
                    // ignore
                } catch (ExecutionException | TimeoutException e) {
                    LOGGER.warn(e.getMessage());
                }
            }
        }
    }

    private void tryApplyStateMachine(ReplicationFailModel model) {

        String success = stateMachine.getString(model.successKey);
        stateMachine.setString(model.successKey, String.valueOf(Integer.valueOf(success) + 1));
        String type = stateMachine.getString("PutOrDel");
        String count = stateMachine.getString(model.countKey);
        if (Integer.valueOf(success) >= Integer.valueOf(count) / 2) {
            if(type=="1"){
                stateMachine.apply(model.logEntry);
            }else {
                stateMachine.delString(model.logEntry.getCommand().getKey());
            }
            stateMachine.delString(model.countKey, model.successKey);
        }
    }

    @Override
    public void destroy() throws Throwable {
        RPC_SERVER.stop();
    }

    public long getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(long currentTerm) {
        this.currentTerm = currentTerm;
    }

    public LogModule getLogModule() {
        return logModule;
    }

    public void setLogModule(LogModule logModule) {
        this.logModule = logModule;
    }

    public String getVotedFor() {
        return this.votedFor;
    }

    public long getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(long commitIndex) {
        this.commitIndex = commitIndex;
    }

    public long getLastApplied() {
        return lastApplied;
    }

    public void setLastApplied(long lastApplied) {
        this.lastApplied = lastApplied;
    }

    public void setVotedFor(String votedFor) {
        this.votedFor = votedFor;
    }

    public Map<Peer, Long> getNextIndexs() {
        return nextIndexs;
    }

    public void setNextIndexs(Map<Peer, Long> nextIndexs) {
        this.nextIndexs = nextIndexs;
    }

    public Map<Peer, Long> getMatchIndexs() {
        return matchIndexs;
    }

    public void setMatchIndexs(Map<Peer, Long> matchIndexs) {
        this.matchIndexs = matchIndexs;
    }
}
