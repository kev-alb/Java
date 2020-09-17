package propra.imageconverter;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import propra.imageconverter.kodierung.*;
import propra.imageconverter.bildformate.*;
import propra.imageconverter.helfer.*;

/*1. Konvertiert ein Bild in ein anderes Bildformat. Eingaben sind der Pfad zum Eingabebild
 * sowie der zum Ausgabebild, mit vorangestelltem --input= bzw. --output=.
 * 2. Dekodiert oder kodiert eine Datei gemaess Base32. Eingabe ist ebenfalls der
 * input-Pfad und entweder --decode-base-32 oder --encode-base-32.
 */
public class ImageConverter {
        
    public static void main(String[] args) throws IOException {
        
       switch (args.length) {
        case 2: //Faelle für Kodierung/Dekodierung und Mindestanforderungen von KE1
            if (args[0].startsWith("--input=") && args[1].startsWith("--output=")) {
                String[] hilf = {args[0], args[1], "--compression=uncompressed"};
                ImageConverter.convert(hilf);
            } else if(args[0].startsWith("--input=") && args[1].startsWith("--decode-base-32")) {
                Base32.dekodiereBase32(args);
            } else if(args[0].startsWith("--input=") && args[1].startsWith("--encode-base-32")) {
                Base32.kodiereBase32(args);
            } else { //Falsche Eingabe der Parameter führt zum Abbruch.
                System.err.println("Falsche Eingabe der Parameter.");
                System.exit(123);
            }
            break;
        case 3: //Fall für Konvertierung mit eventueller Kompression
            ImageConverter.convert(args);
            break;
        default: //Falsche Anzahl von Parametern fuehrt zum Abbruch.
            System.err.println("Falsche Anzahl von Parametern.");
            System.exit(123);
            break;
        }
        
    }
    
    
    //Konvertierung mit eventueller Kompression
    private static void convert(String[] args) throws IOException {
        
        //Unterstuetzte Kompressionen fuer die Ausgabe
        final HashSet<String> compressionAusgabeUnterstuetzt = new HashSet<String>(Arrays.asList(
                "uncompressed", "rle"));
        
        /*Strings input vom Eingabebild fuer den Eingabepfad und
        *output vom Ausgabebild für den Ausgabepfad und
        *compression fuer das Ausgabebild aus den Eingabeparametern erstellen.
        */
        String input = args[0].substring(8);
        String output = args[1].substring(9);
        String compression = args[2].substring(14);
        
        //Programmabbruch, falls der gewuenschte Kompressionstyp fuer die Ausgabe nicht unterstuetzt wird.
        if (! compressionAusgabeUnterstuetzt.contains(compression)) {
            System.err.println("Der gewuenschte Kompressionstyp wird nicht unterstuetzt.");
            System.exit(123);
        }
        
        //Datei-/Bildformate auslesen
        String inputformat = input.substring(input.lastIndexOf('.'));
        String outputformat = output.substring(output.lastIndexOf('.'));     
        
        //Aus der Eingabebild-Datei lesen.
        FileInputStream fis = new FileInputStream(input);            
        
        //Eingabebild in Ausgabebild konvertieren.        
        Bildformat eingabebild=null;
        Bildformat ausgabebild=null;
        try {
            
            switch(inputformat) {
            case ".propra":
                eingabebild = new ProPra(fis);
                break;
            case ".tga":
                eingabebild = new TGA(fis);
                break;
            }
        
        } catch (OptionaleAnforderungenException e) {
            System.err.println(e.getMessage());
            System.exit(123);
        }
        
        fis.close();
        
        try {
            
            switch(outputformat) {
            case ".propra":
                ausgabebild = eingabebild.konvInProPra(compression);
                break;
            case ".tga":
                ausgabebild = eingabebild.konvInTGA(compression);
                break;
            }
        
        } catch (OptionaleAnforderungenException e) {
            System.err.println(e.getMessage());
            System.exit(123);
        }
        
        //Ausgabebild in Datei "output" schreiben
        ausgabebild.schreibeAusgabebild(output);
        
    }           

}

