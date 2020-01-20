package thread;

import java.util.concurrent.*;

public class RaftThreadPool  {
    //返回可用处理器的Java虚拟机的数量
    private static int cup = Runtime.getRuntime().availableProcessors();
    private static int maxPoolSize = cup * 2;
    private static final int queueSize = 1024;
    private static final long keepTime = 1000 * 60;
    private static TimeUnit keepTimeUnit = TimeUnit.MILLISECONDS;
    private static ScheduledExecutorService ss = getScheduled();
    private static ThreadPoolExecutor te = getThreadPool();

    private static ThreadPoolExecutor getThreadPool() {
        return new RaftThreadPoolExecutor(
                cup, //核心线程池大小
                maxPoolSize,//最大线程池大小
                keepTime,//线程最大空闲时间,该参数默认对核心线程无效
                keepTimeUnit,//时间单位
                new LinkedBlockingQueue<>(queueSize),//线程等待队列
                new NameThreadFactory());//线程创建工厂
    }

    private static ScheduledExecutorService getScheduled() {
        //ScheduledThreadPoolExecutor的核心线程池的线程个数为指定的corePoolSize，当核心线程池的线程个数达到corePoolSize后，就会将任务提交给有界阻塞队列DelayedWorkQueue
        return new ScheduledThreadPoolExecutor(cup, new NameThreadFactory());
    }

    //是以上一个任务开始的时间计时，delay时间过去后，
    //检测上一个任务是否执行完毕，如果上一个任务执行完毕，
    //则当前任务立即执行，如果上一个任务没有执行完毕，则需要等上一个任务执行完毕后立即执行
    public static void scheduleAtFixedRate(Runnable r, long initDelay, long delay) {
        ss.scheduleAtFixedRate(r, initDelay, delay, TimeUnit.MILLISECONDS);
    }

    //当达到延时时间initialDelay后，任务开始执行。上一个任务执行结束后到下一次
    //任务执行，中间延时时间间隔为delay。以这种方式，周期性执行任务。
    public static void scheduleWithFixedDelay(Runnable r, long delay) {
        ss.scheduleWithFixedDelay(r, 0, delay, TimeUnit.MILLISECONDS);
    }

    public static <T> Future<T> submit(Callable r) {
        return te.submit(r);
    }

    public static void execute(Runnable r) {
        te.execute(r);
    }

    public static void execute(Runnable r, boolean sync) {
        if (sync) {
            r.run();
        } else {
            te.execute(r);
        }
    }

    static class NameThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new RaftThread("Raft thread", r);
            t.setDaemon(true);
            t.setPriority(5);
            return t;
        }
    }

}
