/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.hms.convertor.core.engine.fixbot.util;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixbot configs class
 *
 * @since 2020-02-24
 */
@Getter
@Setter
public class FixbotParams {
    private static final String ENGINE_PATH_SYMBOL = "-enginePath";

    private static final String MAPPING_PATH_SYMBOL = "-configPath";

    private static final String CACHE_DIRECTORY_SYMBOL = "-repoID";

    private static final String INSPECT_PATH_SYMBOL = "-repoPath";

    private static final String FIX_PATH_SYMBOL = "-fixPath";

    private static final String POLICY_SYMBOL = "-check";

    private static final String EXCLUDE_PATH_SYMBOL = "-excludedPath";

    private String enginePath;

    private String mappingPath;

    private String cacheDirectory;

    private String inspectPath;

    private String fixPath;

    private String policy;

    private List<String> jvmOpt;

    private List<String> excludedPaths;

    public FixbotParams() {
        excludedPaths = new ArrayList<>();
        jvmOpt = new ArrayList<>();
    }

    public void initJvmOpt() {
        jvmOpt = FixbotConfigs.getInstance().getVmOptions(enginePath);
    }

    public String[] toStringArgs() {
        initJvmOpt();
        jvmOpt.add(ENGINE_PATH_SYMBOL);
        jvmOpt.add(enginePath);
        jvmOpt.add(MAPPING_PATH_SYMBOL);
        jvmOpt.add(mappingPath);
        jvmOpt.add(CACHE_DIRECTORY_SYMBOL);
        jvmOpt.add(cacheDirectory);
        jvmOpt.add(INSPECT_PATH_SYMBOL);
        jvmOpt.add(inspectPath);
        jvmOpt.add(FIX_PATH_SYMBOL);
        jvmOpt.add(fixPath);
        jvmOpt.add(POLICY_SYMBOL);
        jvmOpt.add(policy);
        for (int i = 0; ((excludedPaths != null) && (i < excludedPaths.size())); i++) {
            jvmOpt.add(EXCLUDE_PATH_SYMBOL);
            jvmOpt.add(excludedPaths.get(i));
        }
        return jvmOpt.toArray(new String[0]);
    }
}
