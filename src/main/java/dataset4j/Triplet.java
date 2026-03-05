package dataset4j;

import java.util.function.Function;

/**
 * A generic triplet of values, used for three-way joins or when
 * enriching a Pair with additional data.
 *
 * <pre>
 * // Three-way join: employee → department → budget
 * Dataset&lt;Triplet&lt;Employee, Department, Budget&gt;&gt; enriched =
 *     employees
 *         .innerJoin(departments, Employee::dept, Department::dept)
 *         .innerJoin(budgets, p -> p.right().dept(), Budget::dept)
 *         .map(p -> new Triplet&lt;&gt;(p.left().left(), p.left().right(), p.right()));
 *
 * // Or use the convenience method:
 * Dataset&lt;Triplet&lt;Employee, Department, Budget&gt;&gt; enriched =
 *     employees.innerJoin3(
 *         departments, Employee::dept, Department::dept,
 *         budgets, Department::dept, Budget::dept);
 *
 * // Access all three
 * enriched.filter(t -> t.first().age() > 25)
 *         .map(t -> t.first().name() + " @ " + t.second().location()
 *                  + " budget=" + t.third().amount());
 * </pre>
 *
 * @param first  the left/driving row
 * @param second the first joined row
 * @param third  the second joined row
 */
public record Triplet<A, B, C>(A first, B second, C third) {

    /** Map all three into a new value. */
    public <V> V map(TriFunction<A, B, C, V> mapper) {
        return mapper.apply(first, second, third);
    }

    /** Transform the first element. */
    public <A2> Triplet<A2, B, C> mapFirst(Function<A, A2> mapper) {
        return new Triplet<>(mapper.apply(first), second, third);
    }

    /** Transform the second element. */
    public <B2> Triplet<A, B2, C> mapSecond(Function<B, B2> mapper) {
        return new Triplet<>(first, mapper.apply(second), third);
    }

    /** Transform the third element. */
    public <C2> Triplet<A, B, C2> mapThird(Function<C, C2> mapper) {
        return new Triplet<>(first, second, mapper.apply(third));
    }

    /** Drop third, keeping first two as a Pair. */
    public Pair<A, B> dropThird() {
        return new Pair<>(first, second);
    }

    /** Drop first, keeping last two as a Pair. */
    public Pair<B, C> dropFirst() {
        return new Pair<>(second, third);
    }

    /** Positional aliases */
    public A left()   { return first; }
    public B middle() { return second; }
    public C right()  { return third; }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ", " + third + ")";
    }

    /**
     * Functional interface for a function accepting three arguments.
     */
    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
}
