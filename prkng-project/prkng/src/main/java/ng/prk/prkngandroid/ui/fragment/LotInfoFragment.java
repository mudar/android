package ng.prk.prkngandroid.ui.fragment;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaView;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;

import ng.prk.prkngandroid.Const;
import ng.prk.prkngandroid.R;
import ng.prk.prkngandroid.io.PrkngApiError;
import ng.prk.prkngandroid.model.BusinessIntervalList;
import ng.prk.prkngandroid.model.LotAttrs;
import ng.prk.prkngandroid.model.LotCurrentStatus;
import ng.prk.prkngandroid.model.StreetView;
import ng.prk.prkngandroid.model.ui.HumanDuration;
import ng.prk.prkngandroid.ui.activity.OnMarkerInfoClickListener;
import ng.prk.prkngandroid.ui.adapter.LotAgendaListAdapter;
import ng.prk.prkngandroid.ui.thread.LotInfoDownloadTask;
import ng.prk.prkngandroid.ui.thread.base.MarkerInfoUpdateListener;
import ng.prk.prkngandroid.util.AnalyticsUtils;
import ng.prk.prkngandroid.util.IntentUtils;

public class LotInfoFragment extends Fragment implements
        MarkerInfoUpdateListener,
        View.OnClickListener {
    private static final String TAG = "LotInfoFragment";

    private OnMarkerInfoClickListener listener;
    private TextView vTitle;
    private View vPrice;
    private TextView vCapacity;
    private Button vInfoBtn;
    private TextView vMainPrice;
    private TextView vHourlyPrice;
    private TextView vRemainingTime;
    private TextView vRemainingTimePrefix;
    private View vProgressBar;
    private RecyclerView vRecyclerView;
    private LotAgendaListAdapter mAdapter;
    private String mId;
    private String mTitle;
    private long mSubtitle;
    private LatLng mLatLng;
    private BusinessIntervalList mDataset;
    private LotCurrentStatus mStatus;
    private int mCapacity;
    private LotAttrs mAttrs;
    private StreetView mStreetView;
    private boolean isExpanded;
    private StreetViewPanoramaView vStreetViewPanoramaView;
    private View vStreetViewDelayFix;

    public static LotInfoFragment newInstance(String id, String title, LatLng latLng) {
        final LotInfoFragment fragment = new LotInfoFragment();

        final Bundle bundle = new Bundle();
        bundle.putString(Const.BundleKeys.MARKER_ID, id);
        bundle.putString(Const.BundleKeys.MARKER_TITLE, title);
        bundle.putDouble(Const.BundleKeys.LATITUDE, latLng.getLatitude());
        bundle.putDouble(Const.BundleKeys.LONGITUDE, latLng.getLongitude());
        fragment.setArguments(bundle);

        return fragment;
    }

    public static LotInfoFragment clone(LotInfoFragment fragment) {
        LotInfoFragment clone = new LotInfoFragment();

        clone.mSubtitle = fragment.mSubtitle;
        clone.mDataset = fragment.mDataset;
        clone.mStatus = fragment.mStatus;
        clone.mCapacity = fragment.mCapacity;
        clone.mAttrs = fragment.mAttrs;
        clone.mStreetView = fragment.mStreetView;
        clone.mLatLng = fragment.mLatLng;

        final Bundle bundle = fragment.getArguments();
        bundle.putBoolean(Const.BundleKeys.IS_EXPANDED, true);
        clone.setArguments(bundle);

        return clone;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnMarkerInfoClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnMarkerInfoClickListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final Bundle args = getArguments();
        mId = args.getString(Const.BundleKeys.MARKER_ID);
        mTitle = args.getString(Const.BundleKeys.MARKER_TITLE);
        isExpanded = args.getBoolean(Const.BundleKeys.IS_EXPANDED, false);
        mLatLng = new LatLng(args.getDouble(Const.BundleKeys.LATITUDE),
                args.getDouble(Const.BundleKeys.LONGITUDE));

        final View view = inflater.inflate(
                isExpanded ? R.layout.fragment_lot_details : R.layout.fragment_lot_info,
                container,
                false);

        vTitle = (TextView) view.findViewById(R.id.title);
        vRemainingTime = (TextView) view.findViewById(R.id.remaining_time);
        vRemainingTimePrefix = (TextView) view.findViewById(R.id.remaining_time_prefix);
        vPrice = view.findViewById(R.id.price);
        vCapacity = (TextView) view.findViewById(R.id.capacity);
        vInfoBtn = (Button) view.findViewById(R.id.btn_info);
        vProgressBar = view.findViewById(R.id.progress);
        vRecyclerView = (RecyclerView) view.findViewById(R.id.recycler);
        vMainPrice = (TextView) view.findViewById(R.id.main_price);
        vHourlyPrice = (TextView) view.findViewById(R.id.hourly_price);
        vStreetViewPanoramaView = (StreetViewPanoramaView) view.findViewById(R.id.street_view_panorama);
        vStreetViewDelayFix = view.findViewById(R.id.destreet_view_delay_fix);

        mAdapter = new LotAgendaListAdapter(getContext(), R.layout.list_item_lot_agenda);

        if (vStreetViewPanoramaView != null) {
            // StreetView instance must be saved in a separate Bundle
            final Bundle streetViewSavedInstanceState = (savedInstanceState != null) ?
                    savedInstanceState.getBundle(Const.BundleKeys.STREET_VIEW_FIX) : null;
            vStreetViewPanoramaView.onCreate(streetViewSavedInstanceState);
            if (streetViewSavedInstanceState != null) {
                // When restoring instance, the delayFix view is not needed
//                vStreetViewDelayFix.setVisibility(View.GONE);
            }
        }
        setupLayout(view);

        downloadData(getActivity(), mId);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vStreetViewPanoramaView != null) {
            vStreetViewPanoramaView.onResume();
        }

        if (isExpanded) {
            AnalyticsUtils.sendFragmentView(this, mId);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (vStreetViewPanoramaView != null) {
            vStreetViewPanoramaView.onPause();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // We need to save the StreetView saved state on a separate Bundle,
        // this must be done before saving any of your own or your base class's variables
        // Ref: https://code.google.com/p/gmaps-api-issues/issues/detail?id=6237#c9
        if (vStreetViewPanoramaView != null) {
            final Bundle streetViewSaveState = new Bundle(outState);
            vStreetViewPanoramaView.onSaveInstanceState(streetViewSaveState);
            outState.putBundle(Const.BundleKeys.STREET_VIEW_FIX, streetViewSaveState);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (vStreetViewPanoramaView != null) {
            vStreetViewPanoramaView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        if (vStreetViewPanoramaView != null) {
            vStreetViewPanoramaView.onLowMemory();
        }
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (R.id.root_view == id || R.id.btn_info == id) {
            showDetails();
        } else if (id == R.id.btn_nav_back) {
            getActivity().onBackPressed();
        } else if (id == R.id.action_directions) {
            final MainMapFragment map = (MainMapFragment) getActivity()
                    .getSupportFragmentManager().findFragmentByTag(Const.FragmentTags.MAP);
            final Location userLocation = (map != null) ?
                    map.getUserLocation() : null;

            LatLng latLng = null;
            if (userLocation != null) {
                final float speed = userLocation.getSpeed();
                if (Float.compare(speed, Const.UiConfig.DRIVING_MIN_SPEED) > 0) {
                    // User is driving, try to launch driving directions
                    try {
                        final Intent intent = IntentUtils.getNavigationIntent(mLatLng);
                        startActivity(intent);

                        // Exit here, no need to launch GoogleMaps fallback intent
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                latLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
            }
            // Couldn't start GoogleNavigation, fallback to GoogleMaps
            final Intent intent = IntentUtils.getDirectionsIntent(latLng, mLatLng);
            startActivity(intent);
        }
    }

    /**
     * Implements MarkerInfoUpdateListener
     *
     * @param time
     */
    @Override
    public void setRemainingTime(long time) {

    }


    /**
     * Implements MarkerInfoUpdateListener
     *
     * @param status
     * @param capacity
     */
    @Override
    public void setCurrentStatus(LotCurrentStatus status, int capacity) {
        if (!isAdded()) {
            return;
        }

        this.mStatus = status;
        this.mCapacity = capacity;

        final Resources res = getResources();

        if (status != null && !status.isFree()) {
            final int dailyPrice = status.getMainPriceRounded();
            final int hourlyPrice = status.getHourlyPriceRounded();

            if (dailyPrice != Const.UNKNOWN_VALUE) {
                vMainPrice.setText(String.format(
                        res.getString(R.string.currency_round),
                        dailyPrice));
            } else {
                vMainPrice
                        .setVisibility(View.INVISIBLE);
            }

            if (isExpanded) {
                if (hourlyPrice != Const.UNKNOWN_VALUE) {
                    final String sHourlPrice = String.format(res.getString(R.string.currency_round),
                            hourlyPrice);
                    vHourlyPrice
                            .setText(String.format(res.getString(R.string.lot_hourly_price), sHourlPrice));
                } else {
                    vHourlyPrice.setVisibility(View.INVISIBLE);
                }
            }
            final HumanDuration duration = new HumanDuration.Builder(getContext())
                    .millis(status.getRemainingMillis())
                    .lot()
                    .build();
            vRemainingTime.setText(duration.getExpiry());
            vRemainingTimePrefix.setText(duration.getPrefix());
        }

        if (isExpanded) {
            if (capacity != Const.UNKNOWN_VALUE) {
                vCapacity.setText(
                        String.format(res.getString(R.string.lot_capactiy), capacity));
            } else {
                vCapacity.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void setDataset(ArrayList list) {
        mDataset = (BusinessIntervalList) list;
        if (vRecyclerView != null) {
            mAdapter.swapDataset(mDataset);
        }

        hideProgressBar();
    }

    private void hideProgressBar() {
        if (!isExpanded && getView() != null) {
            ObjectAnimator.ofFloat(getView().findViewById(R.id.price), View.ALPHA, 0, 1).start();
            ObjectAnimator.ofFloat(getView().findViewById(R.id.subtitle), View.ALPHA, 0, 1).start();
        }

        vProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void setAttributes(LotAttrs attrs, final StreetView streetView) {
        this.mAttrs = attrs;
        this.mStreetView = streetView;
        if (mAdapter != null) {
            mAdapter.setFooterAttrs(attrs);
        }

        if (vStreetViewPanoramaView != null) {
            vStreetViewPanoramaView.getStreetViewPanoramaAsync(new OnStreetViewPanoramaReadyCallback() {
                @Override
                public void onStreetViewPanoramaReady(final StreetViewPanorama panorama) {
                    vStreetViewPanoramaView.setVisibility(View.VISIBLE);
                    panorama.setUserNavigationEnabled(false);
                    panorama.setPanningGesturesEnabled(false);
                    panorama.setZoomGesturesEnabled(false);

                    if (!TextUtils.isEmpty(streetView.getId())) {
                        panorama.setPosition(streetView.getId());

                        panorama.setOnStreetViewPanoramaChangeListener(new StreetViewPanorama.OnStreetViewPanoramaChangeListener() {
                            @Override
                            public void onStreetViewPanoramaChange(StreetViewPanoramaLocation streetViewPanoramaLocation) {
                                if (streetViewPanoramaLocation == null) {
                                    Log.v(TAG, "Location not found. panoramaId: " + streetView.getId());
                                    return;
                                }

                                try {
                                    if (streetView.getId().equals(streetViewPanoramaLocation.panoId)) {
                                        panorama.animateTo(new StreetViewPanoramaCamera.Builder()
                                                .zoom(Const.UiConfig.STREET_VIEW_ZOOM)
                                                .bearing(streetView.getHead())
                                                .build(), Const.UiConfig.STREET_VIEW_DELAY);

                                        vStreetViewPanoramaView.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                ObjectAnimator
                                                        .ofFloat(vStreetViewDelayFix, View.ALPHA, 1, 0)
                                                        .start();
                                            }
                                        }, Const.UiConfig.STREET_VIEW_DELAY);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    /**
     * Implements MarkerInfoUpdateListener
     *
     * @param e
     */
    @Override
    public void onFailure(PrkngApiError e) {

    }

    private void setupLayout(View view) {
        if (isExpanded) {
            vTitle.setText(mTitle);

            // Setup the recycler view
            final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            vRecyclerView.setLayoutManager(layoutManager);

            vRecyclerView.setAdapter(mAdapter);

            view.findViewById(R.id.btn_nav_back).setOnClickListener(this);
            view.findViewById(R.id.action_directions).setOnClickListener(this);
        } else {
            view.setOnClickListener(this);

            vTitle.setText(mTitle);

            vInfoBtn.setOnClickListener(this);
        }
    }

    private void downloadData(Context context, String id) {
        if (mDataset == null) {
            new LotInfoDownloadTask(context, this)
                    .execute(id);
        } else {
            setDataset(mDataset);
            setCurrentStatus(mStatus, mCapacity);
            setAttributes(mAttrs, mStreetView);
        }
    }

    private void showDetails() {
        listener.expandMarkerInfo(this);
    }

}
