package Entity;

import java.io.Serializable;
import java.util.Objects;

public class LogEntry implements Serializable, Comparable  {
    //日志的索引号
    private long index;
    //日志的任期号
    private long term;
    //日志的kv操作
    private Command command;

    public LogEntry() {
    }

    public LogEntry(long index, long term, Command command) {
        this.index = index;
        this.term = term;
        this.command = command;
    }

    @Override
    public String toString() {
        return "{" +
                "index=" + index +
                ", term=" + term +
                ", command=" + command +
                '}';
    }

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return -1;
        }
        if (this.getIndex() > ((LogEntry) o).getIndex()) {
            return 1;
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogEntry logEntry = (LogEntry) o;
        return term == logEntry.term &&
                Objects.equals(index, logEntry.index) &&
                Objects.equals(command, logEntry.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, term, command);
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public void setIndex(Long index) {
        this.index = index;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public Long getIndex() {
        return index;
    }
}
