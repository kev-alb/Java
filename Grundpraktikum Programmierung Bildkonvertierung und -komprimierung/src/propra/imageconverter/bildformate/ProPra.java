package propra.imageconverter.bildformate;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import propra.imageconverter.helfer.*;

//Klasse fuer das ProPra-Bildformat. Das ProPra-Bildformat ist das neue Standardformat.
public class ProPra extends Bildformat {
    
    static final long kompressionstypUnkomprimiert = 0;
    static final long kompressionstypRLE = 1;
    static final long kompressionstypHuffman = 2;
    private static final HashSet<Long> kompressionstypen = new HashSet<Long>(Arrays.asList(
            ProPra.kompressionstypUnkomprimiert,
            ProPra.kompressionstypRLE,
            ProPra.kompressionstypHuffman));
    
    private final int headerGroesse = 28; //Header-Groesse ist 28
    private final byte[] magischeZahl = {0x50, 0x72, 0x6F, 0x50, 0x72, 0x61, 0x57, 0x53, 0x31, 0x39}; //ProPraWS19 in UTF-8
    private final long  bitsProBildpunktGueltig = 24; //immer 24        
    
    private byte[] formatkennung = new byte[gibMagischeZahlArray().length];
    private byte[] kompressionstyp = new byte[1];
    private byte[] datensegmentgroesse = new byte[8];
    private byte[] pruefsumme = new byte[4];
    
    //Sichtbare geerbte Attribute: byte[] datensegment byte[] bildbreite, byte[] bildhoehe, byte[] bpbp
    {
        bildbreite = new byte[2];
        bildhoehe = new byte[2];
        bpbp = new byte[1];
    }
            
    //Konstruktor zum Einlesen mit anschliessender Pruefung optionaler Anforderungen von KE1
    public ProPra(FileInputStream fis) throws IOException, OptionaleAnforderungenException{
        int dateigroesse = fis.available();
        fis.read(gibFormatkennungArray());
        fis.read(gibBildbreiteArray());
        fis.read(gibBildhoeheArray());
        fis.read(gibBitsProBildpunktArray());
        fis.read(gibKompressionstypArray());
        fis.read(gibDatensegmentgroesseArray());
        fis.read(gibPruefsummeArray());
        setzeDatensegment(new byte[dateigroesse-gibHeaderGroesse()]);
        fis.read(gibDatensegment());
        
        //Prueft magische Zahl im Header
        for (int i = 0; i < gibFormatkennungArray().length; i++) {
            if (gibFormatkennungArray()[i] != gibMagischeZahlArray()[i]) {
                throw new OptionaleAnforderungenException("Falsche magische Zahl im Header.");
            }   
        }
                     
        //Prueft BitsProBildpunkt = 24
        if (gibBitsProBildpunkt() != gibBitsProBildpunktGueltig()) {
            throw new OptionaleAnforderungenException("Bits pro Bildpunkt hat keinen gueltigen Wert (" + gibBitsProBildpunktGueltig() + ").");
        }
                
        //Prueft auf inkonsistente Bilddimension (Nullbreite/Nullhöhe)
        if (gibBildbreite() == 0 || gibBildhoehe() ==0) {
            throw new OptionaleAnforderungenException("Inkonsistente Bilddimension (Nullbreite/Nullhoehe).");
        }
        
        //Prueft auf nicht unterstützten Kompressionstypen.               
        if (! ProPra.kompressionstypen.contains(gibKompressionstyp())) {
            throw new OptionaleAnforderungenException("Nicht unterstuetzter Kompressionstyp.");
        }
        
    }
    
    //Konstruktor für die Konvertierung, initialisiert die unabhaengigen Elemente.
    ProPra(){
        setzeFormatkennung(gibMagischeZahlArray());
        setzeBitsProBildpunkt(gibBitsProBildpunktGueltig());
        //Es bleiben uebrig: bildbreite, bildhoehe, kompressionstyp, datensegmentgroesse, pruefsumme, datensegment
    }
         
    //Berechnet die Pruefsumme aus dem Attribut datensegment. Rueckgabe als long. Methode ist spezifisch für ProPra-Format.
    long berechnePruefsumme() {
        final long x = 65513;
        long p;
        long[] ai = new long[gibDatensegment().length];
        if (gibDatensegment().length == 0){
            p = 1;
        } else {
            //1. Eintrag mit 0 adressiert
            ai[0] = (1 + Byte.toUnsignedInt(gibDatensegment()[0])) % x;
            //vom 2. Eintrag bis n-ten Eintrag
            for (int i = 1; i<gibDatensegment().length; i++) {
                ai[i] = (ai[i-1] + (i+1) + Byte.toUnsignedInt(gibDatensegment()[i])) % x;
            }
            long bi = 1;
            for (int i = 0; i < gibDatensegment().length; i++) {
                bi = (bi + ai[i]) % x;
            }
            p = ai[gibDatensegment().length-1] * (int) Math.pow(2, 16) + bi;
        }
        return p;        
    }
    
    //ProPra in TGA konvertieren
    @Override
    public TGA konvInTGA(String compression) throws IOException, OptionaleAnforderungenException {
        /*Erzeuge ein TGA-Objekt mit dem Konstruktor,
         * der die vom Eingabebild unabhängigen Elemente mit Werten initialisiert.*/
        TGA tga = new TGA();
        
        //Bildtyp anpassen
        switch (compression) {
        case "uncompressed":
            tga.setzeBildtyp(TGA.bildtypRGBUnkomprimiert);
            break;
        case "rle":
            tga.setzeBildtyp(TGA.bildtypRGBRLE);
            break;
        }
        
        //Uebertrage die Werte des Eingabebildes in das TGA-Objekt fuer folgende Header-Elemente:
        tga.setzeNullpunktY(this.gibBildhoehe());
        tga.setzeBildbreite(this.gibBildbreite());
        tga.setzeBildhoehe(this.gibBildhoehe());

        /*Prueft auf falsche Dateigroesse im Header: Ist die angegebene Datensegmentgroesse
         * tatsaechlich gleich der Groesse des eingelesen (also eventuell komprimierten) Datensegments?
         */
        if (this.gibDatensegmentgroesse() != (long) this.gibDatensegment().length) {
            throw new OptionaleAnforderungenException("Falsche Dateigroesse im Header.");
        }
        
        //Prueft auf die Korrektheit der Pruefsumme.
        if (this.berechnePruefsumme() != this.gibPruefsumme()) {
            throw new OptionaleAnforderungenException("Ueberpruefen der Pruefsumme.");
        }
                
        //Datensegment dekomprimieren
        this.dekomprimiereDatensegment();
        
        //Pruefe auf fehlende Daten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge.
        if (Hilfsklasse.datensegmentgroesseAusBreiteHoeheBitsProPixel(this.gibBildbreite(), this.gibBildhoehe(), this.gibBitsProBildpunkt()) > this.gibDatensegment().length) {
            throw new OptionaleAnforderungenException("Fehlende Daten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge/Dateigroesse.");
        }
        
        //Pruefe auf zu viele Bilddaten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge.
        if (Hilfsklasse.datensegmentgroesseAusBreiteHoeheBitsProPixel(this.gibBildbreite(), this.gibBildhoehe(), this.gibBitsProBildpunkt()) < this.gibDatensegment().length) {
            throw new OptionaleAnforderungenException("Zu viele Bilddaten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge.");
        }
        
        /*Datensegment von GBR (ProPra) nach BGR (TGA) umordnen. (Call by value: value ist die Referenz auf das Datensegment.)
         * Beide datensegment-Variablen referenzieren danach dasselbe Array.
         */
        tga.setzeDatensegment(Hilfsklasse.ordneDatensegmentVonGBRNachBGR(this.gibDatensegment()));
          
        //Datensegment komprimieren.
        tga.komprimiereDatensegment(compression);
        this.setzeDatensegment(null); //Zum Sparen von Speicherplatz  (wie Java Garbage Collection). (Annahme: das alte Datensegment wird nicht mehr benoetigt.)

        return tga;
    }
    
    //ProPra in ProPra konvertieren
    @Override
    public ProPra konvInProPra(String compression) throws IOException, OptionaleAnforderungenException {
        
        /*Prueft auf falsche Dateigroesse im Header: Ist die angegebene Datensegmentgroesse
         * tatsaechlich gleich der Groesse des eingelesen (also eventuell komprimierten) Datensegments?
         */
        if (this.gibDatensegmentgroesse() != (long) this.gibDatensegment().length) {
            throw new OptionaleAnforderungenException("Falsche Dateigroesse im Header.");
        }
        
        //Prueft auf die Korrektheit der Pruefsumme.
        if (this.berechnePruefsumme() != this.gibPruefsumme()) {
            throw new OptionaleAnforderungenException("Ueberpruefen der Pruefsumme.");
        }
        
        //Datensegment dekomprimieren
        this.dekomprimiereDatensegment();
        
        //Pruefe auf fehlende Daten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge.
        if (Hilfsklasse.datensegmentgroesseAusBreiteHoeheBitsProPixel(this.gibBildbreite(), this.gibBildhoehe(), this.gibBitsProBildpunkt()) > this.gibDatensegment().length) {
            throw new OptionaleAnforderungenException("Fehlende Daten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge/Dateigroesse.");
        }
        
        //Pruefe auf zu viele Bilddaten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge.
        if (Hilfsklasse.datensegmentgroesseAusBreiteHoeheBitsProPixel(this.gibBildbreite(), this.gibBildhoehe(), this.gibBitsProBildpunkt()) < this.gibDatensegment().length) {
            throw new OptionaleAnforderungenException("Zu viele Bilddaten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge.");
        }
        
        //Kompressionstyp anpassen.
        switch (compression) {
        case "uncompressed":
            this.setzeKompressionstyp(ProPra.kompressionstypUnkomprimiert); //unkomprimiert.
            break;
        case "rle":
            this.setzeKompressionstyp(ProPra.kompressionstypRLE); //rle: lauflaengenkodiert (pixelweise)
            break;
        }
        
        //Datensegment komprimieren.  
        this.komprimiereDatensegment(compression);       

        //Pruefsumme des evenutell komprimierten Datensegments berechnen.
        this.setzePruefsumme(this.berechnePruefsumme());

        //Datensegmentgroesse berechnen
        this.setzeDatensegmentgroesse(this.gibDatensegment().length);
        
        return this;
    }
    
    //Waehlt abhaengig vom Kompressionstyp die (geerbte) Methode zum Dekomprimieren aus.
    private void dekomprimiereDatensegment() throws IOException, OptionaleAnforderungenException {
        switch ((int) this.gibKompressionstyp()) {
        case (int) ProPra.kompressionstypUnkomprimiert:
            break;
        case (int) ProPra.kompressionstypRLE:
            this.dekomprimiereDatensegmentVonRLE();
            break;
        case (int) ProPra.kompressionstypHuffman:
            this.dekomprimiereDatensegmentVonHuffman();
            break;

        }
    }

    
    //Waehlt abhaengig vom gewuenschten Bildtyp (Kompression) die (geerbte) Methode zum Komprimieren aus.
    void komprimiereDatensegment(String compression) throws IOException, OptionaleAnforderungenException{
        switch (compression) {
        case "uncompressed":
            break;
        case "rle":
            //Prueft vorher, ob genuegend Bilddaten, also mindestens 3 Pixel, vorhanden sind.             
            if (this.gibDatensegment().length >= 9) {            
                this.komprimiereDatensegmentInRLE();
            } else {
                throw new OptionaleAnforderungenException("Zu wenig Bilddaten fuer RLE Kompression.");
            }                                                  
            break;
        }
    }
    
    
    //Schreibt alle Attribute nacheinander in die Datei "output".
    @Override
    public void schreibeAusgabebild(String output) throws IOException {
        FileOutputStream fos = new FileOutputStream(output);
        fos.write(gibFormatkennungArray());
        fos.write(gibBildbreiteArray());
        fos.write(gibBildhoeheArray());
        fos.write(gibBitsProBildpunktArray());
        fos.write(gibKompressionstypArray());
        fos.write(gibDatensegmentgroesseArray());
        fos.write(gibPruefsummeArray());
        fos.write(gibDatensegment());
        fos.close();
    }
    
    //Getter-Methoden
    
    int gibHeaderGroesse() {
        return headerGroesse;
    }
    
    byte[] gibMagischeZahlArray() {
        return magischeZahl;
    }
    
    long gibBitsProBildpunktGueltig() {
        return bitsProBildpunktGueltig;
    }
    
    byte[] gibFormatkennungArray() {
        return formatkennung;
    }
    
    long gibBildbreite() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(bildbreite);
    }
    
    byte[] gibBildbreiteArray() {
        return bildbreite;
    }
    
    long gibBildhoehe() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(bildhoehe);
    }
    
    byte[] gibBildhoeheArray() {
        return bildhoehe;
    }
    
    long gibBitsProBildpunkt() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(bpbp);
    }
    
    byte[] gibBitsProBildpunktArray() {
        return bpbp;
    }
    
    long gibKompressionstyp() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(kompressionstyp);
    }
    
    byte[] gibKompressionstypArray() {
        return kompressionstyp;
    }
    
    long gibDatensegmentgroesse() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(datensegmentgroesse);
    }
    
    byte[] gibDatensegmentgroesseArray() {
        return datensegmentgroesse;
    }
    
    long gibPruefsumme() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(pruefsumme);
    }
    
    byte[] gibPruefsummeArray() {
        return pruefsumme;
    }
    
    byte[] gibDatensegment() {
        return datensegment;
    }
    
    
    //Setter-Methoden
    
    void setzeFormatkennung(byte[] magischeZahl) {
        formatkennung = magischeZahl;
    }
    
    void setzeBildbreite(long bildbreite) {
        this.bildbreite = Hilfsklasse.berechneByteArrayLittleEndianAusLong(bildbreite, this.bildbreite.length);
    }
    
    void setzeBildhoehe(long bildhoehe) {
        this.bildhoehe = Hilfsklasse.berechneByteArrayLittleEndianAusLong(bildhoehe, this.bildhoehe.length);
    }
    
    void setzeBitsProBildpunkt(long bpbp) {
        this.bpbp = Hilfsklasse.berechneByteArrayLittleEndianAusLong(bpbp, this.bpbp.length);
    }
    
    void setzeKompressionstyp(long kompressionstyp) {
        this.kompressionstyp = Hilfsklasse.berechneByteArrayLittleEndianAusLong(kompressionstyp, this.kompressionstyp.length);
    }
    
    void setzeDatensegmentgroesse(long datensegmentgroesse) {
        this.datensegmentgroesse = Hilfsklasse.berechneByteArrayLittleEndianAusLong(datensegmentgroesse, this.datensegmentgroesse.length);
    }
    
    void setzePruefsumme(long pruefsumme) {
        this.pruefsumme = Hilfsklasse.berechneByteArrayLittleEndianAusLong(pruefsumme, this.pruefsumme.length);
    }
    
    void setzeDatensegment(byte[] datensegment) {
        this.datensegment = datensegment;
    }
    
}
