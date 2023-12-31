package com.gitlab.mvysny.jdbiorm.condition;

import com.gitlab.mvysny.jdbiorm.JdbiOrm;
import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant;
import org.jetbrains.annotations.NotNull;

import java.text.BreakIterator;
import java.util.*;

/**
 * A FullText filter which performs the case-insensitive full-text search.
 * Any probe text must either contain all words in this query,
 * or the query words must match beginnings of all of the words contained in the probe string.
 * <p></p>
 * See the jdbi-orm README.md on how to configure a full-text search in a SQL database/RDBMS system.
 */
public final class FullTextCondition implements Condition {
    /**
     * Any probe text must either contain words in this query,
     * or the query words must match beginnings of the words contained in the probe.
     * <p></p>
     * Trimmed, not blank.
     */
    @NotNull
    private final String query;

    @NotNull
    private final Expression<?> arg;

    @NotNull
    public static Condition of(@NotNull Expression<?> arg, @NotNull String query) {
        final FullTextCondition condition = new FullTextCondition(arg, query);
        if (condition.getWords().isEmpty()) {
            return Condition.NO_CONDITION;
        }
        return condition;
    }

    public FullTextCondition(@NotNull Expression<?> arg, @NotNull String query) {
        this.query = Objects.requireNonNull(query).trim();
        if (query.isEmpty()) {
            throw new IllegalArgumentException("Parameter query: invalid value " + query + ": must not be blank");
        }
        this.arg = Objects.requireNonNull(arg);
    }

    @NotNull
    public String getQuery() {
        return query;
    }

    @NotNull
    public Expression<?> getArg() {
        return arg;
    }

    /**
     * In order for the probe to match, the probe must either match these words,
     * or the query words must match beginnings of the words contained in the probe.
     */
    public Set<String> getWords() {
        final LinkedList<String> words = splitToWords(query.trim().toLowerCase(), false, Locale.getDefault());
        return new LinkedHashSet<>(words);
    }

    /**
     * Splits text into words, no spaces. Optionally returns the punctuation characters.
     * Uses [BreakIterator.getWordInstance] - see Javadoc for [BreakIterator] for more details.
     * @param string the text to split.
     * @param punctuation defaults to false. If false, punctuation is not returned.
     * @param locale the locale to use, use {@link Locale#getDefault()} if not sure.
     * @return a list of words, never null, may be empty.
     */
    private static LinkedList<String> splitToWords(@NotNull String string, boolean punctuation, @NotNull Locale locale) {
        final BreakIterator bi = BreakIterator.getWordInstance(locale);
        bi.setText(string);
        final LinkedList<String> result = new LinkedList<>();
        while (true) {
            final int current = bi.current();
            final int next = bi.next();
            if (next == BreakIterator.DONE) {
                break;
            }
            final String word = string.substring(current, next).trim();
            if (word.isEmpty()) {
                continue;
            }
            final int c = word.codePointAt(0);
            if (punctuation || Character.isAlphabetic(c) || Character.isDigit(c)) {
                result.add(word);
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FullTextCondition that = (FullTextCondition) o;
        return Objects.equals(query, that.query) && Objects.equals(arg, that.arg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, arg);
    }

    @Override
    public String toString() {
        return query + " ~ " + getWords();
    }

    /**
     * Converts an user input into a MySQL BOOLEAN FULLTEXT search string, by
     * sanitizing input and appending with * to perform starts-with matching.
     * <p></p>
     * In your SQL just use `WHERE ... AND MATCH(search_id) AGAINST (:searchId IN BOOLEAN MODE)`.
     * @param words the list of words
     * @return full-text query string, not null. If blank, there is nothing to search for,
     * and the SQL MATCH ... AGAINST clause must be omitted.
     */
    @NotNull
    private static String toMySQLFulltextBooleanQuery(@NotNull Collection<String> words) {
        final StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isBlank() && mysqlAppearsInIndex(word)) {
                sb.append(" +").append(word).append('*');
            }
        }
        return sb.toString().trim();
    }

    /**
     * Too short words do not appear in MySQL FULLTEXT index - we must not search for
     * such words otherwise MySQL will return nothing!
     */
    private static boolean mysqlAppearsInIndex(@NotNull String word) {
        // By default, words less than 3 characters in length or greater than 84 characters in length do not appear in an InnoDB full-text search index.
        // https://dev.mysql.com/doc/refman/8.0/en/fulltext-stopwords.html
        final int length = word.trim().length();
        return length >= 3 && length <= 84;
    }

    @Override
    public @NotNull ParametrizedSql toSql() {
        final DatabaseVariant databaseVariant = JdbiOrm.databaseVariant;
        if (databaseVariant == DatabaseVariant.MySQLMariaDB) {
            final String booleanQuery = toMySQLFulltextBooleanQuery(getWords());
            if (booleanQuery.isBlank()) {
                return ParametrizedSql.MATCH_ALL;
            }
            final ParametrizedSql sql = arg.toSql();
            return ParametrizedSql.merge("MATCH(" + sql.getSql92() + ") AGAINST (:$parameterName IN BOOLEAN MODE)",
                    sql.getSql92Parameters(), Collections.singletonMap(ParametrizedSql.generateParameterName(this), booleanQuery));
        }
        throw new IllegalArgumentException("Unsupported FullText search for variant " + databaseVariant + ". Set proper variant to JdbiOrm.databaseVariant");
    }
}
