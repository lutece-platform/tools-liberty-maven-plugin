/**
 * (C) Copyright IBM Corporation 2019, 2023.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.maven.utils;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import io.openliberty.tools.common.CommonLoggerI;

public class CommonLogger implements CommonLoggerI {

    private static CommonLogger logger = null;
    private Log loggerImpl;

    public static CommonLogger getInstance(Log mojoLogger) {
        if (logger == null) {
            logger = new CommonLogger(mojoLogger);
        } else {
            logger.setLogger(mojoLogger);
        }

        return logger;
    }

    private CommonLogger(Log mojoLogger) {
        loggerImpl = mojoLogger;
    }

    private void setLogger(Log mojoLogger) {
        loggerImpl = mojoLogger;
    }

    public Log getLog() {
        if (this.loggerImpl == null) {
            this.loggerImpl = new SystemStreamLog();
        }

        return this.loggerImpl;
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            getLog().debug(msg);
        }
    }

    @Override
    public void debug(String msg, Throwable e) {
        if (isDebugEnabled()) {
            getLog().debug(msg, e);
        }
    }

    @Override
    public void debug(Throwable e) {
        if (isDebugEnabled()) {
            getLog().debug(e);
        }
    }

    @Override
    public void warn(String msg) {
        getLog().warn(msg);
    }

    @Override
    public void info(String msg) {
        getLog().info(msg);
    }

    @Override
    public void error(String msg) {
        getLog().error(msg);
    }

    @Override
    public boolean isDebugEnabled() {
        return getLog().isDebugEnabled();
    }

}