package cn.itcast.netty.c3;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;

//Channel可以使用writeAndFlush进行调试，ChannelFuture则不行
//测试EventLoopServer的EventLoopGroup的临时类
//带有 Future，Promise 的类型都是和异步方法配套使用，用来处理结果

public class TestEventLoopClient {
    /*public static void main(String[] args) throws InterruptedException {
        // 1. 启动类
        Channel channel = new Bootstrap()
                // 2. 添加 EventLoop
                .group(new NioEventLoopGroup())
                // 3. 选择客户端 channel 实现
                .channel(NioSocketChannel.class)
                // 4. 添加处理器
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override // 在连接建立后被调用
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                // 5. 连接到服务器
                .connect(new InetSocketAddress("localhost", 8080))
                .sync() //同步获取结果
                .channel();
        System.out.println(channel);
        System.out.println();
    }*/

    //测试sync()的同步方法
    //如果注释sync()方法，由于connect()是异步方法，不使用sync()同步获取连接结果的话，使用writeAndFlush()是无法在服务器上打印出结果的
    //验证方法，在sout的方法打断点等待一段时间后writeAndFlush()向服务器打出结果，则正常了
    public static void main(String[] args) throws InterruptedException {
        // 1. 启动类
        ChannelFuture channelFuture = new Bootstrap()
                // 2. 添加 EventLoop
                .group(new NioEventLoopGroup())
                // 3. 选择客户端 channel 实现
                .channel(NioSocketChannel.class)
                // 4. 添加处理器
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override // 在连接建立后被调用
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                // 5. 连接到服务器
                .connect(new InetSocketAddress("localhost", 8080));
        //channelFuture.sync();
        Channel channel = channelFuture.channel();
        System.out.println();
        channel.writeAndFlush("hello,world");
    }
}
