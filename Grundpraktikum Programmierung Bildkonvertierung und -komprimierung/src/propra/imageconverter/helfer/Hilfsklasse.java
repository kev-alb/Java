package propra.imageconverter.helfer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

//Hilfsklasse, die diverse Hilfsmethoden zur Verfuegung stellt.
public class Hilfsklasse {

    //Eingabe: Bildbreite, -hoehe und Bits pro Bildpunkt. Rueckgabe: Datensegmentgroesse.
    public static long datensegmentgroesseAusBreiteHoeheBitsProPixel(long bildbreite, long bildhoehe, long bpbp) {        
        return bildbreite*bildhoehe*(bpbp/8);
    }    
    
    /*Eingabe: Byte-Array mit Laenge 1, 2, 4 oder 8 in Little Endian. Rueckgabe: Der dazu entsprechende long-Wert.
     * (Bei 8 Bytes wird von 80...00 bis 7f...ff ein negativer Wert zurückgegeben.)
     */
    public static long berechneLongAusBytesLittleEndian(byte[] b) {
        long ergebnis = 0;
        ByteBuffer bbuffer = ByteBuffer.wrap(b);
        switch (b.length) {
        case 1:
            ergebnis = Byte.toUnsignedInt(b[0]);
            break;
        case 2:
            bbuffer.order(ByteOrder.LITTLE_ENDIAN);
            ergebnis = Short.toUnsignedInt(bbuffer.getShort());
            break;
        case 4:
            bbuffer.order(ByteOrder.LITTLE_ENDIAN);
            ergebnis = Integer.toUnsignedLong(bbuffer.getInt());
            break;
        case 8:
            bbuffer.order(ByteOrder.LITTLE_ENDIAN);
            ergebnis = bbuffer.getLong();
            break;
        }
        return ergebnis;
    }
    
    
    /*Stellt einen long-Wert in einem Byte Array der Laenge laenge in Little Endian dar.
     * Durch die Typkonvertierung nach byte werden die niederwertigsten 8 Bits extrahiert.
     * Der Logical Right Shift schiebt die entsprechenden Bits an diese Position.
     */
    public static byte[] berechneByteArrayLittleEndianAusLong(long wert, int laenge) {
        byte[] b = null;
        switch(laenge) {
        case 1:
            b = new byte[] {
                    (byte) wert};
            break;
        case 2:
            b = new byte[] {
                    (byte) wert,
                    (byte) (wert >>> 8)};
            break;
        case 4:
            b = new byte[] {
                    (byte) wert,
                    (byte) (wert >>> 8),
                    (byte) (wert >>> 16),
                    (byte) (wert >>> 24)};
            break;
        case 8:
            b = new byte[] {
                    (byte) wert,
                    (byte) (wert >>> 8),
                    (byte) (wert >>> 16),
                    (byte) (wert >>> 24),
                    (byte) (wert >>> 32),
                    (byte) (wert >>> 40),
                    (byte) (wert >>> 48),
                    (byte) (wert >>> 56)};
        }
        return b;
    }
    
    //Ordnet das Datensegment von GBR nach BGR um.
    public static byte[] ordneDatensegmentVonGBRNachBGR(byte[] datensegment) {
    byte hilf;
    for (int i = 0; i < datensegment.length; i= i+3) {
        hilf = datensegment[i];
        datensegment[i] = datensegment[i+1];
        datensegment[i+1] = hilf;
    }        
    return datensegment;
    }
    
    //Ordnet das Datensegment von BGR nach GBR um.
    public static byte[] ordneDatensegmentVonBGRNachGBR(byte[] datensegment) {
    byte hilf;
    for (int i = 0; i < datensegment.length; i= i+3) {
        hilf = datensegment[i];
        datensegment[i] = datensegment[i+1];
        datensegment[i+1] = hilf;
    }        
    return datensegment;
    }
    
    //Prueft, ob ein Bildpunkt wiederholt wird. Falls ja, wird true zurueckgegeben.
    public static boolean wirdBildpunktWiederholt (byte[] datensegment, int i,
            byte ersterFarbkanal, byte zweiterFarbkanal, byte dritterFarbkanal) {
        
        return (ersterFarbkanal == datensegment[i+3] &&
                zweiterFarbkanal == datensegment[i+4] &&
                dritterFarbkanal == datensegment[i+5] );
        
    }
    
    //Prueft, ob ein Bildpunkt zweimal wiederholt wird. Falls ja, wird true zurueckgegeben.
    public static boolean wirdBildpunktZweimalWiederholt (byte[] datensegment, int i) {
        
        return ( (datensegment[i] == datensegment[i+3] &&
                    datensegment[i+1] == datensegment[i+4] &&
                    datensegment[i+2] == datensegment[i+5] ) 
                    &&
                    (datensegment[i] == datensegment[i+6] &&
                    datensegment[i+1] == datensegment[i+7] &&
                    datensegment[i+2] == datensegment[i+8] ) );
        
    }
    
    //Fuegt die 8 uebergebenen Bits 7 - 0 zu einem Byte zusammen.
    public static int erstelleByteAus8Bits(int bit7,int bit6, int bit5,int bit4,int bit3,int bit2,int bit1,int bit0) {
        return (bit7<<7) | (bit6<<6) | (bit5<<5) | (bit4<<4) | (bit3<<3) | (bit2<<2) | (bit1<<1) | bit0;
    }
    
            
}

