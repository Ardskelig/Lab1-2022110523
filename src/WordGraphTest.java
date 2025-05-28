import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WordGraphTest {

    private WordGraph graph;

    @BeforeEach
    public void setUp() {
        // 初始化图并构建测试文本的单词列表
        graph = new WordGraph();
        List<String> words = Arrays.asList(
                "the", "scientist", "carefully", "analyzed", "the", "data",
                "wrote", "a", "detailed", "report", "and", "shared", "the", "report",
                "with", "the", "team", "but", "the", "team", "requested", "more", "data",
                "so", "the", "scientist", "analyzed", "it", "again"
        );
        graph.buildGraph(words);
    }

    @Test
    public void testBridgeWordExists_analyzed_again() {
        assertEquals("The bridge word from analyzed to again is: it.",
                graph.queryBridgeWords("analyzed", "again"));
    }

    @Test
    public void testNoBridgeWord_it_so() {
        assertEquals("No bridge words from it to so!",
                graph.queryBridgeWords("it", "so"));
    }

    @Test
    public void testWord1NotFound_test_so() {
        assertEquals("No word1 in the graph!",
                graph.queryBridgeWords("test", "so"));
    }

    @Test
    public void testWord2NotFound_so_test() {
        assertEquals("No word2 in the graph!",
                graph.queryBridgeWords("so", "test"));
    }

    @Test
    public void testBothWordsNotFound_Test1_Test2() {
        assertEquals("No word1 or word2 in the graph!",
                graph.queryBridgeWords("Test1", "Test2"));
    }

    @Test
    public void testNullInputs() {
        assertEquals("No word1 in the graph!",
                graph.queryBridgeWords(null, null));
    }

    @Test
    public void testSelfLoop_so_so() {
        assertEquals("No bridge words from so to so!",
                graph.queryBridgeWords("so", "so"));
    }

    @Test
    public void testSpecialCharacters_the_helloWorld() {
        assertEquals("No word2 in the graph!",
                graph.queryBridgeWords("the", "hello-world"));
    }

    @Test
    public void testCapitalLetters_The_so() {
        assertEquals("The bridge word from the to so is: data.",
                graph.queryBridgeWords("The", "so"));
    }

    @Test
    public void testVacantstr_so() {
        assertEquals("No word1 in the graph!",
                graph.queryBridgeWords("", "so"));
    }

    @Test
    public void testNull_unNull() {
        assertEquals("No word1 in the graph!",
                graph.queryBridgeWords(null, "so"));
    }

    @Test
    public void testunNull_Null() {
        assertEquals("No word2 in the graph!",
                graph.queryBridgeWords("so",null));
    }
}