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
package com.rundeck.verb.manifest

class ManifestEntry {
    String id
    String name
    String author
    String description
    String organization
    String artifactType
    String support
    String currentVersion
    String rundeckCompatibility
    String binaryLink
    Long lastRelease
    Collection<String> tags = []
    Collection<String> providesServices = []
    Collection<String> oldVersions = []
    Integer rating //1-5 stars
    Integer installs
    boolean installed
}
