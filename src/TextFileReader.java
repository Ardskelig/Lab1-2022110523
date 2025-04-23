import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

//用于读取文件，返回字符串
public class TextFileReader {
    public static String readFile(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }
}
