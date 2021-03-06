package com.planet.staccato.es.api;

import com.planet.staccato.FieldName;
import com.planet.staccato.es.exception.FilterException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Service;
import org.xbib.cql.CQLParser;
import org.xbib.cql.SortedQuery;
import org.xbib.cql.elasticsearch.ElasticsearchQueryGenerator;

import java.time.Instant;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Optional;


/**
 * This service builds Elasticsearch queries from the api parameters passed to the STAC API.
 *
 * @author joshfix
 * Created on 12/6/17
 */
@Slf4j
@Service
public class DefaultQueryBuilderService implements QueryBuilderService {

    private static final int WEST = 0;
    private static final int SOUTH = 1;
    private static final int EAST = 2;
    private static final int NORTH = 3;
    private static final int DEFAULT_LIMIT = 10;

    /**
     * Builds an Elasticsearch bbox query
     *
     * @param bbox The bbox values passed in the api request
     * @return The Elasticsearch query builder
     */
    @Override
    public Optional<QueryBuilder> bboxBuilder(double[] bbox) {
        if (null == bbox || bbox.length != 4) {
            return Optional.empty();
        }
        Coordinate c1 = new Coordinate(bbox[WEST], bbox[NORTH]);
        Coordinate c2 = new Coordinate(bbox[EAST], bbox[SOUTH]);
        EnvelopeBuilder envelopeBuilder = new EnvelopeBuilder(c1, c2);

        return Optional.of(
                new GeoShapeQueryBuilder(FieldName.GEOMETRY, envelopeBuilder).relation(ShapeRelation.INTERSECTS));
    }

    /**
     * Builds an Elasticsearch temporal query
     *
     * @param time The time values passed in the api request
     * @return The Elasticsearch query builder
     */
    @Override
    public Optional<QueryBuilder> timeBuilder(String time) {
        if (null == time || time.isBlank()) {
            return Optional.empty();
        }
        String startTimeProperty;
        String endTimeProperty;

        if (time.indexOf("/") > 0) {
            String[] timeArray = time.split("/");
            startTimeProperty = timeArray[0];
            endTimeProperty = timeArray[1];

            try {
                // see if the value can be parsed into a period, eg P1Y2M3W4D
                Period period = Period.parse(endTimeProperty);

                Instant start = Instant.parse(startTimeProperty);
                Instant end = start.plus(period);

                start.plus(period.normalized());

                startTimeProperty = start.toString();
                endTimeProperty = end.toString();

            } catch (DateTimeParseException e) {
                // not a period
            }
        } else {
            startTimeProperty = time;
            endTimeProperty = time;
        }

        RangeQueryBuilder rangeQueryBuilder = QueryBuilders
                .rangeQuery(FieldName.DATETIME_FULL)
                .from(startTimeProperty)
                .to(endTimeProperty);

        return Optional.of(rangeQueryBuilder);
    }

    /**
     * Builds an Elasticsearch query
     *
     * @param query The query values passed in the api request
     * @return The Elasticsearch query builder
     */
    @Override
    public Optional<QueryBuilder> queryBuilder(String query) {
        if (query == null || query.isEmpty()) {
            return Optional.empty();
        }
        try {
            CQLParser parser = new CQLParser(query);
            parser.parse();
            ElasticsearchQueryGenerator generator = new ElasticsearchQueryGenerator();
            SortedQuery sq  = parser.getCQLQuery();
            sq.getQuery().getScopedClause().accept(new PropertiesVisitor());
            sq.accept(generator);
            QueryBuilder builder = QueryBuilders.wrapperQuery(generator.getQueryResult());
            return Optional.of(builder);
        } catch (Exception e) {
            throw new FilterException("Error parsing query.");
        }
    }

    @Override
    public Optional<QueryBuilder> idsBuilder(String[] ids) {
        if (null == ids || ids.length == 0) {
            return Optional.empty();
        }
        return Optional.of(QueryBuilders.termsQuery("id", ids));
    }

    /**
     * Sets a default item limit if none was set or if the set limit is invalid.
     *
     * @param limit The maximum number of items passed in the request
     * @return The final maximum number of items that will be returned in the api response
     */
    @Override
    public int getLimit(Integer limit) {
        return (null == limit || limit <= 0) ? DEFAULT_LIMIT : limit;
    }

}
