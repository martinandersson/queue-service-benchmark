package com.martinandersson.mqb.benchmark;

import com.martinandersson.mqb.api.Message;
import com.martinandersson.mqb.api.QueueService;
import com.martinandersson.mqb.impl.reentrantreadwritelock.ReentrantReadWriteLockedQueueService;
import static java.lang.System.out;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import java.util.function.Function;
import java.util.function.Supplier;
import static java.util.stream.Collectors.joining;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.ThreadParams;

/**
 * Queue service benchmarks.
 * 
 * @author Martin Anderson (webmaster at martinandersson.com)
 */
// TODO: Production values!
@Warmup(iterations = 0)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(MICROSECONDS)
@State(Scope.Benchmark)
public class QueueServiceBenchmark
{
    // Iteration- and batch sizes for single shot benchmarks.
    // -----
    
    private static final int SS_ITERATIONS = 1, // TODO: 100
                             SS_BATCH_SIZE = 1; // TODO: 100_000
    
    
    
    // For each trial, we need a queue service to benchmark, and number of queues to use.
    // -----
    
    QueueService qs;
    
    @Param("1") // TODO: 1, 100, 1_000
    int queues;
    
    
    
    // This is the work we do. We write and read.
    // ------
    
    protected final void write(String queue, String message) {
        qs.push(queue, message);
    }
    
    protected final Message read(String queue) {
        final Message m = qs.pull(queue);
        
        if (m != null) {
            qs.complete(m);
        }
        
        return m;
    }
    
    protected final Message read(String queue, ReadStatistics rs) {
        return rs.report(read(queue));
    }
    
    
    
    // Setup
    // -----
    
    private static final Duration MSG_TIMEOUT = Duration.ofDays(999);
    
    public enum Implementation implements Supplier<QueueService> {
        ReentrantReadWriteLock (ReentrantReadWriteLockedQueueService::new);
        // TODO: Add the rest..
        
        private final Function<Duration, QueueService> delegate;
        
        private Implementation(Function<Duration, QueueService> delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public final QueueService get() {
            return delegate.apply(MSG_TIMEOUT);
        }
    }
    
    @Param
    Implementation impl;
    
    @Setup
    public void setupTrial(BenchmarkParams bParams, ThreadParams tParams) {
        qs = impl.get();
        
        if (!SystemProperties.BENCHMARK_FILE.isPresent()) {
            return;
        }
        
        // Console risk being mute for a very long time if we output to file,
        // so be kind and dump benchmark details.
        
        out.println();
        out.println("Running " + bParams.getBenchmark());
        out.println("        " + bParams.generatedBenchmark());

        String params = "        Implementation " + impl + ", " + queues + " queue(s)";

        int[] threads = bParams.getThreadGroups();

        if (threads.length != 2) {
            throw new IllegalArgumentException("Bad \"thread group\" parameter: " +
                    IntStream.of(threads).mapToObj(Integer::toString).collect(joining(",", "[", "]")));
        }

        params += ", " + threads[0] + " reader(s) and " + threads[1] + " writer(s) in " + tParams.getGroupCount() + " group(s)";

        out.println(params);
    }
    
    @State(Scope.Thread)
    public static class QueueName implements Supplier<String> {
        private Iterator<String> name;
        
        @Setup
        public void setupThread(QueueServiceBenchmark state) {
            Supplier<String> gen = () ->
                    "Q" + ThreadLocalRandom.current().nextInt(1, state.queues + 1);
            
            name = new FixedCostLoopingIterator<>(
                    Stream.generate(gen).limit(state.queues));
        }
        
        @Override
        public String get() {
            return name.next();
        }
    }
    
    @State(Scope.Thread)
    public static class QueueMessage {
        final String msg = 'T' + Long.toString(Thread.currentThread().getId());
    }
    
    
    
    // Benchmarks
    // -----
    
    @Group("thrpt")
    @Benchmark
    public void writer_thrpt(QueueName queue, QueueMessage message) {
        write(queue.get(), message.msg);
    }
    
    @Group("thrpt")
    @Benchmark
    public Message reader_thrpt(QueueName queue, ReadStatistics rs) {
        return read(queue.get(), rs);
    }
    
    @Group("avg")
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void writer_avg(QueueName queue, QueueMessage message) {
        write(queue.get(), message.msg);
    }
    
    @Group("avg")
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public Message reader_avg(QueueName queue, ReadStatistics rs) {
        return read(queue.get(), rs);
    }
    
    @Group("ss")
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Measurement(iterations = SS_ITERATIONS, batchSize = SS_BATCH_SIZE)
    public void writer_ss(QueueName queue, QueueMessage message) {
        write(queue.get(), message.msg);
    }
    
    @Group("ss")
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Measurement(iterations = SS_ITERATIONS, batchSize = SS_BATCH_SIZE)
    public Message reader_ss(QueueName queue) {
        return read(queue.get());
    }
}