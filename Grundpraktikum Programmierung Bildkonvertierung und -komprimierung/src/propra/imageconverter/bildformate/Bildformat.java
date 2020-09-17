package propra.imageconverter.bildformate;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import propra.imageconverter.helfer.*;

//Superklasse für alle Bildformate, die 3 Farbkanaele besitzen.
public abstract class Bildformat {
    
    private final int anzFarbkanaele = 3;
    
    byte[] bildbreite;
    byte[] bildhoehe;
    byte[] bpbp; //Bits pro Bildpunkt
    byte[] datensegment;
    
    //Methode zum Schreiben des Ausgabebildes.
    public abstract void schreibeAusgabebild(String output) throws IOException;
    
    /*Methoden fuer die Konvertierung. (Methoden für weitere Formate hier anfügen)
     * Die Konvertierung von Bildern erfolgt zuerst immer in das Standardformat (derzeit ProPra) und anschliessend
     * in das gewuenschte Bildformat des Ausgabildes (z.B. TGA, ProPra, weitere Bildformate).
     */
    public abstract Bildformat konvInProPra(String compression) throws IOException, OptionaleAnforderungenException;
    public abstract Bildformat konvInTGA(String compression) throws IOException, OptionaleAnforderungenException;


    
    //Methoden fuer die Dekomprimierung. (Bei weiteren Kompressionen hier die Methoden anfuegen)
    
    //Methode zum Dekomprimieren des Datensegments von RLE.
    void dekomprimiereDatensegmentVonRLE() throws IOException {
        
        //Dieser Strom enthaelt nach und nach die Bytes für das unkomprimierte Datensegment.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        //Beginn der Dekomprimierung
        int i = 0;
        byte steuerbyte = datensegment[i];
        
        while(i < datensegment.length) {
            
            steuerbyte = datensegment[i];
        
            if ((((Byte.toUnsignedInt(steuerbyte)) >>7) & 0x01) == 1) {
              //Wiederholungszaehler. oder: (steuerbyte >>> 7) & 0x01
                
                //Schreibe die Farbwerte so oft in den Strom wie es Bits 7-0 des Steuerbytes erhoeht um 1 angeben.
                for (int j = 1; j <= (int) ((steuerbyte & 0x7F) + 0x01) ; j++) {
                    baos.write(datensegment, i+1, anzFarbkanaele);
                }
                
                i = i + anzFarbkanaele + 1; //Geht zum naechsten Steuerbyte.   
                
            } else { //implizit heisst das: ((((Byte.toUnsignedInt(steuerbyte)) >>7) & 0x01) == 0). Datenzaehler.
                
                for (int j = 1; j <= (int) ((steuerbyte & 0x7F) + 0x01) ; j++) {
                    baos.write(datensegment, i+1, anzFarbkanaele);
                    i = i+anzFarbkanaele;
                }
                i++;
            }
        
        }
        
        //Ende der Dekomprimierung. Alle Daten im Strom.
            
        //Schreibe die un-/dekomprimierten Daten in ein neues Byte-Array und mache dieses zum Datensegment.
        datensegment = baos.toByteArray();
        
    } //dekomprimiereDatensegmentVonRLE

    
    //Methode zum Dekomprimieren des Datensegments von Huffman.
    void dekomprimiereDatensegmentVonHuffman() throws IOException, OptionaleAnforderungenException {
        
        BitEingabeStrom bes = new BitEingabeStrom(datensegment); //Strom des komprimierten Datensegments.
        int aktBit; //Das aktuelle Bit;
        HuffmanBaum huffmanBaum = null; //Binaerer Huffman-Baum        
        ByteArrayOutputStream baos = new ByteArrayOutputStream(); //Dieser Strom enthaelt nach und nach die Bytes für das dekomprimierte Datensegment.               
        int wert; //Der in den Strom des dekomprimierten Datensegments zu schreibende Wert eines Blattes im Huffman-Baum.
                                       
        //Huffmann-Baum erstellen. Entspricht erstem Bit (= 0).         
        
        if ( (aktBit = bes.read()) == 0 ) {
            
            huffmanBaum = new HuffmanBaum(bes);      
            
        } else { //Kein Huffman-Baum angegeben.
            throw new OptionaleAnforderungenException("Fehler: Datensegment enthaelt keinen Huffman-Baum.");
        }
        
        //Blatt suchen und dessen Wert in den Strom des dekomprimierten Datensegments schreiben.
        
        while ( (aktBit=bes.read()) != -1 ) {
            wert = huffmanBaum.sucheBlattUndGibWert(aktBit);
            //Falls das letzte Bit im Strom einen inneren Knoten liefert, wird nichts in den Strom geschrieben, also der Rest verworfen.
            if (wert != -1) {
                baos.write(wert);
            }                        
        }
                                
        //Schreibe die un-/dekomprimierten Daten in ein neues Byte-Array und mache dieses zum Datensegment.
        datensegment = baos.toByteArray();
        
    } //dekomprimiereDatensegmentVonHuffman
    
    

    //Folgende Methoden dienen der Komprimierung. (Bei weiteren Kompressionstypen, hier Methoden anfuegen)
    
    //Methode zum Komprimieren des Datensegments in RLE.
    void komprimiereDatensegmentInRLE() throws IOException {

        //Dieser Strom enthaelt nach und nach die Bytes für das komprimierte Datensegment.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //---------------------------------
        
        //Beginn der Komprimierung
        
        long anzFarbkanaeleProZeile = Hilfsklasse.berechneLongAusBytesLittleEndian(bildbreite) * anzFarbkanaele;
        byte steuerbyte;
        int i = 0;//beginne beim ersten Bildpunkt (dessen ersten Farbkanal).
        int wiederholungszaehler; //entspricht der Anzahl von direkten Wiederholungen des aktuellen Bildpunktes.
        int datenzaehler; //wenn Steuerbyte als Datenzaehler
        int j = i;
        
        byte farbkanal1;
        byte farbkanal2;
        byte farbkanal3;

        
        //Erstellung der Pakete.
        while(i<datensegment.length) {
        
            //Initialisierung.
            wiederholungszaehler = 0;
            datenzaehler = 0;
            
            j = i; //j merkt sich den Bildpunkt, bei dem i startet.
            
            
            /*RGB Farbkanaele. Also ein Bildpunkt (Farbkanaele des ersten Bildpunktes in der while-Schleife,
             * also nachdem ein Wiederholungs- oder Datenzaehler erstellt wurde und die Daten in den Strom geschrieben wurden.).
             */
            farbkanal1 = datensegment[i];
            farbkanal2 = datensegment[i+1];
            farbkanal3 = datensegment[i+2];
            
            /*Zaehle, wie oft sich der aktuelle Bildpunkt wiederholt. Stopp, wenn (1) Zaehler 127 (voll) oder
             * (2) aktueller Bildpunkt der letzte in der Zeile oder (3) naechster Bildpunkt nicht gleich dem aktuellen ist.
             * Reihenfolge und && statt & wichtig.
             */
            while( wiederholungszaehler < 127 &&
                    (i % anzFarbkanaeleProZeile) < (anzFarbkanaeleProZeile - anzFarbkanaele) &&
                    Hilfsklasse.wirdBildpunktWiederholt(datensegment, i, farbkanal1, farbkanal2, farbkanal3)) {
                
                wiederholungszaehler++;
                i = i + anzFarbkanaele;
            }
            //i zeigt auf den letzten Bildpunkt der gleich dem aktuellen Bildpunkt ist.
            
            //Abbruchanalyse der while-Schleife. Die Belegung der Variablen in der while-Bedingung hat 8 Faelle, 7 Faelle fuehren zum Abbruch.
            if ( !( wiederholungszaehler < 127)) {
                //Fall 1 - 4: Der Wiederholungszaehler ist voll. Die anderen beiden Bedingungen sind irrelevant.
                
                //Steuerbyte erstellen
                steuerbyte = (byte) (wiederholungszaehler | 0x80);
                    
                //Steuerbyte und wiederholten Bildpunkt in Strom schreiben.
                baos.write(steuerbyte);
                baos.write(farbkanal1);
                baos.write(farbkanal2);
                baos.write(farbkanal3);
                
                //Gehe zum naechsten Bildpunkt.
                i = i+ anzFarbkanaele;                                
                
            } else { //Der Wiederholungszaehler ist nicht voll. Faelle 5 - 7.
                
                if (! ((i % anzFarbkanaeleProZeile) < (anzFarbkanaeleProZeile - anzFarbkanaele))) {
                    /*Fall 5 - 6: (Der Wiederholungszaehler hat noch Platz, d.h. liegt im Bereich 0, 1, ..., 126).
                     * UND i zeigt auf den letzten Bildpunkt in der Zeile.
                     * Die dritte Bedingung ist irrelevant.
                     */                        
                    
                    //Steuerbyte erstellen. Bei wiederholungszaehler == 0 könnte man auch einen datenzaehler erstellen.
                    steuerbyte = (byte) (wiederholungszaehler | 0x80);
                        
                    //Steuerbyte und wiederholten Bildpunkt in Strom schreiben.
                    baos.write(steuerbyte);
                    baos.write(farbkanal1);
                    baos.write(farbkanal2);
                    baos.write(farbkanal3);
                    
                    //Gehe zum naechsten Bildpunkt.
                    i = i+ anzFarbkanaele;       
                    
                } else {
                    /*Fall 7: (Der Wiederholungszaehler hat noch Platz und i zeigt nicht auf den letzten Bildpunkt in der Zeile
                     * UND der naechste Bildpunkt ist anders. Also ist nur die dritte Bedingung falsch.
                     * 
                     * Unterscheide zwei Unterfaelle: Wiederholungszaehler >= 2 (a) oder == 0 oder 1 (b).
                     */
                                            
                    if (wiederholungszaehler >= 2) {
                        /*Fall 7a: Falls wiederholungszaehler >= 2, also die drei ersten Bildpunkte hintereinander gleich sind,
                         * lohnt sich ein Wiederholungszaehler.
                         */

                        //Steuerbyte erstellen
                        steuerbyte = (byte) (wiederholungszaehler | 0x80);
                            
                        //Steuerbyte und wiederholten Bildpunkt in Strom schreiben.
                        baos.write(steuerbyte);
                        baos.write(farbkanal1);
                        baos.write(farbkanal2);
                        baos.write(farbkanal3);
                        
                        //Gehe zum naechsten Bildpunkt.
                        i = i+ anzFarbkanaele;      
                        
                    } else {
                        /*Fall 7b: Falls der Wiederholungszaehler nur 0 oder 1 ist, lohnt sich kein Wiederholungszaehler.
                         * Ein Datenzaehler muss her.
                         */
                        
                        //Zur Uebersichtlichkeit wird hier vom Wiederholungszaehler zum Datenzaehler gewechselt.
                        datenzaehler = wiederholungszaehler;
                        
                        /*Zaehle hierfuer, wie lang die Folge von Bildpunkten ist, in der kein Bildpunkt zweimal direkt
                         * aufeinander folgt/zwei benachbarte Bildpunkte nicht gleich sind.
                         * Bedingungen in richtiger Reihenfolge: Stopp, sobald ...
                         * 1. Datenzaehler voll (127) ist,
                         * 2. i auf den vorletzten Bildpunkt einer Zeile zeigt,
                         * 3. die naechsten beiden Bildpunkte gleich dem Bildpunkt von i sind.
                         */ 
                        while( datenzaehler < 127 &&
                                ((i % anzFarbkanaeleProZeile) < anzFarbkanaeleProZeile - anzFarbkanaele - anzFarbkanaele) &&
                                ! (Hilfsklasse.wirdBildpunktZweimalWiederholt(datensegment, i)) ) {

                            datenzaehler++;
                            i = i + anzFarbkanaele;
                        }//while
                                                
                        //Abbruchanalyse der while-Schleife: Wieder 8 Faelle, 7 davon fuehren zum Abbruch.
                        if (!(datenzaehler < 127)) {
                            //Fall I - IV: Der Datenzaehler ist voll. Die anderen beiden Bedingungen sind irrelevant.                            
                                                      
                            //Steuerbyte erstellen und in Strom schreiben
                            steuerbyte = (byte) (datenzaehler & 0x7F);
                            baos.write(steuerbyte);
                            
                            /*Schreibe alle Daten von j (Beginn der Folge) bis einschliesslich dem i-ten Bildpunkt
                             * in den Strom.
                             */
                            for (int k = j; k < i + anzFarbkanaele; k++) {
                                baos.write(datensegment[k]);                   
                            }
                            
                            //Gehe zum naechsten Bildpunkt.
                            i = i+ anzFarbkanaele;  
                            
                        } else { //Der Datenzaehler ist nicht voll. Faelle V - VII.                                                                  
                            
                            if (! ((i % anzFarbkanaeleProZeile) < (anzFarbkanaeleProZeile - anzFarbkanaele - anzFarbkanaele))) {
                                /*Fall V - VI: (Der Datenzaehler hat noch Platz, d.h. liegt im Bereich 0, 1, ..., 126).
                                 * UND i zeigt auf den VORletzten Bildpunkt in der Zeile.
                                 * Die dritte Bedingung ist irrelevant.                                
                                 */
                                
                                /*Der Datenzaehler wird um 1 erhoeht, um auch den letzten Bildpunkt in der Zeile in die Folge /ins Paket zu nehmen
                                 * und gehe zum naechsten, d.h. letzten Bildpunkt in der Zeile.
                                 */
                                datenzaehler++;
                                i = i+ anzFarbkanaele;
                                                                
                                //Steuerbyte erstellen und in Strom schreiben
                                steuerbyte = (byte) (datenzaehler & 0x7F);
                                baos.write(steuerbyte);
                                
                                /*Schreibe alle Daten von j (Beginn der Folge) bis einschliesslich dem i-ten Bildpunkt
                                 * in den Strom.
                                 */
                                for (int k = j; k < i + anzFarbkanaele; k++) {
                                    baos.write(datensegment[k]);                   
                                }
                                
                                //Gehe zum naechsten Bildpunkt, also in die naechste Zeile.
                                i = i+ anzFarbkanaele;  
                                
                                
                            } else {
                                /*Fall VII: (Der Datenzaehler hat noch Platz, d.h. liegt im Bereich 0, 1, ..., 126).
                                 * (und i zeigt nicht auf den VORletzten Bildpunkt in der Zeile)
                                 * UND die naechsten beiden Bildpunkte sind gleich dem Bildpunkt von i. Also ist nur die dritte Bedingung falsch.
                                 * 
                                 * Unterscheide zwei Unterfaelle: Datenzaehler (a) >= 2 oder == 1 oder (b) == 0.
                                 */
                                
                                if(datenzaehler >= 1) {
                                    //Fall VIIa
                                    
                                    /*-Schreibe alle Daten von j bis i-1 in den Strom.
                                     * -Verringere den Datenzaehler um 1, da der i-te Bildpunkt mindestens zweimal wiederholt wird
                                     * und sich somit ein Wiederholungszaehler lohnt (eventuell wiederholt er sich auch noch oefter).
                                     * -Gehe daher auch nicht zum naechsten Bildpunkt, da der i-te Bildpunkt naemlich im
                                     * naechsten (aeussersten) while-Durchlauf erneut auf Wiederholungen geprueft wird.
                                     * -Schreibe alle Daten von j bis i-1 in den Strom. (i-ten Bildpunkt nicht schreiben)
                                     */
                                    
                                    datenzaehler--;
                                    
                                    //Steuerbyte erstellen und in Strom schreiben
                                    steuerbyte = (byte) (datenzaehler & 0x7F);
                                    baos.write(steuerbyte);
                                    
                                    /*Schreibe alle Daten von j (Beginn der Folge) bis vor dem i-ten Bildpunkt
                                     * in den Strom.
                                     */
                                    for (int k = j; k < i; k++) {
                                        baos.write(datensegment[k]);                   
                                    }
                                    

                                } else {
                                    /*Fall VIIb: Datenzaehler == 0. Schreibe nichts in den Strom
                                     * und gehe auch nicht zum naechsten Bildpunkt, da der i-te Bildpunkt
                                     * einen Wiederholungszaehler bekommen wird, dessen Laenge im naechsten
                                     * (aeussersten) while-Durchlauf bestimmt wird.
                                     * Also hier nichts machen.
                                     */
                                    
                                    ;
                                    
                                } //if(datenzaehler >= 1)
                                
                            }//if (! (i < (anzFarbkanaeleProZeile - anzFarbkanaele - anzFarbkanaele)))
                            
                        }// if (!(datenzaehler < 127))
                        
                    } //if (wiederholungszaehler >= 2)
                    
                } // if (! (i < (anzFarbkanaeleProZeile - anzFarbkanaele))) 
                
            }//if ( !( wiederholungszaehler < 127))     
            
        
        }//while(i<datensegment.length)
        
                        
        
        
        //Ende der Komprimierung. Alle Daten im Strom.
        //---------------------------------
        
        
        //Schreibe die komprimierten Daten in ein neues Byte-Array und mache dieses zum Datensegment.
        datensegment = baos.toByteArray();

    } //komprimiereDatensegmentInRLE
                        
    
    

}
