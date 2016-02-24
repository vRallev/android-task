package net.vrallev.android.task.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import net.vrallev.android.task.Task;
import net.vrallev.android.task.TaskExecutor;
import net.vrallev.android.task.TaskResult;

/**
 * @author rwondratschek
 */
public class ViewPagerActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pager);

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);

        FragmentPagerAdapter adapter = new MyAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
    }

    private static final class MyAdapter extends FragmentPagerAdapter {

        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return MyFragment.create(position);
        }

        @Override
        public int getCount() {
            return 10;
        }
    }

    public static class MyFragment extends Fragment {

        private static final String KEY_POSITION = "KEY_POSITION";
        private int mPosition;
        
        public static MyFragment create(int position) {
            Bundle args = new Bundle();
            args.putInt(KEY_POSITION, position);
            MyFragment fragment = new MyFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mPosition = getArguments().getInt(KEY_POSITION, -1);

            TaskExecutor.getInstance().execute(new MyTask(mPosition), MyFragment.this);

            View view = inflater.inflate(R.layout.fragment_view_pager, container, false);

            Button button = (Button) view.findViewById(R.id.button);
            button.setText("Pos " + mPosition);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TaskExecutor.getInstance().execute(new MyTask(mPosition), MyFragment.this);
                }
            });

            return view;
        }

        @TaskResult
        public void onResult(String position) {
            Toast.makeText(getActivity(), String.format("Frag %d, value %s", mPosition, position), Toast.LENGTH_SHORT).show();
        }
    }

    private static final class MyTask extends Task<String> {

        private final int mPosition;

        private MyTask(int position) {
            mPosition = position;
        }

        @Override
        protected String execute() {
            return String.valueOf(mPosition);
        }
    }
}
