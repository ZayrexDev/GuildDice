package guilddice.util.dice.expr;


import guilddice.util.dice.expr.part.DiceExprPart;
import guilddice.util.dice.expr.part.DiceExprPartComplex;
import guilddice.util.dice.expr.part.DiceExprPartNumber;
import guilddice.util.dice.expr.part.DiceExprPartSingle;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

@Getter
public class DiceExpr {
    private final List<DiceExprPart> parts;
    public static final DiceExpr CHECK;
    public static final DiceExpr HUNDRED;
    public static final DiceExpr TEN;
    public static final DiceExpr SIX;
    static {
        CHECK = new DiceExpr(List.of(new DiceExprPartSingle("1d100")));
        HUNDRED = new DiceExpr(List.of(new DiceExprPartSingle("1d100")));
        TEN = new DiceExpr(List.of(new DiceExprPartSingle("1d10")));
        SIX = new DiceExpr(List.of(new DiceExprPartSingle("1d6")));
    }
    public DiceExpr(List<DiceExprPart> parts) {
        this.parts = parts;
    }

    public static DiceExpr parse(String str) {
        if(!isValid(str)) {
            throw new IllegalArgumentException("骰子表达式有误");
        }

        if (!str.startsWith("-")) str = "+" + str;
        final LinkedList<String> compStr = new LinkedList<>();
        int last = 0;
        for (int i = 1; i < str.toCharArray().length; i++) {
            if (str.charAt(i) == '+' || str.charAt(i) == '-') {
                compStr.add(str.substring(last, i));
                last = i;
            }
        }

        if (last != str.length() - 1) compStr.add(str.substring(last));

        final LinkedList<DiceExprPart> parts = new LinkedList<>();

        compStr.forEach(s -> {
            if(isDigit(s)) {
                parts.add(new DiceExprPartNumber(s));
            } else if(s.contains("*")) {
                parts.add(new DiceExprPartComplex(s));
            } else {
                parts.add(new DiceExprPartSingle(s));
            }
        });

        return new DiceExpr(parts);
    }

    public static boolean isDigit(String str) {
        if (str == null) return false;
        if(str.startsWith("+") || str.startsWith("-")) str = str.substring(1);
        return str.chars().allMatch(value -> value >= '0' && value <= '9');
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if(parts.getFirst().isNegative()) sb.append("-");
        sb.append(parts.getFirst().toString(false));
        for (int i = 1; i < parts.size(); i++) {
            sb.append(parts.get(i).toString(true));
        }

        return sb.toString();
    }

    public static boolean isValid(String exp) {
        if(exp == null || exp.isBlank()) return false;
        return exp.chars().allMatch(value -> (value >= '0' && value <= '9') ||
                value == 'd' || value == '+' || value == '*');
    }
}
