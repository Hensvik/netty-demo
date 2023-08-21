package cn.itcast.nio.c3;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

//遍历文件夹 walkFileTree
public class TestFilesWalkFileTree {
    //递归遍历并删除文件（谨慎运行，该方法不走回收站）
    public static void main(String[] args) throws IOException {
//        Files.delete(Paths.get("D:\\Snipaste-1.16.2-x64 - 副本"));
        Files.walkFileTree(Paths.get("D:\\Snipaste-1.16.2-x64 - 副本"), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);     //删除文件
                return super.visitFile(file, attrs);
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);      //删除目录
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    //统计jar文件数量
    private static void m2() throws IOException {
        AtomicInteger jarCount = new AtomicInteger();
        Files.walkFileTree(Paths.get("C:\\Program Files\\Java\\jdk1.8.0_91"), new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".jar")) {
                    System.out.println(file);
                    jarCount.incrementAndGet();
                }
                return super.visitFile(file, attrs);
            }
        });
        System.out.println("jar count:" +jarCount);
    }

    //统计文件数量
    private static void m1() throws IOException {
        AtomicInteger dirCount = new AtomicInteger();
        AtomicInteger fileCount = new AtomicInteger();
        Files.walkFileTree(Paths.get("C:\\Program Files\\Java\\jdk1.8.0_91"), new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                System.out.println("====>"+dir);
                dirCount.incrementAndGet();
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println(file);
                fileCount.incrementAndGet();
                return super.visitFile(file, attrs);
            }
        });
        System.out.println("dir count:" +dirCount);
        System.out.println("file count:" +fileCount);
    }
}
