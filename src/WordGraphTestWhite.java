import org.junit.jupiter.api.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WordGraphTestWhite {

    private static final String OUTPUT_FILE = "test_output.txt";
    private WordGraph wordGraph;

    @BeforeEach
    void setUp() {
        wordGraph = new WordGraph();
    }

    @AfterEach
    void tearDown() throws IOException {
        Path path = Paths.get(OUTPUT_FILE);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    // TC1: 空图抛出异常
    @Test
    void testEmptyGraph_ThrowsException() {
        assertThrows(IllegalStateException.class, () -> wordGraph.randomWalkToFile(OUTPUT_FILE));
    }

    // TC2: 初始节点无出边
    @Test
    void testNodeWithNoOutgoingEdges_WalkTerminatesImmediately() throws IOException {
        // 构造图：A 没有出边
        Field field = null;
        try {
            field = WordGraph.class.getDeclaredField("adjacencyList");
            field.setAccessible(true);
            Map<String, Map<String, Integer>> mockGraph = new HashMap<>();
            mockGraph.put("A", new HashMap<>());
            field.set(wordGraph, mockGraph);
        } catch (Exception e) {
            fail("反射失败：" + e.getMessage());
        }

        wordGraph.randomWalkToFile(OUTPUT_FILE);

        String result = readOutputFile();
        assertEquals("A", result);
    }

    // TC3: 首次尝试就遇到已访问边
    @Test
    void testFirstEdgeAlreadyVisited_TerminateImmediately() throws Exception {
        // 构造图：A -> B
        Field adjacencyListField = WordGraph.class.getDeclaredField("adjacencyList");
        adjacencyListField.setAccessible(true);

        Map<String, Map<String, Integer>> mockGraph = new HashMap<>();
        mockGraph.put("A", new HashMap<>(Map.of("B", 1)));
        mockGraph.put("B", new HashMap<>());

        // 创建 WordGraph 实例并注入模拟图结构
        WordGraph wordGraph = new WordGraph();
        adjacencyListField.set(wordGraph, mockGraph);

        // 设置已访问边为 A->B
        Set<String> initialVisitedEdges = Set.of("A->B");

        // 执行测试
        wordGraph.randomWalkToFile(OUTPUT_FILE, initialVisitedEdges);

        // 验证输出
        String result = readOutputFile();
        assertEquals("A -> B", result);
    }

    // TC4: 走一步后停止
    @Test
    void testOneStepThenStopRequested() throws Exception {
        // 构造图：A->B, B->C
        Map<String, Map<String, Integer>> mockGraph = new HashMap<>();
        mockGraph.put("A", new HashMap<>(Map.of("B", 1)));
        mockGraph.put("B", new HashMap<>(Map.of("C", 1)));

        Field adjacencyListField = WordGraph.class.getDeclaredField("adjacencyList");
        adjacencyListField.setAccessible(true);
        adjacencyListField.set(wordGraph, mockGraph);

        // 强制设置 stopRequested = true
        Field stopRequestedField = WordGraph.class.getDeclaredField("stopRequested");
        stopRequestedField.setAccessible(true);
        stopRequestedField.set(wordGraph, false);

        new Thread(() -> {
            try {
                Thread.sleep(100); // 等待游走开始
                stopRequestedField.set(wordGraph, true);
            } catch (Exception ignored) {}
        }).start();

        wordGraph.randomWalkToFile(OUTPUT_FILE);

        String result = readOutputFile();
        assertEquals("A -> B", result);
    }

    // TC5: 成功完成一次循环
    @Test
    void testCompleteOneLoop_Successfully() throws Exception {
        // 构造图：A->B, B->C, C->B
        Map<String, Map<String, Integer>> mockGraph = new HashMap<>();
        mockGraph.put("A", new HashMap<>(Map.of("B", 1)));
        mockGraph.put("B", new HashMap<>(Map.of("C", 1)));
        mockGraph.put("C", new HashMap<>(Map.of("B", 1)));

        Field adjacencyListField = WordGraph.class.getDeclaredField("adjacencyList");
        adjacencyListField.setAccessible(true);
        adjacencyListField.set(wordGraph, mockGraph);

        wordGraph.randomWalkToFile(OUTPUT_FILE);

        String result = readOutputFile();
        assertTrue(result.startsWith("A -> B -> C -> B"),
                "输出应以 A -> B -> C -> B 开头");
    }

    // 辅助方法：读取输出文件内容
    private String readOutputFile() throws IOException {
        return Files.readString(Paths.get(OUTPUT_FILE));
    }
}