/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package w14filelocking;

import TimeStamp.TimeStamp;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;
import javafx.scene.paint.Color;

/**
 *
 * @author jsf3
 */
public class mappedWrite implements Observer {

    private final KochFractal koch;
    private final File fileMapped;
    private int level;
    private final FileOutputStream writer;
    private final List<Edge> edges;
    private final TimeStamp timeStamp;

    public static void main(String[] args) throws IOException {
        new mappedWrite();
    }

    public mappedWrite() throws IOException {
        this.edges = new ArrayList<>();
        this.koch = new KochFractal();
        this.fileMapped = new File("/media/Fractal/fileMapped.tmp");
        this.writer = new FileOutputStream(fileMapped);
        this.koch.addObserver(this);
        this.level = 1;
        this.timeStamp = new TimeStamp();
        this.generate();
    }

    public void generate() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Welk level gegenereerd worden?: ");
        this.level = scanner.nextInt();
        this.koch.setLevel(level);
        edges.clear();
        this.koch.generateBottomEdge();
        this.koch.generateLeftEdge();
        this.koch.generateRightEdge();

        this.writeFileMapped();
        System.out.println(timeStamp.toString());
        File newdir = new File("/media/Fractal/fileMapped" + level + ".tmp");
        Files.move(fileMapped.toPath(), newdir.toPath(), REPLACE_EXISTING);
        this.generate();
    }

    @Override
    public void update(Observable o, Object arg) {
        Edge e = (Edge) arg;
        edges.add(e);
    }

    public void writeFileMapped() throws IOException {
        this.timeStamp.setBegin("begin writeFileMapped");
        this.fileMapped.delete();
        FileChannel fc = new RandomAccessFile(this.fileMapped, "rw").getChannel();
        long bufferSize = 8 * 1000000;
        MappedByteBuffer mappedBB = fc.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
        long counter = 0;
        mappedBB.putInt(level);
        for (Edge edge : edges) {
            mappedBB.putDouble(edge.X1);
            mappedBB.putDouble(edge.Y1);
            mappedBB.putDouble(edge.X2);
            mappedBB.putDouble(edge.Y2);
            mappedBB.putDouble(Color.valueOf(edge.color).getRed());
            mappedBB.putDouble(Color.valueOf(edge.color).getGreen());
            mappedBB.putDouble(Color.valueOf(edge.color).getBlue());
            counter++;
        }
        this.timeStamp.setEnd("eind writeFileMapped");
        System.out.println("Total edges written: " + counter);
    }
}
