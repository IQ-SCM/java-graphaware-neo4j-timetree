/*
 * Copyright (c) 2013-2020 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.module.timetree.api;

import com.graphaware.common.policy.inclusion.InclusionPolicies;
import com.graphaware.common.policy.inclusion.NodePropertyInclusionPolicy;
import com.graphaware.test.integration.GraphAwareIntegrationTest;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONException;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;

import java.util.concurrent.atomic.AtomicInteger;

import static com.graphaware.test.unit.GraphUnit.assertSameGraph;
import static org.neo4j.graphdb.Label.label;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/**
 * Integration test for {@link TimeTreeApi}.
 */
public class TimeTreeApiWithUUIDTest extends GraphAwareIntegrationTest {

    private final AtomicInteger counter = new AtomicInteger(0);
    private InclusionPolicies ignoreUuid = InclusionPolicies.all().with(
            new NodePropertyInclusionPolicy() {
                @Override
                public boolean include(String s, Node node) {
                    return !s.equals("uuid");
                }
            });

    @Override
    protected void populateDatabase(GraphDatabaseService database) {
        super.populateDatabase(database);

        counter.set(0);
        database.registerTransactionEventHandler(new TransactionEventHandler.Adapter<Void>() {
            @Override
            public Void beforeCommit(TransactionData data) throws Exception {
                for (Node created : data.createdNodes()) {
                    created.setProperty("uuid", "test-uuid-" + counter.incrementAndGet());
                }

                return null;
            }
        });
    }

    @Test
    public void trivialTreeShouldBeCreatedWhenFirstDayIsRequested() throws JSONException {
        //Given
        long dateInMillis = dateToMillis(2013, 5, 4);

        //When
        String result = httpClient.post(getUrl() + "single/" + dateInMillis, HttpStatus.SC_OK);

        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:TimeTreeRoot {uuid:'test-uuid-1'})," +
                "(root)-[:FIRST]->(year:Year {value:2013, uuid: 'test-uuid-2'})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:5, uuid: 'test-uuid-3'})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:FIRST]->(day:Day {value:4, uuid: 'test-uuid-4'})," +
                "(month)-[:CHILD]->(day)," +
                "(month)-[:LAST]->(day)");

        assertEquals("{\"id\":3,\"properties\":{\"value\":4,\"uuid\":\"test-uuid-4\"},\"labels\":[\"Day\"]}", result, true);
    }

    @Test
    public void consecutiveDaysShouldBeCreatedWhenRequested() throws JSONException {

        //Given
        long startDateInMillis = dateToMillis(2013, 5, 4);
        long endDateInMillis = dateToMillis(2013, 5, 7);

        //When
        String result = httpClient.post(getUrl() + "range/" + startDateInMillis + "/" + endDateInMillis, HttpStatus.SC_OK);

        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:TimeTreeRoot)," +
                "(root)-[:FIRST]->(year:Year {value:2013})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:5})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:CHILD]->(day4:Day {value:4})," +
                "(month)-[:CHILD]->(day5:Day {value:5})," +
                "(month)-[:CHILD]->(day6:Day {value:6})," +
                "(month)-[:CHILD]->(day7:Day {value:7})," +
                "(month)-[:FIRST]->(day4)," +
                "(month)-[:LAST]->(day7)," +
                "(day4)-[:NEXT]->(day5)," +
                "(day5)-[:NEXT]->(day6)," +
                "(day6)-[:NEXT]->(day7)", ignoreUuid);

        assertEquals("[{\"id\":3,\"properties\":{\"value\":4,\"uuid\":\"test-uuid-4\"},\"labels\":[\"Day\"]},{\"id\":4,\"properties\":{\"value\":5,\"uuid\":\"test-uuid-5\"},\"labels\":[\"Day\"]},{\"id\":5,\"properties\":{\"value\":6,\"uuid\":\"test-uuid-6\"},\"labels\":[\"Day\"]},{\"id\":6,\"properties\":{\"value\":7,\"uuid\":\"test-uuid-7\"},\"labels\":[\"Day\"]}]", result, true);
    }

    @Test
    public void trivialTreeShouldBeCreatedWhenFirstDayIsRequestedWithCustomRoot() throws JSONException {
        //Given
        long dateInMillis = dateToMillis(2013, 5, 4);

        //When
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().createNode(label("CustomRoot"));
            tx.success();
        }

        String result = httpClient.post(getUrl() + "0/single/" + dateInMillis, HttpStatus.SC_OK);

        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:CustomRoot)," +
                "(root)-[:FIRST]->(year:Year {value:2013})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:5})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:FIRST]->(day:Day {value:4})," +
                "(month)-[:CHILD]->(day)," +
                "(month)-[:LAST]->(day)", ignoreUuid);

        assertEquals("{\"id\":3,\"properties\":{\"value\":4,\"uuid\":\"test-uuid-4\"},\"labels\":[\"Day\"]}", result, true);
    }

    @Test
    public void consecutiveDaysShouldBeCreatedWhenRequestedWithCustomRoot() throws JSONException {

        //Given
        long startDateInMillis = dateToMillis(2013, 5, 4);
        long endDateInMillis = dateToMillis(2013, 5, 7);

        //When
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().createNode(label("CustomRoot"));
            tx.success();
        }

        String result = httpClient.post(getUrl() + "0/range/" + startDateInMillis + "/" + endDateInMillis, HttpStatus.SC_OK);

        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:CustomRoot)," +
                "(root)-[:FIRST]->(year:Year {value:2013})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:5})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:CHILD]->(day4:Day {value:4})," +
                "(month)-[:CHILD]->(day5:Day {value:5})," +
                "(month)-[:CHILD]->(day6:Day {value:6})," +
                "(month)-[:CHILD]->(day7:Day {value:7})," +
                "(month)-[:FIRST]->(day4)," +
                "(month)-[:LAST]->(day7)," +
                "(day4)-[:NEXT]->(day5)," +
                "(day5)-[:NEXT]->(day6)," +
                "(day6)-[:NEXT]->(day7)", ignoreUuid);

        assertEquals("[{\"id\":3,\"properties\":{\"value\":4,\"uuid\":\"test-uuid-4\"},\"labels\":[\"Day\"]},{\"id\":4,\"properties\":{\"value\":5,\"uuid\":\"test-uuid-5\"},\"labels\":[\"Day\"]},{\"id\":5,\"properties\":{\"value\":6,\"uuid\":\"test-uuid-6\"},\"labels\":[\"Day\"]},{\"id\":6,\"properties\":{\"value\":7,\"uuid\":\"test-uuid-7\"},\"labels\":[\"Day\"]}]", result, true);
    }

    @Test
    public void trivialTreeShouldBeCreatedWhenTodayIsRequested() throws JSONException {
        //Given
        DateTime now = DateTime.now(DateTimeZone.UTC);

        //When
        String result = httpClient.post(getUrl() + "now", HttpStatus.SC_OK);

        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:TimeTreeRoot)," +
                "(root)-[:FIRST]->(year:Year {value:" + now.getYear() + "})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:" + now.getMonthOfYear() + "})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:FIRST]->(day:Day {value:" + now.getDayOfMonth() + "})," +
                "(month)-[:CHILD]->(day)," +
                "(month)-[:LAST]->(day)", ignoreUuid);

        assertEquals("{\"id\":3,\"properties\":{\"value\":" + now.getDayOfMonth() + ",\"uuid\":\"test-uuid-4\"},\"labels\":[\"Day\"]}", result, true);
    }

    @Test
    public void trivialTreeShouldBeCreatedWhenTodayIsRequestedWithCustomRoot() throws JSONException {
        //Given
        DateTime now = DateTime.now(DateTimeZone.UTC);

        //When
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().createNode(label("CustomRoot"));
            tx.success();
        }

        String result = httpClient.post(getUrl() + "/0/now", HttpStatus.SC_OK);

        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:CustomRoot)," +
                "(root)-[:FIRST]->(year:Year {value:" + now.getYear() + "})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:" + now.getMonthOfYear() + "})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:FIRST]->(day:Day {value:" + now.getDayOfMonth() + "})," +
                "(month)-[:CHILD]->(day)," +
                "(month)-[:LAST]->(day)", ignoreUuid);

        assertEquals("{\"id\":3,\"properties\":{\"value\":" + now.getDayOfMonth() + ",\"uuid\":\"test-uuid-4\"},\"labels\":[\"Day\"]}", result, true);
    }

    private long dateToMillis(int year, int month, int day) {
        return dateToDateTime(year, month, day).getMillis();
    }

    private DateTime dateToDateTime(int year, int month, int day) {
        return new DateTime(year, month, day, 0, 0, DateTimeZone.UTC);
    }

    private String getUrl() {
        return baseUrl() + "/timetree/";
    }
}
