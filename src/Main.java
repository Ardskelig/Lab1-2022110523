import java.io.IOException;
import java.util.*;
import java.nio.file.*;

public class Main {
    private static WordGraph graph;
    private static TextProcessor processor = new TextProcessor();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        showMainMenu();
    }

    private static void showMainMenu() {
        while (true) {
            System.out.println("\n=== lab1 ===");
            System.out.println("1. 从文件构建单词图");
            System.out.println("2. 展示当前单词图");
            System.out.println("3. 导出DOT可视化文件");
            System.out.println("4. 查询桥接词");
            System.out.println("5. 文本扩展（桥接词插入）");
            System.out.println("6. 计算最短路径");
            System.out.println("7. 计算PageRank");
            System.out.println("8. 执行随机游走");
            System.out.println("0. 退出系统");
            System.out.print("请选择操作编号：");

            int choice = readIntInput();
            try {
                switch (choice) {
                    case 1 -> buildGraphFromFile();
                    case 2 -> printGraph();
                    case 3 -> exportToDot();
                    case 4 -> queryBridgeWords();
                    case 5 -> expandText();
                    case 6 -> findShortestPath();
                    case 7 -> computePageRank();
                    case 8 -> performRandomWalk();
                    case 0 -> {
                        System.out.println("感谢使用，再见！");
                        System.exit(0);
                    }
                    default -> System.out.println("无效的选项，请重新输入");
                }
            } catch (Exception e) {
                System.out.println("操作失败: " + e.getMessage());
            }
        }
    }

    //输入读取
    private static int readIntInput() {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.print("输入格式错误，请重新输入数字：");
            }
        }
    }

    //建立有向树
    private static void buildGraphFromFile() throws IOException {
        System.out.print("请输入文本文件路径：");
        String path = scanner.nextLine().trim();
        String content = Files.readString(Paths.get(path));
        List<String> words = processor.processText(content);
        graph = new WordGraph();
        graph.buildGraph(words);
        System.out.println("单词图构建完成，共包含 " + graph.nodeCount() + " 个节点");
    }
    //展示有向树
    private static void printGraph() {
        validateGraphExists();
        graph.printGraph();
    }
    //导出.dot文件
    private static void exportToDot() throws IOException {
        validateGraphExists();
        System.out.print("请输入输出文件名（默认graph.dot）：");
        String filename = scanner.nextLine().trim();
        filename = filename.isEmpty() ? "graph.dot" : filename;
        graph.exportToDot(filename);
        System.out.println("DOT文件已生成: " + filename);
    }
    //获取桥接词
    private static void queryBridgeWords() {
        validateGraphExists();
        System.out.print("请输入第一个单词：");
        String word1 = scanner.nextLine().trim().toLowerCase();
        System.out.print("请输入第二个单词：");
        String word2 = scanner.nextLine().trim().toLowerCase();
        String result = graph.queryBridgeWords(word1, word2);
        System.out.println("查询结果: " + result);
    }
    //扩展文本
    private static void expandText() {
        validateGraphExists();
        System.out.print("请输入待扩展文本：");
        String input = scanner.nextLine().trim();
        String result = graph.generateNewTextWithBridges(input, processor);
        System.out.println("扩展后的文本：\n" + result);
    }
    //最短路径
    private static void findShortestPath() {
        validateGraphExists();
        System.out.print("请输入起点单词：");
        String source = scanner.nextLine().trim().toLowerCase();

        // 检查起点是否存在
        if (!graph.existsNode(source)) {
            System.out.println("错误：起点单词 '" + source + "' 不在图中");
            return;
        }

        System.out.print("请输入终点单词（留空显示所有路径）：");
        String target = scanner.nextLine().trim().toLowerCase();
        if (target.isEmpty()) {
            // 显示所有路径的逻辑
            Map<String, List<List<String>>> allPaths = graph.getAllShortestPaths(source);
            if (allPaths.isEmpty()) {
                System.out.println("没有找到任何可达路径");
                return;
            }
            System.out.println("\n从 " + source + " 出发的所有最短路径：");
            allPaths.forEach((node, paths) -> {
                System.out.println("\n到达 " + node + " 的路径"); // 假设添加了权重计算方法
                paths.forEach(path -> {
                    Collections.reverse(path); // 路径是反向存储的需要反转
                    System.out.println(String.join(" -> ", path));
                });
            });
        } else {
            // 检查终点是否存在
            if (!graph.existsNode(target)) {
                System.out.println("错误：终点单词 '" + target + "' 不在图中");
                return;
            }

            // 显示单目标路径
            List<List<String>> paths = graph.getShortestPaths(source, target);
            if (paths.isEmpty()) {
                System.out.println("没有从 " + source + " 到 " + target + " 的路径");
                return;
            }
            System.out.println("\n从 " + source + " 到 " + target + " 的最短路径");
            paths.forEach(path -> {
                Collections.reverse(path); // 路径是反向存储的需要反转
                System.out.println(String.join(" -> ", path));
            });
        }
    }


    //计算pagerank
    private static void computePageRank() {
        validateGraphExists();
        System.out.print("请输入阻尼系数（默认0.85）：");
        String damping = scanner.nextLine().trim();
        double d = damping.isEmpty() ? 0.85 : Double.parseDouble(damping);
        graph.computeAndPrintPageRank(d);
    }
    //随机游走
    private static void performRandomWalk() {
        validateGraphExists();
        System.out.print("请输入输出文件名（默认walk_output.txt）：");
        String filename = scanner.nextLine().trim();
        filename = filename.isEmpty() ? "walk_output.txt" : filename;
        System.out.println("按下 Enter 键开始随机游走...");
        scanner.nextLine(); // 吃掉输入缓冲区中残留的回车

        try {
            System.out.println("开始随机游走，按下enter可提前终止");
            graph.randomWalkToFile(filename);
            System.out.println("随机游走结果已保存至 " + filename);
        } catch (IOException e) {
            System.out.println("文件保存失败: " + e.getMessage());
        }
    }

    private static void validateGraphExists() {
        if (graph == null || graph.nodeCount() == 0) {
            throw new IllegalStateException("请先构建单词图（选项1）");
        }
    }
}
