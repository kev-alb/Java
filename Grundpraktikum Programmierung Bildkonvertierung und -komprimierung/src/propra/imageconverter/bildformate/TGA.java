package propra.imageconverter.bildformate;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import propra.imageconverter.helfer.*;

//Klasse fuer das TGA-Bildformat.
public class TGA extends Bildformat{
    
    static final long bildtypRGBUnkomprimiert = 2;
    static final long bildtypRGBRLE = 10;
    private static final HashSet<Long> bildtypen = new HashSet<Long>(Arrays.asList(
            TGA.bildtypRGBUnkomprimiert, TGA.bildtypRGBRLE));

    private final int headerGroesse = 18; // Header-Groesse ist 18
    private final long laengeBildIDGueltig = 0; // immer 0
    private final long farbpalettentypGueltig = 0; // immer 0
    private final long palettenbeginnGueltig = 0; // immer 0
    private final long palettenlaengeGueltig = 0; // immer 0
    private final long groessePaletteneintragGueltig = 0; // immer 0
    private final long nullpunktXGueltig = 0; // immer 0
    private final long bitsProBildpunktGueltig = 24; // immer 24
    private final long babGueltig = 0x20; // immer 0x20

    private byte[] laengeBildID = new byte[1];
    private byte[] farbpalettentyp = new byte[1];
    private byte[] bildtyp = new byte[1];
    private byte[] palettenbeginn = new byte[2];
    private byte[] palettenlaenge = new byte[2];
    private byte[] groessePaletteneintrag = new byte[1];
    private byte[] nullpunktX = new byte[2];
    private byte[] nullpunktY = new byte[2];
    private byte[] bab = new byte[1]; // Bild-Attribut-Byte

    //Sichtbare geerbte Attribute: byte[] datensegment byte[] bildbreite, byte[] bildhoehe, byte[] bpbp
    {
        bildbreite = new byte[2];
        bildhoehe = new byte[2];
        bpbp = new byte[1];
    }

    //Konstruktor zum Einlesen mit anschliessender Pruefung optionaler Anforderungen von KE1
    public TGA(FileInputStream fis) throws IOException, OptionaleAnforderungenException {
        int dateigroesse = fis.available();
        fis.read(gibLaengeBildIDArray());
        fis.read(gibFarbpalettentypArray());
        fis.read(gibBildtypArray());
        fis.read(gibPalettenbeginnArray());
        fis.read(gibPalettenlaengeArray());
        fis.read(gibGroessePaletteneintragArray());
        fis.read(gibNullpunktXArray());
        fis.read(gibNullpunktYArray());
        fis.read(gibBildbreiteArray());
        fis.read(gibBildhoeheArray());
        fis.read(gibBitsProBildpunktArray());
        fis.read(gibBildAttributByteArray());
        setzeDatensegment(new byte[dateigroesse - gibHeaderGroesse()]);
        fis.read(gibDatensegment());

        //Prueft BitsProBildpunkt = 24
        if (gibBitsProBildpunkt() != gibBitsProBildpunktGueltig()) {
            throw new OptionaleAnforderungenException(
                    "Bits pro Bildpunkt hat keinen gueltigen Wert (" + gibBitsProBildpunktGueltig() + ").");
        }

        //Prueft auf inkonsistente Bilddimension (Nullbreite/Nullhöhe)
        if (gibBildbreite() == 0 || gibBildhoehe() == 0) {
            throw new OptionaleAnforderungenException("Inkonsistente Bilddimension (Nullbreite/Nullhoehe).");
        }

        //Prueft auf nicht unterstuetzten Bildtyp.
        if (!TGA.bildtypen.contains(gibBildtyp())) {
            throw new OptionaleAnforderungenException("Nicht unterstuetzter Bildtyp.");
        }

    }

    //Konstruktor für die Konvertierung, initialisiert die unabhaengigen Elemente.
    TGA() {
        setzeLaengeBildID(gibLaengeBildIDGueltig());
        setzeFarbpalettentyp(gibFarbpalettentypGueltig());
        setzePalettenbeginn(gibPalettenbeginnGueltig());
        setzePalettenlaenge(gibPalettenlaengeGueltig());
        setzeGroessePaletteneintrag(gibGroessePaletteneintragGueltig());
        setzeNullpunktX(gibNullpunktXGueltig());
        setzeBitsProBildpunkt(gibBitsProBildpunktGueltig());
        setzeBildAttributByte(gibBildAttributByteGueltig());
        //Es bleiben uebrig: bildtyp, nullpunktY, bildbreite, bildhoehe und datensegment
    }

    //TGA in das Standardformat ProPra konvertieren
    @Override
    public ProPra konvInProPra(String compression) throws IOException, OptionaleAnforderungenException {
        /*
         * Erzeuge ein ProPra-Objekt mit dem Konstruktor, der die vom Eingabebild
         * unabhaengigen Elemente mit Werten initialisiert.
         */
        ProPra propra = new ProPra();

        // Es bleiben uebrig: bildbreite, bildhoehe, kompressionstyp,
        // datensegmentgroesse, pruefsumme, datensegment

        propra.setzeBildbreite(this.gibBildbreite());
        propra.setzeBildhoehe(this.gibBildhoehe());

        // Kompressionstyp ist abhaengig von der Eingabe der gewuenschten Kompression
        switch (compression) {
        case "uncompressed":
            propra.setzeKompressionstyp(ProPra.kompressionstypUnkomprimiert);
            break;
        case "rle":
            propra.setzeKompressionstyp(ProPra.kompressionstypRLE);
            break;
        }

        // Datensegment dekomprimieren
        this.dekomprimiereDatensegment();

        // Pruefe auf fehlende Daten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge.
        if (Hilfsklasse.datensegmentgroesseAusBreiteHoeheBitsProPixel(this.gibBildbreite(), this.gibBildhoehe(), this.gibBitsProBildpunkt()) > this.gibDatensegment().length) {
            throw new OptionaleAnforderungenException(
                    "Fehlende Daten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge/Dateigroesse.");
        }

        // Pruefe auf zu viele Bilddaten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge.
        if (Hilfsklasse.datensegmentgroesseAusBreiteHoeheBitsProPixel(this.gibBildbreite(), this.gibBildhoehe(), this.gibBitsProBildpunkt()) < this.gibDatensegment().length) {
            throw new OptionaleAnforderungenException(
                    "Zu viele Bilddaten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge.");
        }

        /*Datensegment von BGR (TGA) nach GBR (ProPra) umordnen. (Call by value: value ist die Referenz auf das Datensegment.)
         * Beide datensegment-Variablen referenzieren danach dasselbe Array.
         */
        propra.setzeDatensegment(Hilfsklasse.ordneDatensegmentVonBGRNachGBR(this.gibDatensegment()));

        // Datensegment komprimieren.
        propra.komprimiereDatensegment(compression);
        this.setzeDatensegment(null); // Zum Sparen von Speicherplatz (wie Java Garbage Collection). (Annahme: das alte Datensegment wird nicht mehr benoetigt.)

        // Pruefsumme des eventuell komprimierten Datensegments neu berechnen.
        propra.setzePruefsumme(propra.berechnePruefsumme());

        // Datensegmentgroesse berechnen
        propra.setzeDatensegmentgroesse(propra.gibDatensegment().length);

        return propra;
    }

    // TGA in TGA konvertieren
    @Override
    public TGA konvInTGA(String compression) throws IOException, OptionaleAnforderungenException {

        // Datensegment dekomprimieren
        this.dekomprimiereDatensegment();

        // Pruefe auf fehlende Daten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge.
        if (Hilfsklasse.datensegmentgroesseAusBreiteHoeheBitsProPixel(this.gibBildbreite(), this.gibBildhoehe(), this.gibBitsProBildpunkt()) > this.gibDatensegment().length) {
            throw new OptionaleAnforderungenException(
                    "Fehlende Daten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge/Dateigroesse.");
        }

        // Pruefe auf zu viele Bilddaten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge.
        if (Hilfsklasse.datensegmentgroesseAusBreiteHoeheBitsProPixel(this.gibBildbreite(), this.gibBildhoehe(), this.gibBitsProBildpunkt()) < this.gibDatensegment().length) {
            throw new OptionaleAnforderungenException(
                    "Zu viele Bilddaten: Pixelanzahl aus Daten im Header passt nicht mit der Datenmenge.");
        }

        // Bildtyp anpassen
        switch (compression) {
        case "uncompressed":
            this.setzeBildtyp(TGA.bildtypRGBUnkomprimiert);
            break;
        case "rle":
            this.setzeBildtyp(TGA.bildtypRGBRLE);
            break;
        }

        // Datensegment komprimieren.
        this.komprimiereDatensegment(compression);

        return this;
    }

    // Waehlt abhaengig vom Bildtyp die (geerbte) Methode zum Dekomprimieren aus.
    private void dekomprimiereDatensegment() throws IOException {
        switch ((int) this.gibBildtyp()) {
        case (int) TGA.bildtypRGBUnkomprimiert:
            break;
        case (int) TGA.bildtypRGBRLE:
            this.dekomprimiereDatensegmentVonRLE();
            break;
        }
    }

    // Waehlt abhaengig vom gewuenschten Bildtyp (Kompression) die (geerbte) Methode zum Komprimieren aus.
    void komprimiereDatensegment(String compression) throws IOException, OptionaleAnforderungenException {
        switch (compression) {
        case "uncompressed":
            break;
        case "rle":
            // Prueft vorher, ob genuegend Bilddaten, also mindestens 3 Pixel, vorhanden sind.
            if (this.gibDatensegment().length >= 9) {
                this.komprimiereDatensegmentInRLE();
            } else {
                throw new OptionaleAnforderungenException("Zu wenig Bilddaten fuer RLE Kompression.");
            }
            break;
        }
    }

    // Schreibt alle Attribute nacheinander in die Datei "output".
    @Override
    public void schreibeAusgabebild(String output) throws IOException {
        FileOutputStream fos = new FileOutputStream(output);
        fos.write(gibLaengeBildIDArray());
        fos.write(gibFarbpalettentypArray());
        fos.write(gibBildtypArray());
        fos.write(gibPalettenbeginnArray());
        fos.write(gibPalettenlaengeArray());
        fos.write(gibGroessePaletteneintragArray());
        fos.write(gibNullpunktXArray());
        fos.write(gibNullpunktYArray());
        fos.write(gibBildbreiteArray());
        fos.write(gibBildhoeheArray());
        fos.write(gibBitsProBildpunktArray());
        fos.write(gibBildAttributByteArray());
        fos.write(gibDatensegment());
        fos.close();
    }

    // Getter-Methoden

    int gibHeaderGroesse() {
        return headerGroesse;
    }

    long gibLaengeBildIDGueltig() {
        return laengeBildIDGueltig;
    }

    long gibFarbpalettentypGueltig() {
        return farbpalettentypGueltig;
    }

    long gibPalettenbeginnGueltig() {
        return palettenbeginnGueltig;
    }

    long gibPalettenlaengeGueltig() {
        return palettenlaengeGueltig;
    }

    long gibGroessePaletteneintragGueltig() {
        return groessePaletteneintragGueltig;
    }

    long gibNullpunktXGueltig() {
        return nullpunktXGueltig;
    }

    long gibBitsProBildpunktGueltig() {
        return bitsProBildpunktGueltig;
    }

    long gibBildAttributByteGueltig() {
        return babGueltig;
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

    long gibLaengeBildID() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(laengeBildID);
    }

    byte[] gibLaengeBildIDArray() {
        return laengeBildID;
    }

    long gibFarbpalettentyp() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(farbpalettentyp);
    }

    byte[] gibFarbpalettentypArray() {
        return farbpalettentyp;
    }

    long gibBildtyp() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(bildtyp);
    }

    byte[] gibBildtypArray() {
        return bildtyp;
    }

    long gibPalettenbeginn() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(palettenbeginn);
    }

    byte[] gibPalettenbeginnArray() {
        return palettenbeginn;
    }

    long gibPalettenlaenge() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(palettenlaenge);
    }

    byte[] gibPalettenlaengeArray() {
        return palettenlaenge;
    }

    long gibGroessePaletteneintrag() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(groessePaletteneintrag);
    }

    byte[] gibGroessePaletteneintragArray() {
        return groessePaletteneintrag;
    }

    long gibNullpunktX() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(nullpunktX);
    }

    byte[] gibNullpunktXArray() {
        return nullpunktX;
    }

    long gibNullpunktY() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(nullpunktY);
    }

    byte[] gibNullpunktYArray() {
        return nullpunktY;
    }

    long gibBildAttributByte() {
        return Hilfsklasse.berechneLongAusBytesLittleEndian(bab);
    }

    byte[] gibBildAttributByteArray() {
        return bab;
    }

    byte[] gibDatensegment() {
        return datensegment;
    }

    // Setter-Methoden

    void setzeBildbreite(long bildbreite) {
        this.bildbreite = Hilfsklasse.berechneByteArrayLittleEndianAusLong(bildbreite, this.bildbreite.length);
    }

    void setzeBildhoehe(long bildhoehe) {
        this.bildhoehe = Hilfsklasse.berechneByteArrayLittleEndianAusLong(bildhoehe, this.bildhoehe.length);
    }

    void setzeBitsProBildpunkt(long bpbp) {
        this.bpbp = Hilfsklasse.berechneByteArrayLittleEndianAusLong(bpbp, this.bpbp.length);
    }

    void setzeLaengeBildID(long laengeBildID) {
        this.laengeBildID = Hilfsklasse.berechneByteArrayLittleEndianAusLong(laengeBildID, this.laengeBildID.length);
    }

    void setzeFarbpalettentyp(long farbpalettentyp) {
        this.farbpalettentyp = Hilfsklasse.berechneByteArrayLittleEndianAusLong(farbpalettentyp,
                this.farbpalettentyp.length);
    }

    void setzeBildtyp(long bildtyp) {
        this.bildtyp = Hilfsklasse.berechneByteArrayLittleEndianAusLong(bildtyp, this.bildtyp.length);
    }

    void setzePalettenbeginn(long palettenbeginn) {
        this.palettenbeginn = Hilfsklasse.berechneByteArrayLittleEndianAusLong(palettenbeginn,
                this.palettenbeginn.length);
    }

    void setzePalettenlaenge(long palettenlaenge) {
        this.palettenlaenge = Hilfsklasse.berechneByteArrayLittleEndianAusLong(palettenlaenge,
                this.palettenlaenge.length);
    }

    void setzeGroessePaletteneintrag(long groessePaletteneintrag) {
        this.groessePaletteneintrag = Hilfsklasse.berechneByteArrayLittleEndianAusLong(groessePaletteneintrag,
                this.groessePaletteneintrag.length);
    }

    void setzeNullpunktX(long nullpunktX) {
        this.nullpunktX = Hilfsklasse.berechneByteArrayLittleEndianAusLong(nullpunktX, this.nullpunktX.length);
    }

    void setzeNullpunktY(long nullpunktY) {
        this.nullpunktY = Hilfsklasse.berechneByteArrayLittleEndianAusLong(nullpunktY, this.nullpunktY.length);
    }

    void setzeBildAttributByte(long bab) {
        this.bab = Hilfsklasse.berechneByteArrayLittleEndianAusLong(bab, this.bab.length);
    }

    void setzeDatensegment(byte[] datensegment) {
        this.datensegment = datensegment;
    }

}
