package raft;

import Entity.LogEntry;

/**
 * 日志处理模块
 */
public interface LogModule {
    //写入日志
    void write(LogEntry logEntry);
    //读取日志
    LogEntry read(Long index);
    //删除以startIndex索引为开头的所有日志
    void removeOnStartIndex(Long startIndex);
    //获取最新日志
    LogEntry getLast();
    //获取最新日志的索引
    Long getLastIndex();
}
