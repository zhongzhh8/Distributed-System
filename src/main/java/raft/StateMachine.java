package raft;

import Entity.LogEntry;

public interface StateMachine{
    //将日志提交到状态机
    void apply(LogEntry logEntry);

    LogEntry get(String key);

    String getString(String key);

    void setString(String key, String value);

    void delString(String... key);

}
