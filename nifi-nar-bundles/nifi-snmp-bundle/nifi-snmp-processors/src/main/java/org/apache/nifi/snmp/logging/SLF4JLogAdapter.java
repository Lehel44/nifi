/*_############################################################################
  _##
  _##  SNMP4J - JavaLogAdapter.java
  _##
  _##  Copyright (C) 2003-2020  Frank Fock (SNMP4J.org)
  _##
  _##  Licensed under the Apache License, Version 2.0 (the "License");
  _##  you may not use this file except in compliance with the License.
  _##  You may obtain a copy of the License at
  _##
  _##      http://www.apache.org/licenses/LICENSE-2.0
  _##
  _##  Unless required by applicable law or agreed to in writing, software
  _##  distributed under the License is distributed on an "AS IS" BASIS,
  _##  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  _##  See the License for the specific language governing permissions and
  _##  limitations under the License.
  _##
  _##########################################################################*/
package org.apache.nifi.snmp.logging;

import org.slf4j.Logger;
import org.snmp4j.log.LogAdapter;
import org.snmp4j.log.LogLevel;

import java.io.Serializable;
import java.util.Iterator;
import java.util.logging.Handler;

public class SLF4JLogAdapter implements LogAdapter {

    private final Logger logger;

    public SLF4JLogAdapter(Logger logger) {
        this.logger = logger;
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }


    public void debug(Serializable message) {
        if (isDebugEnabled()) {
            logger.debug(message.toString());
        }
    }

    public void info(CharSequence message) {
        if (isInfoEnabled()) {
            logger.info(message.toString());
        }
    }

    public void warn(Serializable message) {
        if (isWarnEnabled()) {
            logger.warn(message.toString());
        }
    }

    public void error(Serializable message) {
        logger.error(message.toString());
    }

    public void error(CharSequence message, Throwable t) {
        logger.error(message.toString(), t);
    }

    public void fatal(Object message) {
        logger.error(message.toString());
    }

    public void fatal(CharSequence message, Throwable t) {
        logger.error(message.toString(), t);
    }


    public LogLevel getEffectiveLogLevel() {
        return LogLevel.ALL;
    }

    public Iterator<Handler> getLogHandler() {
        return null;
    }

    public LogLevel getLogLevel() {
        return getEffectiveLogLevel();
    }

    public String getName() {
        return logger.getName();
    }

    public void setLogLevel(LogLevel logLevel) {
        // no need to set log level
    }

    private void log(LogLevel logLevel, String msg, Throwable t) {
        if (logLevel == LogLevel.ERROR || logLevel == LogLevel.FATAL) {
            if (t == null) {
                error(msg);
            } else {
                error(msg, t);
            }
        } else if (logLevel == LogLevel.WARN) {
            warn(msg);
        } else if (logLevel == LogLevel.INFO) {
            info(msg);
        } else {
            debug(msg);
        }
    }
}
