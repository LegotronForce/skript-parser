package io.github.syst3ms.skriptparser.parsing;

import io.github.syst3ms.skriptparser.Main;
import io.github.syst3ms.skriptparser.expressions.CondExprCompare;
import io.github.syst3ms.skriptparser.expressions.ExprWhether;
import io.github.syst3ms.skriptparser.file.FileElement;
import io.github.syst3ms.skriptparser.file.FileParser;
import io.github.syst3ms.skriptparser.file.FileSection;
import io.github.syst3ms.skriptparser.lang.Conditional;
import io.github.syst3ms.skriptparser.lang.Expression;
import io.github.syst3ms.skriptparser.lang.SimpleLiteral;
import io.github.syst3ms.skriptparser.lang.While;
import io.github.syst3ms.skriptparser.pattern.CompoundElement;
import io.github.syst3ms.skriptparser.pattern.ExpressionElement;
import io.github.syst3ms.skriptparser.pattern.TextElement;
import io.github.syst3ms.skriptparser.registration.SkriptRegistration;
import io.github.syst3ms.skriptparser.types.PatternType;
import io.github.syst3ms.skriptparser.types.TypeManager;
import io.github.syst3ms.skriptparser.types.comparisons.Comparator;
import io.github.syst3ms.skriptparser.types.comparisons.Comparators;
import io.github.syst3ms.skriptparser.types.comparisons.Relation;
import io.github.syst3ms.skriptparser.util.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static io.github.syst3ms.skriptparser.parsing.TestExpressions.ExprRandom;
import static io.github.syst3ms.skriptparser.parsing.TestExpressions.ExprSquared;
import static io.github.syst3ms.skriptparser.parsing.TestExpressions.ExprSubstring;
import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
public class SyntaxParserTest {

    static {
        SkriptRegistration registration = new SkriptRegistration("unit-tests");
        registration.addType(
                Object.class,
                "object",
                "object¦s"
        );
        registration.addType(
                Number.class,
                "number",
                "number¦s",
                s -> {
                    Number n;
                    try {
                        n = Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        try {
                            n = Double.parseDouble(s);
                        } catch (NumberFormatException e1) {
                            try {
                                n = new BigInteger(s);
                            } catch (NumberFormatException e2) {
                                try {
                                    n = new BigDecimal(s);
                                } catch (NumberFormatException e3) {
                                    return null;
                                }
                            }
                        }
                    }
                    return n;
                },
                o -> {
                    if (o == null)
                        return TypeManager.NULL_REPRESENTATION;
                    if (o instanceof Long) {
                        return o.toString();
                    } else if (o instanceof Double) {
                        return Double.toString(o.doubleValue());
                    } else if (o instanceof BigInteger || o instanceof BigDecimal) {
                        return o.toString(); // Both BigInteger and BigDecimal override toString
                    }
                    assert false;
                    return null; // Can't happen, so we don't really have to worry about that
                }
        );
        registration.addType(
                String.class,
                "string",
                "string¦s"
        );
        registration.addType(
                Boolean.class,
                "boolean",
                "boolean¦s",
                s -> {
                    if (s.equalsIgnoreCase("true")) {
                        return true;
                    } else if (s.equalsIgnoreCase("false")) {
                        return false;
                    } else {
                        return null;
                    }
                }
        );
        registration.register();
        registration.addExpression(
                ExprSquared.class,
                Number.class,
                true,
                "the number %number% squared"
        );
        registration.addExpression(
                ExprRandom.class,
                Number.class,
                true,
                "random (0¦number|1¦integer) between %number% and %number% [2¦exclusively]"
        );
        registration.addExpression(
                ExprSubstring.class,
                String.class,
                true,
                "substring %string% from %number% to %number%"
        );
        registration.addExpression(
                ExprWhether.class,
                Boolean.class,
                true,
                "whether %~=boolean%"
        );
        registration.addExpression(
                CondExprCompare.class,
                Boolean.class,
                true,
                1,
                CondExprCompare.PATTERNS.getPatterns()
        );
        registration.addSection(
                While.class,
                "while %=boolean%"
        );
        registration.addEffect(
                TestEffects.EffPrintln.class,
                "println %string%"
        );
        Comparators.registerComparator(Number.class, Number.class, new Comparator<Number, Number>() {
            @Override
            public Relation apply(Number number, Number number2) {
                if (number instanceof BigDecimal || number2 instanceof BigDecimal) {
                    if (number instanceof BigDecimal && number2 instanceof BigDecimal)
                        return Relation.get(((BigDecimal) number).compareTo((BigDecimal) number2));
                    else if (number instanceof BigDecimal)
                        return Relation.get(((BigDecimal) number).compareTo(new BigDecimal(number2.toString())));
                    else
                        return Relation.get(-((BigDecimal) number2).compareTo(new BigDecimal(number.toString())));
                } else if (number instanceof Double || number2 instanceof Double) {
                    return Relation.get(number.doubleValue() - number2.doubleValue());
                } else {
                    return Relation.get(number.longValue() - number2.longValue());
                }
            }

            @Override
            public boolean supportsOrdering() {
                return true;
            }
        });
        registration.addConverter(
                Number.class,
                Boolean.class,
                n -> {
                    long l = n.longValue();
                    return l != 0;
                }
        );
        registration.register();
    }

    public void assertExpressionEquals(Expression<?> expected, Expression<?> actual) {
        if (expected == actual)
            return;
        if (expected == null || actual == null)
            fail();
        assertArrayEquals(expected.getValues(null), actual.getValues(null));
    }

    public void assertExpressionTrue(Expression<?> actual) {
        assertExpressionEquals(new SimpleLiteral<>(Boolean.class, true), actual);
    }

    @Test
    public void parseExpression() {
        PatternType<Number> numberType = new PatternType<>(TypeManager.getByClassExact(Number.class), true);
        assertExpressionEquals(new SimpleLiteral<>(Long.class, 2L), SyntaxParser.parseExpression("2", numberType));
        int expectedInt = SyntaxParser.parseExpression("random integer between 0 and 10", numberType)
                                      .getSingle(null)
                                      .intValue();
        assertTrue(0 <= expectedInt && expectedInt <= 10);
        double expectedDouble = SyntaxParser.parseExpression("random number between 9.9999 and 10 exclusively", numberType)
                                            .getSingle(null)
                                            .doubleValue();
        assertTrue(9.9999 + Double.MIN_VALUE <= expectedDouble && expectedDouble <= 10 - Double.MIN_VALUE);
        PatternType<String> stringType = new PatternType<>(TypeManager.getByClassExact(String.class), true);
        assertExpressionEquals(
                new SimpleLiteral<>(String.class, "Hello"),
                SyntaxParser.parseExpression("substring \"Hello\" from 0 to 5", stringType)
        );
        assertExpressionEquals(
                new SimpleLiteral<>(String.class, "Hello, I am \"raw\""),
                SyntaxParser.parseExpression("substring R\"$(Hello, I am \"raw\")$\" from 0 to 17", stringType)
        );
        assertExpressionEquals(
                new SimpleLiteral<>(String.class, "I am \"raw\" too !"),
                SyntaxParser.parseExpression("substring 'I am \"raw\" too !' from 0 to 16", stringType)
        );
        assertExpressionEquals(
            new SimpleLiteral<>(Number.class, 1L, 2L, 3L),
            SyntaxParser.parseExpression("1, 2 and 3", new PatternType<>(TypeManager.getByClass(Number.class), false))
        );
        assertExpressionTrue(
                SyntaxParser.parseBooleanExpression("whether 5 is greater than 0", false)
        );
        assertExpressionTrue(
                SyntaxParser.parseBooleanExpression("whether 5 is between 1 and 10", false)
        );
        assertExpressionTrue(
                SyntaxParser.parseExpression("1", SyntaxParser.BOOLEAN_PATTERN_TYPE)
        );
        assertExpressionTrue(
                SyntaxParser.parseBooleanExpression("whether 2 != 5", false)
        );
        assertExpressionTrue(
                SyntaxParser.parseBooleanExpression("  \r   -3   iS    \t   eQuAl TO\t\t\t\t  -3     ", true)
        );
        // These tests try to push the parser to its limits more than anything else
        /*
        assertExpressionEquals(
                new SimpleLiteral<>(Boolean.class, true, false, true),
                SyntaxParser.parseExpression("whether 2 <= 4, (whether 5 is greater than or equal to 6) and true", new PatternType<>(TypeManager.getByClass(Boolean.class), false))
        );
        assertExpressionTrue(
                SyntaxParser.parseBooleanExpression("whether whether 2 <= 4, (whether 10 is greater than or equal to 6) and true are true", false)
        );
        */
    }

    @Test
    public void parseSection() throws Exception {
        FileParser fileParser = new FileParser();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("section-test.txt").getFile());
        List<String> lines = FileUtils.readAllLines(file);
        List<FileElement> elements = fileParser.parseFileLines("section-test", lines, 0, 1);
        FileSection sec = (FileSection) elements.get(0);
        SyntaxParser.parseSection(sec);
    }
}