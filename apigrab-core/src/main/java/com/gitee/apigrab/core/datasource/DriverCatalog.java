package com.gitee.apigrab.core.datasource;

import com.gitee.apigrab.persistence.entity.enums.DbType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 外部 JDBC 驱动目录索引，目录布局对齐 sqlrest / dbswitch：
 * {@code drivers/<类型名小写>/<版本>/*.jar}。
 *
 * <p>国产数据库驱动是厂商私有 jar，不进入 Maven classpath；本类在启动时按配置
 * 扫描并把「类型 + 版本 + jar 路径」暴露给前端，数据源表单选中后写入 driverPath，
 * 由 {@link DynamicDataSourceManager} 用 URLClassLoader 隔离加载。
 */
@Component
@Slf4j
public class DriverCatalog {

    @Value("${apigrab.datasource.drivers-path:drivers}")
    private String driversPath;

    private volatile List<Map<String, Object>> cache;

    /** 已发现的驱动包（懒加载 + 启动后缓存）。 */
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> local = cache;
        if (local == null) {
            synchronized (this) {
                local = cache;
                if (local == null) {
                    local = scan(configuredRoots());
                    cache = local;
                    log.info("Indexed {} external JDBC driver package(s) from {}", local.size(), driversPath);
                }
            }
        }
        return local;
    }

    /** 运行中放入新驱动 jar 后可调用，使列表重新扫描。 */
    public void refresh() {
        cache = null;
    }

    /** 解析配置的驱动根目录，可逗号分隔多个；相对路径以进程工作目录为基准。 */
    public List<File> configuredRoots() {
        List<File> roots = new ArrayList<>();
        if (driversPath == null || driversPath.trim().isEmpty()) {
            return roots;
        }
        for (String part : driversPath.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            File file = new File(trimmed);
            if (!file.isAbsolute()) {
                File cwd = new File(System.getProperty("user.dir"));
                File candidate = new File(cwd, trimmed);
                // 开发态（spring-boot:run 在 apigrab-admin 目录启动）驱动目录位于上一级仓库根。
                if (!candidate.isDirectory()) {
                    File parentCandidate = new File(cwd, "../" + trimmed);
                    if (parentCandidate.isDirectory()) {
                        candidate = parentCandidate;
                    }
                }
                file = candidate;
            }
            roots.add(file);
        }
        return roots;
    }

    /**
     * 扫描驱动根目录。包级静态方法便于脱离 Spring 容器做单元测试。
     */
    public static List<Map<String, Object>> scan(List<File> roots) {
        List<Map<String, Object>> packages = new ArrayList<>();
        if (roots == null) {
            return packages;
        }
        for (File root : roots) {
            if (root == null || !root.isDirectory()) {
                continue;
            }
            File[] typeDirs = root.listFiles(File::isDirectory);
            if (typeDirs == null) {
                continue;
            }
            Arrays.sort(typeDirs, Comparator.comparing(File::getName));
            for (File typeDir : typeDirs) {
                DbType dbType = DbType.fromName(typeDir.getName());
                if (dbType == null) {
                    continue;
                }
                File[] versionDirs = typeDir.listFiles(File::isDirectory);
                if (versionDirs != null) {
                    Arrays.sort(versionDirs, Comparator.comparing(File::getName));
                    for (File versionDir : versionDirs) {
                        addPackage(packages, dbType, versionDir.getName(), collectJars(versionDir));
                    }
                }
                // 也允许把 jar 直接放在 drivers/<type>/ 下（无版本子目录）
                addPackage(packages, dbType, "default", jarFiles(typeDir));
            }
        }
        return packages;
    }

    private static void addPackage(List<Map<String, Object>> packages, DbType dbType,
                                   String version, List<File> jars) {
        if (jars.isEmpty()) {
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("dbType", dbType.name());
        item.put("label", dbType.getDisplayName());
        item.put("version", version);
        item.put("driverClass", dbType.getDriverClass());
        item.put("driverPath", jars.stream().map(f -> f.getAbsolutePath()).collect(Collectors.joining(",")));
        item.put("jarFiles", jars.stream().map(File::getName).collect(Collectors.toList()));
        packages.add(item);
    }

    /** 递归收集目录（含子目录）下的 jar，按路径排序保证 ClassLoader 顺序稳定。 */
    private static List<File> collectJars(File directory) {
        List<File> jars = new ArrayList<>(jarFiles(directory));
        File[] subDirs = directory.listFiles(File::isDirectory);
        if (subDirs != null) {
            Arrays.sort(subDirs, Comparator.comparing(File::getName));
            for (File subDir : subDirs) {
                jars.addAll(collectJars(subDir));
            }
        }
        return jars;
    }

    private static List<File> jarFiles(File directory) {
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (files == null) {
            return new ArrayList<>();
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        return new ArrayList<>(Arrays.asList(files));
    }
}
