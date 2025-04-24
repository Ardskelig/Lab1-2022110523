import java.io.*;
import java.util.*;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WordGraph {
    private final Map<String, Map<String, Integer>> adjacencyList;
    private volatile boolean stopRequested = false;

    public WordGraph() {
        adjacencyList = new HashMap<>();
    }

    public void buildGraph(List<String> words) {
        // 确保所有单词都作为节点存在
        for (String word : words) {
            adjacencyList.putIfAbsent(word, new HashMap<>());
        }
        // 建立有向边
        for (int i = 0; i < words.size() - 1; i++) {
            String source = words.get(i);
            String target = words.get(i + 1);
            Map<String, Integer> edges = adjacencyList.get(source);
            edges.put(target, edges.getOrDefault(target, 0) + 1);
        }
    }

    // 获取节点总数
    public int nodeCount() {
        return adjacencyList.size();
    }

    public int getEdgeWeight(String source, String target) {
        return adjacencyList.getOrDefault(source, new HashMap<>()).getOrDefault(target, 0);
    }
    //控制台打印有向图
    public void printGraph() {
        for (Map.Entry<String, Map<String, Integer>> entry : adjacencyList.entrySet()) {
            String source = entry.getKey();
            Map<String, Integer> targets = entry.getValue();
            for (Map.Entry<String, Integer> targetEntry : targets.entrySet()) {
                String target = targetEntry.getKey();
                int weight = targetEntry.getValue();
                System.out.println(source + " -> " + target + " (" + weight + ")");
            }
        }
    }
    //导出.dot文件
    public void exportToDot(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("digraph WordGraph {\n");
            for (Map.Entry<String, Map<String, Integer>> entry : adjacencyList.entrySet()) {
                String source = entry.getKey();
                for (Map.Entry<String, Integer> targetEntry : entry.getValue().entrySet()) {
                    String target = targetEntry.getKey();
                    int weight = targetEntry.getValue();
                    writer.write("  \"" + source + "\" -> \"" + target + "\" [label=\"" + weight + "\"];\n");
                }
            }
            writer.write("}\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //查询桥接词
    public String queryBridgeWords(String word1, String word2) {
        //word1 or word2 不存在
        if (!adjacencyList.containsKey(word1) || !adjacencyList.containsKey(word2)) {
            return "No " + (adjacencyList.containsKey(word1) ? "word2" :
                    (adjacencyList.containsKey(word2) ? "word1" : "word1 or word2")) + " in the graph!";
        }
        //获取word1的下一个节点
        Map<String, Integer> word1Edges = adjacencyList.get(word1);
        List<String> bridgeWords = new ArrayList<>();
        //对每一个进行检查能否到达word2
        for (String potentialBridge : word1Edges.keySet()) {
            Map<String, Integer> bridgeEdges = adjacencyList.get(potentialBridge);
            if (bridgeEdges != null && bridgeEdges.containsKey(word2)) {
                bridgeWords.add(potentialBridge);
            }
        }
        if (bridgeWords.isEmpty()) {
            return "No bridge words from " + word1 + " to " + word2 + "!";
        } else if (bridgeWords.size() == 1) {
            return "The bridge word from " + word1 + " to " + word2 + " is: " + bridgeWords.get(0) + ".";
        } else {
            StringJoiner joiner = new StringJoiner(", ", "", ".");
            for (int i = 0; i < bridgeWords.size(); i++) {
                joiner.add(bridgeWords.get(i));
            }
            return "The bridge words from " + word1 + " to " + word2 + " are: " + joiner.toString();
        }
    }
    //获取所有的桥接词，用于添加桥接词的随机选择
    public List<String> getBridgeWords(String word1, String word2) {
        List<String> bridgeWords = new ArrayList<>();
        if (!adjacencyList.containsKey(word1)) return bridgeWords;

        Map<String, Integer> word1Edges = adjacencyList.get(word1);
        for (String potentialBridge : word1Edges.keySet()) {
            Map<String, Integer> bridgeEdges = adjacencyList.get(potentialBridge);
            if (bridgeEdges != null && bridgeEdges.containsKey(word2)) {
                bridgeWords.add(potentialBridge);
            }
        }
        return bridgeWords;
    }

    //新文本插入桥接词
    public String generateNewTextWithBridges(String inputLine, TextProcessor processor) {
        List<String> inputWords = processor.processText(inputLine);
        if (inputWords.size() < 2) return inputLine; // 不足以形成桥接对

        StringBuilder newText = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < inputWords.size() - 1; i++) {
            String word1 = inputWords.get(i);
            String word2 = inputWords.get(i + 1);

            newText.append(word1).append(" "); // 先加第一个词

            // 获取所有 bridge words
            List<String> bridges = getBridgeWords(word1, word2);
            if (!bridges.isEmpty()) {
                String bridge = bridges.get(random.nextInt(bridges.size()));
                newText.append(bridge).append(" "); // 插入 bridge word
            }
        }

        // 最后加上最后一个词
        newText.append(inputWords.get(inputWords.size() - 1));
        return newText.toString();
    }

    //求最短路径
    public List<List<String>> getShortestPaths(String start, String end) {
        if (!adjacencyList.containsKey(start) || (end != null && !adjacencyList.containsKey(end))) {
            return new ArrayList<>();
        }

        Map<String, Object> shortestPaths = computeShortestPaths(start);
        if (shortestPaths == null) {
            return new ArrayList<>();
        }

        Map<String, Integer> distances = (Map<String, Integer>) shortestPaths.get("distances");
        Map<String, List<String>> predecessors = (Map<String, List<String>>) shortestPaths.get("predecessors");

        if (end != null) {
            int distance = distances.get(end);
            if (distance == Integer.MAX_VALUE) {
                return new ArrayList<>();
            }
            List<List<String>> paths = new ArrayList<>();
            List<String> path = new ArrayList<>();
            path.add(end);
            backtrack(end, start, predecessors, path, paths);
            return paths;
        } else {
            return new ArrayList<>();
        }
    }

    public Map<String, List<List<String>>> getAllShortestPaths(String start) {
        if (!adjacencyList.containsKey(start)) {
            return new HashMap<>();
        }

        Map<String, Object> shortestPaths = computeShortestPaths(start);
        Map<String, Integer> distances = (Map<String, Integer>) shortestPaths.get("distances");
        Map<String, List<String>> predecessors = (Map<String, List<String>>) shortestPaths.get("predecessors");

        Map<String, List<List<String>>> allPaths = new HashMap<>();
        for (String node : adjacencyList.keySet()) {
            if (node.equals(start)) {
                continue;
            }
            int distance = distances.get(node);
            if (distance == Integer.MAX_VALUE) {
                continue;
            }
            List<List<String>> paths = new ArrayList<>();
            List<String> path = new ArrayList<>();
            path.add(node);
            backtrack(node, start, predecessors, path, paths);
            allPaths.put(node, paths);
        }
        return allPaths;
    }

    private Map<String, Object> computeShortestPaths(String start) {
        if (!adjacencyList.containsKey(start)) {
            return null;
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, List<String>> predecessors = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(Comparator.comparingInt(NodeDistance::getDistance));

        for (String node : adjacencyList.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
            predecessors.put(node, new ArrayList<>());
        }
        distances.put(start, 0);
        queue.add(new NodeDistance(start, 0));

        while (!queue.isEmpty() && !stopRequested) {
            NodeDistance current = queue.poll();
            String u = current.node;
            int currentDist = current.distance;

            if (currentDist > distances.get(u)) {
                continue;
            }

            for (Map.Entry<String, Integer> entry : adjacencyList.get(u).entrySet()) {
                String v = entry.getKey();
                int weight = entry.getValue();
                int newDist = currentDist + weight;

                if (newDist < distances.get(v)) {
                    distances.put(v, newDist);
                    predecessors.get(v).clear();
                    predecessors.get(v).add(u);
                    queue.add(new NodeDistance(v, newDist));
                } else if (newDist == distances.get(v)) {
                    predecessors.get(v).add(u);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("distances", distances);
        result.put("predecessors", predecessors);
        return result;
    }

    private void backtrack(String current, String start, Map<String, List<String>> predecessors, List<String> path, List<List<String>> result) {
        if (current.equals(start)) {
            List<String> reversedPath = new ArrayList<>(path);
            Collections.reverse(reversedPath);
            result.add(reversedPath);
            return;
        }

        for (String pred : predecessors.get(current)) {
            path.add(pred);
            backtrack(pred, start, predecessors, path, result);
            path.remove(path.size() - 1);
        }
    }

    private static class NodeDistance {
        private final String node;
        private final int distance;

        public NodeDistance(String node, int distance) {
            this.node = node;
            this.distance = distance;
        }

        public int getDistance() {
            return distance;
        }
    }
    // 在WordGraph类中添加辅助方法
    public boolean existsNode(String word) {
        return adjacencyList.containsKey(word);
    }


//    public void printShortestPaths(String start, String end) {
//        //起始单词为空
//        if (start == null || start.isEmpty()) {
//            System.out.println("Invalid start word.");
//            return;
//        }
//        //如果不在图中
//        if (!adjacencyList.containsKey(start)) {
//            System.out.println("Start word '" + start + "' not found in graph.");
//            return;
//        }
//        if (end != null) {
//            if (!adjacencyList.containsKey(end)) {
//                System.out.println("End word '" + end + "' not found in graph.");
//                return;
//            }
//            Map<String, List<String>> predecessors = new HashMap<>();
//            Map<String, Integer> distances = new HashMap<>();
//            bfs(start, end, predecessors, distances);
//            if (!distances.containsKey(end)) {
//                System.out.println("No path exists from '" + start + "' to '" + end + "'.");
//                return;
//            }
//            List<List<String>> paths = getAllPaths(start, end, predecessors);
//            System.out.println("Shortest path(s) from '" + start + "' to '" + end + "':");
//            for (List<String> path : paths) {
//                System.out.println(String.join(" -> ", path));
//            }
//        } else {
//            Map<String, List<String>> predecessors = new HashMap<>();
//            Map<String, Integer> distances = new HashMap<>();
//            bfs(start, null, predecessors, distances);
//            for (String target : adjacencyList.keySet()) {
//                if (target.equals(start)) {
//                    continue;
//                }
//                if (!distances.containsKey(target)) {
//                    System.out.println("No path from '" + start + "' to '" + target + "'.");
//                    continue;
//                }
//                List<List<String>> paths = getAllPaths(start, target, predecessors);
//                System.out.println("Shortest path(s) from '" + start + "' to '" + target + "':");
//                for (List<String> path : paths) {
//                    System.out.println(String.join(" -> ", path));
//                }
//            }
//        }
//    }
//
//    private void bfs(String start, String end, Map<String, List<String>> predecessors, Map<String, Integer> distances) {
//        Queue<String> queue = new LinkedList<>();
//        predecessors.clear();
//        distances.clear();
//        distances.put(start, 0);
//        queue.add(start);
//        predecessors.put(start, new ArrayList<>());
//        boolean endFound = false;
//        while (!queue.isEmpty()) {
//            int levelSize = queue.size();
//            boolean foundInLevel = false;
//            for (int i = 0; i < levelSize; i++) {
//                String current = queue.poll();
//                if (end != null && current.equals(end)) {
//                    foundInLevel = true;
//                }
//                int currentDistance = distances.get(current);
//                for (String neighbor : adjacencyList.getOrDefault(current, Collections.emptyMap()).keySet()) {
//                    if (!distances.containsKey(neighbor)) {
//                        distances.put(neighbor, currentDistance + 1);
//                        List<String> predList = new ArrayList<>();
//                        predList.add(current);
//                        predecessors.put(neighbor, predList);
//                        queue.add(neighbor);
//                    } else if (distances.get(neighbor) == currentDistance + 1) {
//                        predecessors.get(neighbor).add(current);
//                    }
//                }
//            }
//            if (end != null && foundInLevel) {
//                break;
//            }
//        }
//    }
//
//    private List<List<String>> getAllPaths(String start, String end, Map<String, List<String>> predecessors) {
//        List<List<String>> paths = new ArrayList<>();
//        if (start.equals(end)) {
//            List<String> path = new ArrayList<>();
//            path.add(start);
//            paths.add(path);
//            return paths;
//        }
//        List<String> preds = predecessors.get(end);
//        if (preds == null || preds.isEmpty()) {
//            return paths;
//        }
//        for (String pred : preds) {
//            List<List<String>> predPaths = getAllPaths(start, pred, predecessors);
//            for (List<String> path : predPaths) {
//                List<String> newPath = new ArrayList<>(path);
//                newPath.add(end);
//                paths.add(newPath);
//            }
//        }
//        return paths;
//    }

    // PageRank计算方法
    public void computeAndPrintPageRank(double d) {
        Set<String> nodes = adjacencyList.keySet();
        if (nodes.isEmpty()) {
            System.out.println("Graph is empty.");
            return;
        }

        // 预处理出链权重和反向邻接表
        Map<String, Double> outWeights = new HashMap<>();
        Map<String, List<String>> reverseAdjacency = new HashMap<>();

        for (String source : nodes) {
            Map<String, Integer> edges = adjacencyList.get(source);
            // 计算总出链权重
            double total = edges.values().stream().mapToInt(Integer::intValue).sum();
            outWeights.put(source, total);
            // 构建反向邻接表
            for (String target : edges.keySet()) {
                reverseAdjacency.putIfAbsent(target, new ArrayList<>());
                reverseAdjacency.get(target).add(source);
            }
        }

        // 初始化参数
        final double DAMPING =d;
        final int MAX_ITERATIONS = 100;
        final double EPSILON = 1e-8;
        int nodeCount = nodes.size();

        // 初始化PageRank值
        Map<String, Double> pageRank = new HashMap<>();
        double initialValue = 1.0 / nodeCount;
        Map<String, Double> finalPageRank = pageRank;
        nodes.forEach(node -> finalPageRank.put(node, initialValue));

        // PageRank迭代计算
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Map<String, Double> newRank = new HashMap<>();

            // 计算悬挂节点总PR值
            double danglingSum = nodes.stream()
                    .filter(node -> outWeights.get(node) == 0)
                    .mapToDouble(pageRank::get)
                    .sum();

            // 计算每个节点的新PR值
            for (String node : nodes) {
                // 来自正常链接的贡献
                Map<String, Double> finalPageRank1 = pageRank;
                double rankFromLinks = reverseAdjacency.getOrDefault(node, Collections.emptyList())
                        .stream()
                        .mapToDouble(source -> {
                            double sourceOut = outWeights.get(source);
                            return sourceOut > 0 ?
                                    finalPageRank1.get(source) * adjacencyList.get(source).get(node) / sourceOut :
                                    0;
                        })
                        .sum();

                // 综合所有组成部分
                double newValue = (1 - DAMPING) / nodeCount +
                        DAMPING * (rankFromLinks + danglingSum / nodeCount);
                newRank.put(node, newValue);
            }

            // 检查收敛
            double delta = 0.0;
            for (String node : nodes) {
                delta += Math.abs(newRank.get(node) - pageRank.get(node));
            }
            pageRank = newRank;

            if (delta < EPSILON) {
                System.out.println("Converged after " + (i+1) + " iterations");
                break;
            }
        }

        // 格式化输出结果
        System.out.println("\nPageRank Values:");
        pageRank.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry -> System.out.printf("%-5s %.6f%n", entry.getKey(), entry.getValue()));
    }

    //随机游走算法
    public void randomWalkToFile(String filename) throws IOException {
        if (adjacencyList.isEmpty()) {
            throw new IllegalStateException("Graph is empty");
        }

        List<String> nodes = new ArrayList<>(adjacencyList.keySet());
        String current = nodes.get(new Random().nextInt(nodes.size()));
        List<String> pathNodes = new ArrayList<>(Collections.singletonList(current));
        Set<String> visitedEdges = new HashSet<>();

        startStopMonitor();

        try {
            while (true) {
                Map<String, Integer> edges = adjacencyList.get(current);
                if (edges == null || edges.isEmpty()) break;

                String next = selectNextNodeWithoutWeights(edges);
                String edgeKey = current + "->" + next;

                if (visitedEdges.contains(edgeKey)) {
                    pathNodes.add(next);
                    break;
                }

                visitedEdges.add(edgeKey);
                pathNodes.add(next);
                current = next;

                if (stopRequested) break;
            }
        } finally {
            stopRequested = true;
        }

        writePathToFile(filename, pathNodes);
    }
    //根据权重挑选节点
    private String selectNextNodeWithWeights(Map<String, Integer> edges) {
        List<String> nodes = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int totalWeight = edges.values().stream().mapToInt(Integer::intValue).sum();

        edges.forEach((node, weight) -> {
            nodes.add(node);
            weights.add(weight);
        });

        int randomValue = new Random().nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < weights.size(); i++) {
            cumulative += weights.get(i);
            if (randomValue < cumulative) {
                return nodes.get(i);
            }
        }
        return nodes.get(nodes.size() - 1);
    }

    // 根据所有可达节点均匀随机选择一个
    private String selectNextNodeWithoutWeights(Map<String, Integer> edges) {
        List<String> nodes = new ArrayList<>(edges.keySet());
        return nodes.get(new Random().nextInt(nodes.size()));
    }

    //提前终止
    private void startStopMonitor() {
        Thread monitorThread = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                while (!stopRequested) {
                    if (reader.ready()) {
                        reader.readLine();  // 用户按下回车后中止游走
                        stopRequested = true;
                        break;
                    }
                    Thread.sleep(100);
                }
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.start();
    }
    //写入文件
    private void writePathToFile(String filename, List<String> path) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(String.join(" -> ", path));
        }
    }

}
