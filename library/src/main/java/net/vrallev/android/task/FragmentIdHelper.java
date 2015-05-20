package net.vrallev.android.task;

import android.support.v4.app.Fragment;

import java.util.List;

/**
 * @author rwondratschek
 */
/*package*/ final class FragmentIdHelper {

    private FragmentIdHelper() {
        // no op
    }

    public static boolean equals(String fragmentId1, String fragmentId2, boolean compareIndex) {
        String[] split1 = fragmentId1.split("/");
        String[] split2 = fragmentId2.split("/");

        if (split1.length != 3 || split2.length != 3) {
            return false;
        }

        if (compareIndex && !split1[0].equals(split2[0])) {
            return false;
        }

        for (int i = 1; i < split1.length; i++) {
            if (!split1[i].equals(split2[i])) {
                return false;
            }
        }

        return true;
    }

    public static String getFragmentId(Fragment fragment) {
        String index = getIndex(fragment);
        int id = fragment.getId();

        String tag = fragment.getTag();
        if (tag == null) {
            tag = "null";
        }

        return index + '/' + id + '/' + tag;
    }

    private static String getIndex(Fragment fragment) {
        String index;
        if (fragment.getParentFragment() != null) {
            index = getIndex(fragment.getParentFragment()) + "-";
        } else {
            index = "";
        }

        List<Fragment> fragments = fragment.getFragmentManager().getFragments();
        if (fragments != null) {
            for (int i = 0; i < fragments.size(); i++) {
                if (fragment.equals(fragments.get(i))) {
                    index += i;
                    break;
                }
            }
        }

        return index;
    }
}
