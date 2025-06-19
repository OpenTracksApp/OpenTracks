package de.dennisguse.opentracks.introduction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.TrackListActivity;
import de.dennisguse.opentracks.databinding.IntroductionBinding;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.IntentUtils;

public class IntroductionActivity extends AbstractActivity {

    private IntroductionBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        if (!PreferencesUtils.shouldShowIntroduction()) {
            startActivity(new Intent(this, TrackListActivity.class));
            finish();
        }

        viewBinding.nextButton.setOnClickListener(v -> {
            int nextItemId = viewBinding.introductionViewPager.getCurrentItem() + 1;
            if (nextItemId < viewBinding.introductionViewPager.getAdapter().getItemCount()) {
                viewBinding.introductionViewPager.setCurrentItem(nextItemId);
            } else {
                PreferencesUtils.setShowIntroduction(false);
                startActivity(IntentUtils.newIntent(this, TrackListActivity.class));
                finish();
            }
        });
        viewBinding.introductionViewPager.setAdapter(new CustomFragmentPagerAdapter(this));
        viewBinding.introductionViewPager.setUserInputEnabled(false);
    }

    @NonNull
    @Override
    protected View createRootView() {
        viewBinding = IntroductionBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewBinding = null;
    }

    private static class CustomFragmentPagerAdapter extends FragmentStateAdapter {

        private static final List<FragmentCreator> fragmentCreators = List.of(
                WelcomeFragment::newInstance,
                OSMDashboardFragment::newInstance
        );

        public CustomFragmentPagerAdapter(@NonNull FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            FragmentCreator fc = fragmentCreators.get(position);
            if (fc != null) {
                return fc.newInstance();
            }

            throw new RuntimeException("There isn't Fragment associated with the position: " + position);
        }

        @Override
        public int getItemCount() {
            return fragmentCreators.size();
        }
    }

    private interface FragmentCreator {
        Fragment newInstance();
    }
}
