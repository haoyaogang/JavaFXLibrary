/*
 * Copyright 2017-2018   Eficode Oy
 * Copyright 2018-       Robot Framework Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javafxlibrary.utils.finder;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Window;
import javafxlibrary.utils.RobotLog;
import javafxlibrary.utils.TestFxAdapter;
import org.testfx.api.FxRobotInterface;

import java.util.*;

public class Finder {

    private String[] queries;
    private Set<Node> results = new LinkedHashSet<>();
    private FxRobotInterface robot;

    public Finder() {
        this.robot = TestFxAdapter.getRobot();
    }

    public Node find(String query) {
        // TODO: Remove old style lookup queries
        // Use TestFX lookup for queries with no prefixes
        if (!QueryParser.startsWithPrefix(query)) {
            RobotLog.warn("You are using deprecated lookup queries! See library documentation for information about " +
                    "the updated lookup query syntax.");
            return robot.lookup(query).query();
        }

        List<Window> windows = robot.listTargetWindows();
        RobotLog.debug("Finding with query \"" + query + "\" from " + windows.size() + " windows");

        for (Window window : windows) {
            RobotLog.debug("Finding from window " + window);
            Node result = find(query, window.getScene().getRoot());
            if (result != null)
                return result;

        }
        RobotLog.debug("Find finished, nothing was found with query: " + query);
        return null;
    }

    public Node find(String query, Parent root) {
        // TODO: Remove old style lookup queries
        // Use TestFX lookup for queries with no prefixes
        if (!QueryParser.startsWithPrefix(query)) {
            RobotLog.warn("You are using deprecated lookup queries! See library documentation for information about " +
                    "the updated lookup query syntax.");
            return robot.from(root).lookup(query).query();
        }

        this.queries = QueryParser.getIndividualQueries(query);
        return find(root, 0);
    }

    private Node find(Parent root, int queryIndex) {
        String query = queries[queryIndex];

        if (queryIndex < queries.length - 1) {
            // lookupResults might be unmodifiable, copy contents to a new Set
            Set<Node> lookupResults = executeFindAll(root, query);
            Set<Node> nodes = new LinkedHashSet<>();
            nodes.addAll(lookupResults);
            nodes.remove(root);

            for (Node node : nodes) {
                if (node instanceof Parent) {
                    Node result = find((Parent) node, queryIndex + 1);
                    if (result != null) {
                        return result;
                    }
                }
            }
            return null;
        } else {
            return executeFind(root, query);
        }
    }

    public Set<Node> findAll(String query) {
        // TODO: Remove old style lookup queries
        // Use TestFX lookup for queries with no prefixes
        if (!QueryParser.startsWithPrefix(query)) {
            RobotLog.warn("You are using deprecated lookup queries! See library documentation for information about " +
                    "the updated lookup query syntax.");
            return robot.lookup(query).queryAll();
        }

        List<Window> windows = robot.listTargetWindows();
        RobotLog.debug("Finding All with query \"" + query + "\" from " + windows.size() + " windows");

        for (Window window : windows) {
            RobotLog.debug("Finding all from window " + window);
            findAll(query, window.getScene().getRoot());
        }
        return results;
    }

    public Set<Node> findAll(String query, Parent root) {
        // TODO: Remove old style lookup queries
        // Use TestFX lookup for queries with no prefixes
        if (!QueryParser.startsWithPrefix(query)) {
            RobotLog.warn("You are using deprecated lookup queries! See library documentation for information about " +
                    "the updated lookup query syntax.");
            return robot.from(root).lookup(query).query();
        }

        this.queries = QueryParser.getIndividualQueries(query);
        return findAll(root, 0);
    }

    private Set<Node> findAll(Parent root, int queryIndex) {
        String query = queries[queryIndex];
        Set<Node> lookupResults = executeFindAll(root, query);
        Set<Node> nodes = new LinkedHashSet<>();
        nodes.addAll(lookupResults);
        nodes.remove(root);

        if (queryIndex < queries.length - 1) {
            for (Node node : nodes)
                if (node instanceof Parent)
                    findAll((Parent) node, queryIndex + 1);
        } else {
            results.addAll(nodes);
        }

        return results;
    }

    private Node executeFind(Parent root, String query) {
        RobotLog.debug("Executing find with root: " + root + " and query: " + query);
        FindOperation findOperation = new FindOperation(root, new Query(query), false);
        return (Node) findOperation.executeLookup();
    }

    // TODO: Add support for using indexes in queries (css=VBox[3]), xPath already implements this
    private Set<Node> executeFindAll(Parent root, String query) {
        RobotLog.debug("Executing find all with root: " + root + " and query: " + query);
        FindOperation findOperation = new FindOperation(root, new Query(query), true);
        return (Set<Node>) findOperation.executeLookup();
    }
}