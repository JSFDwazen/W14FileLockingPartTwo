/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package w14filelocking;

import java.io.Serializable;

/**
 *
 * @author Tenshi
 */
public class Edge implements Serializable {

    public double X1, Y1, X2, Y2;
    public String color;

    public Edge(double X1, double Y1, double X2, double Y2, String color) {
        this.X1 = X1;
        this.Y1 = Y1;
        this.X2 = X2;
        this.Y2 = Y2;
        this.color = color;
    }
}
