/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.workflow.lite;


import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;


import org.apache.oozie.service.ActionService;
import org.apache.oozie.service.LiteWorkflowStoreService;
import org.apache.oozie.service.SchemaService;
import org.apache.oozie.service.Services;
import org.apache.oozie.service.SchemaService.SchemaName;
import org.apache.oozie.service.TestSchemaService;
import org.apache.oozie.workflow.WorkflowException;
import org.apache.oozie.workflow.lite.TestLiteWorkflowLib.TestActionNodeHandler;
import org.apache.oozie.workflow.lite.TestLiteWorkflowLib.TestDecisionNodeHandler;
import org.apache.oozie.test.XTestCase;
import org.apache.oozie.util.IOUtils;
import org.apache.oozie.ErrorCode;
import org.apache.oozie.action.hadoop.DistcpActionExecutor;
import org.apache.oozie.action.hadoop.HiveActionExecutor;
import org.apache.hadoop.conf.Configuration;

public class TestLiteWorkflowAppParser extends XTestCase {
    public static String dummyConf = "<java></java>";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        new Services().init();
        Services.get().get(ActionService.class).register(HiveActionExecutor.class);
        Services.get().get(ActionService.class).register(DistcpActionExecutor.class);
    }

    @Override
    protected void tearDown() throws Exception {
        Services.get().destroy();
        super.tearDown();
    }

    public void testParserGlobal() throws Exception {
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
            LiteWorkflowStoreService.LiteControlNodeHandler.class,
            LiteWorkflowStoreService.LiteDecisionHandler.class,
            LiteWorkflowStoreService.LiteActionHandler.class);

        LiteWorkflowApp app = parser.validateAndParse(IOUtils.getResourceAsReader("wf-schema-valid-global.xml", -1),
                new Configuration());

        String d = app.getNode("d").getConf();
        String expectedD =
             "<map-reduce xmlns=\"uri:oozie:workflow:0.4\">\r\n" +
             "  <prepare>\r\n" +
             "    <delete path=\"/tmp\" />\r\n" +
             "    <mkdir path=\"/tmp\" />\r\n" +
             "  </prepare>\r\n" +
             "  <streaming>\r\n" +
             "    <mapper>/mycat.sh</mapper>\r\n" +
             "    <reducer>/mywc.sh</reducer>\r\n" +
             "  </streaming>\r\n" +
             "  <job-xml>/tmp</job-xml>\r\n" +
             "  <file>/tmp</file>\r\n" +
             "  <archive>/tmp</archive>\r\n" +
             "  <job-tracker>foo</job-tracker>\r\n" +
             "  <name-node>bar</name-node>\r\n" +
             "  <configuration>\r\n" +
             "    <property>\r\n" +
             "      <name>a</name>\r\n" +
             "      <value>A</value>\r\n" +
             "    </property>\r\n" +
             "    <property>\r\n" +
             "      <name>b</name>\r\n" +
             "      <value>B</value>\r\n" +
             "    </property>\r\n" +
             "  </configuration>\r\n" +
             "</map-reduce>";
        assertEquals(expectedD.replaceAll(" ",""), d.replaceAll(" ", ""));

    }

    public void testParserGlobalJobXML() throws Exception {
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
            LiteWorkflowStoreService.LiteControlNodeHandler.class,
            LiteWorkflowStoreService.LiteDecisionHandler.class,
            LiteWorkflowStoreService.LiteActionHandler.class);

        LiteWorkflowApp app = parser.validateAndParse(IOUtils.getResourceAsReader("wf-schema-valid-global-jobXml.xml", -1),
                new Configuration());

        String d = app.getNode("d").getConf();
        String expectedD =
             "<map-reduce xmlns=\"uri:oozie:workflow:0.4\">\r\n" +
             "  <prepare>\r\n" +
             "    <delete path=\"/tmp\" />\r\n" +
             "    <mkdir path=\"/tmp\" />\r\n" +
             "  </prepare>\r\n" +
             "  <streaming>\r\n" +
             "    <mapper>/mycat.sh</mapper>\r\n" +
             "    <reducer>/mywc.sh</reducer>\r\n" +
             "  </streaming>\r\n" +
             "  <job-xml>/tmp</job-xml>\r\n" +
             "  <file>/tmp</file>\r\n" +
             "  <archive>/tmp</archive>\r\n" +
             "  <job-tracker>foo</job-tracker>\r\n" +
             "  <name-node>bar</name-node>\r\n" +
             "  <job-xml>/spam1</job-xml>\r\n" +
             "  <job-xml>/spam2</job-xml>\r\n" +
             "  <configuration>\r\n" +
             "    <property>\r\n" +
             "      <name>a</name>\r\n" +
             "      <value>A</value>\r\n" +
             "    </property>\r\n" +
             "    <property>\r\n" +
             "      <name>b</name>\r\n" +
             "      <value>B</value>\r\n" +
             "    </property>\r\n" +
             "  </configuration>\r\n" +
             "</map-reduce>";
        assertEquals(expectedD.replaceAll(" ",""), d.replaceAll(" ", ""));

    }

    public void testParserGlobalLocalAlreadyExists() throws Exception{
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);

        LiteWorkflowApp app = parser.validateAndParse(IOUtils.getResourceAsReader("wf-schema-valid-global.xml", -1),
                new Configuration());

        String e = app.getNode("e").getConf();
        String expectedE =
                "<pig xmlns=\"uri:oozie:workflow:0.4\">\r\n" +
                "  <prepare>\r\n" +
                "    <delete path=\"/tmp\" />\r\n" +
                "    <mkdir path=\"/tmp\" />\r\n" +
                "  </prepare>\r\n" +
                "  <configuration>\r\n" +
                "    <property>\r\n" +
                "      <name>a</name>\r\n" +
                "      <value>A2</value>\r\n" +
                "    </property>\r\n" +
                "    <property>\r\n" +
                "      <name>b</name>\r\n" +
                "      <value>B</value>\r\n" +
                "    </property>\r\n" +
                "  </configuration>\r\n" +
                "  <script>/tmp</script>\r\n" +
                "  <param>x</param>\r\n" +
                "  <file>/tmp</file>\r\n" +
                "  <file>/tmp</file>\r\n" +
                "  <job-tracker>foo</job-tracker>\r\n" +
                "  <name-node>bar</name-node>\r\n" +
                "</pig>";
        assertEquals(expectedE.replaceAll(" ", ""), e.replaceAll(" ", ""));

    }

    public void testParserGlobalExtensionActions() throws Exception {
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
            LiteWorkflowStoreService.LiteControlNodeHandler.class,
            LiteWorkflowStoreService.LiteDecisionHandler.class,
            LiteWorkflowStoreService.LiteActionHandler.class);

        LiteWorkflowApp app = parser.validateAndParse(IOUtils.getResourceAsReader("wf-schema-valid-global-ext.xml", -1),
                new Configuration());

        String a = app.getNode("a").getConf();
        String expectedA =
             "<hive xmlns=\"uri:oozie:hive-action:0.2\">\r\n" +
             "  <prepare>\r\n" +
             "    <delete path=\"/tmp\" />\r\n" +
             "    <mkdir path=\"/tmp\" />\r\n" +
             "  </prepare>\r\n" +
             "  <configuration>\r\n" +
             "    <property>\r\n" +
             "      <name>c</name>\r\n" +
             "      <value>C</value>\r\n" +
             "    </property>\r\n" +
             "    <property>\r\n" +
             "      <name>a</name>\r\n" +
             "      <value>A</value>\r\n" +
             "    </property>\r\n" +
             "    <property>\r\n" +
             "      <name>b</name>\r\n" +
             "      <value>B</value>\r\n" +
             "    </property>\r\n" +
             "  </configuration>\r\n" +
             "  <script>script.q</script>\r\n" +
             "  <param>INPUT=/tmp/table</param>\r\n" +
             "  <param>OUTPUT=/tmp/hive</param>\r\n" +
             "  <job-tracker>foo</job-tracker>\r\n" +
             "  <name-node>bar</name-node>\r\n" +
             "</hive>";
        System.out.println("AAA " + expectedA.replaceAll(" ", ""));
        assertEquals(expectedA.replaceAll(" ",""), a.replaceAll(" ", ""));
    }

    public void testParserGlobalExtensionActionsLocalAlreadyExists() throws Exception {
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
            LiteWorkflowStoreService.LiteControlNodeHandler.class,
            LiteWorkflowStoreService.LiteDecisionHandler.class,
            LiteWorkflowStoreService.LiteActionHandler.class);

        LiteWorkflowApp app = parser.validateAndParse(IOUtils.getResourceAsReader("wf-schema-valid-global-ext.xml", -1),
                new Configuration());

        String b = app.getNode("b").getConf();
        String expectedB =
             "<distcp xmlns=\"uri:oozie:distcp-action:0.1\">\r\n" +
             "  <job-tracker>blah</job-tracker>\r\n" +
             "  <name-node>meh</name-node>\r\n" +
             "  <prepare>\r\n" +
             "    <delete path=\"/tmp2\" />\r\n" +
             "    <mkdir path=\"/tmp2\" />\r\n" +
             "  </prepare>\r\n" +
             "  <configuration>\r\n" +
             "    <property>\r\n" +
             "      <name>a</name>\r\n" +
             "      <value>A2</value>\r\n" +
             "    </property>\r\n" +
             "    <property>\r\n" +
             "      <name>b</name>\r\n" +
             "      <value>B</value>\r\n" +
             "    </property>\r\n" +
             "  </configuration>\r\n" +
             "  <arg>/tmp/data.txt</arg>\r\n" +
             "  <arg>/tmp2/data.txt</arg>\r\n" +
             "</distcp>";
        assertEquals(expectedB.replaceAll(" ",""), b.replaceAll(" ", ""));
    }

    public void testParserGlobalExtensionActionsNoGlobal() throws Exception {
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
            LiteWorkflowStoreService.LiteControlNodeHandler.class,
            LiteWorkflowStoreService.LiteDecisionHandler.class,
            LiteWorkflowStoreService.LiteActionHandler.class);

        // If no global section is defined, some extension actions (e.g. hive) must still have name-node and job-tracker elements
        // or the handleGlobal() method will throw an exception

        parser.validateAndParse(IOUtils.getResourceAsReader("wf-schema-valid-global-ext-no-global.xml", -1), new Configuration());

        try {
            parser.validateAndParse(IOUtils.getResourceAsReader("wf-schema-invalid-global-ext-no-global.xml", -1),
                    new Configuration());
            fail();
        }
        catch (WorkflowException ex) {
            assertEquals(ErrorCode.E0701, ex.getErrorCode());
        }
        catch (Exception ex) {
            fail();
        }
    }

    public void testParser() throws Exception {
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                                                                 LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                                                 LiteWorkflowStoreService.LiteDecisionHandler.class,
                                                                 LiteWorkflowStoreService.LiteActionHandler.class);

        parser.validateAndParse(IOUtils.getResourceAsReader("wf-schema-valid.xml", -1), new Configuration());

        try {
            parser.validateAndParse(IOUtils.getResourceAsReader("wf-loop1-invalid.xml", -1), new Configuration());
            fail();
        }
        catch (WorkflowException ex) {
            assertEquals(ErrorCode.E0707, ex.getErrorCode());
        }
        catch (Exception ex) {
            fail();
        }

        try {
            parser.validateAndParse(IOUtils.getResourceAsReader("wf-unsupported-action.xml", -1), new Configuration());
            fail();
        }
        catch (WorkflowException ex) {
            assertEquals(ErrorCode.E0723, ex.getErrorCode());
        }
        catch (Exception ex) {
            fail();
        }

        try {
            parser.validateAndParse(IOUtils.getResourceAsReader("wf-loop2-invalid.xml", -1), new Configuration());
            fail();
        }
        catch (WorkflowException ex) {
            assertEquals(ErrorCode.E0706, ex.getErrorCode());
        }
        catch (Exception ex) {
            fail();
        }

        try {
            parser.validateAndParse(IOUtils.getResourceAsReader("wf-transition-invalid.xml", -1), new Configuration());
            fail();
        }
        catch (WorkflowException ex) {
            assertEquals(ErrorCode.E0708, ex.getErrorCode());
        }
        catch (Exception ex) {
            fail();
        }
    }

    // Test for validation of workflow definition against pattern defined in schema to complete within 3 seconds
    public void testWfValidationFailure() throws Exception {
        SchemaService wss = Services.get().get(SchemaService.class);
        final LiteWorkflowAppParser parser = new LiteWorkflowAppParser(wss.getSchema(SchemaName.WORKFLOW),
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class, LiteWorkflowStoreService.LiteActionHandler.class);

        Thread testThread = new Thread() {
            public void run() {
                try {
                    // Validate against wf def
                    parser.validateAndParse(new StringReader(TestSchemaService.APP_NEG_TEST), new Configuration());
                    fail("Expected to catch WorkflowException but didn't encounter any");
                } catch (WorkflowException we) {
                    assertEquals(ErrorCode.E0701, we.getErrorCode());
                    assertTrue(we.getCause().toString().contains("SAXParseException"));
                } catch (Exception e) {
                    fail("Expected to catch WorkflowException but an unexpected error happened");
                }

            }
        };
        testThread.start();
        Thread.sleep(3000);
        // Timeout if validation takes more than 3 seconds
        testThread.interrupt();

        if (testThread.isInterrupted()) {
            throw new TimeoutException("the pattern validation took too long to complete");
        }
    }

    /*
     * 1->ok->2
     * 2->ok->end
     */
   public void testWfNoForkJoin() throws WorkflowException  {
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);

        LiteWorkflowApp def = new LiteWorkflowApp("name", "def",
            new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
            .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "two", "three"))
            .addNode(new ActionNodeDef("two", dummyConf, TestActionNodeHandler.class, "end", "end"))
            .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "end", "end"))
            .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

        try {
            invokeForkJoin(parser, def);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected Exception");
        }
    }

    /*
    f->(2,3)
    (2,3)->j
    */
    public void testSimpleForkJoin() throws WorkflowException {
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);

        LiteWorkflowApp def = new LiteWorkflowApp("wf", "<worklfow-app/>",
        new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
        .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f", "end"))
        .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                 Arrays.asList(new String[]{"two", "three"})))
        .addNode(new ActionNodeDef("two", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "four"))
        .addNode(new ActionNodeDef("four", dummyConf, TestActionNodeHandler.class, "end", "end"))
        .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
        .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

        try {
            invokeForkJoin(parser, def);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected Exception");
        }
    }

    /*
     f->(2,3)
     2->f2
     3->j
     f2->(4,5,6)
     (4,5,6)->j2
     j2->7
     7->j
    */
    public void testNestedForkJoin() throws WorkflowException{
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);

        LiteWorkflowApp def = new LiteWorkflowApp("testWf", "<worklfow-app/>",
        new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
        .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f","end"))
        .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                 Arrays.asList(new String[]{"two", "three"})))
        .addNode(new ActionNodeDef("two", dummyConf, TestActionNodeHandler.class, "f2", "k"))
        .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new ForkNodeDef("f2", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                 Arrays.asList(new String[]{"four", "five", "six"})))
        .addNode(new ActionNodeDef("four", dummyConf, TestActionNodeHandler.class, "j2", "k"))
        .addNode(new ActionNodeDef("five", dummyConf, TestActionNodeHandler.class, "j2", "k"))
        .addNode(new ActionNodeDef("six", dummyConf, TestActionNodeHandler.class, "j2", "k"))
        .addNode(new JoinNodeDef("j2", LiteWorkflowStoreService.LiteControlNodeHandler.class, "seven"))
        .addNode(new ActionNodeDef("seven", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "end"))
        .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
        .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

        try {
            invokeForkJoin(parser, def);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected Exception");
        }
    }

    /*
      f->(2,3)
      2->j
      3->end
    */
    public void testForkJoinFailure() throws WorkflowException{
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);

        LiteWorkflowApp def = new LiteWorkflowApp("testWf", "<worklfow-app/>",
        new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
        .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f","end"))
        .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                 Arrays.asList(new String[]{"two", "three"})))
        .addNode(new ActionNodeDef("two", dummyConf, TestActionNodeHandler.class, "j","k"))
        .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "end","k"))
        .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "k"))
        .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
        .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

        try {
            invokeForkJoin(parser, def);
            fail("Expected to catch an exception but did not encounter any");
        } catch (Exception ex) {
            WorkflowException we = (WorkflowException) ex.getCause();
            assertEquals(ErrorCode.E0737, we.getErrorCode());
            // Make sure the message contains the nodes and type involved in the invalid transition to end
            assertTrue(we.getMessage().contains("three"));
            assertTrue(we.getMessage().contains("node [end]"));
            assertTrue(we.getMessage().contains("type [end]"));
        }
    }

    /*
     f->(2,3,4)
     2->j
     3->j
     4->f2
     f2->(5,6)
     5-j2
     6-j2
     j-j2
     j2-end
    */
    public void testNestedForkJoinFailure() throws WorkflowException {
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);

        LiteWorkflowApp def = new LiteWorkflowApp("testWf", "<worklfow-app/>",
            new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
            .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f","end"))
            .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                     Arrays.asList(new String[]{"four", "three", "two"})))
            .addNode(new ActionNodeDef("two", dummyConf, TestActionNodeHandler.class, "j","k"))
            .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "j","k"))
            .addNode(new ActionNodeDef("four", dummyConf, TestActionNodeHandler.class, "f2","k"))
            .addNode(new ForkNodeDef("f2", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                     Arrays.asList(new String[]{"five", "six"})))
            .addNode(new ActionNodeDef("five", dummyConf, TestActionNodeHandler.class, "j2", "k"))
            .addNode(new ActionNodeDef("six", dummyConf, TestActionNodeHandler.class, "j2", "k"))
            .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "j2"))
            .addNode(new JoinNodeDef("j2", LiteWorkflowStoreService.LiteControlNodeHandler.class, "k"))
            .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
            .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

        try {
            invokeForkJoin(parser, def);
            fail("Expected to catch an exception but did not encounter any");
        } catch (Exception ex) {
            WorkflowException we = (WorkflowException) ex.getCause();
            assertEquals(ErrorCode.E0730, we.getErrorCode());
        }
    }

    /*
     f->(2,3)
     2->ok->3
     2->fail->j
     3->ok->j
     3->fail->k
    */
    public void testTransitionFailure1() throws WorkflowException{
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);

        LiteWorkflowApp def = new LiteWorkflowApp("name", "def",
        new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
        .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f", "end"))
        .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                 Arrays.asList(new String[]{"two", "three"})))
        .addNode(new ActionNodeDef("two", dummyConf, TestActionNodeHandler.class, "three", "j"))
        .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "k"))
        .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
        .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

        try {
            invokeForkJoin(parser, def);
            fail("Expected to catch an exception but did not encounter any");
        } catch (Exception ex) {
            WorkflowException we = (WorkflowException) ex.getCause();
            assertEquals(ErrorCode.E0734, we.getErrorCode());
            // Make sure the message contains the nodes involved in the invalid transition
            assertTrue(we.getMessage().contains("two"));
            assertTrue(we.getMessage().contains("three"));
        }

    }

    /*
    f->(2,3)
    2->fail->3
    2->ok->j
    3->ok->j
    3->fail->k
   */
   public void testTransitionFailure2() throws WorkflowException{
       LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
               LiteWorkflowStoreService.LiteControlNodeHandler.class,
               LiteWorkflowStoreService.LiteDecisionHandler.class,
               LiteWorkflowStoreService.LiteActionHandler.class);

       LiteWorkflowApp def = new LiteWorkflowApp("name", "def",
       new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
       .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f", "end"))
       .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                Arrays.asList(new String[]{"two","three"})))
       .addNode(new ActionNodeDef("two", dummyConf, TestActionNodeHandler.class, "j", "three"))
       .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "j", "k"))
       .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "k"))
       .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
       .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

       try {
           invokeForkJoin(parser, def);
           fail("Expected to catch an exception but did not encounter any");
       } catch (Exception ex) {
           WorkflowException we = (WorkflowException) ex.getCause();
           assertEquals(ErrorCode.E0734, we.getErrorCode());
           // Make sure the message contains the nodes involved in the invalid transition
           assertTrue(we.getMessage().contains("two"));
           assertTrue(we.getMessage().contains("three"));
       }

   }

   /*
   f->(2,3)
   2->ok->j
   3->ok->4
   2->fail->4
   4->ok->j
  */
   public void testTransitionFailure3() throws WorkflowException{
       LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
               LiteWorkflowStoreService.LiteControlNodeHandler.class,
               LiteWorkflowStoreService.LiteDecisionHandler.class,
               LiteWorkflowStoreService.LiteActionHandler.class);

       LiteWorkflowApp def = new LiteWorkflowApp("name", "def",
       new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
       .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f", "end"))
       .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                Arrays.asList(new String[]{"two", "three"})))
       .addNode(new ActionNodeDef("two", dummyConf, TestActionNodeHandler.class, "j", "four"))
       .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "four", "k"))
       .addNode(new ActionNodeDef("four", dummyConf, TestActionNodeHandler.class, "j", "k"))
       .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "k"))
       .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
       .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

       try {
           invokeForkJoin(parser, def);
           fail("Expected to catch an exception but did not encounter any");
       } catch (Exception ex) {
           WorkflowException we = (WorkflowException) ex.getCause();
           assertEquals(ErrorCode.E0735, we.getErrorCode());
           // Make sure the message contains the node involved in the invalid transition
           assertTrue(we.getMessage().contains("four"));
       }
   }

   /*
    * f->(2,3)
    * 2->ok->j
    * 3->ok->j
    * j->end
    * 2->error->f1
    * 3->error->f1
    * f1->(4,5)
    * (4,5)->j1
    * j1->end
    */
   public void testErrorTransitionForkJoin() throws WorkflowException {
       LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
               LiteWorkflowStoreService.LiteControlNodeHandler.class,
               LiteWorkflowStoreService.LiteDecisionHandler.class,
               LiteWorkflowStoreService.LiteActionHandler.class);

       LiteWorkflowApp def = new LiteWorkflowApp("wf", "<worklfow-app/>",
       new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
       .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f", "end"))
       .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                Arrays.asList(new String[]{"two", "three"})))
       .addNode(new ActionNodeDef("two", dummyConf,  TestActionNodeHandler.class, "j", "f1"))
       .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "j", "f1"))
       .addNode(new ForkNodeDef("f1", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                Arrays.asList(new String[]{"four", "five"})))
       .addNode(new ActionNodeDef("four", dummyConf,  TestActionNodeHandler.class, "j1", "k"))
       .addNode(new ActionNodeDef("five", dummyConf, TestActionNodeHandler.class, "j1", "k"))
       .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "six"))
       .addNode(new JoinNodeDef("j1", LiteWorkflowStoreService.LiteControlNodeHandler.class, "six"))
       .addNode(new ActionNodeDef("six", dummyConf, TestActionNodeHandler.class, "end", "end"))
       .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
       .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

       try {
           invokeForkJoin(parser, def);
       } catch (Exception e) {
           e.printStackTrace();
           fail("Unexpected Exception");
       }
   }

    /*
    f->(2,3)
    2->decision node->{4,5,4}
    4->j
    5->j
    3->j
    */
    public void testDecisionForkJoin() throws WorkflowException{
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);
        LiteWorkflowApp def = new LiteWorkflowApp("name", "def",
        new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
        .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f","end"))
        .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                 Arrays.asList(new String[]{"two", "three"})))
        .addNode(new DecisionNodeDef("two", dummyConf, TestDecisionNodeHandler.class,
                                     Arrays.asList(new String[]{"four","five","four"})))
        .addNode(new ActionNodeDef("four", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new ActionNodeDef("five", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "end"))
        .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
        .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

        try {
            invokeForkJoin(parser, def);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected Exception");
        }
    }

    /*
    f->(2,3)
    2->decision node->{4,j,4}
    3->decision node->{j,5,j}
    4->j
    5->j
    */
    public void testDecisionsToJoinForkJoin() throws WorkflowException{
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);
        LiteWorkflowApp def = new LiteWorkflowApp("name", "def",
        new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
        .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f","end"))
        .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                 Arrays.asList(new String[]{"two","three"})))
        .addNode(new DecisionNodeDef("two", dummyConf, TestDecisionNodeHandler.class,
                                     Arrays.asList(new String[]{"four","j","four"})))
        .addNode(new DecisionNodeDef("three", dummyConf, TestDecisionNodeHandler.class,
                                     Arrays.asList(new String[]{"j","five","j"})))
        .addNode(new ActionNodeDef("four", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new ActionNodeDef("five", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "end"))
        .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
        .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

        try {
            invokeForkJoin(parser, def);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected Exception");
        }
    }

    /*
    f->(2,3)
    2->decision node->{4,k,4}
    3->decision node->{k,5,k}
    4->j
    5->j
    */
    public void testDecisionsToKillForkJoin() throws WorkflowException{
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);
        LiteWorkflowApp def = new LiteWorkflowApp("name", "def",
        new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
        .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f","end"))
        .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                 Arrays.asList(new String[]{"two","three"})))
        .addNode(new DecisionNodeDef("two", dummyConf, TestDecisionNodeHandler.class,
                                     Arrays.asList(new String[]{"four","k","four"})))
        .addNode(new DecisionNodeDef("three", dummyConf, TestDecisionNodeHandler.class,
                                     Arrays.asList(new String[]{"k","five","k"})))
        .addNode(new ActionNodeDef("four", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new ActionNodeDef("five", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "end"))
        .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
        .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

        try {
            invokeForkJoin(parser, def);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected Exception");
        }
    }

    /*
     *f->(2,3)
     *2->decision node->{3,4}
     *3->j
     *4->j
     */
    public void testDecisionForkJoinFailure() throws WorkflowException{
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);
        LiteWorkflowApp def = new LiteWorkflowApp("name", "def",
        new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
        .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f","end"))
        .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                 Arrays.asList(new String[]{"two","three"})))
        .addNode(new DecisionNodeDef("two", dummyConf, TestDecisionNodeHandler.class,
                                     Arrays.asList(new String[]{"four","three"})))
        .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new ActionNodeDef("four", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "k"))
        .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
        .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

        try {
            invokeForkJoin(parser, def);
            fail("Expected to catch an exception but did not encounter any");
        } catch (Exception ex) {
            WorkflowException we = (WorkflowException) ex.getCause();
            assertEquals(ErrorCode.E0734, we.getErrorCode());
            // Make sure the message contains the nodes involved in the invalid transition
            assertTrue(we.getMessage().contains("two"));
            assertTrue(we.getMessage().contains("three"));
        }
    }
    
    /*
     *f->(2,3)
     *2->decision node->{4,end}
     *3->j
     *4->j
     */
    public void testDecisionToEndForkJoinFailure() throws WorkflowException{
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);
        LiteWorkflowApp def = new LiteWorkflowApp("name", "def",
            new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
        .addNode(new ActionNodeDef("one", dummyConf, TestActionNodeHandler.class, "f","end"))
        .addNode(new ForkNodeDef("f", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                 Arrays.asList(new String[]{"two", "three"})))
        .addNode(new DecisionNodeDef("two", dummyConf, TestDecisionNodeHandler.class,
                                     Arrays.asList(new String[]{"four","end"})))
        .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new ActionNodeDef("four", dummyConf, TestActionNodeHandler.class, "j", "k"))
        .addNode(new JoinNodeDef("j", LiteWorkflowStoreService.LiteControlNodeHandler.class, "end"))
        .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
        .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

        try {
            invokeForkJoin(parser, def);
            fail("Expected to catch an exception but did not encounter any");
        } catch (Exception ex) {
            WorkflowException we = (WorkflowException) ex.getCause();
            assertEquals(ErrorCode.E0737, we.getErrorCode());
            // Make sure the message contains the nodes and type involved in the invalid transition to end
            assertTrue(we.getMessage().contains("two"));
            assertTrue(we.getMessage().contains("node [end]"));
            assertTrue(we.getMessage().contains("type [end]"));
        }
    }

    /*
     * 1->decision node->{f1, f2}
     * f1->(2,3)
     * f2->(4,5)
     * (2,3)->j1
     * (4,5)->j2
     * j1->end
     * j2->end
     */
    public void testDecisionMultipleForks() throws WorkflowException{
        LiteWorkflowAppParser parser = new LiteWorkflowAppParser(null,
                LiteWorkflowStoreService.LiteControlNodeHandler.class,
                LiteWorkflowStoreService.LiteDecisionHandler.class,
                LiteWorkflowStoreService.LiteActionHandler.class);
        LiteWorkflowApp def = new LiteWorkflowApp("name", "def",
        new StartNodeDef(LiteWorkflowStoreService.LiteControlNodeHandler.class, "one"))
        .addNode(new DecisionNodeDef("one", dummyConf, TestDecisionNodeHandler.class,
                                     Arrays.asList(new String[]{"f1","f2"})))
        .addNode(new ForkNodeDef("f1", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                 Arrays.asList(new String[]{"two", "three"})))
        .addNode(new ForkNodeDef("f2", LiteWorkflowStoreService.LiteControlNodeHandler.class,
                                 Arrays.asList(new String[]{"four","five"})))
        .addNode(new ActionNodeDef("two", dummyConf, TestActionNodeHandler.class, "j1", "k"))
        .addNode(new ActionNodeDef("three", dummyConf, TestActionNodeHandler.class, "j1", "k"))
        .addNode(new ActionNodeDef("four", dummyConf, TestActionNodeHandler.class, "j2", "k"))
        .addNode(new ActionNodeDef("five", dummyConf, TestActionNodeHandler.class, "j2", "k"))
        .addNode(new JoinNodeDef("j1", LiteWorkflowStoreService.LiteControlNodeHandler.class, "end"))
        .addNode(new JoinNodeDef("j2", LiteWorkflowStoreService.LiteControlNodeHandler.class, "end"))
        .addNode(new KillNodeDef("k", "kill", LiteWorkflowStoreService.LiteControlNodeHandler.class))
        .addNode(new EndNodeDef("end", LiteWorkflowStoreService.LiteControlNodeHandler.class));

        try {
            invokeForkJoin(parser, def);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected Exception");
        }
    }

    // Invoke private validateForkJoin method using Reflection API
    private void invokeForkJoin(LiteWorkflowAppParser parser, LiteWorkflowApp def) throws Exception {
        Class<? extends LiteWorkflowAppParser> c = parser.getClass();
        Class<?> d = Class.forName("org.apache.oozie.workflow.lite.LiteWorkflowAppParser$VisitStatus");
        Field f = d.getField("VISITING");
        Map traversed = new HashMap();
        traversed.put(def.getNode(StartNodeDef.START).getName(), f);
        Method validate = c.getDeclaredMethod("validate", LiteWorkflowApp.class, NodeDef.class, Map.class);
        validate.setAccessible(true);
        // invoke validate method to populate the fork and join list
        validate.invoke(parser, def, def.getNode(StartNodeDef.START), traversed);
        Method validateForkJoin = c.getDeclaredMethod("validateForkJoin", LiteWorkflowApp.class);
        validateForkJoin.setAccessible(true);
        // invoke validateForkJoin
        validateForkJoin.invoke(parser, def);
    }

}
