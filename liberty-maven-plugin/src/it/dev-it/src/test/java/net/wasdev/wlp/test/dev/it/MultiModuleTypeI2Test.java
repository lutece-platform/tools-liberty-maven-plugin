/*******************************************************************************
 * (c) Copyright IBM Corporation 2021.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package net.wasdev.wlp.test.dev.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.shared.utils.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultiModuleTypeI2Test extends BaseMultiModuleTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      // project structure [parent, List of children]: [parent2: parent1, war], [parent1: ear]
      setUpMultiModule("typeI2", "ear", null);
      run();
   }

   @Test
   public void runTest() throws Exception {
      // start with missing dependencies in parent1 and parent2 pom.xml
      assertTrue(verifyLogMessageExists(
               "The recompileDependencies parameter is set to \"true\". On a file change all dependent modules will be recompiled.",
               20000));
      File targetJarClass = new File(tempProj, "jar/target/classes/io/openliberty/guides/multimodules/lib/Converter.class");
      assertTrue(targetJarClass.exists());
   
      // verify ear test class did not compile successfully
      File targetEarClass = new File(tempProj,
            "ear/target/test-classes/it/io/openliberty/guides/multimodules/IT.class");
      assertFalse(targetEarClass.exists());

      // verify war source class did not compile successfully
      File targetWarClass = new File(tempProj, "war/target/classes/io/openliberty/guides/multimodules/web/HeightsBean.class");
      assertFalse(targetWarClass.exists());

      clearLogFile();

      // add a dependency to parent pom and check that it resolves compile errors in
      // child modules
      File parent2Pom = new File(tempProj, "parent2/pom.xml");
      assertTrue(parent2Pom.exists());
      replaceString("<!-- SUB JAKARTAEE -->",
            "<dependency> <groupId>jakarta.platform</groupId> <artifactId>jakarta.jakartaee-api</artifactId> <version>8.0.0</version> <scope>provided</scope> </dependency>",
            parent2Pom);
      Thread.sleep(5000); // wait for compilation
      assertTrue(getLogTail(), verifyLogMessageExists("guide-maven-multimodules-war source compilation was successful.", 10000));
      assertTrue(getLogTail(), verifyLogMessageExists("guide-maven-multimodules-ear tests compilation had errors.", 10000));
      assertTrue(targetWarClass.exists());
      assertFalse(targetEarClass.exists()); // verify ear class is still failing to resolve

      File parent1Pom = new File(tempProj, "parent1/pom.xml");
      assertTrue(parent1Pom.exists());
      replaceString("<!-- SUB JUNIT -->",
            "<dependency> <groupId>org.junit.jupiter</groupId> <artifactId>junit-jupiter</artifactId> <version>5.6.2</version> <scope>test</scope> </dependency>",
            parent1Pom);
      Thread.sleep(5000); // wait for compilation
      assertTrue(targetEarClass.exists());

      testEndpointsAndUpstreamRecompile();
   }

}

