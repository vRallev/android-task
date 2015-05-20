package net.vrallev.android.task.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rwondratschek
 */
public class ListTask extends BaseTask<List<String>> {

    @Override
    protected List<String> otherExecute() {
        // explicitly create an ArrayList and return default list
        List<String> result = new ArrayList<>();

        int items = (int) (Math.random() * 100);
        for (int i = 0; i < items; i++) {
            result.add("Test");
        }

//        return result;
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<List<String>> getResultClass() {
        return (Class) List.class;
    }
}
