package cn.itcast.netty.c1;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

@Slf4j
public class TestByteBuffer {
    public static void main(String[] args) {
        // FileChannel
        // 1.输入输出流，2.RandomAccessFile
        try (FileChannel channel = new FileInputStream("data.txt").getChannel()){
            // 准备缓冲区 分配10位 大小
            ByteBuffer buffer = ByteBuffer.allocate(10);
            while(true){
                int len = channel.read(buffer);
                log.debug("读取到的字节数{}",len);
                if(len == -1){
                    break;
                }

                // 从channel读取数据，向buffer写入
                channel.read(buffer);
                // 打印
                // 切换至读模式
                buffer.flip();
                while(buffer.hasRemaining()){// 是否还有剩余数据
                    byte b = buffer.get();
                    log.debug("实际字节{}",(char)b);
                }
                // 切换 buffer 写模式
                buffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
