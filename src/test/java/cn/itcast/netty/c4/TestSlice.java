package cn.itcast.netty.c4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import static cn.itcast.netty.c4.TestByteBuf.log;

public class TestSlice {
    public static void main(String[] args) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(10);
        buf.writeBytes(new byte[]{'a','b','c','d','e','f','g','h','i','j'});
        log(buf);

        // 在切片过程中，没有发生数据复制
        ByteBuf f1 = buf.slice(0, 5);
        //调用 retain 方法计数加 1，表示调用者没用完之前，其它 handler 即使调用了 release 也不会造成回收
        f1.retain();
        // 'a','b','c','d','e', 'x'
        ByteBuf f2 = buf.slice(5, 5);
        f2.retain();
        log(f1);
        log(f2);

        System.out.println("释放原有 byteBuf 内存");
        buf.release();
        //注意：切片后仍然是原来内存位置，如果release再尝试调用f1的话会报错，所以前面需要调用retain方法
        log(f1);


        f1.release();
        f2.release();
        /*System.out.println("========================");
        f1.setByte(0, 'b');
        log(f1);
        log(buf);*/
    }
}
