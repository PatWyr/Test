package com.gitlab.mvysny.jdbiorm.condition;

import com.gitlab.mvysny.jdbiorm.EntityMeta;
import com.gitlab.mvysny.jdbiorm.JdbiOrm;
import com.gitlab.mvysny.jdbiorm.TableProperty;
import com.gitlab.mvysny.jdbiorm.quirks.DatabaseVariant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.BreakIterator;
import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * The database table column which is fulltext-matched against {@link #getQuery()}.
     */
    @NotNull
    private final Expression<?> arg;

    /**
     * Creates the full text condition. If there's nothing in the query to search for,
     * returns {@link Condition#NO_CONDITION}.
     * @param arg The database table column which is fulltext-matched against given query.
     * @param query Any probe text must either contain words in this query, or the query words must match beginnings of the words contained in the probe.
     *              You can pass in raw user input here; the query is trimmed and any special characters are removed or escaped properly.
     * @return the condition.
     */
    @NotNull
    public static Condition of(@NotNull Expression<?> arg, @Nullable String query) {
        if (query == null) {
            return Condition.NO_CONDITION;
        }
        final String trimmedQuery = query.trim();
        if (trimmedQuery.isEmpty()) {
            return Condition.NO_CONDITION;
        }
        final FullTextCondition condition = new FullTextCondition(arg, trimmedQuery);
        if (condition.getWords().isEmpty()) {
            return Condition.NO_CONDITION;
        }
        return condition;
    }

    private FullTextCondition(@NotNull Expression<?> arg, @NotNull String query) {
        this.query = Objects.requireNonNull(query).trim();
        if (query.isEmpty()) {
            throw new IllegalArgumentException("Parameter query: invalid value " + query + ": must not be blank");
        }
        this.arg = Objects.requireNonNull(arg);
    }

    /**
     * Any probe text must either contain words in this query,
     * or the query words must match beginnings of the words contained in the probe.
     * <p></p>
     * Trimmed, not blank.
     */
    @NotNull
    public String getQuery() {
        return query;
    }

    /**
     * The database table column which is fulltext-matched against {@link #getQuery()}.
     */
    @NotNull
    public Expression<?> getArg() {
        return arg;
    }

    @Nullable
    private transient LinkedHashSet<String> words = null;

    /**
     * In order for the probe to match, the probe must either match these words,
     * or the query words must match beginnings of the words contained in the probe.
     * <p></p>
     * Constructed from {@link #getQuery()}.
     */
    public Set<String> getWords() {
        if (words == null) {
            words = new LinkedHashSet<>(splitToWords(query.trim().toLowerCase(), false, Locale.getDefault()));
        }
        // defensive copy, to prevent mutation
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
        return arg + " ~ " + getWords();
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
        final String parameterName = ParametrizedSql.generateParameterName(this);
        final ParametrizedSql sql = arg.toSql();
        if (databaseVariant == DatabaseVariant.MySQLMariaDB) {
            final String booleanQuery = toMySQLFulltextBooleanQuery(getWords());
            if (booleanQuery.isBlank()) {
                return ParametrizedSql.MATCH_ALL;
            }
            return ParametrizedSql.merge("MATCH(" + sql.getSql92() + ") AGAINST (:" + parameterName + " IN BOOLEAN MODE)",
                    sql.getSql92Parameters(), Collections.singletonMap(parameterName, booleanQuery));
        }
        if (databaseVariant == DatabaseVariant.PostgreSQL) {
            final String query = getWords().stream().map(it -> it + ":*").collect(Collectors.joining(" & "));
            // see https://www.postgresql.org/docs/9.5/textsearch-controls.html#TEXTSEARCH-PARSING-QUERIES for more documentation
            return ParametrizedSql.merge("to_tsvector('english', " + sql.getSql92() + ") @@ to_tsquery('english', :" + parameterName + ")",
                    sql.getSql92Parameters(), Collections.singletonMap(parameterName, query));
        }
        if (databaseVariant == DatabaseVariant.MSSQL) {
            final String query = getWords().stream().map(it -> "\"" + it + "*\"").collect(Collectors.joining(", "));
            return ParametrizedSql.merge("CONTAINS(" + sql.getSql92() + ", :" + parameterName + ")",
                    sql.getSql92Parameters(), Collections.singletonMap(parameterName, query));
        }
        if (databaseVariant == DatabaseVariant.H2) {
            final EntityMeta<?> meta = getEntityMeta();
            final String idColumn = meta.getIdProperty().get(0).getDbName().getQualifiedName();
            final String query = getWords().stream()
                    .map(it -> it + "*")
                    .collect(Collectors.joining(" AND "));
            // Need to CAST(FT.KEYS[1] AS BIGINT) otherwise IN won't match anything
            return ParametrizedSql.merge(idColumn + " IN (SELECT CAST(FT.KEYS[1] AS BIGINT) AS ID FROM FTL_SEARCH_DATA(:" + parameterName + ", 0, 0) FT WHERE FT.`TABLE`='" + meta.getDatabaseTableName().toUpperCase() + "')",
                    sql.getSql92Parameters(), Collections.singletonMap(parameterName, query));
        }

        throw new IllegalArgumentException("Unsupported FullText search for variant " + databaseVariant + ". Set proper variant to JdbiOrm.databaseVariant");
    }

    @NotNull
    private EntityMeta<?> getEntityMeta() {
        if (arg instanceof TableProperty) {
            return EntityMeta.of(((TableProperty<?, ?>) arg).getEntityClass());
        }
        throw new IllegalStateException(arg + ": expected TableProperty");
    }
}
