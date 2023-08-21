package cn.itcast.nio.c4;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import static cn.itcast.nio.c2.ByteBufferUtil.*;

@Slf4j
public class SimpleServer {
    public static void main(String[] args) throws IOException {
        //使用 nio 来理解阻塞模式，单线程处理

        //0.初始数据
        ByteBuffer buffer = ByteBuffer.allocate(16);

        //1.创建了服务器
        ServerSocketChannel ssc = ServerSocketChannel.open();

        //2.绑定监听端口
        ssc.bind(new InetSocketAddress(8080));

        //3.连接集合
        List<SocketChannel> channels = new ArrayList<>();
        while (true){
            //4.accept 建立与客户端的连接, SocketChannel 用来与客户端之间的通信
            log.debug("connecting...");
            SocketChannel sc = ssc.accept();    //阻塞方法，线程停止运行，等待用户端发送数据
            log.debug("connected...{}",sc);
            channels.add(sc);

            for (SocketChannel channel : channels) {
                //5.接收客户端发送的数据
                //read为读取数据进入缓冲区
                log.debug("before read...{}",channel);
                channel.read(buffer);
                buffer.flip();
                debugRead(buffer);
                log.debug("after read...{}",channel);
            }
        }
    }

}
