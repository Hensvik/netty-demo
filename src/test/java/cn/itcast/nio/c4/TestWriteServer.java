package cn.itcast.nio.c4;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import static cn.itcast.nio.c2.ByteBufferUtil.debugAll;

@Slf4j
public class TestWriteServer {
    private static void split(ByteBuffer source) {
        source.flip();
        for (int i = 0; i < source.limit(); i++) {
            // 找到一条完整消息
            // 如果遇到了 \n 的换行符
            if (source.get(i) == '\n') {
                int length = i + 1 - source.position();
                // 把这条完整消息存入新的 ByteBuffer
                ByteBuffer target = ByteBuffer.allocate(length);
                // 从 source 读，向 target 写
                for (int j = 0; j < length; j++) {
                    target.put(source.get());
                }
                debugAll(target);
            }
        }
        source.compact();
    }

    public static void main(String[] args) throws IOException {
        // 1. 创建 selector, 管理多个 channel
        Selector selector = Selector.open();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        // 2. 建立 selector 和 channel 的联系（注册）
        // SelectionKey 就是将来事件发生后，通过它可以知道事件和哪个channel的事件
        SelectionKey sscKey = ssc.register(selector, 0, null);
        // 设置key 只关注 accept 事件
        sscKey.interestOps(SelectionKey.OP_ACCEPT);
        log.debug("sscKey:{}", sscKey);
        ssc.bind(new InetSocketAddress(8080));
        while (true) {
            // 3. select 方法, 没有事件发生，线程阻塞，有事件，线程才会恢复运行
            // select 在事件未处理时，它不会阻塞, 事件发生后要么处理，要么取消，不能置之不理
            // 即监听事件
            selector.select();
            // 4. 处理事件, selectedKeys 内部包含了所有发生的事件
            Iterator<SelectionKey> iter = selector.selectedKeys().iterator(); // accept, read
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                // 处理key 时，要从 selectedKeys 集合中删除，否则下次处理就会有问题，因为是删除，所以需要用迭代器的方法遍历
                // 如果不进行删除，该时间依然在队列中，会一直遍历处理该事件
                iter.remove();
                log.debug("key: {}", key);
                // 5. 区分事件类型
                if (key.isAcceptable()) { // 如果是 accept事件
                    //accept事件用 ServerSocketChannel处理
                    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                    SocketChannel sc = channel.accept(); //阻塞方法
                    sc.configureBlocking(false);    //将socketChannel设置为非阻塞

                    //将buffer新建放在这个位置，可以单独为每个channel创建buffer附件
                    // 将一个byteBuffer 作为附件放到selectionKey上
                    ByteBuffer buffer = ByteBuffer.allocate(16); //分配buffer 4个字节
                    SelectionKey scKey = sc.register(selector, 0, buffer);
                    //设置关注读事件
                    scKey.interestOps(SelectionKey.OP_READ);
                    log.debug("{}", sc);
                    log.debug("scKey:{}", scKey);
                } else if (key.isReadable()) { // 如果是 read事件
                    //这里使用try catch的原因是为了不让客户端的异常关闭影响到服务器端
                    try {
                        SocketChannel channel = (SocketChannel) key.channel(); // 拿到触发事件的channel

                        //获取byteBuffer上的附件
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        int read = channel.read(buffer); // 如果是正常断开，read 的方法的返回值是 -1
                        if(read == -1) {
                            key.cancel();
                        } else {
                            /*buffer.flip();
//                            debugAll(buffer);
                            System.out.println(Charset.defaultCharset().decode(buffer));*/

                            split(buffer);
                            //如果空间用完，则进行扩容
                            if(buffer.position() == buffer.limit()){
                                ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity()*2);
                                //切换模式
                                buffer.flip();
                                newBuffer.put(buffer);
                                key.attach(newBuffer);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        key.cancel();  // 因为客户端断开了,因此需要将 key 取消（从 selector 的 keys 集合中真正删除 key）
                    }
                }
            }
        }
    }
}
