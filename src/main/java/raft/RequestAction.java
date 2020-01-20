package raft;

import Entity.EntryParam;
import Entity.EntryResponse;
import Entity.VoteParam;
import Entity.VoteResponse;

public interface RequestAction {
    //处理投票请求
    VoteResponse requestVote(VoteParam param);
    //处理附加日志请求
    EntryResponse appendEntries(EntryParam param);
}
