/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package com.rundeck.repository

import com.rundeck.repository.auth.AuthenticationHelper
import com.rundeck.repository.auth.JWTParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class RepositoryUtils {

    static String loginViaConsoleAndGetAccessToken() {

        Console console = System.console()
        console.println("Spark Login")
        String un = console.readLine("username: ")
        String pwd = console.readPassword("password: ")

        return loginAndGetAccessToken(un,pwd)

    }

    static String loginAndGetAccessToken(String un, String pwd) {
        AuthenticationHelper helper = new AuthenticationHelper("us-east-1_SbetwQtB6", "156fhibkhq5ie52jthm7o7ejo8", "us-east-1", "");
        return helper.doSRPAuthentication(un,pwd).getIdToken()
    }

    static void callAPIGwWithAccessToken(String accessToken) {
        println "using access token: " + accessToken
        println JWTParser.getPayload(accessToken)
        OkHttpClient client = new OkHttpClient()
        Request rq = new Request.Builder().url("https://43qvy1ln3e.execute-api.us-east-1.amazonaws.com/dev/hook")
                                          .addHeader("Authorization",accessToken)
                                          .build()
        Response resp = null
        try {
            resp = client.newCall(rq).execute()
            println resp.code()
        } finally {
            if(resp) resp.close()
        }

    }

}
