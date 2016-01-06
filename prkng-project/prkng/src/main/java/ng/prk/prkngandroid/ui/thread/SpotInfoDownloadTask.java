package ng.prk.prkngandroid.ui.thread;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import ng.prk.prkngandroid.io.ApiClient;
import ng.prk.prkngandroid.io.PrkngService;
import ng.prk.prkngandroid.model.LinesGeoJSONFeature;
import ng.prk.prkngandroid.model.RestrIntervalsList;
import ng.prk.prkngandroid.model.SpotRules;
import ng.prk.prkngandroid.ui.adapter.SpotAgendaListAdapter;
import ng.prk.prkngandroid.ui.thread.base.MarkerInfoUpdateListener;
import ng.prk.prkngandroid.util.CalendarUtils;
import ng.prk.prkngandroid.util.PrkngPrefs;

public class SpotInfoDownloadTask extends AsyncTask<String, Void, SpotRules> {
    private final static String TAG = "SpotInfo";

    private final Context context;
    private final MarkerInfoUpdateListener listener;
    private SpotAgendaListAdapter mAdapter;

    public SpotInfoDownloadTask(Context context, SpotAgendaListAdapter adapter, MarkerInfoUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        this.mAdapter = adapter;
    }

    @Override
    protected SpotRules doInBackground(String... params) {
        Log.v(TAG, "doInBackground");

//        String spotId = "416753"; // arret + Interdiction
//        String spotId = "458653"; // arret + Interdiction
//        String spotId = "416663"; //  tues + friday
//        String spotId = "462798"; //  paid + interdiction
//        String spotId = "448955"; // arret + paid (ConcurrentModificationException)
//        String spotId = "417832"; // arret mardi 9:30-10:30, timeRemaining = 7 days
//        String spotId = "442896"; // 48h+ parking allowed
//        String spotId = "417002"; // 6day+ parking allowed
//        String spotId = "447204"; // mercredi NoParking 9-10. looped week
//        String spotId = "429308"; // arret x2 + paid x2
//        String spotId = "429400"; // Allowed all week
//        String spotId = "419907"; // TimeMax (60) 9-16
//        String spotId = "417152"; // reserve s3r 15h-09h
//        String spotId = "444521"; // reserve s3r 15h-09h
//        String spotId = "448231 "; // TimeMax (120) + freeparking
//        String spotId = "445340"; // TimeMax (60) + TimeMax (60)
//        String spotId = "454257"; // TimeMax merged with TimeMaxPaid (60)

        final String spotId = params[0];
        Log.v(TAG, "spotId = " + spotId);

        final PrkngService service = ApiClient.getService();
        try {
            final String apiKey = PrkngPrefs.getInstance(context).getApiKey();

            LinesGeoJSONFeature spotFeatures = ApiClient.getParkingSpotInfo(service, apiKey, spotId);
            return spotFeatures.getProperties().getSpotRules();

        } catch (NullPointerException e) {
            e.printStackTrace();
        }


        return null;
    }

    @Override
    protected void onPostExecute(SpotRules spotRules) {
        Log.v(TAG, "onPostExecute");
        if (spotRules != null) {
            Log.v(TAG, "rules = " + spotRules.toString());
            final RestrIntervalsList parkingAgenda = spotRules.getParkingSpotAgenda();
            mAdapter.swapDataset(parkingAgenda);

            listener.setRemainingTime(
                    SpotRules.getRemainingTime(parkingAgenda, CalendarUtils.todayMillis())
            );
        }

//        SpotRuleAgenda agenda = spotRules.get(0).getAgenda();
//        Log.v(TAG, "timeMax = " + spotRules.get(0).getTimeMaxParking());
//        Log.v(TAG, "monday = " + agenda.getMonday().get(0));
    }
}
