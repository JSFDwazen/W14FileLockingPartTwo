/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package w14filelocking;

import GUI.JSF31KochFractalFX;
import TimeStamp.TimeStamp;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.scene.paint.Color;

/**
 *
 * @author jsf3
 */
public class mappedRead {

    private File fileMapped;
    private int level;
    private final List<Edge> edges;

    private static final boolean EXCLUSIVE = false;
    private static final boolean SHARED = true;
    private static final int NBYTES = 64;
    private static final int STATUS_NOT_READ = 1;
    private static final int STATUS_READ = 0;
    private int counter = 0;

    private int MAXVAL;

    private FileChannel ch;
    private MappedByteBuffer mappedBB;
    private RandomAccessFile raf;

    private Thread t;

    public static void main(String[] args) throws IOException {
        new mappedRead();
    }

    public mappedRead() throws IOException {
        this.edges = new ArrayList<>();
        this.fileMapped = new File("/media/Fractal/fileMappedLock.tmp");
        this.raf = new RandomAccessFile(this.fileMapped, "rw");
        this.ch = this.raf.getChannel();
        this.mappedBB = this.ch.map(FileChannel.MapMode.READ_WRITE, 0, NBYTES);
        readConsumer();
        System.out.println(" done");
        System.out.println("number of edges: " + edges.size());
        JSF31KochFractalFX.edges = this.edges;
        this.t = new Thread() {
            public void run() {
                JSF31KochFractalFX.main(new String[0]);
            }
        };
        this.t.start();
    }

    public void readConsumer() {
        FileLock exclusiveLock = null;
        try {
            boolean finished = false;
            System.out.print("loading");
            while (!finished) {
                // Probeer het lock te verkrijgen
                exclusiveLock = ch.lock(0, NBYTES, EXCLUSIVE);

                // layout: 
                //      0 .. 3 :    4 bytes int with maxvalue
                //      4 .. 7 :    4 bytes int with status
                //      8 .. 63:    56 bytes double with value
                // Vraag de maximumwaarde, status en geproduceerde waarde op
                mappedBB.position(0);
                MAXVAL = mappedBB.getInt();
                int status = mappedBB.getInt();

                if (status == STATUS_NOT_READ) {
                    // Nieuwe waarde gelezen. Zet status in bestand
                    double X1 = mappedBB.getDouble();
                    double Y1 = mappedBB.getDouble();
                    double X2 = mappedBB.getDouble();
                    double Y2 = mappedBB.getDouble();
                    String color = new Color(mappedBB.getDouble(), mappedBB.getDouble(), mappedBB.getDouble(), 1).toString();

                    //create edge
                    Edge edge = new Edge(X1, Y1, X2, Y2, color);
                    edges.add(edge);
                    counter++;
                    mappedBB.position(4);
                    mappedBB.putInt(STATUS_READ);
                    // Bepaal of we klaar zijn, dat is als de gelezen waarde
                    // gelijk is aan de maxVal in bytes 0 .. 3 van het bestand
                    System.out.print(".");
                    finished = (counter == MAXVAL);
                }
                // release the lock
                exclusiveLock.release();
            }
        } catch (IOException ex) {
            Logger.getLogger(mappedWrite.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (exclusiveLock != null) {
                try {
                    exclusiveLock.release();
                } catch (IOException ex) {
                    Logger.getLogger(mappedWrite.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
