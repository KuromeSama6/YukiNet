package moe.hiktal.YukiNet;

import org.apache.commons.lang.SystemUtils;

public class Util {
    public static boolean IsSupportedOperatingSystem() {
        return SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_AIX || SystemUtils.IS_OS_UNIX;
    }
}
