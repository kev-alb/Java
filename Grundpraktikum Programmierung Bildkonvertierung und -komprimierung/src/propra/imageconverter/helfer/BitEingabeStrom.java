package propra.imageconverter.helfer;

import java.io.ByteArrayInputStream;

//Strom für bitweises Einlesen
public class BitEingabeStrom {

    private int bytePuffer;
    private int aktuelleBitPosition;
    private int bit;
    private ByteArrayInputStream bais;
  
    public BitEingabeStrom(byte[] byteArr) {        
        bais = new ByteArrayInputStream(byteArr);
        bytePufferAuffuellen();
        aktuelleBitPosition = 7;
    }
  
  
    /*Gibt das naechste Bit im Strom zurueck. Solange der Strom nicht am Ende ist,
     * sind die moeglichen Werte 0 oder 1. Wenn der Strom am Ende ist, wird -1 zurueckgegeben.
     */
    public int read() {
      
        //Falls alle Bits aus dem Strom gelesen wurden.
        if (bytePuffer == -1) {
            return bytePuffer;
        }
                     
        //Naechstes Bit aus Puffer extrahieren. 
        bit = (bytePuffer >>> aktuelleBitPosition) & 0x01;
        aktuelleBitPosition--;
      
        //Eventuell noch den Puffer auffuellen.
        if (aktuelleBitPosition == -1) {
            bytePufferAuffuellen();
            aktuelleBitPosition = 7;
        }
                      
        return bit;
    }
  
    //Liest die naechsten 8 Bits ueber die Byte-Grenze hinweg und gibt diese als Byte zurueck.
    int readByte() {
        int bit7 = this.read();
        int bit6 = this.read();
        int bit5 = this.read();
        int bit4 = this.read();
        int bit3 = this.read();
        int bit2 = this.read();
        int bit1 = this.read();
        int bit0 = this.read();
        return Hilfsklasse.erstelleByteAus8Bits(bit7, bit6, bit5, bit4, bit3, bit2, bit1, bit0);
    }
  
    //Fuellt den Puffer mit dem naechsten Byte aus dem Strom auf.
    private void bytePufferAuffuellen() {        
        bytePuffer = bais.read();           
    }      
  
}
