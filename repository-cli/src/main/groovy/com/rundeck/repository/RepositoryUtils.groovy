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

import com.fasterxml.jackson.databind.ObjectMapper
import com.rundeck.repository.auth.AuthenticationHelper
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

class RepositoryUtils {
    public static final String TOKEN_CACHE_PATH = "/.repoCliTkCache"
    static ObjectMapper mapper = new ObjectMapper()

    static String loginViaConsoleAndGetAccessToken(RepoInfo info) {

        Console console = System.console()
        console.println("Rundeck Login")
        String un = console.readLine("username: ")
        String pwd = new String(console.readPassword("password: "))

        return loginAndGetAccessToken(info.cognitoPool,info.clientId,info.region, un,pwd)

    }

    static String loginAndGetAccessToken(String cognitoPoolId, String clientId, String region, String un, String pwd) {
        AuthenticationHelper helper = new AuthenticationHelper(cognitoPoolId, clientId, region, "");
        String token = helper.doSRPAuthentication(un,pwd).getIdToken()
        cacheToken(token)
        return token
    }

    static private cacheToken(String token) {
        File cacheFile = new File(System.getProperty("user.home")+TOKEN_CACHE_PATH)
        def writer = cacheFile.newWriter(false)
        writer.append(token)
        writer.flush()
    }

    static String callAPIGwWithAccessToken(String url, String accessToken, String payload) {
        OkHttpClient client = new OkHttpClient()
        def rb = new Request.Builder().url(url)
                             .addHeader("Authorization",accessToken)
        if(payload) {
            rb.post(RequestBody.create(MediaType.parse("application/json"),payload))
        }
        Request rq = rb.build()
        Response resp = null
        String out = null
        try {
            resp = client.newCall(rq).execute()
            out = resp.body().string()
            if(!resp.successful) throw new Exception("Call Failed: " + out)
        } finally {
            if(resp) resp.close()
        }
        return out
    }

    static UploadOperationResult upload(String url, String absoluteFilePath, String contentType = 'application/octet-stream') {
        OkHttpClient client = new OkHttpClient()
        def rb = new Request.Builder().url(url)
        rb.put(RequestBody.create(MediaType.parse(contentType),new File(absoluteFilePath)))
        Response rsp = null
        UploadOperationResult result = new UploadOperationResult()
        try {
            rsp = client.newCall(rb.build()).execute()
            result.status = rsp.code()
            result.msg = rsp.body().string()
        } finally {
            if(rsp) rsp.close()
        }
        return result
    }

    static RepoInfo getRepoInfo(final String repoInfoUrl) {
        OkHttpClient client = new OkHttpClient()
        Request rq = new Request.Builder().url(repoInfoUrl).build()
        Response resp = null
        RepoInfo repoInfo = null
        try {
            resp = client.newCall(rq).execute()
            repoInfo = mapper.readValue(resp.body().string(),RepoInfo)
        } finally {
            if(resp) resp.close()
        }
        return repoInfo
    }
}
