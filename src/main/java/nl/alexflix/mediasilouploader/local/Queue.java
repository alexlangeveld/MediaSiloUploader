//package nl.alexflix.mediasilouploader.local;
//
//import nl.alexflix.mediasilouploader.local.types.Export;
//import nl.alexflix.mediasilouploader.Main;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.concurrent.LinkedBlockingQueue;
//
//public class Queue implements Runnable{
//
//    private static LinkedBlockingQueue<Export> queue = new LinkedBlockingQueue<>();
//    @Override
//    public void run() {
//        while (!Main.exit()) {
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
//    public static synchronized void addToQueue(File file) {
//        Export export = new Export(file);
//        queue.offer(export);
//
//    }
//    public static synchronized Export getNext() {
//        Export rtn = queue.poll();
//        return rtn;
//    }
//
//
//}
