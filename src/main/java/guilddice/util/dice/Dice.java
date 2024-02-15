package guilddice.util.dice;

import guilddice.util.dice.expr.DiceExpr;
import guilddice.util.dice.result.DiceResult;

import java.util.LinkedList;
import java.util.List;

public class Dice {
    public DiceResult roll(DiceExpr expression) {
        final List<List<Integer>> results = new LinkedList<>();
        expression.parts().forEach(e -> results.add(e.calculate()));
        return new DiceResult(results);
    }
}
