package propra.imageconverter.helfer;

/*Klasse, die nur formal die optionalen Anforderungen der KE1 als Subklasse von Exception deklariert.
 * Einige weitere Ausnahmen laufen ebenfalls hierueber, z.B. eine zur RLE-Kompression von KE2 oder zum Huffman-Baum von KE3.
 */
public class OptionaleAnforderungenException extends Exception{

    public OptionaleAnforderungenException(String message){
        super(message);
    }

}
