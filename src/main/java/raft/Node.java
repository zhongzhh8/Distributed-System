package raft;

import Entity.EntryParam;
import Entity.EntryResponse;
import Entity.VoteParam;
import Entity.VoteResponse;
import client.KVRequest;
import client.KVResponse;

public interface Node<T> {
    //处理投票请求
    VoteResponse handleRequestVote(VoteParam param);
    //处理附加日志请求
    EntryResponse handleAppendEntries(EntryParam param);
    //处理client请求
    KVResponse handleClientRequest(KVRequest request);
    //处理重定向请求，转发给leader
    KVResponse redirect(KVRequest request);
    //结点配置
    void setConfig(NodeConfig config);
    //初始化结点
    void init() throws Throwable;
    //销毁结点
    void destroy() throws Throwable;
}
