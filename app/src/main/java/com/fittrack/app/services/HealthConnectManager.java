package com.fittrack.app.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.aggregate.AggregationResult;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.request.AggregateRequest;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.time.TimeRangeFilter;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.ResultKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;

public class HealthConnectManager {

    private final Context context;
    private final HealthConnectClient healthConnectClient;
    private final ExecutorService executor;

    // ---------------------------------------------------------
    // CONSTRUCTOR
    // ---------------------------------------------------------

    public HealthConnectManager(Context context) {

        this.context =
                context.getApplicationContext();

        healthConnectClient =
                HealthConnectClient.getOrCreate(
                        this.context
                );

        executor =
                Executors.newSingleThreadExecutor();
    }

    // ---------------------------------------------------------
    // CHECK HEALTH CONNECT AVAILABILITY
    // ---------------------------------------------------------

    public boolean isAvailable() {

        int status =
                HealthConnectClient.getSdkStatus(
                        context
                );

        return status ==
                HealthConnectClient.SDK_AVAILABLE;
    }

    // ---------------------------------------------------------
    // REQUIRED PERMISSIONS
    //
    // STEPS      -> HEALTH CONNECT
    // HEART RATE -> HEALTH CONNECT
    //
    // CALORIES   -> FIRESTORE
    //
    // IMPORTANT:
    // READ_ACTIVE_CALORIES_BURNED IS NOT INCLUDED
    // ---------------------------------------------------------

    public Set<String> getRequiredPermissions() {

        if (!isAvailable()) {

            return Collections.emptySet();
        }

        return Set.of(
                "android.permission.health.READ_STEPS",
                "android.permission.health.READ_HEART_RATE"
        );
    }

    // ---------------------------------------------------------
    // CHECK GRANTED PERMISSIONS
    // ---------------------------------------------------------

    public void checkPermissions(
            @NonNull PermissionCallback callback
    ) {

        if (!isAvailable()) {

            callback.onResult(false);

            return;
        }

        executor.execute(() -> {

            try {

                healthConnectClient
                        .getPermissionController()
                        .getGrantedPermissions(
                                new PermissionContinuation(
                                        callback,
                                        getRequiredPermissions()
                                )
                        );

            } catch (Exception e) {

                callback.onError(e);
            }
        });
    }

    public void getGrantedPermissions(
            @NonNull GrantedPermissionsSetCallback callback
    ) {

        if (!isAvailable()) {

            callback.onResult(Collections.emptySet());

            return;
        }

        executor.execute(() -> {

            try {

                healthConnectClient
                        .getPermissionController()
                        .getGrantedPermissions(
                                new GrantedSetContinuation(
                                        callback
                                )
                        );

            } catch (Exception e) {

                callback.onError(e);
            }
        });
    }

    // ---------------------------------------------------------
    // GET TODAY'S STEPS
    // ---------------------------------------------------------

    public void getTodaySteps(
            @NonNull StepsCallback callback
    ) {

        if (!isAvailable()) {

            callback.onError(
                    new IllegalStateException(
                            "Health Connect is not available."
                    )
            );

            return;
        }

        executor.execute(() -> {

            try {

                ZonedDateTime now =
                        ZonedDateTime.now(
                                ZoneId.systemDefault()
                        );

                ZonedDateTime startOfDay =
                        now.toLocalDate()
                                .atStartOfDay(
                                        ZoneId.systemDefault()
                                );

                Instant start =
                        startOfDay.toInstant();

                Instant end =
                        now.toInstant();

                AggregateRequest request =
                        new AggregateRequest(
                                Collections.singleton(
                                        StepsRecord.COUNT_TOTAL
                                ),
                                TimeRangeFilter.between(
                                        start,
                                        end
                                ),
                                Collections.emptySet()
                        );

                healthConnectClient.aggregate(
                        request,
                        new StepsContinuation(
                                callback
                        )
                );

            } catch (Exception e) {

                callback.onError(e);
            }
        });
    }

    // ---------------------------------------------------------
    // GET TODAY'S HEART RATE
    // ---------------------------------------------------------

    public void getTodayHeartRate(
            @NonNull HeartRateCallback callback
    ) {

        if (!isAvailable()) {

            callback.onError(
                    new IllegalStateException(
                            "Health Connect is not available."
                    )
            );

            return;
        }

        executor.execute(() -> {

            try {

                ZonedDateTime now =
                        ZonedDateTime.now(
                                ZoneId.systemDefault()
                        );

                ZonedDateTime startOfDay =
                        now.toLocalDate()
                                .atStartOfDay(
                                        ZoneId.systemDefault()
                                );

                Instant start =
                        startOfDay.toInstant();

                Instant end =
                        now.toInstant();

                // -------------------------------------------------
                // Convert Java Class to Kotlin KClass
                // -------------------------------------------------

                KClass<HeartRateRecord>
                        heartRateKClass =
                        JvmClassMappingKt.getKotlinClass(
                                HeartRateRecord.class
                        );

                // -------------------------------------------------
                // Heart Rate Read Request
                // -------------------------------------------------

                ReadRecordsRequest<HeartRateRecord>
                        request =
                        new ReadRecordsRequest<HeartRateRecord>(
                                heartRateKClass,
                                TimeRangeFilter.between(
                                        start,
                                        end
                                ),
                                Collections.emptySet(),
                                false,
                                1000,
                                null
                        );

                healthConnectClient.readRecords(
                        request,
                        new HeartRateContinuation(
                                callback
                        )
                );

            } catch (Exception e) {

                callback.onError(e);
            }
        });
    }

    // =========================================================
    // PERMISSION CONTINUATION
    // =========================================================

    private static class PermissionContinuation
            implements Continuation<Set<String>> {

        private final PermissionCallback callback;
        private final Set<String> requiredPermissions;

        PermissionContinuation(
                PermissionCallback callback,
                Set<String> requiredPermissions
        ) {

            this.callback =
                    callback;

            this.requiredPermissions =
                    requiredPermissions;
        }

        @NonNull
        @Override
        public CoroutineContext getContext() {

            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void resumeWith(
                Object result
        ) {

            try {

                ResultKt.throwOnFailure(
                        result
                );

                Set<String> grantedPermissions =
                        (Set<String>) result;

                boolean granted =
                        grantedPermissions != null
                                && grantedPermissions
                                .containsAll(
                                        requiredPermissions
                                );

                callback.onResult(
                        granted
                );

            } catch (Throwable error) {

                if (error instanceof Exception) {

                    callback.onError(
                            (Exception) error
                    );

                } else {

                    callback.onError(
                            new Exception(error)
                    );
                }
            }
        }
    }

    // =========================================================
    // GRANTED SET CONTINUATION
    // =========================================================

    private static class GrantedSetContinuation
            implements Continuation<Set<String>> {

        private final GrantedPermissionsSetCallback callback;

        GrantedSetContinuation(
                GrantedPermissionsSetCallback callback
        ) {

            this.callback = callback;
        }

        @NonNull
        @Override
        public CoroutineContext getContext() {

            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void resumeWith(
                Object result
        ) {

            try {

                ResultKt.throwOnFailure(
                        result
                );

                Set<String> grantedPermissions =
                        (Set<String>) result;

                if (grantedPermissions == null) {
                    grantedPermissions = Collections.emptySet();
                }

                callback.onResult(
                        grantedPermissions
                );

            } catch (Throwable error) {

                if (error instanceof Exception) {

                    callback.onError(
                            (Exception) error
                    );

                } else {

                    callback.onError(
                            new Exception(error)
                    );
                }
            }
        }
    }

    // =========================================================
    // STEPS CONTINUATION
    // =========================================================

    private static class StepsContinuation
            implements Continuation<AggregationResult> {

        private final StepsCallback callback;

        StepsContinuation(
                StepsCallback callback
        ) {

            this.callback =
                    callback;
        }

        @NonNull
        @Override
        public CoroutineContext getContext() {

            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(
                Object result
        ) {

            try {

                ResultKt.throwOnFailure(
                        result
                );

                AggregationResult aggregationResult =
                        (AggregationResult) result;

                Long steps =
                        aggregationResult.get(
                                StepsRecord.COUNT_TOTAL
                        );

                if (steps == null) {

                    steps = 0L;
                }

                callback.onStepsLoaded(
                        steps
                );

            } catch (Throwable error) {

                if (error instanceof Exception) {

                    callback.onError(
                            (Exception) error
                    );

                } else {

                    callback.onError(
                            new Exception(error)
                    );
                }
            }
        }
    }

    // =========================================================
    // HEART RATE CONTINUATION
    // =========================================================

    private static class HeartRateContinuation
            implements Continuation<Object> {

        private final HeartRateCallback callback;

        HeartRateContinuation(
                HeartRateCallback callback
        ) {

            this.callback =
                    callback;
        }

        @NonNull
        @Override
        public CoroutineContext getContext() {

            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void resumeWith(
                Object result
        ) {

            try {

                ResultKt.throwOnFailure(
                        result
                );

                if (result == null) {

                    callback.onHeartRateLoaded(
                            0
                    );

                    return;
                }

                // -------------------------------------------------
                // Get records using reflection
                // -------------------------------------------------

                Method getRecordsMethod =
                        result.getClass()
                                .getMethod(
                                        "getRecords"
                                );

                List<HeartRateRecord> records =
                        (List<HeartRateRecord>)
                                getRecordsMethod.invoke(
                                        result
                                );

                if (records == null
                        || records.isEmpty()) {

                    callback.onHeartRateLoaded(
                            0
                    );

                    return;
                }

                long latestBpm =
                        0;

                Instant latestTime =
                        null;

                // -------------------------------------------------
                // Find latest heart-rate sample
                // -------------------------------------------------

                for (
                        HeartRateRecord record
                        : records
                ) {

                    if (record == null) {
                        continue;
                    }

                    List<HeartRateRecord.Sample>
                            samples =
                            record.getSamples();

                    if (samples == null) {
                        continue;
                    }

                    for (
                            HeartRateRecord.Sample sample
                            : samples
                    ) {

                        if (sample == null) {
                            continue;
                        }

                        Instant sampleTime =
                                sample.getTime();

                        long bpm =
                                sample.getBeatsPerMinute();

                        if (
                                latestTime == null
                                        || sampleTime.isAfter(
                                        latestTime
                                )
                        ) {

                            latestTime =
                                    sampleTime;

                            latestBpm =
                                    bpm;
                        }
                    }
                }

                callback.onHeartRateLoaded(
                        latestBpm
                );

            } catch (Throwable error) {

                if (error instanceof Exception) {

                    callback.onError(
                            (Exception) error
                    );

                } else {

                    callback.onError(
                            new Exception(error)
                    );
                }
            }
        }
    }

    // =========================================================
    // CALLBACKS
    // =========================================================

    public interface PermissionCallback {

        void onResult(
                boolean granted
        );

        void onError(
                Exception error
        );
    }

    public interface GrantedPermissionsSetCallback {

        void onResult(
                Set<String> grantedPermissions
        );

        void onError(
                Exception error
        );
    }

    public interface StepsCallback {

        void onStepsLoaded(
                long steps
        );

        void onError(
                Exception error
        );
    }

    public interface HeartRateCallback {

        void onHeartRateLoaded(
                long bpm
        );

        void onError(
                Exception error
        );
    }
}