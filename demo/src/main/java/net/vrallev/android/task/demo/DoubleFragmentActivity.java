package net.vrallev.android.task.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.vrallev.android.task.TaskExecutor;
import net.vrallev.android.task.TaskResult;

/**
 * @author rwondratschek
 */
@SuppressWarnings("ConstantConditions")
public class DoubleFragmentActivity extends AppCompatActivity {

    private static final String TAG_FRAGMENT = "TAG_FRAGMENT";
    private static final String TAG_REMOVE = "TAG_REMOVE";

    private static final boolean USE_TAGS = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_double_fragment);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, DoubleFragment.create(TAG_REMOVE), TAG_REMOVE)
                    .add(R.id.fragment_container, DoubleFragment.create("1"), USE_TAGS ? "1" : null)
                    .add(DoubleFragment.create("2"), TAG_FRAGMENT)
                    .add(R.id.fragment_container, DoubleFragment.create("3"), USE_TAGS ? "3" : null)
                    .add(R.id.fragment_container, new NestedFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_double_fragment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tag_frag:
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT);
                if (fragment instanceof DoubleFragment) {
                    ((DoubleFragment) fragment).launchTask();
                }
                return true;

            case R.id.action_remove:
                testRemove();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void testRemove() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        Fragment fragment = fragmentManager.findFragmentByTag(TAG_REMOVE);
        if (fragment == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, DoubleFragment.create("NEW"), TAG_REMOVE)
                    .commit();
            return;
        }

        TaskExecutor.getInstance().execute(new IntegerTask(1_000), fragment);

        fragmentManager.beginTransaction()
                .remove(fragment)
                .commit();

        fragmentManager.executePendingTransactions();
    }

    public static class DoubleFragment extends Fragment {

        private static final String KEY_CUSTOM_ID = "KEY_CUSTOM_ID";

        public static DoubleFragment create(String customId) {
            Bundle args = new Bundle();
            args.putString(KEY_CUSTOM_ID, customId);

            DoubleFragment fragment = new DoubleFragment();
            fragment.setArguments(args);
            return fragment;
        }

        private String mCustomId;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mCustomId = getArguments().getString(KEY_CUSTOM_ID);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if (container == null) {
                // only tag
                return null;
            }

            View view = inflater.inflate(R.layout.fragment_double, container, false);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
            params.weight = 1;

            Button button = (Button) view.findViewById(R.id.button);
            button.setText(button.getText() + " " + mCustomId);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchTask();
                }
            });

            return view;
        }

        public void launchTask() {
            TaskExecutor.getInstance().execute(new IntegerTask(3_000), this);
        }

        @TaskResult
        public void onInteger(Integer integer) {
            Toast.makeText(getActivity(), "Id " + mCustomId, Toast.LENGTH_SHORT).show();
        }
    }

    public static class NestedFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_double_fragment, container, false);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
            params.weight = 1;

            if (savedInstanceState == null) {
                getChildFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, DoubleFragment.create("10"), USE_TAGS ? "10" : null)
                        .add(R.id.fragment_container, DoubleFragment.create("20"), USE_TAGS ? "20" : null)
                        .commit();
            }

            return view;
        }
    }
}
