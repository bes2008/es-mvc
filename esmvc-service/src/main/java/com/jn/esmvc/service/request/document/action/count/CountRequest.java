package com.jn.esmvc.service.request.document.action.count;

import com.jn.langx.util.Objs;
import com.jn.langx.util.hash.HashCodeBuilder;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.common.Strings;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.Arrays;

import static org.elasticsearch.action.search.SearchRequest.DEFAULT_INDICES_OPTIONS;

public class CountRequest extends ActionRequest {
    private String[] indices = Strings.EMPTY_ARRAY;
    private String[] types = Strings.EMPTY_ARRAY;
    private String routing;
    private String preference;
    private SearchSourceBuilder searchSourceBuilder;
    private IndicesOptions indicesOptions = DEFAULT_INDICES_OPTIONS;

    private String countColumn = "_id";

    public CountRequest() {
        this.searchSourceBuilder = new SearchSourceBuilder();
    }

    /**
     * Constructs a new count request against the indices. No indices provided here means that count will execute on all indices.
     */
    public CountRequest(String... indices) {
        this(indices, new SearchSourceBuilder());
    }

    /**
     * Constructs a new search request against the provided indices with the given search source.
     */
    public CountRequest(String[] indices, SearchSourceBuilder searchSourceBuilder) {
        indices(indices);
        this.searchSourceBuilder = searchSourceBuilder;
    }

    public ActionRequestValidationException validate() {
        return null;
    }

    /**
     * Sets the indices the count will be executed on.
     */
    public CountRequest indices(String... indices) {
        Objs.requireNonNull(indices, "indices must not be null");
        for (String index : indices) {
            Objs.requireNonNull(index, "index must not be null");
        }
        this.indices = indices;
        return this;
    }

    /**
     * The source of the count request.
     */
    public CountRequest source(SearchSourceBuilder searchSourceBuilder) {
        this.searchSourceBuilder = Objs.requireNonNull(searchSourceBuilder, "source must not be null");
        return this;
    }

    /**
     * The document types to execute the count against. Defaults to be executed against all types.
     *
     * @deprecated Types are going away, prefer filtering on a type.
     */
    @Deprecated
    public CountRequest types(String... types) {
        Objs.requireNonNull(types, "types must not be null");
        for (String type : types) {
            Objs.requireNonNull(type, "type must not be null");
        }
        this.types = types;
        return this;
    }

    /**
     * The routing values to control the shards that the search will be executed on.
     */
    public CountRequest routing(String routing) {
        this.routing = routing;
        return this;
    }

    /**
     * A comma separated list of routing values to control the shards the count will be executed on.
     */
    public CountRequest routing(String... routings) {
        this.routing = Strings.arrayToCommaDelimitedString(routings);
        return this;
    }

    /**
     * Returns the indices options used to resolve indices. They tell for instance whether a single index is accepted, whether an empty
     * array will be converted to _all, and how wildcards will be expanded if needed.
     *
     * @see org.elasticsearch.action.support.IndicesOptions
     */
    public CountRequest indicesOptions(IndicesOptions indicesOptions) {
        this.indicesOptions = Objs.requireNonNull(indicesOptions, "indicesOptions must not be null");
        return this;
    }

    /**
     * Sets the preference to execute the count. Defaults to randomize across shards. Can be set to {@code _local} to prefer local shards
     * or a custom value, which guarantees that the same order will be used across different requests.
     */
    public CountRequest preference(String preference) {
        this.preference = preference;
        return this;
    }

    public IndicesOptions indicesOptions() {
        return this.indicesOptions;
    }

    public String routing() {
        return this.routing;
    }

    public String preference() {
        return this.preference;
    }

    public String[] indices() {
        return Arrays.copyOf(this.indices, this.indices.length);
    }

    public Float minScore() {
        return this.searchSourceBuilder.minScore();
    }

    public CountRequest minScore(Float minScore) {
        this.searchSourceBuilder.minScore(minScore);
        return this;
    }

    public int terminateAfter() {
        return this.searchSourceBuilder.terminateAfter();
    }

    public CountRequest terminateAfter(int terminateAfter) {
        this.searchSourceBuilder.terminateAfter(terminateAfter);
        return this;
    }

    public String[] types() {
        return Arrays.copyOf(this.types, this.types.length);
    }

    public SearchSourceBuilder source() {
        return this.searchSourceBuilder;
    }

    public String countColumn() {
        return countColumn;
    }

    public CountRequest countColumn(String countColumn) {
        this.countColumn = countColumn;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CountRequest that = (CountRequest) o;
        return Objs.equals(indicesOptions, that.indicesOptions) &&
                Arrays.equals(indices, that.indices) &&
                Arrays.equals(types, that.types) &&
                Objs.equals(routing, that.routing) &&
                Objs.equals(preference, that.preference) &&
                Objs.equals(countColumn, that.countColumn)
                ;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .with(indicesOptions)
                .with(routing)
                .with(preference)
                .with(indices)
                .with(types)
                .with(countColumn)
                .build();
    }
}
