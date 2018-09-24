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
package com.rundeck.verb.client.validators

import com.dtolabs.rundeck.core.plugins.ScriptPluginScanner
import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.ResponseCodes
import com.rundeck.verb.ResponseMessage
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.validator.ArtifactBinaryValidator


class ScriptPluginArtifactValidator implements ArtifactBinaryValidator {
    @Override
    ResponseBatch validate(final File binaryFile) {
        //the file name is very important to the script loader,
        // attempt to read the root dir from the zip and rename the file based on that name
        File binFile
        if(!binaryFile.name.endsWith(".zip")) {
            binFile = ArtifactUtils.renameScriptFile(binaryFile)
        } else {
            binFile = binaryFile
        }

        ResponseBatch response = new ResponseBatch()
        if(ScriptPluginScanner.validatePluginFile(binFile)) {
            response.addMessage(ResponseMessage.success())
        } else {
            response.addMessage(new ResponseMessage(code: ResponseCodes.INVALID_BINARY, message:"Script plugin is not valid. Please check the logs for specific error messages"))
        }
        return response
    }
}
