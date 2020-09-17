package propra.imageconverter.kodierung;

import java.io.*;
import java.util.Arrays;

//Klasse zum Dekodieren und Kodieren von Base32.
public class Base32 {
        
    //Dekodierung von Base32
    public static void dekodiereBase32(String[] args) throws IOException {
        
        //Strings input und output erstellen        
        String input = args[0].substring(8);
        String output = input.substring(0, input.lastIndexOf(".base-32"));
              
        //Streams zum Lesen und Schreiben der Eingabe- bzw. Ausgabedatei.
        FileInputStream fis = new FileInputStream(input);
        FileOutputStream fos = new FileOutputStream(output);
        
        //Variablendeklarationen
        long anzEingelesenerTextZeichenBytes = 0;
        long dateigroesse = fis.available();
        byte[] array8Zeichen = new byte[8]; //Textzeichen
        int[] array8Indizes = new int[8]; //enthaelt die Indizes der eingelesen Textzeichen im base32Alphabet.
        byte[] arrayRestZeichen; //Falls Anzahl Textzeichen kein Vielfaches von 8
        byte[] array5Bytes = new byte[5]; //Binaerdaten
        int index1, index2, index3, index4, index5, index6, index7; //Zur Uebersichtlichkeit, falls Anzahl Textzeichen kein Vielfaches von 8.
        //base32Alphabet: 0123456789ABCDEFGHIJKLMNOPQRSTUV. UTF-8
        int[] base32Alphabet = {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
                0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
                0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56};
        
        //Dekodierung 
        while (anzEingelesenerTextZeichenBytes <= dateigroesse - 8) {
            
            //Lies die naechsten 8 Bytes (Textzeichen) als byte-Werte ein.
            fis.read(array8Zeichen);            
                                                          
            anzEingelesenerTextZeichenBytes = anzEingelesenerTextZeichenBytes + 8;
            
            /*Zu den eingelesenen UTF-8 Zeichen ihre Indizes im Base32-Alphabet finden.
             * Das i-te Textzeichen in array8Zeichen[] gehoert zum i-ten Index-Wert in array8Indizes[].
             */
            for (int i = 0; i < array8Zeichen.length; i++) {
                array8Indizes[i] = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(array8Zeichen[i]));
            }            
            
            /*Erstelle die 5 Binaerdaten-Bytes (Rohdaten).
             *Logische Verschiebung: links <<, rechts >>>. Schiebt 0en nach.            
             */
            array5Bytes[0] = (byte) ( ((array8Indizes[0] & 0x1F) << 3) | ((array8Indizes[1] & 0x1C) >>> 2) ); //(43210)(432) (Byte 1)
            array5Bytes[1] = (byte) ( ((array8Indizes[1] & 0x03) << 6) | ((array8Indizes[2] & 0x1F) << 1)
                    | ((array8Indizes[3] & 0x10) >>> 4) ); //(10)(43210)(4) (Byte2)
            array5Bytes[2] = (byte) ( ((array8Indizes[3] & 0x0F) << 4) | ((array8Indizes[4] & 0x1E) >>> 1) ); //(3210)(4321) (Byte 3)
            array5Bytes[3] = (byte) ( ((array8Indizes[4] & 0x01) << 7) | ((array8Indizes[5] & 0x1F) << 2)
                    | ((array8Indizes[6] & 0x18) >>> 3) ); //(0)(43210)(43) (Byte4)
            array5Bytes[4] = (byte) ( ((array8Indizes[6] & 0x07) << 5) | (array8Indizes[7] & 0x1F) ); //(210)(43210) (Byte 5)
            

            //Die Bytes (Binaerdaten) in den Stream schreiben.
            fos.write(array5Bytes);
   
        } // while (anzEingelesenerBytes <= dateigroesse -8)
        

        /*Die restlichen maximal 7 Zeichen behandeln.
         * Fuer ... zu kodierende Binaerdaten-Bytes gibt es folgende moegliche
         * Anzahlen restlicher Textzeichen/Indizes (von denen der letzte Index mit 0en gefuellt wurde):
         * 1 Byte: 2 Indizes, 2 Bytes: 4, 3 Bytes: 5, 4 Bytes: 7
         */
        if (dateigroesse - anzEingelesenerTextZeichenBytes == 2) {
            //Noch 2 Textzeichen/Indizes uebrig: 1 Byte.            
            arrayRestZeichen = new byte[2];
            fis.read(arrayRestZeichen);
            index1 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[0]));
            index2 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[1]));
            fos.write((byte) ((index1 & 0x1F) << 3) | ((index2 & 0x1C) >>>2) ); //(43210)(432) (Byte 1)
            
        } else if (dateigroesse - anzEingelesenerTextZeichenBytes == 4) {
            //Noch 4 Textzeichen/Indizes uebrig: 2 Bytes.            
            arrayRestZeichen = new byte[4];
            fis.read(arrayRestZeichen);
            index1 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[0]));
            index2 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[1]));
            index3 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[2]));
            index4 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[3]));
            fos.write((byte) ((index1 & 0x1F) << 3) | ((index2 & 0x1C) >>>2) ); //(43210)(432) (Byte 1)
            fos.write((byte) ( ((index2 & 0x03) << 6) | ((index3 & 0x1F) << 1)
                    | ((index4 & 0x10) >>> 4) ) ); //(10)(43210)(4) (Byte2)
                        
        } else if (dateigroesse - anzEingelesenerTextZeichenBytes == 5) {
            //Noch 5 Textzeichen/Indizes uebrig: 3 Bytes.            
            arrayRestZeichen = new byte[5];
            fis.read(arrayRestZeichen);
            index1 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[0]));
            index2 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[1]));
            index3 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[2]));
            index4 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[3]));
            index5 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[4]));
            fos.write((byte) ( ((index1 & 0x1F) << 3) | ((index2 & 0x1C) >>>2) ) ); //(43210)(432) (Byte 1)
            fos.write((byte) ( ((index2 & 0x03) << 6) | ((index3 & 0x1F) << 1)
                    | ((index4 & 0x10) >>> 4) ) ); //(10)(43210)(4) (Byte2)
            fos.write((byte) ( ((index4 & 0x0F) << 4) | ((index5 & 0x1E) >>> 1) ) ); //(3210)(4321) (Byte 3)
            
        } else if (dateigroesse - anzEingelesenerTextZeichenBytes == 7) {
            //Noch 7 Textzeichen/Indizes uebrig: 4 Bytes.            
            arrayRestZeichen = new byte[7];
            fis.read(arrayRestZeichen);
            index1 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[0]));
            index2 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[1]));
            index3 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[2]));
            index4 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[3]));
            index5 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[4]));
            index6 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[5]));
            index7 = Arrays.binarySearch(base32Alphabet, Byte.toUnsignedInt(arrayRestZeichen[6]));
            fos.write((byte) ( ((index1 & 0x1F) << 3) | ((index2 & 0x1C) >>>2) ) ); //(43210)(432) (Byte 1)
            fos.write((byte) ( ((index2 & 0x03) << 6) | ((index3 & 0x1F) << 1)
                    | ((index4 & 0x10) >>> 4) ) ); //(10)(43210)(4) (Byte2)
            fos.write((byte) ( ((index4 & 0x0F) << 4) | ((index5 & 0x1E) >>> 1) ) ); //(3210)(4321) (Byte 3)
            fos.write((byte) ( ((index5 & 0x01) << 7) | ((index6 & 0x1F) << 2)
                    | ((index7 & 0x18) >>> 3) ) ); //(0)(43210)(43) (Byte4)
            
        } //if (dateigroesse - anzEingelesenerTextZeichenBytes == 2)

        
        fis.close();
        fos.close();

        
    } //dekodiereBase32
    
           
    //Kodierung in Base32
    public static void kodiereBase32(String[] args) throws IOException {        
        
         //Strings input und output erstellen        
         String input = args[0].substring(8);
         String output = input + "." + args[1].substring(9);
         
         //Streams zum Lesen und Schreiben von Eingabe- bzw. Ausgabedatei.
         FileInputStream fis = new FileInputStream(input);
         FileOutputStream fos = new FileOutputStream(output);
         
         //Variablendeklarationen
         long anzEingelesenerBytes = 0;
         long dateigroesse = fis.available();
         byte[] array5Bytes = new byte[5]; //Binaerdaten
         byte[] arrayRestBytes;
         byte[] array8Zeichen = new byte[8]; //Textzeichen
         int byte1, byte2, byte3, byte4, byte5;
         //base32Alphabet: 0123456789ABCDEFGHIJKLMNOPQRSTUV. UTF-8
         int[] base32Alphabet = {0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
                 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
                 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56};
                           
         //Kodierung 
         while (anzEingelesenerBytes <= dateigroesse - 5) {
             
             //Lies die naechsten 5 Bytes als byte-Werte ein.
             fis.read(array5Bytes);
             
             //Erweiterung auf 32-Bit int: 0...0 (7654) (3210), da Operatoren unten sonst eine (signed) int-Konvertierung erzwingen wuerden.
             byte1 = Byte.toUnsignedInt(array5Bytes[0]);
             byte2 = Byte.toUnsignedInt(array5Bytes[1]);
             byte3 = Byte.toUnsignedInt(array5Bytes[2]);
             byte4 = Byte.toUnsignedInt(array5Bytes[3]);
             byte5 = Byte.toUnsignedInt(array5Bytes[4]);
             
             anzEingelesenerBytes = anzEingelesenerBytes + 5;
             
             /*Erstelle die 8 Index-Bytes. 0000 0000 - 0001 1111
              *Logische Verschiebung: links <<, rechts >>>. Schiebt 0en nach.            
              */
             array8Zeichen[0] = (byte) (byte1 >>> 3); //000(76543) (Zeichen1)
             array8Zeichen[1] = (byte) (((byte1 & 0x07) << 2) | (byte2 >>> 6)); //000(210)00 | 0000 00(76) = 000(210)(76) (Zeichen 2)
             array8Zeichen[2] = (byte) ((byte2 >>> 1) & 0x1F); // 0(7654321) & 0001 1111 = 000(54321) (Zeichen 3)
             array8Zeichen[3] = (byte) (((byte2 & 0x01) << 4) | (byte3 >>> 4)); // (0000 000(0) << 4) | 0000 (7654) = 000(0)(7654) (Zeichen 4)
             array8Zeichen[4] = (byte) (((byte3 & 0x0F) << 1) | ((byte4 & 0x80) >>> 7)); // 0000 (3210) << 1 | (7)000 0000 >>> 7 = 000(3210)(7) (Zeichen 5)
             array8Zeichen[5] = (byte) ((byte4 >>> 2) & 0x1F); //00(765432) & 0001 1111 = 000(65432) (Zeichen 6)
             array8Zeichen[6] = (byte) (((byte4 & 0x03) << 3) | (byte5 >>> 5)); // (0000 0(210) << 3) | 0000 0(765) = 00(210)(765) (Zeichen 7)
             array8Zeichen[7] = (byte) (byte5 & 0x1F); // 000(43210) (Zeichen 8)                                      
             
             //Die Indizes den UTF-8 Zeichen aus dem Base32-Alphabet zuordnen.
             for (int i = 0; i < array8Zeichen.length; i++) {
                 array8Zeichen[i] = (byte) base32Alphabet[Byte.toUnsignedInt(array8Zeichen[i])];
             }

             //Die UTF-8 Zeichen in den Stream schreiben.
             fos.write(array8Zeichen);
    
         } // while (anzEingelesenerBytes <= dateigroesse -5)
         
         
         //Die restlichen maximal 4 Bytes behandeln.
         if (dateigroesse - anzEingelesenerBytes == 1) {
             //Noch 1 Byte uebrig. 2 Zeichen.
             arrayRestBytes = new byte[1];
             fis.read(arrayRestBytes);
             byte1 = Byte.toUnsignedInt(arrayRestBytes[0]);
             fos.write((byte) base32Alphabet[(byte1 >>> 3)]); //000(76543) (Zeichen1)
             fos.write((byte) base32Alphabet[((byte1 & 0x07) << 2)]); //000(210)00 (Zeichen 2)
             
         } else if(dateigroesse - anzEingelesenerBytes == 2) {
             //Noch 2 Bytes uebrig. 4 Zeichen.
             arrayRestBytes = new byte[2];
             fis.read(arrayRestBytes);
             byte1=Byte.toUnsignedInt(arrayRestBytes[0]);
             byte2=Byte.toUnsignedInt(arrayRestBytes[1]);
             fos.write((byte) base32Alphabet[(byte1 >>> 3)]); //000(76543) (Zeichen1)
             fos.write((byte) base32Alphabet[(((byte1 & 0x07) << 2) | (byte2 >>> 6))]); //000(210)00 | 0000 00(76) = 000(210)(76) (Zeichen 2)
             fos.write((byte) base32Alphabet[((byte2 >>> 1) & 0x1F)]); // 0(7654321) & 0001 1111 = 000(54321) (Zeichen 3)
             fos.write((byte) base32Alphabet[((byte2 & 0x01) << 4)]); // 0000 000(0) << 4 = 000(0) 0000 (Zeichen 4)
             
         } else if(dateigroesse - anzEingelesenerBytes == 3) {
             //Noch 3 Bytes uebrig. 5 Zeichen.
             arrayRestBytes = new byte[3];
             fis.read(arrayRestBytes);
             byte1=Byte.toUnsignedInt(arrayRestBytes[0]);
             byte2=Byte.toUnsignedInt(arrayRestBytes[1]);
             byte3=Byte.toUnsignedInt(arrayRestBytes[2]);
             fos.write((byte) base32Alphabet[(byte1 >>> 3)]); //000(76543) (Zeichen1)
             fos.write((byte) base32Alphabet[(((byte1 & 0x07) << 2) | (byte2 >>> 6))]); //000(210)00 | 0000 00(76) = 000(210)(76) (Zeichen 2)
             fos.write((byte) base32Alphabet[((byte2 >>> 1) & 0x1F)]); // 0(7654321) & 0001 1111 = 000(54321) (Zeichen 3)           
             fos.write((byte) base32Alphabet[(((byte2 & 0x01) << 4) | (byte3 >>> 4))]); // 000(0) 0000 | 0000 (7654) = 000(0) (7654) (Zeichen 4)
             fos.write((byte) base32Alphabet[((byte3 & 0x0F) << 1)]); // 000(3210)0 (Zeichen 5)

         } else if(dateigroesse - anzEingelesenerBytes == 4) {
             //Noch 4 Bytes uebrig. 7 Zeichen.
             arrayRestBytes = new byte[4];
             fis.read(arrayRestBytes);
             byte1=Byte.toUnsignedInt(arrayRestBytes[0]);
             byte2=Byte.toUnsignedInt(arrayRestBytes[1]);
             byte3=Byte.toUnsignedInt(arrayRestBytes[2]);
             byte4=Byte.toUnsignedInt(arrayRestBytes[3]);
             fos.write((byte) base32Alphabet[(byte1 >>> 3)]); //000(76543) (Zeichen1)
             fos.write((byte) base32Alphabet[(((byte1 & 0x07) << 2) | (byte2 >>> 6))]); //000(210)00 | 0000 00(76) = 000(210)(76) (Zeichen 2)
             fos.write((byte) base32Alphabet[((byte2 >>> 1) & 0x1F)]); // 0(7654321) & 0001 1111 = 000(54321) (Zeichen 3)           
             fos.write((byte) base32Alphabet[(((byte2 & 0x01) << 4) | (byte3 >>> 4))]); // 000(0) 0000 | 0000 (7654) = 000(0) (7654) (Zeichen 4)                          
             fos.write((byte) base32Alphabet[(((byte3 & 0x0F) << 1) | (byte4 >>> 7))]); // 000(3210)0 | 0000 000(7) = 000(3210)(7) (Zeichen 5)             
             fos.write((byte) base32Alphabet[((byte4 >>> 2) & 0x1F)]); // 00(765432) & 0001 1111 = 000(65432) (Zeichen 6)
             fos.write((byte) base32Alphabet[((byte4 & 0x03) << 3)]); // 0000 00(10) << 3 = 000(10)000 (Zeichen 7)

         } //if (dateigroesse - anzEingelesenerBytes == 1)
         
         
         fis.close();
         fos.close();
        
         
    } // kodiereBase32
                
    
}
