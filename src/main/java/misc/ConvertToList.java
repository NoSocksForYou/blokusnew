package misc;

import blokus.Board;
import blokus.MyPieceManager;
import uis.Texel;

import java.util.*;

public class ConvertToList {
    public static Texel[][] convertToList (String original, String verticalDelimiter, String horizontalDelimiter, char transparentChar) {

        String[] split = original.split(verticalDelimiter);


        List<List<Character>> notNormalized = new Vector<>();
        for (String row : split) {
            notNormalized.add(new ArrayList<>());

            List<Character> current = notNormalized.get(notNormalized.size() - 1);
            for (String character : row.split(horizontalDelimiter)) {
                if (character.length() > 0) {
                    current.add(character.charAt(0));
                } else {
                    current.add(transparentChar);
                }
            }
        }

        return normalizeArray(notNormalized, transparentChar);
    }

    private static Texel[][] normalizeArray (List<List<Character>> notNormalized, char transparentChar) {
        Texel[][] normalizedArray = new Texel[notNormalized.size()][getLongestRowLen(notNormalized)];

        for (int y = 0; y < normalizedArray.length; y++) {
            for (int x = 0; x < normalizedArray[y].length; x++) {
                normalizedArray[y][x] = new Texel(transparentChar);
            }
        }

        for (int y = 0; y < notNormalized.size(); y++) {
            for (int x = 0; x < notNormalized.get(y).size(); x++) {
                normalizedArray[y][x] = new Texel(notNormalized.get(y).get(x));
            }
        }

        return normalizedArray;
    }

    private static int getLongestRowLen(List<List<Character>> notNormalized) {
        return Collections.max(notNormalized, (characters, t1) -> characters.size() - t1.size()).size();
    }

    public static void main (String[] arghs) {
        Arrays.stream(convertToList(new Board(14, 14, new MyPieceManager(2)).toString(), "\n", " ", '$')).forEach((row) -> System.out.println(Arrays.toString(row)));

    }
}
