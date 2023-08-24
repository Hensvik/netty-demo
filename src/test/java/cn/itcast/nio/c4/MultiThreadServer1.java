package cn.itcast.nio.c4;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.itcast.nio.c2.ByteBufferUtil.debugAll;

//work.register()以及sc.register()会导致冲突，worker.register()需要在sc.register()的前提下运行才不会导致阻塞
//1为使用ConcurrentLinkedQueue的队列方法

@Slf4j
public class MultiThreadServer1 {
    public static void main(String[] args) throws IOException {
        Thread.currentThread().setName("boss");
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        Selector boss = Selector.open();
        SelectionKey bossKey = ssc.register(boss, 0, null);
        bossKey.interestOps(SelectionKey.OP_ACCEPT);
        ssc.bind(new InetSocketAddress(8080));
        // 1. 创建固定数量的 worker 并初始化
        Worker[] workers = new Worker[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker("worker-" + i);
        }
        AtomicInteger index = new AtomicInteger();
        while(true) {
            boss.select();
            Iterator<SelectionKey> iter = boss.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    log.debug("connected...{}", sc.getRemoteAddress());
                    // 2. 关联 selector
                    log.debug("before register...{}", sc.getRemoteAddress());
                    // round robin 轮询
                    workers[index.getAndIncrement() % workers.length].register(sc); // boss 调用 初始化 selector , 启动 worker-0
                    log.debug("after register...{}", sc.getRemoteAddress());
                }
            }
        }
    }
    static class Worker implements Runnable{
        private Thread thread;
        private Selector selector;
        private String name;
        private volatile boolean start = false; // 还未初始化
        private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
        public Worker(String name) {
            this.name = name;
        }

        // 初始化线程，和 selector
        public void register(SocketChannel sc) throws IOException {
            if(!start) {
                selector = Selector.open();
                thread = new Thread(this, name);
                thread.start(); //启动线程，执行run方法
                start = true;
            }
            //方法一:使用同步队列
            //向队列添加任务，但并未立刻执行
            //-----------------
            queue.add(()->{
                try {
                    sc.register(selector, SelectionKey.OP_READ, null); // boss
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            });
            //-----------------
            selector.wakeup(); // 唤醒 select 方法 boss 避免阻塞以解决多线程下 sc.register()和selector.select()的冲突问题，
            sc.register(selector, SelectionKey.OP_READ, null); // boss
        }

        @Override
        public void run() {
            while(true) {
                try {
                    selector.select(); // worker-0  阻塞

                    //方法一
                    //-----------------
                    Runnable task = queue.poll();
                    if(task != null){
                        task.run(); // 在此处执行了 sc.register(selector,SelectionKey.OP_READ,null);
                    }
                    //-----------------

                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        if (key.isReadable()) {
                            ByteBuffer buffer = ByteBuffer.allocate(16);
                            SocketChannel channel = (SocketChannel) key.channel();
                            log.debug("read...{}", channel.getRemoteAddress());
                            channel.read(buffer);
                            buffer.flip();
                            debugAll(buffer);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}