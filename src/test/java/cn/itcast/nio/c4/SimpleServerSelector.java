package cn.itcast.nio.c4;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static cn.itcast.nio.c2.ByteBufferUtil.*;

@Slf4j
public class SimpleServerSelector {
    public static void main(String[] args) throws IOException {
        //1.创建selector，管理多个channel
        Selector selector = Selector.open();

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);

        //2.建立selector和channel的联系（注册）
        // SelectionKey 就是将来时间发生后，通过它可以知道事件和哪个channel的事件
        //0表示不关注任何事件
        SelectionKey sscKey = ssc.register(selector, 0, null);
        //key只关注accept事件
        sscKey.interestOps(SelectionKey.OP_ACCEPT);
        log.debug("register key:{}",sscKey);

        ssc.bind(new InetSocketAddress(8080));

        List<SocketChannel> channels = new ArrayList<>();
        while (true){
            //3.select方法，没有事件发生，线程阻塞；有时间，线程才会恢复运行
            //select 在事件未处理时，是不会被阻塞的，因此事件发生后，要么处理，要么取消，不能置之不理
            selector.select();
            //4.处理事件(事件集合)，selectedKeys内部包含了所有发生的事件
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while(iterator.hasNext()){
                SelectionKey key = iterator.next();
                // 处理key时，要从selectedKeys 集合中删除，否则下次处理就会有问题
                iterator.remove();

                log.debug("key:{}",key);

                //5.区分事件类型
                if(key.isAcceptable()){
                    //获取触发事件的channel
                    //如果注释以下代码，在没有事情处理时，会陷入死循环，这个是selector的工作原理
                    //---------------
                    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                    SocketChannel sc = channel.accept();
                    sc.configureBlocking(false);    //设置非阻塞模式
                    ByteBuffer buffer = ByteBuffer.allocate(16);
                    //将一个byteBuffer作为附件关联到selectionKey上
                    SelectionKey scKey = sc.register(selector, 0, buffer);
                    scKey.interestOps(SelectionKey.OP_READ);
                    log.debug("{}",sc);
                    log.debug("触发了accept事件");
                    //---------------
                }else if(key.isReadable()){ //如果是read
                    try {
                        //扩容做法
                        //----------------------
                        SocketChannel channel = (SocketChannel) key.channel();  //拿到触发事件的channel
                        //获取 selectionKey上关联的附件
                        ByteBuffer buffer = (ByteBuffer)key.attachment();
                        int read = channel.read(buffer);
                        if(read==-1){   //如果时正常断开，read的方法的返回值是-1
                            key.cancel();
                        }else{
                            split(buffer);
                            if(buffer.position() == buffer.limit()){
                                ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity()*2);
                                buffer.flip();  //切换成读模式
                                newBuffer.put(buffer);
                                key.attach(newBuffer);
                                //debugRead(buffer);
                                System.out.println(Charset.defaultCharset().decode(buffer));
                                //一个汉字占3-4个字节，如果分配的buffer大小不足的话会出现乱码
                            }
                        }
                        //----------------------
                    } catch (IOException e) {
                        e.printStackTrace();
                        key.cancel();   //客户端断开了，所以需要将key取消（从 selector的key集合中真正删除key）
                    }
                }
                //key.cancel();
            }
        }
    }

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
}
