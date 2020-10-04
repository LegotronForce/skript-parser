package io.github.syst3ms.skriptparser.expressions;

import io.github.syst3ms.skriptparser.Parser;
import io.github.syst3ms.skriptparser.lang.Expression;
import io.github.syst3ms.skriptparser.lang.Literal;
import io.github.syst3ms.skriptparser.lang.TriggerContext;
import io.github.syst3ms.skriptparser.log.ErrorType;
import io.github.syst3ms.skriptparser.parsing.ParseContext;
import io.github.syst3ms.skriptparser.util.Time;
import org.jetbrains.annotations.Nullable;

/**
 * Midnight and noon time constants and the ability to use
 * expressions like {@code 12 o' clock} for literal usage.
 *
 * @name Time Constants
 * @pattern (midnight|noon|midday)
 * @pattern %*integer% o'[ ]clock
 * @since ALPHA
 * @author Mwexim
 */
public class LitTimeConstants implements Literal<Time> {
    static {
        Parser.getMainRegistration().addExpression(
                LitTimeConstants.class,
                Time.class,
                true,
                "(noon|midday|1:midnight)",
                "%*integer% o'[ ]clock"
        );
    }

    private Literal<Long> hours;
    private boolean onClock;
    private boolean midnight;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, ParseContext parseContext) {
        onClock = matchedPattern == 1;
        midnight = parseContext.getParseMark() == 0;
        if (onClock) {
            hours = (Literal<Long>) expressions[0];
            if (hours.getSingle().isPresent()) {
                long h = hours.getSingle().get();
                if (h < 0 || 24 < h) {
                    parseContext.getLogger().error("The given hour ('"
                                    + h + "') is not in between 0 and 24",
                            ErrorType.SEMANTIC_ERROR
                    );
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Time[] getValues() {
        if (onClock) {
            return hours.getSingle()
                    .map(h -> Time.of(h.intValue(), 0, 0, 0))
                    .map(t -> new Time[] {t})
                    .orElse(new Time[0]);
        } else {
            return new Time[] {midnight ? Time.MIDNIGHT : Time.NOON};
        }
    }

    @Override
    public String toString(@Nullable TriggerContext ctx, boolean debug) {
        if (onClock) {
            return hours.toString(ctx, debug) + " o' clock";
        } else {
            return midnight ? "midnight" : "noon";
        }
    }
}