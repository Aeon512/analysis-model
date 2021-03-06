package edu.hm.hafner.analysis.parser;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.IssueBuilder;
import edu.hm.hafner.analysis.LookaheadParser;
import edu.hm.hafner.analysis.ParsingException;
import edu.hm.hafner.util.LookaheadStream;

import static j2html.TagCreator.*;

/**
 * A parser for ErrorProne warnings during a Maven build.
 *
 * @author Ullrich Hafner
 */
public class ErrorProneParser extends LookaheadParser {
    private static final long serialVersionUID = 8434408068719510740L;

    private static final Pattern URL_PATTERN = Pattern.compile("\\s+\\(see (?<url>http\\S+)\\s*\\)");
    private static final Pattern FIX_PATTERN = Pattern.compile("\\s+Did you mean '(?<code>.*)'\\?");
    private static final String WARNINGS_PATTERN
            = "\\[(?<severity>WARNING|ERROR)\\]\\s+"
            + "(?<file>.+):"
            + "\\[(?<line>\\d+),(?<column>\\d+)\\]\\s+"
            + "\\[(?<type>\\w+)\\]\\s+"
            + "(?<message>.*)";

    /**
     * Creates a new instance of {@link ErrorProneParser}.
     */
    public ErrorProneParser() {
        super(WARNINGS_PATTERN);
    }

    @Override
    protected Optional<Issue> createIssue(final Matcher matcher, final LookaheadStream lookahead,
            final IssueBuilder builder) throws ParsingException {
        builder.setFileName(matcher.group("file"))
                .setLineStart(matcher.group("line"))
                .setColumnStart(matcher.group("column"))
                .setType(matcher.group("type"))
                .setMessage(matcher.group("message"));
        builder.guessSeverity(matcher.group("severity"));
        StringBuilder description = new StringBuilder();
        StringBuilder url = new StringBuilder();
        while (lookahead.hasNext("^\\s+.*")) {
            String line = lookahead.next();
            Matcher urlMatcher = URL_PATTERN.matcher(line);
            if (urlMatcher.matches()) {
                url.append(p().with(
                        a().withHref(urlMatcher.group("url"))
                        .withText("See ErrorProne documentation."))
                        .render());
            }
            else {
                Matcher fixMatcher = FIX_PATTERN.matcher(line);
                if (fixMatcher.matches()) {
                    description.append("Did you mean: ");
                    description.append(pre().with(
                            code().withText(fixMatcher.group("code"))).render());
                }
            }
        }

        return builder.setDescription(description.toString() + url.toString()).buildOptional();
    }
}
