package name.modid.behavior;

/**
 * 行为文件解析失败（格式错误、未知操作/表达式等）
 */
public class BehaviorParseException extends Exception {

    public BehaviorParseException(String message) {
        super(message);
    }
}
