package Entity;

import java.util.Arrays;
/**
 * 附加日志RPC参数. handlerAppendEntries
 */
public class EntryParam extends Param {
    //领导人的Id，以便于跟随者重定向请求
    private String leaderId;
    //紧随新的日志条目之前的索引值
    private long prevLogIndex;
    //prevLogIndex 条目的任期号
    private long preLogTerm;
    //准备存储的日志条目（表示心跳时为空；一次性发送多个是为了提高效率）
    private LogEntry[] entries;
    //领导人已经提交的日志的索引值
    private long leaderCommit;

    public EntryParam() {
    }

    public EntryParam(String leaderId, long prevLogIndex, long preLogTerm, LogEntry[] entries, long leaderCommit) {
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.preLogTerm = preLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }

    @Override
    public String toString() {
        return "EntryParam{" +
                "leaderId='" + leaderId + '\'' +
                ", prevLogIndex=" + prevLogIndex +
                ", preLogTerm=" + preLogTerm +
                ", entries=" + Arrays.toString(entries) +
                ", leaderCommit=" + leaderCommit +
                ", term=" + term +
                ", serverId='" + serverId + '\'' +
                '}';

    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public long getPrevLogIndex() {
        return prevLogIndex;
    }

    public void setPrevLogIndex(long prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public long getPreLogTerm() {
        return preLogTerm;
    }

    public void setPreLogTerm(long preLogTerm) {
        this.preLogTerm = preLogTerm;
    }

    public LogEntry[] getEntries() {
        return entries;
    }

    public void setEntries(LogEntry[] entries) {
        this.entries = entries;
    }

    public long getLeaderCommit() {
        return leaderCommit;
    }

    public void setLeaderCommit(long leaderCommit) {
        this.leaderCommit = leaderCommit;
    }
}