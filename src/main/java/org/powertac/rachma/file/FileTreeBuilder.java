package org.powertac.rachma.file;

import java.io.IOException;
import java.nio.file.Path;

public interface FileTreeBuilder {

    FileNode build(Path path) throws IOException;

}