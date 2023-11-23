package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScannerBaseTest {
    @Test
    public void testGetParents() {
        final String MULTI_DESC_PATH = "/path/to/project/maven-example/pom.xml";
        final String MULTI1_DESC_PATH = "/path/to/project/maven-example/multi1/pom.xml";
        final String MULTI3_DESC_PATH = "/path/to/project/maven-example/multi3/pom.xml";

        final String MULTI_COMP_ID = "org.jfrog.test:multi:3.7.x-SNAPSHOT";
        final String MULTI1_COMP_ID = "org.jfrog.test:multi1:3.7.x-SNAPSHOT";
        final String MULTI3_COMP_ID = "org.jfrog.test:multi3:3.7.x-SNAPSHOT";
        final String SPRING_AOP_COMP_ID = "org.springframework:spring-aop:2.5.6";
        final String LOG4J_COMP_ID = "log4j:log4j:1.2.17";

        DepTree depTree = new DepTree(MULTI_COMP_ID, new HashMap<>() {{
            put(MULTI_COMP_ID, new DepTreeNode().descriptorFilePath(MULTI_DESC_PATH).children(Set.of(MULTI1_COMP_ID, MULTI3_COMP_ID, LOG4J_COMP_ID)));
            put(MULTI1_COMP_ID, new DepTreeNode().descriptorFilePath(MULTI1_DESC_PATH).children(Set.of(LOG4J_COMP_ID)));
            put(MULTI3_COMP_ID, new DepTreeNode().descriptorFilePath(MULTI3_DESC_PATH).children(Set.of(MULTI1_COMP_ID, LOG4J_COMP_ID, SPRING_AOP_COMP_ID)));
            put(SPRING_AOP_COMP_ID, new DepTreeNode().children(Set.of()));
            put(LOG4J_COMP_ID, new DepTreeNode().children(Set.of()));
        }});

        Map<String, Set<String>> expectedParents = new HashMap<>() {{
            put(MULTI1_COMP_ID, new HashSet<>() {{
                add(MULTI_COMP_ID);
                add(MULTI3_COMP_ID);
            }});
            put(MULTI3_COMP_ID, new HashSet<>() {{
                add(MULTI_COMP_ID);
            }});
            put(SPRING_AOP_COMP_ID, new HashSet<>() {{
                add(MULTI3_COMP_ID);
            }});
            put(LOG4J_COMP_ID, new HashSet<>() {{
                add(MULTI_COMP_ID);
                add(MULTI1_COMP_ID);
                add(MULTI3_COMP_ID);
            }});
        }};
        Map<String, Set<String>> actualParents = ScannerBase.getParents(depTree);
        Assert.assertEquals(expectedParents, actualParents);
    }
}
