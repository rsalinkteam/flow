/*
 * Copyright 2017 Lithium Technologies, Inc.
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

package com.lithium.flow.runner;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.config.Config;
import com.lithium.flow.filer.Filer;
import com.lithium.flow.filer.Record;
import com.lithium.flow.filer.RecordPath;
import com.lithium.flow.shell.Shell;
import com.lithium.flow.util.Logs;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

/**
 * @author Matt Ayres
 */
public class MavenJarProvider implements JarProvider {
	private static final Logger log = Logs.getLogger();

	private final Filer srcFiler;

	public MavenJarProvider(@Nonnull Config config, @Nonnull Filer srcFiler) {
		checkNotNull(config);
		this.srcFiler = checkNotNull(srcFiler);
	}

	@Override
	public boolean copy(@Nonnull String path, @Nonnull Shell shell, @Nonnull Filer destFiler, @Nonnull String destDir)
			throws IOException {

		String name = RecordPath.getName(path);
		String destPath = destDir + "/" + name;
		Record srcRecord = srcFiler.getRecord(path);

		long srcSize = srcRecord.getSize();
		long destSize = destFiler.getRecord(destPath).getSize();
		if (srcSize == destSize) {
			return true;
		}

		log.debug("jar: ({} <-> {}) {} -> {}", srcSize, destSize, path, destPath);

		String m2Marker = "/.m2/repository/";
		int index = path.indexOf(m2Marker);
		if (index == -1) {
			return false;
		}

		String m2Path = path.substring(index + m2Marker.length());
		String libPath = destDir + "/" + name;
		String command = "curl -sL http://search.maven.org/remotecontent?filepath=" + m2Path + " > " + libPath;

		log.debug("maven download: {}", destPath);
		shell.exec(command).exit();

		return srcSize == destFiler.getRecord(destPath).getSize();
	}
}
