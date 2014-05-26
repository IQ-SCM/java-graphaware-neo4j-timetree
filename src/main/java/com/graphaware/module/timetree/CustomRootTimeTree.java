/*
 * Copyright (c) 2014 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.module.timetree;

import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Node;

public class CustomRootTimeTree extends SingleTimeTree {

    private final Node customRoot;

    public CustomRootTimeTree(Node customRoot)
    {
        super(customRoot.getGraphDatabase());
        this.customRoot = customRoot;
    }

    public CustomRootTimeTree( Node customRoot, Resolution resolution)
    {
        super(customRoot.getGraphDatabase(), resolution);
        this.customRoot = customRoot;
    }

    public CustomRootTimeTree( Node customRoot, DateTimeZone timeZone)
    {
        super(customRoot.getGraphDatabase(), timeZone);
        this.customRoot = customRoot;
    }

    public CustomRootTimeTree(Node customRoot, DateTimeZone timeZone, Resolution resolution)
    {
        super(customRoot.getGraphDatabase(), timeZone, resolution);
        this.customRoot = customRoot;
    }

    @Override
    protected Node getTimeRoot()
    {
        return customRoot;
    }
}
