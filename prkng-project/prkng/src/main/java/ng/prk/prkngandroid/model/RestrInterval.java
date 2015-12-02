package ng.prk.prkngandroid.model;

import android.text.format.DateUtils;

import java.util.concurrent.TimeUnit;

import ng.prk.prkngandroid.Const;
import ng.prk.prkngandroid.util.Interval;

/**
 * Restriction Interval
 */
public class RestrInterval extends Interval implements
        Const.ParkingRestrType {
    private final static String TAG = "RestrInterval";

    private int dayOfWeek;
    private int type;
    private int timeMax;

    private RestrInterval(Builder builder) {
        super(builder.startMillis, builder.endMillis);

        this.dayOfWeek = builder.dayOfWeek;
        this.type = builder.type;
        this.timeMax = builder.timeMax;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    /**
     * @return Restriction type
     * @see ng.prk.prkngandroid.Const.ParkingRestrType
     */
    public int getType() {
        return type;
    }

    public int getTimeMax() {
        return timeMax;
    }

    /**
     * Compares type and timeMax
     *
     * @param another The interval to compare to
     * @return true if has same type and timeMax value
     */
    public boolean hasSameType(RestrInterval another) {
        return (this.type == another.getType()) && (this.timeMax == another.getTimeMax());
    }

    /**
     * Check if restriction applies all day (24 hours)
     *
     * @return true for all-day restriction
     */
    public boolean isAllDay() {
        return endMillis - startMillis >= DateUtils.DAY_IN_MILLIS;
    }

    /**
     * Check if restriction rule is stronger.
     * Priority order is ALL_TIMES then TIME_MAX_PAID.
     * For PAID, TIME_MAX a merge is needed.
     *
     * @param another The interval to examine
     * @return true if rule is stronger
     */
    public boolean overrules(RestrInterval another) {
        // TODO incomplete, adding other types can improve performance
        if (type == ALL_TIMES) {
            return true;
        } else if (type == TIME_MAX_PAID) {
            if (another.getType() == PAID) {
                return true;
            } else if (another.getType() == TIME_MAX) {
                if (getTimeMax() <= another.getTimeMax()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Subtract an interval from the current.
     * When the other interval contains the current, result is empty.
     * When the other interval is contained, result is 2 intervals with a gap.
     *
     * @param another the Interval to examine
     * @return List of intervals, can have a gap
     */
    public RestrIntervalsList subtract(RestrInterval another) {
        final RestrIntervalsList intervalsList = new RestrIntervalsList();

        if (another.contains(this)) {
            // The subtracted interval is bigger than current, return empty result.
            return intervalsList;
        } else if (contains(another)) {
            // Split current into 2 parts, surrounding the other interval
            if (Float.compare(this.startMillis, another.getStartMillis()) < 0) {
                // The first (leading) part
                intervalsList.add(new Builder(dayOfWeek)
                                .startMillis(this.startMillis)
                                .endMillis(another.getStartMillis())
                                .type(this.type)
                                .timeMax(this.timeMax)
                                .build()
                );
            }

            if (Float.compare(another.getEndMillis(), this.endMillis) < 0) {
                // The last (trailing) part
                intervalsList.add(new Builder(dayOfWeek)
                                .startMillis(another.getEndMillis())
                                .endMillis(this.endMillis)
                                .type(this.type)
                                .timeMax(this.timeMax)
                                .build()
                );
            }
        } else if (startsBefore(another)) {
            // Keep the first (leading) part only
            if (Float.compare(this.startMillis, another.getStartMillis()) < 0) {
                intervalsList.add(new Builder(dayOfWeek)
                        .startMillis(this.startMillis)
                        .endMillis(another.getStartMillis())
                        .type(this.type)
                        .timeMax(this.timeMax)
                        .build()
                );
            }
        } else if (startsAfter(another)) {
            // Keep the last (trailing) part only
            if (Float.compare(another.getEndMillis(), this.endMillis) < 0) {
                intervalsList.add(new Builder(dayOfWeek)
                                .startMillis(another.getEndMillis())
                                .endMillis(this.endMillis)
                                .type(this.type)
                                .timeMax(this.timeMax)
                                .build()
                );
            }
        }

        return intervalsList;
    }

    @Override
    public String toString() {
        return "RestrInterval{" +
                "type=" + type +
                " hourStart=" + TimeUnit.MILLISECONDS.toHours(startMillis) +
                ", hourEnd=" + TimeUnit.MILLISECONDS.toHours(endMillis) +
                '}';
    }

    public static class Builder {
        private int dayOfWeek;
        private int type;
        private int timeMax;
        private long startMillis;
        private long endMillis;

        public Builder(int dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
            this.type = NONE;
            this.timeMax = Const.UNKNOWN_VALUE;
            this.startMillis = Const.UNKNOWN_VALUE;
            this.endMillis = Const.UNKNOWN_VALUE;
        }

        public Builder type(int type) {
            this.type = type;
            return this;
        }

        public Builder timeMax(int timeMax) {
            this.timeMax = timeMax;
            return this;
        }

        public Builder startMillis(long millis) {
            this.startMillis = millis;
            return this;
        }

        public Builder endMillis(long millis) {
            this.endMillis = millis;
            return this;
        }

        public Builder startHour(float hour) {
            this.startMillis = (long) (hour * DateUtils.HOUR_IN_MILLIS);
            return this;
        }

        public Builder endHour(float hour) {
            this.endMillis = (long) (hour * DateUtils.HOUR_IN_MILLIS);
            return this;
        }

        public Builder interval(Interval interval) {
            this.startMillis = interval.getStartMillis();
            this.endMillis = interval.getEndMillis();
            return this;
        }

        public RestrInterval build() {
            return new RestrInterval(this);
        }
    }
}