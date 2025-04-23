import java.util.ArrayList;
import java.util.List;

public class TextProcessor {
    public List<String> processText(String text) {
        // 替换换行符和非字母字符为空格，转小写
        String cleaned = text.toLowerCase()
                .replaceAll("[^a-z]", " ")
                .replaceAll("\\r?\\n", " ");
        String[] words = cleaned.split("\\s+");
        List<String> wordList = new ArrayList<>();
        for (String word : words) {
            if (!word.isEmpty()) {
                wordList.add(word);
            }
        }
        return wordList;
    }
}
