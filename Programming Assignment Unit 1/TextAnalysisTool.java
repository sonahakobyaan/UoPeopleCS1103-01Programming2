import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * TextAnalysisTool
 * A program that analyzes a block of text entered by the user:
 * - Character count
 * - Word count
 * - Most common character
 * - Frequency of a specific character (case-insensitive)
 * - Frequency of a specific word (case-insensitive)
 * - Number of unique words (case-insensitive)
 */
public class TextAnalysisTool {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 1. User Input (validated to ensure it is not empty)
        String inputText = getValidText(scanner);

        // 2. Character Count
        int charCount = countCharacters(inputText);
        System.out.println("\nTotal number of characters: " + charCount);

        // 3. Word Count
        int wordCount = countWords(inputText);
        System.out.println("Total number of words: " + wordCount);

        // 4. Most Common Character
        char mostCommonChar = findMostCommonCharacter(inputText);
        System.out.println("Most common character: '" + mostCommonChar + "'");

        // 5. Character Frequency
        char targetChar = getValidCharacter(scanner);
        int charFrequency = getCharacterFrequency(inputText, targetChar);
        System.out.println("The character '" + targetChar + "' appears "
                + charFrequency + " time(s) in the text.");

        // 6. Word Frequency
        String targetWord = getValidWord(scanner);
        int wordFrequency = getWordFrequency(inputText, targetWord);
        System.out.println("The word \"" + targetWord + "\" appears "
                + wordFrequency + " time(s) in the text.");

        // 7. Unique Words
        int uniqueWordCount = countUniqueWords(inputText);
        System.out.println("Number of unique words: " + uniqueWordCount);

        scanner.close();
    }

    /**
     * Prompts the user for a paragraph of text and validates that it is not empty.
     */
    private static String getValidText(Scanner scanner) {
        String text;
        while (true) {
            System.out.println("Please enter a paragraph or lengthy text for analysis:");
            text = scanner.nextLine().trim();
            if (!text.isEmpty()) {
                break;
            }
            System.out.println("Input cannot be empty. Please try again.");
        }
        return text;
    }

    /**
     * Prompts the user for a single character and validates the input length.
     */
    private static char getValidCharacter(Scanner scanner) {
        String input;
        while (true) {
            System.out.println("\nEnter a character to check its frequency in the text:");
            input = scanner.nextLine().trim();
            if (input.length() == 1) {
                break;
            }
            System.out.println("Invalid input. Please enter exactly one character.");
        }
        return input.charAt(0);
    }

    /**
     * Prompts the user for a single word and validates that it contains no spaces.
     */
    private static String getValidWord(Scanner scanner) {
        String input;
        while (true) {
            System.out.println("\nEnter a word to check its frequency in the text:");
            input = scanner.nextLine().trim();
            if (!input.isEmpty() && !input.contains(" ")) {
                break;
            }
            System.out.println("Invalid input. Please enter a single word.");
        }
        return input;
    }

    /**
     * Counts the total number of characters in the text, including whitespace.
     */
    private static int countCharacters(String text) {
        return text.length();
    }

    /**
     * Counts the total number of words in the text, assuming words are
     * separated by one or more whitespace characters.
     */
    private static int countWords(String text) {
        String[] words = text.trim().split("\\s+");
        return words.length;
    }

    /**
     * Finds the most frequently occurring character in the text
     * (whitespace excluded, case-insensitive).
     */
    private static char findMostCommonCharacter(String text) {
        Map<Character, Integer> frequencyMap = new HashMap<>();
        String lowerText = text.toLowerCase();

        for (int i = 0; i < lowerText.length(); i++) {
            char currentChar = lowerText.charAt(i);
            if (!Character.isWhitespace(currentChar)) {
                frequencyMap.put(currentChar, frequencyMap.getOrDefault(currentChar, 0) + 1);
            }
        }

        char mostCommon = ' ';
        int maxCount = 0;
        for (Map.Entry<Character, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostCommon = entry.getKey();
            }
        }
        return mostCommon;
    }

    /**
     * Counts how many times a specific character appears in the text,
     * ignoring case.
     */
    private static int getCharacterFrequency(String text, char target) {
        char lowerTarget = Character.toLowerCase(target);
        String lowerText = text.toLowerCase();
        int count = 0;

        for (int i = 0; i < lowerText.length(); i++) {
            if (lowerText.charAt(i) == lowerTarget) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts how many times a specific word appears in the text,
     * ignoring case and surrounding punctuation.
     */
    private static int getWordFrequency(String text, String targetWord) {
        String[] words = text.toLowerCase().trim().split("\\s+");
        String lowerTarget = targetWord.toLowerCase();
        int count = 0;

        for (String word : words) {
            String cleanedWord = word.replaceAll("[^a-zA-Z0-9]", "");
            if (cleanedWord.equals(lowerTarget)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts the number of unique words in the text, ignoring case and punctuation.
     */
    private static int countUniqueWords(String text) {
        String[] words = text.toLowerCase().trim().split("\\s+");
        Map<String, Boolean> uniqueWords = new HashMap<>();

        for (String word : words) {
            String cleanedWord = word.replaceAll("[^a-zA-Z0-9]", "");
            if (!cleanedWord.isEmpty()) {
                uniqueWords.put(cleanedWord, true);
            }
        }
        return uniqueWords.size();
    }
}