/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package w14filelocking;

import TimeStamp.TimeStamp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.paint.Color;

/**
 *
 * @author jsf3
 */
public class mappedWrite implements Observer {

    private final KochFractal koch;
    private final File fileMapped;
    private int level;
    private FileChannel ch;
    private MappedByteBuffer mappedBB;
    private RandomAccessFile raf;
    private List<Edge> edges;

    private final boolean EXCLUSIVE = false;
    private final boolean SHARED = true;
    private int MAXVAL;
    private final int NBYTES = 64;
    private final int STATUS_NOT_READ = 1;
    private final int STATUS_READ = 0;
    private boolean sleep = false;

    public static void main(String[] args) throws IOException {
        new mappedWrite();
    }

    public mappedWrite() throws IOException {
        this.koch = new KochFractal();
        this.fileMapped = new File("/media/Fractal/fileMappedLock.tmp");
        this.raf = new RandomAccessFile(this.fileMapped, "rw");
        this.ch = this.raf.getChannel();
        this.mappedBB = this.ch.map(FileChannel.MapMode.READ_WRITE, 0, this.NBYTES);
        this.edges = new ArrayList<>();
        this.koch.addObserver(this);
        this.level = 1;
        this.generate();
    }

    public void generate() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Welk level gegenereerd worden?: ");
        this.level = scanner.nextInt();
        System.out.print("met sleep voor visibility?(y/n): ");
        String yn = scanner.next();
        sleep = yn.equals("Y") || yn.equals("y");
        this.koch.setLevel(level);
        MAXVAL = level == 1 ? 3 : (int) (3 * Math.pow(4, level - 1));
        this.koch.generateBottomEdge();
        this.koch.generateLeftEdge();
        this.koch.generateRightEdge();
        writeAsProducer();
    }

    @Override
    public void update(Observable o, Object arg) {
        Edge e = (Edge) arg;
        edges.add(e);
    }

    public void writeAsProducer() {

        FileLock exclusiveLock = null;
        try {
            int counter = 0;
            while (counter < edges.size()) // Probeer het lock te verkrijgen
            {
                exclusiveLock = ch.lock(0, NBYTES, EXCLUSIVE);

                /**
                 * Now modify the data . . .
                 */
                // layout: 
                //      0 .. 3 :    4 bytes int with maxvalue
                //      4 .. 7 :    4 bytes int with status
                //      8 .. 63:    56 bytes double with value
                // Vraag waarde van status op
                mappedBB.position(4);
                int status = mappedBB.getInt();

                // Alleen als de voorgaande geproduceerde waarde is gelezen
                // dwz status != STATUS_NOT_READ
                // <of> 
                // als er nog niets geproduceerd is kunnen we schrijven
                if (((status != STATUS_NOT_READ))) {
                    // Ga naar het begin van het bestand
                    mappedBB.position(0);
                    Edge edge = edges.get(counter);
                    // Schrijf maxwaarde weg
                    mappedBB.putInt(MAXVAL);
                    mappedBB.putInt(STATUS_NOT_READ);
                    mappedBB.putDouble(edge.X1);
                    mappedBB.putDouble(edge.Y1);
                    mappedBB.putDouble(edge.X2);
                    mappedBB.putDouble(edge.Y2);
                    mappedBB.putDouble(Color.valueOf(edge.color).getRed());
                    mappedBB.putDouble(Color.valueOf(edge.color).getGreen());
                    mappedBB.putDouble(Color.valueOf(edge.color).getBlue());
                    counter++;
                }
                if (sleep) {
                    Thread.sleep(10);
                }
                // release the lock
                exclusiveLock.release();
            }
        } catch (IOException | InterruptedException ex) {
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
