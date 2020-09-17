package propra.imageconverter.helfer;

//Klasse fuer einen (binaeren) Huffman-Baum.
public class HuffmanBaum {

    private Knoten wurzel;
    private BitEingabeStrom bes;
    
    public HuffmanBaum(BitEingabeStrom bes) {     
        this.bes = bes;
        wurzel = new Knoten(-1); //Wurzel/inneren Knoten mit -1 markieren.
        wurzel.linkesKindHinzufuegen(bes.read());
        wurzel.rechtesKindHinzufuegen(bes.read());
    }
    
    //Sucht das Blatt zu einer Folge von Bits, wobei das erste Bit schon uebergeben wurde. Gibt den Wert des Blattes zurueck.
    public int sucheBlattUndGibWert(int erstesBit) {
        Knoten zeiger; //Hilfszeiger, der der Bitfolge entsprechend ueber die Knoten wandert.
        int aktBit = erstesBit; //Aktuelles Bit der Bitfolge. Zu Beginn entspricht es dem ersten Bit der Bitfolge.
        
        //Da das erste Bit schon uebergeben wurde, muss der Hilfszeiger auf eines der beiden Kinder der Wurzel gesetzt werden.
        if (erstesBit == 0) {
            zeiger = this.wurzel.gibLinkesKind();
        } else { //erstesBit == 1
            zeiger = this.wurzel.gibRechtesKind();
        }
        
        /*Suche nach dem Blatt fortsetzen. Stopp, wenn Blatt erreicht oder Bitstrom zuende ist.
         * Im Falle eines Blattes, also erste Bedingung false, verhindert && das Lesen des naechsten Bits.
         */
        while ( !(zeiger.gibLinkesKind() == null & zeiger.gibRechtesKind() == null) && (aktBit = bes.read()) != -1) {
            
            if (aktBit == 0) {
                zeiger = zeiger.gibLinkesKind();
            } else { //aktBit == 1
                zeiger = zeiger.gibRechtesKind();
            }
                      
        }
        
        return zeiger.gibWert(); //Den Wert des Blattes, also das Byte-Symbol (bzw. Sonderfall: Innerer Knoten, also -1) zurueckgeben.
    }
    
    
    //Innere Klasse fuer einen Knoten
    private class Knoten {
        private int wert;
        private Knoten linkesKind = null;
        private Knoten rechtesKind = null;
        
        private Knoten(int wert) {
            this.wert = wert;
        }
        
        //Fuegt dem this-Knoten rekursiv einen linken Teilbaum als Kind hinzu.
        private void linkesKindHinzufuegen(int aktBit) {
            if (aktBit == 0) {
                this.linkesKind = new Knoten(-1); //Linkes Kind ist ein innerer Knoten. Inneren Knoten mit -1 markieren.
                this.linkesKind.linkesKindHinzufuegen(bes.read());
                this.linkesKind.rechtesKindHinzufuegen(bes.read());
            } else { //aktBit == 1, linkes Kind ist ein Blatt. Abbruchbedingung
                this.linkesKind = new Knoten(bes.readByte());
            }
            
        }
        
        //Fuegt dem this-Knoten rekursiv einen rechten Teilbaum als Kind hinzu.
        private void rechtesKindHinzufuegen(int aktBit) {
            if (aktBit == 0) {
                this.rechtesKind = new Knoten(-1); //Rechtes Kind ist ein innerer Knoten. Inneren Knoten mit -1 markieren.
                this.rechtesKind.linkesKindHinzufuegen(bes.read());
                this.rechtesKind.rechtesKindHinzufuegen(bes.read());
            } else { //aktBit == 1, rechtes Kind ist ein Blatt. Abbruchbedingung
                this.rechtesKind = new Knoten(bes.readByte());
            }
        }
        
        //Getter-Methoden
        
        private int gibWert() {
            return this.wert;
        }
        
        private Knoten gibLinkesKind() {
            return this.linkesKind;
        }
        
        private Knoten gibRechtesKind() {
            return this.rechtesKind;
        }
        
    }
    
    
  }
