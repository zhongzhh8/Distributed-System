package raft;

import Entity.*;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

public class RequestActionImpl implements RequestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestActionImpl.class);

    public final NodeImpl node;
    //投票锁
    public final ReentrantLock voteLock = new ReentrantLock();
    //附加日志锁
    public final ReentrantLock appendLock = new ReentrantLock();

    public RequestActionImpl(NodeImpl node) {
        this.node = node;
    }

    //接收node处理投票请求
    @Override
    public VoteResponse requestVote(VoteParam param) {
        try {
            VoteResponse voteResponse = new VoteResponse();
            if(!voteLock.tryLock()){
                voteResponse.setTerm(node.getCurrentTerm());
                voteResponse.setVoteGranted(false);
                return voteResponse;
            }
            // 对方Term没有自己新
            if(param.getTerm()<node.getCurrentTerm()){
                LOGGER.info("选举请求方的Term比自己小，拒绝投票");
                voteResponse.setTerm(node.getCurrentTerm());
                voteResponse.setVoteGranted(false);
                return voteResponse;
            }

            // (当前节点并没有投票 或者 已经投票过了且是对方节点) && 对方日志和自己一样新
            LOGGER.info("结点 {} 目前已投票给 [{}], 发来选举请求方的Candidate的Id : {}", node.peerSet.getSelf(), node.getVotedFor(), param.getCandidateId());
            LOGGER.info("结点 {} 目前的term {}, Candidate的term : {}", node.peerSet.getSelf(), node.getCurrentTerm(), param.getTerm());

            if ((StringUtil.isNullOrEmpty(node.getVotedFor()) || node.getVotedFor().equals(param.getCandidateId()))) {  //未投票，或已投票给发来请求的对方
                if (node.getLogModule().getLast() != null) {
                    // 对方term没有自己新
                    if (node.getLogModule().getLast().getTerm() > param.getLastLogTerm()) {
                        return VoteResponse.fail();
                    }
                    // 对方日志index没有自己新
                    if (node.getLogModule().getLastIndex() > param.getLastLogIndex()) {
                        return VoteResponse.fail();
                    }
                }
                // 切换状态
                node.status = NodeStatus.FOLLOWER;
                // 更新
                node.peerSet.setLeader(new Peer(param.getCandidateId()));
                node.setCurrentTerm(param.getTerm());
                node.setVotedFor(param.serverId);
                // 返回成功
                voteResponse.setTerm(node.getCurrentTerm());
                voteResponse.setVoteGranted(true);
                return voteResponse;
            }
            voteResponse.setTerm(node.getCurrentTerm());
            voteResponse.setVoteGranted(false);
            return voteResponse;
        }finally {
            voteLock.unlock();
        }

    }

    //接收node处理附加日志请求
    @Override
    public EntryResponse appendEntries(EntryParam param) {
        EntryResponse entryResponse = EntryResponse.fail();
        try{
            if(!appendLock.tryLock()){
                return entryResponse;
            }
            //无论成功失败首先设置返回值,返回自己的term
            entryResponse.setTerm(node.getCurrentTerm());

            if (param.getTerm() < node.getCurrentTerm()) {//对方Term没有自己新
                return entryResponse;
            }else{//对方Term比有自己新
                node.preHeartBeatTime = System.currentTimeMillis();
                node.preElectionTime = System.currentTimeMillis();
                node.peerSet.setLeader(new Peer(param.getLeaderId()));
                LOGGER.debug("结点 {} 成为 FOLLOWER, 目前的Term : {}, 对方的Term : {},  serverId : {}",
                        node.peerSet.getSelf(), node.getCurrentTerm(), param.getTerm(), param.getServerId());
                node.status = NodeStatus.FOLLOWER;
            }
            //使用对方的term
            node.setCurrentTerm(param.getTerm());
            //收到的是心跳
            if(param.getEntries() == null || param.getEntries().length == 0){
                LOGGER.info("成功接收leader结点 {} 发来的心跳 , leader的term : {}, 我的term : {}",
                        param.getLeaderId(), param.getTerm(), node.getCurrentTerm());
                entryResponse.setTerm(node.getCurrentTerm());
                entryResponse.setSuccess(true);
                return entryResponse;
            }

            //收到的是日志
            if (node.getLogModule().getLastIndex() != 0 && param.getPrevLogIndex() != 0) {
                LogEntry logEntry;
                if ((logEntry = node.getLogModule().read(param.getPrevLogIndex())) != null) {
                    // 如果日志在 prevLogIndex 位置处的日志条目的任期号和 prevLogTerm 不匹配，则返回 false,需要减小 nextIndex 重试.
                    if (logEntry.getTerm() != param.getPreLogTerm()) {
                        return entryResponse;
                    }
                } else {
                    //index 不对, 需要递减 nextIndex 重试.
                    return entryResponse;
                }
            }

            //本地存在的日志和leader的日志冲突（索引值相同，任期不同）了，以 leader 的为准，删除自身的。
            LogEntry existLog = node.getLogModule().read(((param.getPrevLogIndex() + 1)));
            if (existLog != null && existLog.getTerm() != param.getEntries()[0].getTerm()) {
                // 删除这一条和之后所有的, 然后写入日志和状态机.
                node.getLogModule().removeOnStartIndex(param.getPrevLogIndex() + 1);
            } else if (existLog != null) {
                //没有冲突，直接返回
                entryResponse.setSuccess(true);
                return entryResponse;
            }

            // 写进日志并且应用到状态机
            for (LogEntry entry : param.getEntries()) {
                node.getLogModule().write(entry);
                node.stateMachine.apply(entry);
                entryResponse.setSuccess(true);
            }

            //如果 leaderCommit > commitIndex，令 commitIndex 等于 leaderCommit 和 新日志条目索引值中较小的一个
            if (param.getLeaderCommit() > node.getCommitIndex()) {
                int commitIndex = (int) Math.min(param.getLeaderCommit(), node.getLogModule().getLastIndex());
                node.setCommitIndex(commitIndex);
                node.setLastApplied(commitIndex);
            }
            entryResponse.setTerm(node.getCurrentTerm());
            node.status = NodeStatus.FOLLOWER;
            return entryResponse;
        }finally {
            appendLock.unlock();
        }
    }
}
