/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package iRpc.vote;

import iRpc.dataBridge.vote.DLedgerResponseCode;

public class PreConditions {

    public static void check(boolean expression, DLedgerResponseCode code) throws RuntimeException {
        check(expression, code, null);
    }

    public static void check(boolean expression, DLedgerResponseCode code, String message) throws RuntimeException {
        if (!expression) {
            message = message == null ? code.toString()
                : code.toString() + " " + message;
            throw new RuntimeException(message);
        }
    }

    public static void check(boolean expression, DLedgerResponseCode code, String format,
        Object... args) throws RuntimeException {
        if (!expression) {
            String message = code.toString() + " " + String.format(format, args);
            throw new RuntimeException( message);
        }
    }
}
