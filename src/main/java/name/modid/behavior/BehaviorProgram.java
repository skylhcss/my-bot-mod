package name.modid.behavior;

import java.util.List;
import java.util.Map;

/**
 * 行为脚本的抽象语法树（AST）
 *
 * 由 {@link BehaviorParser} 从 JSON 行为文件解析产生，
 * 由 {@link BehaviorRuntime} 逐 tick 解释执行。
 *
 * 语句为统一的 tagged 结构（op + 命名表达式参数 + 嵌套语句块），
 * 表达式为 sealed record 层次（Java 17 用 instanceof 模式匹配求值）。
 */
public final class BehaviorProgram {

    /** 行为名（显示用，缺省取文件名） */
    public final String name;
    /** 行为描述 */
    public final String description;
    /** 顶层是否循环执行 */
    public final boolean loop;
    /** 顶层语句序列 */
    public final List<Stmt> body;
    /** 来源文件名（不含路径，用于 UI 与错误消息） */
    public final String sourceFile;

    public BehaviorProgram(String name, String description, boolean loop, List<Stmt> body, String sourceFile) {
        this.name = name;
        this.description = description;
        this.loop = loop;
        this.body = body;
        this.sourceFile = sourceFile;
    }

    /** 粗略的语句总数（含嵌套），用于 UI 展示与预算参考 */
    public int statementCount() {
        return countStmts(body);
    }

    private static int countStmts(List<Stmt> list) {
        int n = 0;
        for (Stmt s : list) {
            n++;
            for (List<Stmt> block : s.blocks().values()) {
                n += countStmts(block);
            }
        }
        return n;
    }

    // ==================== 语句 ====================

    /**
     * 语句节点
     *
     * @param op     操作名（say/wait/move/repeat/while/if/...）
     * @param args   命名表达式参数（如 text/ticks/x/y/z/cond/var/value...；var 等标识符以 Str 字面量存放）
     * @param blocks 嵌套语句块（body/then/else，无则为空 Map）
     */
    public record Stmt(String op, Map<String, Expr> args, Map<String, List<Stmt>> blocks) {

        public Expr arg(String key) {
            return args.get(key);
        }

        public List<Stmt> block(String key) {
            return blocks.getOrDefault(key, List.of());
        }
    }

    // ==================== 表达式 ====================

    /** 表达式节点：字面量 / 变量 / 一元 / 二元 / 传感器 */
    public sealed interface Expr permits Num, Str, Bool, Var, Bin, Un, Sensor {
    }

    /** 数字字面量 */
    public record Num(double v) implements Expr {
    }

    /** 字符串字面量 */
    public record Str(String v) implements Expr {
    }

    /** 布尔字面量 */
    public record Bool(boolean v) implements Expr {
    }

    /** 变量引用 */
    public record Var(String name) implements Expr {
    }

    /** 二元运算：+ - * / % == != < > <= >= and or concat */
    public record Bin(String op, Expr left, Expr right) implements Expr {
    }

    /** 一元运算：not / neg */
    public record Un(String op, Expr operand) implements Expr {
    }

    /** 传感器/内置函数：health、posX、invCount(item)、random(min,max) 等 */
    public record Sensor(String name, List<Expr> args) implements Expr {
    }
}
