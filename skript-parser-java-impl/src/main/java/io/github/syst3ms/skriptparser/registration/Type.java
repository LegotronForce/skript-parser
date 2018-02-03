package io.github.syst3ms.skriptparser.registration;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A basic definition of a type. This doesn't handle number (single/plural), see {@link PatternType} for that.
 *
 */
// TODO use a Skript-like syntax for plural stuff
public class Type<T> {
    public static final Predicate<String> pluralGroupChecker = Pattern.compile("(?<!\\\\)\\(\\?<plural>[a-zA-Z]+\\)").asPredicate();
    private Class<T> typeClass;
    private String baseName;
    private Pattern syntaxPattern;
    private Function<String, ? extends T> literalParser;
    private Function<Object, String> toStringFunction;

    /**
     * Constructs a new Type. This consructor doesn't handle any exceptions inherent to the regex pattern.
     *
     * @param typeClass the class this type represents
     * @param baseName the basic name to represent this type with. It should be more or less a lowercase version of the Java class.
     * @param pattern the regex pattern this type should match.
     *                Plural form should be contained in a group named {@literal plural}.
     *                If the name has an irregular plural (i.e "party" becomes "parties"), then use the following contruct : {@literal part(y|(?<plural>ies))}
     */
    public Type(Class<T> typeClass, String baseName, String pattern) {
        this(typeClass, baseName, pattern, null);
    }

    /**
     * Constructs a new Type. This consructor doesn't handle any exceptions inherent to the regex pattern.
     *
     * @param typeClass the class this type represents
     * @param baseName the basic name to represent this type with. It should be more or less a lowercase version of the Java class.
     * @param pattern the regex pattern this type should match.
     *                Plural form should be contained in a group named {@literal plural}.
     *                If the name has an irregular plural (i.e "party" becomes "parties"), then use the following contruct : {@literal part(y|(?<plural>ies))}
     * @param literalParser the function that would parse literals for the given type. If the parser throws an exception on parsing, it will be
     *                      catched and the type will be ignored.
     */
    public Type(Class<T> typeClass, String baseName, String pattern, Function<String, ? extends T> literalParser) {
        this(typeClass, baseName, pattern, literalParser, Objects::toString);
    }

    @SuppressWarnings("unchecked")
    public Type(Class<T> typeClass, String baseName, String pattern, Function<String, ? extends T> literalParser, Function<? super T, String> toStringFunction) {
        this.typeClass = typeClass;
        this.baseName = baseName;
        this.literalParser = literalParser;
        this.toStringFunction = (Function<Object, String>) toStringFunction;
        pattern = pattern.trim();
        // Not handling exceptions here, developer responsability
        if (!pluralGroupChecker.test(pattern)) {
            syntaxPattern = Pattern.compile(pattern + "(?<plural>)??"); // Lazy optional group is required in this case
        } else {
            syntaxPattern = Pattern.compile(pattern);
        }
    }

    public Function<String, ? extends T> getLiteralParser() {
        return literalParser;
    }

    public Pattern getSyntaxPattern() {
        return syntaxPattern;
    }

    public Class<T> getTypeClass() {
        return typeClass;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Type)) {
            return false;
        } else {
            Type<?> o = (Type<?>) obj;
            return typeClass.equals(o.typeClass) && baseName.equals(o.baseName) && syntaxPattern.pattern().equals(o.syntaxPattern.pattern());
        }
    }

    @Override
    public int hashCode() {
        return syntaxPattern.pattern().hashCode();
    }

    public String getBaseName() {
        return baseName;
    }

    public Function<Object, String> getToStringFunction() {
        return toStringFunction;
    }
}